package xyz.srnyx.personalphantoms.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.annoyingapi.command.AnnoyingCommand;
import xyz.srnyx.annoyingapi.command.AnnoyingSender;
import xyz.srnyx.annoyingapi.command.selector.Selector;
import xyz.srnyx.annoyingapi.command.selector.SelectorOptional;
import xyz.srnyx.annoyingapi.cooldown.AnnoyingCooldown;

import xyz.srnyx.personalphantoms.PersonalPhantoms;
import xyz.srnyx.personalphantoms.utility.TimeFormatter;
import xyz.srnyx.personalphantoms.utility.TimeFormatter.TimeFormat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


public class NoPhantomsCmd extends AnnoyingCommand {
    @NotNull private final PersonalPhantoms plugin;
    @NotNull private final PermissionNode permissions;

    public NoPhantomsCmd(@NotNull PersonalPhantoms plugin) {
        this.plugin = plugin;

        // Setup fine-grained permissions
        this.permissions = PermissionNode.create("pp.nophantoms")
                .registerArgument("reload", "pp.reload")
                .registerArgument("get", "pp.nophantoms.get")
                .registerArguments("pp.nophantoms.toggle", "toggle", "enable", "disable");
    }

    @Override @NotNull
    public PersonalPhantoms getAnnoyingPlugin() {
        return plugin;
    }

    @Override
    public String getPermission() {
        return "pp.nophantoms";
    }

    public void onCommand(@NotNull AnnoyingSender sender) {
        final CommandSender cmdSender = sender.cmdSender;
        final int length = sender.args.length;

        // reload
        if (sender.argEquals(0, "reload")) {
            if (!permissions.hasPermission(cmdSender, "reload")) {
                plugin.getMessageSender().send(cmdSender, "error.no-permission");
                return;
            }
            plugin.reloadPlugin();
            plugin.getMessageSender().send(cmdSender, "reload");
            return;
        }

        // This command handles both "/nophantoms" AND "/phantoms" (for convenience)
        // Both commands work the same way (enable = เปิด phantom, disable = ปิด phantom)

        // Check for silent mode (-s flag)
        boolean silent = false;
        int effectiveLength = length;
        if (length > 0 && sender.argEquals(length - 1, "-s")) {
            silent = true;
            effectiveLength = length - 1;
        }

        if (effectiveLength == 1) {
            // get
            if (sender.argEquals(0, "get")) {
                if (!permissions.hasPermission(cmdSender, "get")) {
                    plugin.getMessageSender().send(cmdSender, "error.no-permission");
                    return;
                }
                if (sender.checkPlayer()) {
                    final boolean phantomsEnabled = plugin.hasPhantomsEnabled(sender.getPlayer());
                    plugin.getMessageSender().send(cmdSender, phantomsEnabled ? "get.self-enabled" : "get.self-disabled");
                }
                return;
            }

            // <toggle|enable|disable>
            if (sender.argEquals(0, "toggle", "enable", "disable")) {
                final String action = sender.args[0].toLowerCase();
                if (!permissions.hasPermission(cmdSender, action)) {
                    plugin.getMessageSender().send(cmdSender, "error.no-permission");
                    return;
                }

                // Console must specify player
                if (!sender.checkPlayer()) {
                    plugin.getMessageSender().send(cmdSender, "error.console-specify-player");
                    return;
                }
                final Player player = sender.getPlayer();

                // Check if on cooldown
                if (!cmdSender.hasPermission("pp.nophantoms.bypass")) {
                    final AnnoyingCooldown cooldown = plugin.cooldownManager.getCooldownElseNew("NoPhantomsCmd", player.getUniqueId().toString());
                    final long duration = getPermissionValue(player, "pp.nophantoms.cooldown.")
                            .map(value -> value.longValue() * 1000) // Convert to milliseconds
                            .orElse(plugin.config.commandCooldown);
                    if (cooldown.isOnCooldownStart(duration)) {
                        plugin.getMessageSender().builder("nophantoms.cooldown")
                                .replace("cooldown", TimeFormatter.format(cooldown.getRemaining(), TimeFormat.SHORT))
                                .send(cmdSender);
                        return;
                    }
                }

                // Determine new status based on action
                // Both /phantoms and /nophantoms work the same way:
                // enable = เปิดใช้งาน phantom (true)
                // disable = ปิดใช้งาน phantom (false)
                final Boolean enablePhantoms;
                if (sender.argEquals(0, "toggle")) {
                    enablePhantoms = null; // toggle
                } else if (sender.argEquals(0, "enable")) {
                    enablePhantoms = true; // enable phantoms
                } else { // disable
                    enablePhantoms = false; // disable phantoms
                }

                // Edit
                final boolean newStatus = editKey(player, enablePhantoms);
                if (!silent) {
                    plugin.getMessageSender().send(cmdSender, newStatus ? "nophantoms.self-enabled" : "nophantoms.self-disabled");
                }
                return;
            }

            plugin.getMessageSender().send(cmdSender, "error.invalid-arguments");
            return;
        }

        // Check args and permission
        if (effectiveLength != 2) {
            plugin.getMessageSender().send(cmdSender, "error.invalid-arguments");
            return;
        }
        if (!sender.checkPermission("pp.nophantoms.others")) return;

        // Block selectors in console
        if (cmdSender instanceof ConsoleCommandSender && sender.args[1].startsWith("@")) {
            plugin.getMessageSender().send(cmdSender, "error.console-no-selectors");
            return;
        }

        // Get targets - bypass selector validation for player names to avoid invalid-argument error from AnnoyingAPI
        final List<OfflinePlayer> targets;
        final String targetArg = sender.args[1];

        if (targetArg.startsWith("@")) {
            // Selector - use AnnoyingAPI's selector resolution
            final SelectorOptional<Player> selectorResult = sender.getSelector(1, Player.class);

            // Use orElse to get the list or handle empty/null case
            final List<Player> onlinePlayers = selectorResult.orElse((arg) -> {
                // If selector returns nothing, send error message and return null
                plugin.getMessageSender().send(cmdSender, "error.invalid-selector");
                return null;
            });

            // If selector failed or returned null
            if (onlinePlayers == null) return;

            // If selector is valid but no players matched
            if (onlinePlayers.isEmpty()) {
                plugin.getMessageSender().send(cmdSender, "error.no-players-found");
                return;
            }

            targets = new ArrayList<>(onlinePlayers);
        } else {
            // Player name - create list directly (bypass selector validation to avoid invalid-argument error)
            targets = new ArrayList<>();
            targets.add(Bukkit.getOfflinePlayer(targetArg));
        }

        // get [<player>]
        if (sender.argEquals(0, "get")) {
            if (!permissions.hasPermission(cmdSender, "get")) {
                plugin.getMessageSender().send(cmdSender, "error.no-permission");
                return;
            }
            for (final OfflinePlayer target : targets) {
                final boolean phantomsEnabled = plugin.hasPhantomsEnabled(target);
                plugin.getMessageSender().builder(phantomsEnabled ? "get.other-enabled" : "get.other-disabled")
                        .replace("target", target.getName())
                        .send(cmdSender);
            }
            return;
        }

        // <toggle|enable|disable> [<player>]
        if (sender.argEquals(0, "toggle", "enable", "disable")) {
            final String action = sender.args[0].toLowerCase();
            if (!permissions.hasPermission(cmdSender, action)) {
                plugin.getMessageSender().send(cmdSender, "error.no-permission");
                return;
            }
            for (final OfflinePlayer target : targets) {
                // Determine new status based on action
                // Both /phantoms and /nophantoms work the same way:
                // enable = เปิดใช้งาน phantom (true)
                // disable = ปิดใช้งาน phantom (false)
                final Boolean enablePhantoms;
                if (sender.argEquals(0, "toggle")) {
                    enablePhantoms = null; // toggle
                } else if (sender.argEquals(0, "enable")) {
                    enablePhantoms = true; // enable phantoms
                } else { // disable
                    enablePhantoms = false; // disable phantoms
                }

                final boolean newStatus = editKey(target, enablePhantoms);
                if (!silent) {
                    plugin.getMessageSender().builder(newStatus ? "nophantoms.toggler-enabled" : "nophantoms.toggler-disabled")
                            .replace("target", target.getName())
                            .send(cmdSender);
                    final Player targetOnline = target.getPlayer();
                    if (targetOnline != null) {
                        plugin.getMessageSender().builder(newStatus ? "nophantoms.other-enabled" : "nophantoms.other-disabled")
                                .replace("toggler", cmdSender.getName())
                                .send(targetOnline);
                    }
                }
            }
            return;
        }

        plugin.getMessageSender().send(cmdSender, "error.invalid-arguments");
    }

    @Override @Nullable
    public Collection<String> onTabComplete(@NotNull AnnoyingSender sender) {
        final CommandSender cmdSender = sender.cmdSender;
        final int length = sender.args.length;

        // <reload|get|toggle|enable|disable>
        if (length == 1) {
            final List<String> list = new ArrayList<>();
            if (permissions.hasPermission(cmdSender, "reload")) list.add("reload");
            if (permissions.hasPermission(cmdSender, "get")) list.add("get");
            if (permissions.hasPermission(cmdSender, "toggle")) list.add("toggle");
            if (permissions.hasPermission(cmdSender, "enable")) list.add("enable");
            if (permissions.hasPermission(cmdSender, "disable")) list.add("disable");
            return list;
        }

        // <get|toggle|enable|disable> [<player>]
        if (length == 2 && !sender.argEquals(0, "reload") && cmdSender.hasPermission("pp.nophantoms.others")) {
            return Selector.addKeys(getOnlinePlayerNames(), OfflinePlayer.class);
        }

        // <get|toggle|enable|disable> [<player>] -s
        if (length == 3 && sender.argEquals(0, "get", "toggle", "enable", "disable") && cmdSender.hasPermission("pp.nophantoms.others")) {
            final List<String> list = new ArrayList<>();
            list.add("-s");
            return list;
        }

        // <toggle|enable|disable> -s (for self)
        if (length == 2 && sender.argEquals(0, "toggle", "enable", "disable") && cmdSender.hasPermission("pp.nophantoms")) {
            final List<String> list = new ArrayList<>();
            list.add("-s");
            return list;
        }

        return null;
    }

    /**
     * Get permission value from player's permissions
     * Replacement for BukkitUtility.getPermissionValue()
     *
     * @param   player  the player to check permissions for
     * @param   prefix  the permission prefix to search for (e.g., "pp.nophantoms.cooldown.")
     *
     * @return          the numeric value from the permission, or empty if not found
     */
    @NotNull
    private Optional<Double> getPermissionValue(@NotNull Player player, @NotNull String prefix) {
        for (final PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            final String permission = info.getPermission();
            if (!info.getValue() || !permission.startsWith(prefix)) continue;

            try {
                return Optional.of(Double.parseDouble(permission.substring(prefix.length())));
            } catch (final NumberFormatException ignored) {
                // Continue searching if parsing fails
            }
        }
        return Optional.empty();
    }

    /**
     * Get all online player names
     * Replacement for BukkitUtility.getOnlinePlayerNames()
     *
     * @return  list of online player names
     */
    @NotNull
    private List<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    /**
     * Edit the key status for a player
     *
     * @param   offline         the player to edit the key status for
     *
     * @param   enablePhantoms  whether to enable or disable phantoms for the player
     *
     * @return                  the new status of the key (true if phantoms enabled, false if disabled)
     */
    private boolean editKey(@NotNull OfflinePlayer offline, @Nullable Boolean enablePhantoms) {
        // Determine new status
        if (enablePhantoms == null) enablePhantoms = !plugin.hasPhantomsEnabled(offline); // toggle

        // Update using new database-aware method
        plugin.setPhantomsEnabled(offline, enablePhantoms);

        // Update statistic for online players
        final Player online = offline.getPlayer();
        if (online != null && plugin.isWhitelistedWorld(online.getWorld())) {
            if (enablePhantoms) {
                // Set statistic to 1 hour (so phantoms will attack)
                online.setStatistic(Statistic.TIME_SINCE_REST, 72000);
            } else {
                // Reset statistic (so phantoms won't attack)
                PersonalPhantoms.resetStatistic(online);
            }
        }

        return enablePhantoms;
    }
}
