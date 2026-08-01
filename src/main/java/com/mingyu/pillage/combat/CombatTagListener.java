package com.mingyu.pillage.combat;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class CombatTagListener implements Listener {

    private final CombatTagManager combatTagManager;

    public CombatTagListener(CombatTagManager combatTagManager) {
        this.combatTagManager = combatTagManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        // Only tags plain person-vs-person hits (no projectiles/pets), matching "둘이 싸울 때만".
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        if (attacker.equals(victim)) return;

        combatTagManager.tag(attacker.getUniqueId());
        combatTagManager.tag(victim.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        combatTagManager.clear(event.getPlayer().getUniqueId());
    }
}
