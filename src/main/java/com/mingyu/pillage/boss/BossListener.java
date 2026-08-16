package com.mingyu.pillage.boss;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public final class BossListener implements Listener {

    private final BossManager manager;

    public BossListener(BossManager manager) {
        this.manager = manager;
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
