package xyz.srnyx.uniphantoms;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.annoyingapi.AnnoyingPAPIExpansion;


public class PersonalPlaceholders extends AnnoyingPAPIExpansion {
    @NotNull private final UniPhantoms plugin;

    public PersonalPlaceholders(@NotNull UniPhantoms plugin) {
        this.plugin = plugin;
    }

    @Override @NotNull
    public UniPhantoms getAnnoyingPlugin() {
        return plugin;
    }

    @Override @NotNull
    public String getIdentifier() {
        return "phantoms";
    }

    @Override @Nullable
    public String onPlaceholderRequest(@Nullable Player player, @NotNull String identifier) {
        // %phantoms_enabled% - Returns "true" if phantoms are enabled for the player
        if (player != null && identifier.equals("enabled")) {
            return String.valueOf(plugin.hasPhantomsEnabled(player));
        }

        // %phantoms_disabled% - Returns "true" if phantoms are disabled for the player
        if (player != null && identifier.equals("disabled")) {
            return String.valueOf(!plugin.hasPhantomsEnabled(player));
        }

        // %phantoms_status% - Returns "enabled" or "disabled"
        if (player != null && identifier.equals("status")) {
            return plugin.hasPhantomsEnabled(player) ? "enabled" : "disabled";
        }

        // %phantoms_status_word% - Returns "enabled" or "disabled" as words (alias for status)
        if (player != null && identifier.equals("status_word")) {
            return plugin.hasPhantomsEnabled(player) ? "enabled" : "disabled";
        }

        // %phantoms_status_<player>% - Check another player's status
        if (identifier.startsWith("status_")) {
            final Player target = Bukkit.getPlayer(identifier.substring(7));
            return target == null ? "N/A" : (plugin.hasPhantomsEnabled(target) ? "enabled" : "disabled");
        }

        // %phantoms_enabled_<player>% - Check if specific player has phantoms enabled
        if (identifier.startsWith("enabled_")) {
            final Player target = Bukkit.getPlayer(identifier.substring(8));
            return target == null ? "false" : String.valueOf(plugin.hasPhantomsEnabled(target));
        }

        // %phantoms_disabled_<player>% - Check if specific player has phantoms disabled
        if (identifier.startsWith("disabled_")) {
            final Player target = Bukkit.getPlayer(identifier.substring(9));
            return target == null ? "false" : String.valueOf(!plugin.hasPhantomsEnabled(target));
        }

        // %phantoms_world_enabled% - Check if phantoms are enabled in current world
        if (player != null && identifier.equals("world_enabled")) {
            return String.valueOf(plugin.isWhitelistedWorld(player.getWorld()));
        }

        // %phantoms_total_enabled% - Count players with phantoms enabled
        if (identifier.equals("total_enabled")) {
            long count = Bukkit.getOnlinePlayers().stream()
                    .filter(plugin::hasPhantomsEnabled)
                    .count();
            return String.valueOf(count);
        }

        // %phantoms_total_disabled% - Count players with phantoms disabled
        if (identifier.equals("total_disabled")) {
            long count = Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !plugin.hasPhantomsEnabled(p))
                    .count();
            return String.valueOf(count);
        }

        // %phantoms_percentage_enabled% - Percentage of online players with phantoms enabled
        if (identifier.equals("percentage_enabled")) {
            final long total = Bukkit.getOnlinePlayers().size();
            if (total == 0) return "0";

            final long enabled = Bukkit.getOnlinePlayers().stream()
                    .filter(plugin::hasPhantomsEnabled)
                    .count();

            return String.valueOf((enabled * 100) / total);
        }

        // %phantoms_percentage_disabled% - Percentage of online players with phantoms disabled
        if (identifier.equals("percentage_disabled")) {
            final long total = Bukkit.getOnlinePlayers().size();
            if (total == 0) return "0";

            final long disabled = Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !plugin.hasPhantomsEnabled(p))
                    .count();

            return String.valueOf((disabled * 100) / total);
        }

        return null;
    }
}
