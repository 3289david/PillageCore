package com.mingyu.pillage.pvp;

import com.mingyu.pillage.data.dao.DeathLocationDao;
import com.mingyu.pillage.data.dao.KillLogDao;
import com.mingyu.pillage.data.dao.StatsDao;
import com.mingyu.pillage.donor.DonorManager;
import com.mingyu.pillage.raid.RaidManager;
import com.mingyu.pillage.team.Team;
import com.mingyu.pillage.team.TeamManager;
import com.mingyu.pillage.util.Msg;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

public final class PvpListener implements Listener {

    private final KillLogDao killLogDao;
    private final StatsDao statsDao;
    private final DeathLocationDao deathLocationDao;
    private final TeamManager teamManager;
    private final RaidManager raidManager;
    private final KillStreakManager killStreakManager;
    private final DonorManager donorManager;

    public PvpListener(KillLogDao killLogDao, StatsDao statsDao, DeathLocationDao deathLocationDao,
                        TeamManager teamManager, RaidManager raidManager, KillStreakManager killStreakManager,
                        DonorManager donorManager) {
        this.killLogDao = killLogDao;
        this.statsDao = statsDao;
        this.deathLocationDao = deathLocationDao;
        this.teamManager = teamManager;
        this.raidManager = raidManager;
        this.killStreakManager = killStreakManager;
        this.donorManager = donorManager;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        String weapon = null;
        if (killer != null) {
            ItemStack hand = killer.getInventory().getItemInMainHand();
            if (hand.getType() != Material.AIR) {
                weapon = prettyName(hand.getType());
            }
        }

        killLogDao.log(killer == null ? null : killer.getUniqueId(), victim.getUniqueId(), weapon);

        if (killer != null) {
            Component feed = donorManager.displayName(killer)
                    .append(Msg.of(" &7⚔ &f" + (weapon == null ? "맨손" : weapon) + " &7➤ "))
                    .append(donorManager.displayName(victim));
            // Instance-local, not server-wide - a kill in one mini-server shouldn't show up in
            // the feed for players on a completely different instance.
            for (Player online : victim.getWorld().getPlayers()) {
                online.sendMessage(feed);
            }
            // Our own kill feed already announced this death - don't also show vanilla's plain-text one.
            event.deathMessage(null);
        }

        statsDao.addDeath(victim.getUniqueId());
        killStreakManager.onDeath(victim);

        if (killer != null && !killer.equals(victim)) {
            statsDao.addKill(killer.getUniqueId());
            killStreakManager.onKill(killer);

            Team killerTeam = teamManager.getTeam(killer.getUniqueId());
            Team victimTeam = teamManager.getTeam(victim.getUniqueId());
            boolean differentTeams = killerTeam == null || victimTeam == null || killerTeam.id() != victimTeam.id();
            if (differentTeams) {
                if (killerTeam != null) {
                    teamManager.addKill(killerTeam);
                }
                if (victimTeam != null) {
                    raidManager.registerRaidKill(victimTeam);
                }
            }
        }

        deathLocationDao.save(victim.getUniqueId(), victim.getLocation());
    }

    private String prettyName(Material material) {
        String[] parts = material.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }
}
