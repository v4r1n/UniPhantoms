package xyz.srnyx.personalphantoms.message;

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

    public MiniMessageSender(@NotNull AnnoyingPlugin plugin) {
        this.plugin = plugin;
        this.audiences = BukkitAudiences.create(plugin);
        this.miniMessage = MiniMessage.miniMessage();
        this.legacySerializer = LegacyComponentSerializer.legacyAmpersand();

        // Load messages.yml from plugin data folder
        final File messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        // If file doesn't exist, save default from resources
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        // Load the YAML file
        this.messages = YamlConfiguration.loadConfiguration(messagesFile);

        // Load defaults from JAR if available
        final InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            final FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultStream, StandardCharsets.UTF_8)
            );
            messages.setDefaults(defaultConfig);
        }
    }

    /**
     * Send a message from messages.yml to a CommandSender
     *
     * @param sender the recipient
     * @param key the message key
     * @param replacements placeholder replacements
     */
    public void send(@NotNull CommandSender sender, @NotNull String key, @NotNull Map<String, String> replacements) {
        final String message = getMessage(key, replacements);
        if (message == null || message.isEmpty()) return;

        // For console, convert to plain text with Legacy colors instead of MiniMessage
        if (sender instanceof ConsoleCommandSender) {
            // Parse MiniMessage to Component first
            final Component component = parseMessage(message);
            // Convert Component to Legacy format (with § color codes) for console
            final String legacyText = legacySerializer.serialize(component);
            // Send as plain text to console
            sender.sendMessage(legacyText);
        } else {
            // For players, use Adventure API with MiniMessage
            final Component component = parseMessage(message);
            audiences.sender(sender).sendMessage(component);
        }
    }

    /**
     * Send a message from messages.yml to a Player
     *
     * @param player the recipient
     * @param key the message key
     * @param replacements placeholder replacements
     */
    public void send(@NotNull Player player, @NotNull String key, @NotNull Map<String, String> replacements) {
        final String message = getMessage(key, replacements);
        if (message == null || message.isEmpty()) return;

        // Parse and send the message
        final Component component = parseMessage(message);
        audiences.player(player).sendMessage(component);
    }

    /**
     * Send a simple message without replacements
     *
     * @param sender the recipient
     * @param key the message key
     */
    public void send(@NotNull CommandSender sender, @NotNull String key) {
        send(sender, key, new HashMap<>());
    }

    /**
     * Get raw message from messages.yml with replacements
     *
     * @param key the message key
     * @param replacements placeholder replacements
     * @return the processed message
     */
    @NotNull
    private String getMessage(@NotNull String key, @NotNull Map<String, String> replacements) {
        // Get message from cached resource
        String message = messages.getString(key);
        if (message == null) return "";

        // Replace global placeholders first
        message = replaceGlobalPlaceholders(message);

        // Replace custom placeholders
        for (final Map.Entry<String, String> entry : replacements.entrySet()) {
            final String placeholder = entry.getKey().startsWith("%") ? entry.getKey() : "%" + entry.getKey() + "%";
            message = message.replace(placeholder, entry.getValue());
        }

        return message;
    }

    /**
     * Replace global placeholders from plugin.global-placeholders
     *
     * @param message the message to process
     * @return processed message
     */
    @NotNull
    private String replaceGlobalPlaceholders(@NotNull String message) {
        // Get all global placeholders from cached resource
        final Map<String, String> globals = new HashMap<>();

        if (messages.getConfigurationSection("plugin.global-placeholders") != null) {
            for (final String key : messages.getConfigurationSection("plugin.global-placeholders").getKeys(false)) {
                final String value = messages.getString("plugin.global-placeholders." + key);
                if (value != null) {
                    globals.put("%" + key + "%", value);
                }
            }
        }

        // Replace all global placeholders
        for (final Map.Entry<String, String> entry : globals.entrySet()) {
            message = message.replace(entry.getKey(), entry.getValue());
        }

        return message;
    }

    /**
     * Parse message with both Legacy and MiniMessage support
     * Converts Legacy (&) to proper format, then parses MiniMessage
     *
     * @param message the message to parse
     * @return parsed Component
     */
    @NotNull
    private Component parseMessage(@NotNull String message) {
        // First, convert legacy color codes (&) to MiniMessage format
        // But preserve existing MiniMessage tags
        String processed = convertLegacyToMiniMessage(message);

        // Parse with MiniMessage
        return miniMessage.deserialize(processed);
    }

    /**
     * Convert legacy color codes to MiniMessage format
     * Preserves existing MiniMessage tags
     *
     * @param message the message with legacy codes
     * @return message with MiniMessage format
     */
    @NotNull
    private String convertLegacyToMiniMessage(@NotNull String message) {
        // Map of legacy codes to MiniMessage tags
        final Map<String, String> colorMap = new HashMap<>();
        colorMap.put("&0", "<black>");
        colorMap.put("&1", "<dark_blue>");
        colorMap.put("&2", "<dark_green>");
        colorMap.put("&3", "<dark_aqua>");
        colorMap.put("&4", "<dark_red>");
        colorMap.put("&5", "<dark_purple>");
        colorMap.put("&6", "<gold>");
        colorMap.put("&7", "<gray>");
        colorMap.put("&8", "<dark_gray>");
        colorMap.put("&9", "<blue>");
        colorMap.put("&a", "<green>");
        colorMap.put("&b", "<aqua>");
        colorMap.put("&c", "<red>");
        colorMap.put("&d", "<light_purple>");
        colorMap.put("&e", "<yellow>");
        colorMap.put("&f", "<white>");
        colorMap.put("&k", "<obfuscated>");
        colorMap.put("&l", "<bold>");
        colorMap.put("&m", "<strikethrough>");
        colorMap.put("&n", "<underlined>");
        colorMap.put("&o", "<italic>");
        colorMap.put("&r", "<reset>");

        String result = message;

        // Replace all legacy codes with MiniMessage equivalents
        for (final Map.Entry<String, String> entry : colorMap.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }

        return result;
    }

    /**
     * Builder class for easy message construction
     */
    public static class Builder {
        @NotNull private final MiniMessageSender sender;
        @NotNull private final String key;
        @NotNull private final Map<String, String> replacements;

        public Builder(@NotNull MiniMessageSender sender, @NotNull String key) {
            this.sender = sender;
            this.key = key;
            this.replacements = new HashMap<>();
        }

        /**
         * Add a placeholder replacement
         *
         * @param placeholder the placeholder (with or without %)
         * @param value the replacement value
         * @return this builder
         */
        @NotNull
        public Builder replace(@NotNull String placeholder, @NotNull String value) {
            replacements.put(placeholder, value);
            return this;
        }

        /**
         * Add a placeholder replacement (object version)
         *
         * @param placeholder the placeholder
         * @param value the replacement value
         * @return this builder
         */
        @NotNull
        public Builder replace(@NotNull String placeholder, @NotNull Object value) {
            return replace(placeholder, String.valueOf(value));
        }

        /**
         * Send to CommandSender
         *
         * @param recipient the recipient
         */
        public void send(@NotNull CommandSender recipient) {
            sender.send(recipient, key, replacements);
        }

        /**
         * Send to Player
         *
         * @param recipient the recipient
         */
        public void send(@NotNull Player recipient) {
            sender.send(recipient, key, replacements);
        }
    }

    /**
     * Create a builder for this message
     *
     * @param key the message key
     * @return new Builder
     */
    @NotNull
    public Builder builder(@NotNull String key) {
        return new Builder(this, key);
    }
}
