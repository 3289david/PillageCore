package com.mingyu.pillage.combat;

import com.mingyu.pillage.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class CombatFleeListener implements Listener {

    private final CombatTagManager combatTagManager;

    public CombatFleeListener(CombatTagManager combatTagManager) {
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

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        var cause = event.getCause();
        // CHORUS_FRUIT is deprecated in favor of CONSUMABLE_EFFECT on newer servers - check both so
        // this keeps working regardless of which one the running Paper build actually fires.
        if (cause != PlayerTeleportEvent.TeleportCause.ENDER_PEARL
                && cause != PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT
                && cause != PlayerTeleportEvent.TeleportCause.CONSUMABLE_EFFECT) {
            return;
        }
        Player player = event.getPlayer();
        if (combatTagManager.isInCombat(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(Msg.of("&c전투 중에는 도주할 수 없습니다. (" + combatTagManager.remainingSeconds(player.getUniqueId()) + "초 남음)"));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        combatTagManager.clear(event.getPlayer().getUniqueId());
    }
}
