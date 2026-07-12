package com.mingyu.pillage.raid;

import com.mingyu.pillage.team.Team;
import com.mingyu.pillage.team.TeamManager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class RaidListener implements Listener {

    private final RaidManager raidManager;
    private final TeamManager teamManager;

    public RaidListener(RaidManager raidManager, TeamManager teamManager) {
        this.raidManager = raidManager;
        this.teamManager = teamManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRaidTrigger(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = resolveAttacker(event);
        if (attacker == null || attacker.equals(victim)) return;

        Team victimTeam = teamManager.getTeam(victim.getUniqueId());
        if (victimTeam == null) return;
        if (teamManager.isSameTeam(attacker.getUniqueId(), victim.getUniqueId())) return;

        raidManager.startOrExtendRaid(victimTeam, attacker);
    }

    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            return player;
        }
        if (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }
}
