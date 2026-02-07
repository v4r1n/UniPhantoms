package xyz.srnyx.personalphantoms;

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

import xyz.srnyx.personalphantoms.config.ConfigVersion;
import xyz.srnyx.personalphantoms.message.MiniMessageSender;
import xyz.srnyx.personalphantoms.utility.ErrorReporter;

import java.util.HashMap;
import java.util.Map;


public class PersonalPhantoms extends AnnoyingPlugin {
    @NotNull public static final String KEY = "pp_no-phantoms";

    public ConfigYml config;
    @NotNull private final Map<String, TaskWrapper> tasks = new HashMap<>();

    // New systems
    @Nullable private ErrorReporter errorReporter;
    @Nullable private MiniMessageSender messageSender;

    public PersonalPhantoms() {
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
                        "xyz.srnyx.personalphantoms.commands",
                        "xyz.srnyx.personalphantoms.listeners"))
                .papiExpansionToRegister(() -> new PersonalPlaceholders(this));
    }

    @Override
    public void enable() {
        // Initialize error reporter first
        errorReporter = new ErrorReporter(getLogger(), getDataFolder(), true);

        // Initialize message sender
        messageSender = new MiniMessageSender(this);

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
    }

    @Override
    public void reload() {
        config = new ConfigYml(this);

        // Update error reporter settings
        if (errorReporter == null) {
            errorReporter = new ErrorReporter(getLogger(), getDataFolder(), config.errorReporting.saveToFile);
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
            // Daytime: 0-12000
            // Nighttime: 12000-24000
            final long time = world.getTime();
            final boolean isNight = time >= 12000;

            // Run immediately if nighttime
            if (isNight) resetAllStatistics(world);

            // Get delay
            Long worldDelay = delay; // Configured specific delay
            if (worldDelay == null) worldDelay = isNight ? 36000 - time : 12000 - time; // Automatic calculation

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
     * Check if phantoms are enabled for a player
     * Uses StringData from AnnoyingAPI
     */
    public boolean hasPhantomsEnabled(@NotNull OfflinePlayer player) {
        return hasPhantomsEnabled(new StringData(this, player));
    }

    /**
     * Set phantom status for a player
     * Uses StringData from AnnoyingAPI
     */
    public void setPhantomsEnabled(@NotNull OfflinePlayer player, boolean enabled) {
        new StringData(this, player).set(KEY, enabled ? null : "true");

        // Log if debug mode is enabled
        if (config.debugMode) {
            errorReporter.info("Data", "Updated phantoms for " + player.getName() + ": " + enabled);
        }
    }

    /**
     * Get the error reporter instance
     */
    @Nullable
    public ErrorReporter getErrorReporter() {
        return errorReporter;
    }

    /**
     * Get the MiniMessage sender instance
     */
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
