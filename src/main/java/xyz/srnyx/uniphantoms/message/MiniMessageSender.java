package xyz.srnyx.uniphantoms.message;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;

import xyz.srnyx.annoyingapi.AnnoyingPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;


/**
 * Custom message sender with MiniMessage support for AnnoyingAPI
 * Handles both Legacy (&) and MiniMessage (<>) formats
 */
public class MiniMessageSender {
    @NotNull private final AnnoyingPlugin plugin;
    @NotNull private final BukkitAudiences audiences;
    @NotNull private final MiniMessage miniMessage;
    @NotNull private final LegacyComponentSerializer legacySerializer;
    @NotNull private final FileConfiguration messages;
    @NotNull private final Map<String, String> globalPlaceholders;

    private static final Map<String, String> LEGACY_COLOR_MAP;
    static {
        final Map<String, String> map = new HashMap<>();
        map.put("&0", "<black>");
        map.put("&1", "<dark_blue>");
        map.put("&2", "<dark_green>");
        map.put("&3", "<dark_aqua>");
        map.put("&4", "<dark_red>");
        map.put("&5", "<dark_purple>");
        map.put("&6", "<gold>");
        map.put("&7", "<gray>");
        map.put("&8", "<dark_gray>");
        map.put("&9", "<blue>");
        map.put("&a", "<green>");
        map.put("&b", "<aqua>");
        map.put("&c", "<red>");
        map.put("&d", "<light_purple>");
        map.put("&e", "<yellow>");
        map.put("&f", "<white>");
        map.put("&k", "<obfuscated>");
        map.put("&l", "<bold>");
        map.put("&m", "<strikethrough>");
        map.put("&n", "<underlined>");
        map.put("&o", "<italic>");
        map.put("&r", "<reset>");
        LEGACY_COLOR_MAP = map;
    }

    public MiniMessageSender(@NotNull AnnoyingPlugin plugin, @NotNull BukkitAudiences audiences) {
        this.plugin = plugin;
        this.audiences = audiences;
        this.miniMessage = MiniMessage.miniMessage();
        this.legacySerializer = LegacyComponentSerializer.legacyAmpersand();

        // Load messages.yml from plugin data folder
        final File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        this.messages = YamlConfiguration.loadConfiguration(messagesFile);

        // Load defaults from JAR if available
        final InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            final FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultStream, StandardCharsets.UTF_8)
            );
            messages.setDefaults(defaultConfig);
        }

        // Cache global placeholders at construction time
        this.globalPlaceholders = new HashMap<>();
        final ConfigurationSection section = messages.getConfigurationSection("plugin.global-placeholders");
        if (section != null) {
            for (final String key : section.getKeys(false)) {
                final String value = section.getString(key);
                if (value != null) {
                    globalPlaceholders.put("%" + key + "%", value);
                }
            }
        }
    }

    public void send(@NotNull CommandSender sender, @NotNull String key, @NotNull Map<String, String> replacements) {
        final String message = getMessage(key, replacements);
        if (message.isEmpty()) return;

        final Component component = parseMessage(message);
        if (sender instanceof ConsoleCommandSender) {
            sender.sendMessage(legacySerializer.serialize(component));
        } else {
            audiences.sender(sender).sendMessage(component);
        }
    }

    public void send(@NotNull Player player, @NotNull String key, @NotNull Map<String, String> replacements) {
        final String message = getMessage(key, replacements);
        if (message.isEmpty()) return;

        audiences.player(player).sendMessage(parseMessage(message));
    }

    public void send(@NotNull CommandSender sender, @NotNull String key) {
        send(sender, key, new HashMap<>());
    }

    @NotNull
    private String getMessage(@NotNull String key, @NotNull Map<String, String> replacements) {
        String message = messages.getString(key);
        if (message == null) return "";

        message = replaceGlobalPlaceholders(message);

        for (final Map.Entry<String, String> entry : replacements.entrySet()) {
            final String placeholder = entry.getKey().startsWith("%") ? entry.getKey() : "%" + entry.getKey() + "%";
            message = message.replace(placeholder, entry.getValue());
        }

        return message;
    }

    @NotNull
    private String replaceGlobalPlaceholders(@NotNull String message) {
        String result = message;
        for (final Map.Entry<String, String> entry : globalPlaceholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    @NotNull
    private Component parseMessage(@NotNull String message) {
        return miniMessage.deserialize(convertLegacyToMiniMessage(message));
    }

    @NotNull
    private String convertLegacyToMiniMessage(@NotNull String message) {
        String result = message;
        for (final Map.Entry<String, String> entry : LEGACY_COLOR_MAP.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public static class Builder {
        @NotNull private final MiniMessageSender sender;
        @NotNull private final String key;
        @NotNull private final Map<String, String> replacements;

        public Builder(@NotNull MiniMessageSender sender, @NotNull String key) {
            this.sender = sender;
            this.key = key;
            this.replacements = new HashMap<>();
        }

        @NotNull
        public Builder replace(@NotNull String placeholder, @NotNull String value) {
            replacements.put(placeholder, value);
            return this;
        }

        @NotNull
        public Builder replace(@NotNull String placeholder, @NotNull Object value) {
            return replace(placeholder, String.valueOf(value));
        }

        public void send(@NotNull CommandSender recipient) {
            sender.send(recipient, key, replacements);
        }

        public void send(@NotNull Player recipient) {
            sender.send(recipient, key, replacements);
        }
    }

    @NotNull
    public Builder builder(@NotNull String key) {
        return new Builder(this, key);
    }
}
