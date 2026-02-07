package xyz.srnyx.personalphantoms.message;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;


/**
 * Advanced message builder with MiniMessage support, hover/click events
 */
public class MessageBuilder {
    @NotNull private final Plugin plugin;
    @NotNull private final BukkitAudiences audiences;
    @NotNull private final MiniMessage miniMessage;
    @NotNull private final Map<String, String> placeholders;
    @NotNull private String message;

    public MessageBuilder(@NotNull Plugin plugin, @NotNull String message) {
        this.plugin = plugin;
        this.audiences = BukkitAudiences.create(plugin);
        this.miniMessage = MiniMessage.miniMessage();
        this.placeholders = new HashMap<>();
        this.message = message;
    }

    /**
     * Add a placeholder replacement
     *
     * @param placeholder the placeholder (with or without %)
     * @param value the value to replace with
     * @return this builder
     */
    @NotNull
    public MessageBuilder replace(@NotNull String placeholder, @NotNull String value) {
        final String key = placeholder.startsWith("%") && placeholder.endsWith("%")
                ? placeholder
                : "%" + placeholder + "%";
        placeholders.put(key, value);
        return this;
    }

    /**
     * Add a placeholder with object value (toString)
     *
     * @param placeholder the placeholder
     * @param value the value
     * @return this builder
     */
    @NotNull
    public MessageBuilder replace(@NotNull String placeholder, @NotNull Object value) {
        return replace(placeholder, String.valueOf(value));
    }

    /**
     * Add hover text to the message
     *
     * @param hoverText the text to show on hover
     * @return this builder
     */
    @NotNull
    public MessageBuilder hover(@NotNull String hoverText) {
        // Wrap message with hover event using MiniMessage format
        message = "<hover:show_text:'" + hoverText + "'>" + message + "</hover>";
        return this;
    }

    /**
     * Add click event to run command
     *
     * @param command the command to run (without /)
     * @return this builder
     */
    @NotNull
    public MessageBuilder clickCommand(@NotNull String command) {
        message = "<click:run_command:'/" + command + "'>" + message + "</click>";
        return this;
    }

    /**
     * Add click event to suggest command
     *
     * @param command the command to suggest
     * @return this builder
     */
    @NotNull
    public MessageBuilder clickSuggest(@NotNull String command) {
        message = "<click:suggest_command:'/" + command + "'>" + message + "</click>";
        return this;
    }

    /**
     * Add click event to open URL
     *
     * @param url the URL to open
     * @return this builder
     */
    @NotNull
    public MessageBuilder clickUrl(@NotNull String url) {
        message = "<click:open_url:'" + url + "'>" + message + "</click>";
        return this;
    }

    /**
     * Build the component
     *
     * @return the final Component
     */
    @NotNull
    public Component build() {
        String processed = message;

        // Replace all placeholders
        for (final Map.Entry<String, String> entry : placeholders.entrySet()) {
            processed = processed.replace(entry.getKey(), entry.getValue());
        }

        // Parse with MiniMessage
        return miniMessage.deserialize(processed);
    }

    /**
     * Send the message to a CommandSender
     *
     * @param sender the recipient
     */
    public void send(@NotNull CommandSender sender) {
        audiences.sender(sender).sendMessage(build());
    }

    /**
     * Send the message to a Player
     *
     * @param player the recipient
     */
    public void send(@NotNull Player player) {
        audiences.player(player).sendMessage(build());
    }

    /**
     * Broadcast the message to all players
     */
    public void broadcast() {
        audiences.all().sendMessage(build());
    }

    /**
     * Create a new MessageBuilder
     *
     * @param plugin the plugin instance
     * @param message the message template
     * @return new MessageBuilder
     */
    @NotNull
    public static MessageBuilder create(@NotNull Plugin plugin, @NotNull String message) {
        return new MessageBuilder(plugin, message);
    }

    /**
     * Quick send without builder
     *
     * @param plugin the plugin
     * @param sender the recipient
     * @param message the message
     */
    public static void sendSimple(@NotNull Plugin plugin, @NotNull CommandSender sender, @NotNull String message) {
        new MessageBuilder(plugin, message).send(sender);
    }
}
