package xyz.srnyx.uniphantoms.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import org.jetbrains.annotations.NotNull;

import xyz.srnyx.annoyingapi.AnnoyingListener;

import xyz.srnyx.uniphantoms.UniPhantoms;


public class MobListener extends AnnoyingListener {
    @NotNull private final UniPhantoms plugin;

    public MobListener(@NotNull UniPhantoms plugin) {
        this.plugin = plugin;
    }

    @Override @NotNull
    public UniPhantoms getAnnoyingPlugin() {
        return plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTargetLivingEntity(@NotNull EntityTargetLivingEntityEvent event) {
        if (event.getEntity().getType() != EntityType.PHANTOM) return;
        final LivingEntity target = event.getTarget();
        if (target instanceof Player && plugin.isWhitelistedWorld(target.getWorld()) && !plugin.hasPhantomsEnabled((Player) target)) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(@NotNull EntityDamageByEntityEvent event) {
        final Entity damager = event.getDamager();
        if (!plugin.isWhitelistedWorld(damager.getWorld())) return;
        final Entity target = event.getEntity();
        // Player attacking Phantom
        if (damager instanceof Player && target.getType() == EntityType.PHANTOM && !plugin.hasPhantomsEnabled((Player) damager)) {
            event.setCancelled(true);
            return;
        }
        // Phantom attacking Player
        if (damager.getType() == EntityType.PHANTOM && target instanceof Player && !plugin.hasPhantomsEnabled((Player) target)) event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final boolean enabled = plugin.hasPhantomsEnabled(player);
        plugin.cachePhantomStatus(player.getUniqueId(), enabled);
        if (plugin.isWhitelistedWorld(player.getWorld()) && !enabled) UniPhantoms.resetStatistic(player);
    }

    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        plugin.uncachePhantomStatus(event.getPlayer().getUniqueId());
    }
}
