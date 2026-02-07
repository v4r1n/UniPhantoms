package xyz.srnyx.uniphantoms;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.annoyingapi.AnnoyingPlugin;
import xyz.srnyx.annoyingapi.PluginPlatform;
import xyz.srnyx.annoyingapi.data.StringData;
import xyz.srnyx.annoyingapi.scheduler.TaskWrapper;

import xyz.srnyx.uniphantoms.config.ConfigVersion;
import xyz.srnyx.uniphantoms.message.MiniMessageSender;
import xyz.srnyx.uniphantoms.utility.ErrorReporter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class UniPhantoms extends AnnoyingPlugin {
    @NotNull public static final String KEY = "pp_no-phantoms";

    public ConfigYml config;
    @NotNull private final Map<String, TaskWrapper> tasks = new HashMap<>();
    @NotNull private final Map<UUID, Boolean> phantomCache = new ConcurrentHashMap<>();

    // Shared systems
    @Nullable private BukkitAudiences audiences;
    @Nullable private ErrorReporter errorReporter;
    @Nullable private MiniMessageSender messageSender;

    public UniPhantoms() {
        options
                .pluginOptions(pluginOptions -> pluginOptions.updatePlatforms(
                        PluginPlatform.modrinth("lzjYdd5h"),
                        PluginPlatform.hangar(this),
                        PluginPlatform.spigot("106381")))
                .dataOptions(dataOptions -> dataOptions
                        .enabled(true)
                        .entityDataColumns(KEY))
                .registrationOptions
                .automaticRegistration(automaticRegistration -> automaticRegistration.packages(
                        "xyz.srnyx.uniphantoms.commands",
                        "xyz.srnyx.uniphantoms.listeners"))
                .papiExpansionToRegister(() -> new PersonalPlaceholders(this));
    }

    @Override
    public void enable() {
        // Initialize shared BukkitAudiences (single instance for the plugin lifecycle)
        audiences = BukkitAudiences.create(this);

        // Initialize error reporter first
        errorReporter = new ErrorReporter(getLogger(), getDataFolder(), true);

        // Initialize message sender with shared audiences
        messageSender = new MiniMessageSender(this, audiences);

        // Check and migrate config if needed
        final ConfigVersion configVersion = new ConfigVersion(this);
        if (configVersion.needsMigration(getConfig())) {
            if (!configVersion.migrate(getConfig())) {
                errorReporter.warn("Config Migration", "Failed to migrate config, using default values");
            }
            reloadConfig(); // Reload after migration
        }

        reload();
    }

    @Override
    public void disable() {
        // Cancel all tasks
        tasks.values().forEach(TaskWrapper::cancel);
        tasks.clear();
        phantomCache.clear();

        // Close shared BukkitAudiences to prevent listener leaks
        if (audiences != null) {
            audiences.close();
            audiences = null;
        }
    }

    @Override
    public void reload() {
        config = new ConfigYml(this);

        // Update error reporter settings
        errorReporter = new ErrorReporter(getLogger(), getDataFolder(), config.errorReporting.saveToFile);

        // Recreate message sender to reload messages.yml
        if (audiences != null) {
            messageSender = new MiniMessageSender(this, audiences);
        }

        // Start tasks
        final Long delay = config.statisticTask.delay;
        final long period = config.statisticTask.period;
        for (final World world : Bukkit.getWorlds()) {
            final String name = world.getName();
            if (!isWhitelistedWorld(world)) continue;

            // Cancel previous task
            final TaskWrapper previousTask = tasks.get(name);
            if (previousTask != null) previousTask.cancel();

            // Get time & isNight
            final long time = world.getTime();
            final boolean isNight = time >= 12000;

            // Run immediately if nighttime
            if (isNight) resetAllStatistics(world);

            // Get delay
            Long worldDelay = delay;
            if (worldDelay == null) worldDelay = isNight ? 36000 - time : 12000 - time;

            // Start periodic task
            tasks.put(name, scheduler.runGlobalTaskTimer(() -> resetAllStatistics(world), worldDelay, period));
        }
    }

    /**
     * Check if phantoms are enabled for a player (StringData version - for legacy support)
     */
    public boolean hasPhantomsEnabled(@NotNull StringData data) {
        return data.getOptional(KEY)
                .map(value -> !value.equals("true"))
                .orElse(config.def);
    }

    /**
     * Check if phantoms are enabled for a player (with cache for online players)
     */
    public boolean hasPhantomsEnabled(@NotNull OfflinePlayer player) {
        final Boolean cached = phantomCache.get(player.getUniqueId());
        if (cached != null) return cached;
        final boolean result = hasPhantomsEnabled(new StringData(this, player));
        if (player.isOnline()) phantomCache.put(player.getUniqueId(), result);
        return result;
    }

    /**
     * Set phantom status for a player (updates cache)
     */
    public void setPhantomsEnabled(@NotNull OfflinePlayer player, boolean enabled) {
        new StringData(this, player).set(KEY, enabled ? null : "true");
        phantomCache.put(player.getUniqueId(), enabled);

        if (config.debugMode && errorReporter != null) {
            errorReporter.info("Data", "Updated phantoms for " + player.getName() + ": " + enabled);
        }
    }

    public void cachePhantomStatus(@NotNull UUID uuid, boolean enabled) {
        phantomCache.put(uuid, enabled);
    }

    public void uncachePhantomStatus(@NotNull UUID uuid) {
        phantomCache.remove(uuid);
    }

    @Nullable
    public BukkitAudiences getAudiences() {
        return audiences;
    }

    @Nullable
    public ErrorReporter getErrorReporter() {
        return errorReporter;
    }

    @Nullable
    public MiniMessageSender getMessageSender() {
        return messageSender;
    }

    private void resetAllStatistics(@NotNull World world) {
        for (final Player player : world.getPlayers()) if (!hasPhantomsEnabled(player)) resetStatistic(player);
    }

    public boolean isWhitelistedWorld(@NotNull World world) {
        return config.worldsBlacklist.list == null || config.worldsBlacklist.list.contains(world.getName()) == config.worldsBlacklist.treatAsWhitelist;
    }

    public static void resetStatistic(@NotNull Player player) {
        player.setStatistic(Statistic.TIME_SINCE_REST, 0);
    }
}
