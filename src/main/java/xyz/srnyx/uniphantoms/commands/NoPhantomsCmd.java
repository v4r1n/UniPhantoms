package xyz.srnyx.uniphantoms.commands;

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

import xyz.srnyx.uniphantoms.UniPhantoms;
import xyz.srnyx.uniphantoms.message.MiniMessageSender;
import xyz.srnyx.uniphantoms.utility.TimeFormatter;
import xyz.srnyx.uniphantoms.utility.TimeFormatter.TimeFormat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


public class NoPhantomsCmd extends AnnoyingCommand {
    @NotNull private final UniPhantoms plugin;
    @NotNull private final PermissionNode permissions;

    public NoPhantomsCmd(@NotNull UniPhantoms plugin) {
        this.plugin = plugin;

        this.permissions = PermissionNode.create("pp.nophantoms")
                .registerArgument("reload", "pp.reload")
                .registerArgument("get", "pp.nophantoms.get")
                .registerArguments("pp.nophantoms.toggle", "toggle", "enable", "disable");
    }

    @Override @NotNull
    public UniPhantoms getAnnoyingPlugin() {
        return plugin;
    }

    @Override
    public String getPermission() {
        return "pp.nophantoms";
    }

    private void sendMessage(@NotNull CommandSender sender, @NotNull String key) {
        final MiniMessageSender ms = plugin.getMessageSender();
        if (ms != null) ms.send(sender, key);
    }

    @Nullable
    private MiniMessageSender.Builder messageBuilder(@NotNull String key) {
        final MiniMessageSender ms = plugin.getMessageSender();
        return ms != null ? ms.builder(key) : null;
    }

    @Nullable
    private Boolean determinePhantomAction(@NotNull AnnoyingSender sender) {
        if (sender.argEquals(0, "toggle")) return null;
        if (sender.argEquals(0, "enable")) return true;
        return false;
    }

    public void onCommand(@NotNull AnnoyingSender sender) {
        final CommandSender cmdSender = sender.cmdSender;
        final int length = sender.args.length;

        // reload
        if (sender.argEquals(0, "reload")) {
            if (!permissions.hasPermission(cmdSender, "reload")) {
                sendMessage(cmdSender, "error.no-permission");
                return;
            }
            plugin.reloadPlugin();
            sendMessage(cmdSender, "reload");
            return;
        }

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
                    sendMessage(cmdSender, "error.no-permission");
                    return;
                }
                if (cmdSender instanceof Player) {
                    final boolean phantomsEnabled = plugin.hasPhantomsEnabled(sender.getPlayer());
                    sendMessage(cmdSender, phantomsEnabled ? "get.self-enabled" : "get.self-disabled");
                }
                return;
            }

            // <toggle|enable|disable>
            if (sender.argEquals(0, "toggle", "enable", "disable")) {
                final String action = sender.args[0].toLowerCase();
                if (!permissions.hasPermission(cmdSender, action)) {
                    sendMessage(cmdSender, "error.no-permission");
                    return;
                }

                if (!(cmdSender instanceof Player)) {
                    sendMessage(cmdSender, "error.console-specify-player");
                    return;
                }
                final Player player = sender.getPlayer();

                // Check if on cooldown
                if (!cmdSender.hasPermission("pp.nophantoms.bypass")) {
                    final AnnoyingCooldown cooldown = plugin.cooldownManager.getCooldownElseNew("NoPhantomsCmd", player.getUniqueId().toString());
                    final long duration = getPermissionValue(player, "pp.nophantoms.cooldown.")
                            .map(value -> value.longValue() * 1000)
                            .orElse(plugin.config.commandCooldown);
                    if (cooldown.isOnCooldownStart(duration)) {
                        final MiniMessageSender.Builder builder = messageBuilder("nophantoms.cooldown");
                        if (builder != null) {
                            builder.replace("cooldown", TimeFormatter.format(cooldown.getRemaining(), TimeFormat.SHORT))
                                    .send(cmdSender);
                        }
                        return;
                    }
                }

                final Boolean enablePhantoms = determinePhantomAction(sender);
                final boolean newStatus = editKey(player, enablePhantoms);
                if (!silent) {
                    sendMessage(cmdSender, newStatus ? "nophantoms.self-enabled" : "nophantoms.self-disabled");
                }
                return;
            }

            sendMessage(cmdSender, "error.invalid-arguments");
            return;
        }

        // Check args and permission
        if (effectiveLength != 2) {
            sendMessage(cmdSender, "error.invalid-arguments");
            return;
        }
        if (!cmdSender.hasPermission("pp.nophantoms.others")) {
            sendMessage(cmdSender, "error.no-permission");
            return;
        }

        // Block selectors in console
        if (cmdSender instanceof ConsoleCommandSender && sender.args[1].startsWith("@")) {
            sendMessage(cmdSender, "error.console-no-selectors");
            return;
        }

        // Get targets
        final List<OfflinePlayer> targets;
        final String targetArg = sender.args[1];

        if (targetArg.startsWith("@")) {
            final SelectorOptional<Player> selectorResult = sender.getSelector(1, Player.class);
            final List<Player> onlinePlayers = selectorResult.orElse((arg) -> {
                sendMessage(cmdSender, "error.invalid-selector");
                return null;
            });
            if (onlinePlayers == null) return;
            if (onlinePlayers.isEmpty()) {
                sendMessage(cmdSender, "error.no-players-found");
                return;
            }
            targets = new ArrayList<>(onlinePlayers);
        } else {
            final OfflinePlayer target = Bukkit.getOfflinePlayer(targetArg);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                sendMessage(cmdSender, "error.player-not-found");
                return;
            }
            targets = new ArrayList<>();
            targets.add(target);
        }

        // get [<player>]
        if (sender.argEquals(0, "get")) {
            if (!permissions.hasPermission(cmdSender, "get")) {
                sendMessage(cmdSender, "error.no-permission");
                return;
            }
            for (final OfflinePlayer target : targets) {
                final String targetName = target.getName();
                if (targetName == null) {
                    sendMessage(cmdSender, "error.player-not-found");
                    continue;
                }
                final boolean phantomsEnabled = plugin.hasPhantomsEnabled(target);
                final MiniMessageSender.Builder builder = messageBuilder(phantomsEnabled ? "get.other-enabled" : "get.other-disabled");
                if (builder != null) {
                    builder.replace("target", targetName).send(cmdSender);
                }
            }
            return;
        }

        // <toggle|enable|disable> [<player>]
        if (sender.argEquals(0, "toggle", "enable", "disable")) {
            final String action = sender.args[0].toLowerCase();
            if (!permissions.hasPermission(cmdSender, action)) {
                sendMessage(cmdSender, "error.no-permission");
                return;
            }
            final Boolean enablePhantoms = determinePhantomAction(sender);
            for (final OfflinePlayer target : targets) {
                final String targetName = target.getName();
                if (targetName == null) {
                    sendMessage(cmdSender, "error.player-not-found");
                    continue;
                }

                final boolean newStatus = editKey(target, enablePhantoms);
                if (!silent) {
                    final MiniMessageSender.Builder togglerBuilder = messageBuilder(newStatus ? "nophantoms.toggler-enabled" : "nophantoms.toggler-disabled");
                    if (togglerBuilder != null) {
                        togglerBuilder.replace("target", targetName).send(cmdSender);
                    }
                    final Player targetOnline = target.getPlayer();
                    if (targetOnline != null) {
                        final MiniMessageSender.Builder otherBuilder = messageBuilder(newStatus ? "nophantoms.other-enabled" : "nophantoms.other-disabled");
                        if (otherBuilder != null) {
                            otherBuilder.replace("toggler", cmdSender.getName()).send(targetOnline);
                        }
                    }
                }
            }
            return;
        }

        sendMessage(cmdSender, "error.invalid-arguments");
    }

    @Override @Nullable
    public Collection<String> onTabComplete(@NotNull AnnoyingSender sender) {
        final CommandSender cmdSender = sender.cmdSender;
        final int length = sender.args.length;

        if (length == 1) {
            final List<String> list = new ArrayList<>();
            if (permissions.hasPermission(cmdSender, "reload")) list.add("reload");
            if (permissions.hasPermission(cmdSender, "get")) list.add("get");
            if (permissions.hasPermission(cmdSender, "toggle")) list.add("toggle");
            if (permissions.hasPermission(cmdSender, "enable")) list.add("enable");
            if (permissions.hasPermission(cmdSender, "disable")) list.add("disable");
            return list;
        }

        // <get|toggle|enable|disable> [<player>|-s]
        if (length == 2 && !sender.argEquals(0, "reload")) {
            final List<String> list = new ArrayList<>();
            if (sender.argEquals(0, "toggle", "enable", "disable") && cmdSender.hasPermission("pp.nophantoms")) {
                list.add("-s");
            }
            if (cmdSender.hasPermission("pp.nophantoms.others")) {
                list.addAll(Selector.addKeys(getOnlinePlayerNames(), OfflinePlayer.class));
            }
            return list;
        }

        // <get|toggle|enable|disable> <player> -s
        if (length == 3 && sender.argEquals(0, "get", "toggle", "enable", "disable") && cmdSender.hasPermission("pp.nophantoms.others")) {
            final List<String> list = new ArrayList<>();
            list.add("-s");
            return list;
        }

        return null;
    }

    @NotNull
    private Optional<Double> getPermissionValue(@NotNull Player player, @NotNull String prefix) {
        for (final PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            final String permission = info.getPermission();
            if (!info.getValue() || !permission.startsWith(prefix)) continue;

            try {
                return Optional.of(Double.parseDouble(permission.substring(prefix.length())));
            } catch (final NumberFormatException ignored) {
            }
        }
        return Optional.empty();
    }

    @NotNull
    private List<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    private boolean editKey(@NotNull OfflinePlayer offline, @Nullable Boolean enablePhantoms) {
        if (enablePhantoms == null) enablePhantoms = !plugin.hasPhantomsEnabled(offline);

        plugin.setPhantomsEnabled(offline, enablePhantoms);

        final Player online = offline.getPlayer();
        if (online != null && plugin.isWhitelistedWorld(online.getWorld())) {
            if (enablePhantoms) {
                online.setStatistic(Statistic.TIME_SINCE_REST, 72000);
            } else {
                UniPhantoms.resetStatistic(online);
            }
        }

        return enablePhantoms;
    }
}
