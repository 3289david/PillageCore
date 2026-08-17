package com.mingyu.pillage.boss;

import com.mingyu.pillage.instance.InstanceManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public final class BossListener implements Listener {

    private final BossManager manager;
    private final InstanceManager instanceManager;

    public BossListener(BossManager manager, InstanceManager instanceManager) {
        this.manager = manager;
        this.instanceManager = instanceManager;
    }

    // pillage_boss is a standalone world outside the instance system, so dying to real boss
    // damage there would otherwise leave the player stuck respawning inside that flat arena
    // world forever - send them back to the main server instead. Must run at HIGHEST, after
    // InstanceContextListener's HIGH-priority "respawn must stay in the death world" safety net,
    // or that listener would immediately overwrite this back to the boss world's spawn.
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        if (manager.world() == null || !event.getPlayer().getWorld().equals(manager.world())) return;
        event.setRespawnLocation(instanceManager.mainSpawn());
    }

    // The boss's HP is a custom pool tracked entirely outside vanilla health (see BossManager) -
    // every hit it takes is cancelled here and, if it came from a player, forwarded as a manual
    // pool deduction instead. Registered against the base EntityDamageEvent (which
    // EntityDamageByEntityEvent shares a handler list with) so this is the one and only place
    // that runs for any damage source, avoiding a double-handling race against cancellation.
    @EventHandler(ignoreCancelled = true)
    public void onBossDamage(EntityDamageEvent event) {
        LivingEntity boss = manager.isAlive() ? manager.currentBossEntity() : null;
        if (boss == null || !event.getEntity().equals(boss)) return;
        event.setCancelled(true);
        if (event instanceof EntityDamageByEntityEvent byEntity && byEntity.getDamager() instanceof Player player) {
            manager.onBossDamaged(player, event.getFinalDamage());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (manager.world() != null && event.getBlock().getWorld().equals(manager.world())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (manager.world() != null && event.getBlock().getWorld().equals(manager.world())) {
            event.setCancelled(true);
        }
    }
}
