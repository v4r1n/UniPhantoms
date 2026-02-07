package xyz.srnyx.uniphantoms.commands;

import org.bukkit.command.CommandSender;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;


/**
 * Fine-grained permission system for command arguments
 * Allows different permissions for each command argument
 */
public class PermissionNode {
    @NotNull private final String basePermission;
    @NotNull private final Map<String, String> argumentPermissions;

    public PermissionNode(@NotNull String basePermission) {
        this.basePermission = basePermission;
        this.argumentPermissions = new HashMap<>();
    }

    /**
     * Register a permission for a specific argument
     *
     * @param argument the argument (e.g., "toggle", "enable")
     * @param permission the permission node (e.g., "pp.nophantoms.toggle")
     * @return this builder
     */
    @NotNull
    public PermissionNode registerArgument(@NotNull String argument, @NotNull String permission) {
        argumentPermissions.put(argument.toLowerCase(), permission);
        return this;
    }

    /**
     * Register multiple arguments with the same permission
     *
     * @param permission the permission node
     * @param arguments the arguments
     * @return this builder
     */
    @NotNull
    public PermissionNode registerArguments(@NotNull String permission, @NotNull String... arguments) {
        for (final String argument : arguments) {
            argumentPermissions.put(argument.toLowerCase(), permission);
        }
        return this;
    }

    /**
     * Check if sender has permission for a specific argument
     *
     * @param sender the command sender
     * @param argument the argument to check
     * @return true if sender has permission
     */
    public boolean hasPermission(@NotNull CommandSender sender, @NotNull String argument) {
        final String permission = argumentPermissions.get(argument.toLowerCase());

        // If no specific permission is registered, check base permission
        if (permission == null) {
            return sender.hasPermission(basePermission);
        }

        // Check specific permission
        return sender.hasPermission(permission);
    }

    /**
     * Get the permission node for a specific argument
     *
     * @param argument the argument
     * @return the permission node, or base permission if not found
     */
    @NotNull
    public String getPermission(@NotNull String argument) {
        return argumentPermissions.getOrDefault(argument.toLowerCase(), basePermission);
    }

    /**
     * Get the base permission
     *
     * @return the base permission
     */
    @NotNull
    public String getBasePermission() {
        return basePermission;
    }

    /**
     * Create a builder for permission nodes
     *
     * @param basePermission the base permission
     * @return new PermissionNode builder
     */
    @NotNull
    public static PermissionNode create(@NotNull String basePermission) {
        return new PermissionNode(basePermission);
    }
}
