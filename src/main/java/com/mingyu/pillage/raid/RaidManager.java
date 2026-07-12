package com.mingyu.pillage.raid;

import com.mingyu.pillage.team.Team;
import com.mingyu.pillage.team.TeamManager;
import com.mingyu.pillage.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class RaidManager {

    private final JavaPlugin plugin;
    private final TeamManager teamManager;
    private final long raidDurationMillis;
    private final String alertMessageTemplate;
    private final int winKillThreshold;

    private final Map<Integer, Long> raidUntil = new HashMap<>();
    private final Map<Integer, UUID> lastAttacker = new HashMap<>();
    private final Map<Integer, Integer> attackerTeamId = new HashMap<>();
    private final Map<Integer, Integer> raidKillCount = new HashMap<>();

    public RaidManager(JavaPlugin plugin, TeamManager teamManager, int raidDurationMinutes,
                        String alertMessageTemplate, int winKillThreshold) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.raidDurationMillis = TimeUnit.MINUTES.toMillis(raidDurationMinutes);
        this.alertMessageTemplate = alertMessageTemplate;
        this.winKillThreshold = winKillThreshold;
    }

    /** Call whenever a member of {@code victimTeam} is attacked/killed by a player from another team. */
    public void startOrExtendRaid(Team victimTeam, Player attacker) {
        boolean wasActive = isTeamInRaid(victimTeam.id());
        raidUntil.put(victimTeam.id(), System.currentTimeMillis() + raidDurationMillis);
        lastAttacker.put(victimTeam.id(), attacker.getUniqueId());

        Team attackerTeam = teamManager.getTeam(attacker.getUniqueId());
        if (attackerTeam != null) {
            attackerTeamId.put(victimTeam.id(), attackerTeam.id());
        }

        if (!wasActive) {
            broadcastAlert(victimTeam, attacker);
            raidKillCount.put(victimTeam.id(), 0);
            scheduleResolution(victimTeam.id());
        }
    }

    /** Call whenever an attacking-team player actually kills a member of the raided team. */
    public void registerRaidKill(Team victimTeam) {
        if (!isTeamInRaid(victimTeam.id())) return;
        raidKillCount.merge(victimTeam.id(), 1, Integer::sum);
    }

    private void scheduleResolution(int teamId) {
        Long until = raidUntil.get(teamId);
        if (until == null) return;
        long delayTicks = Math.max(1, (until - System.currentTimeMillis()) / 50);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Long stillUntil = raidUntil.get(teamId);
            if (stillUntil == null) return;
            if (System.currentTimeMillis() < stillUntil) {
                // raid got extended by further attacks since this task was scheduled - reschedule
                scheduleResolution(teamId);
                return;
            }
            resolveRaid(teamId);
        }, delayTicks);
    }

    private void resolveRaid(int teamId) {
        Team victimTeam = teamManager.allTeams().stream().filter(t -> t.id() == teamId).findFirst().orElse(null);
        int kills = raidKillCount.getOrDefault(teamId, 0);
        Integer atkTeamId = attackerTeamId.get(teamId);

        raidUntil.remove(teamId);
        raidKillCount.remove(teamId);
        lastAttacker.remove(teamId);
        attackerTeamId.remove(teamId);

        if (victimTeam == null) return;

        if (kills >= winKillThreshold && atkTeamId != null) {
            Team attackerTeam = teamManager.allTeams().stream().filter(t -> t.id() == atkTeamId).findFirst().orElse(null);
            if (attackerTeam != null) {
                teamManager.addRaidWon(attackerTeam);
                teamManager.addLootScore(attackerTeam, kills * 10);
                broadcastResult(victimTeam, attackerTeam.name() + " 팀의 약탈에 실패했습니다.");
                broadcastResult(attackerTeam, victimTeam.name() + " 팀 약탈에 성공했습니다! (+" + (kills * 10) + " 약탈 점수)");
            }
        } else {
            teamManager.addRaidDefended(victimTeam);
            broadcastResult(victimTeam, "약탈을 방어해냈습니다!");
        }
    }

    private void broadcastResult(Team team, String message) {
        for (UUID member : team.members().keySet()) {
            Player online = Bukkit.getPlayer(member);
            if (online != null) {
                online.sendMessage(Msg.of("&6[레이드] &f" + message));
            }
        }
    }

    private void broadcastAlert(Team team, Player attacker) {
        String message = alertMessageTemplate.replace("%attacker%", attacker.getName());
        for (UUID member : team.members().keySet()) {
            Player online = Bukkit.getPlayer(member);
            if (online != null) {
                online.sendMessage(Msg.of(message));
            }
        }
    }

    public boolean isTeamInRaid(int teamId) {
        Long until = raidUntil.get(teamId);
        return until != null && until > System.currentTimeMillis();
    }

    public long remainingSeconds(int teamId) {
        Long until = raidUntil.get(teamId);
        if (until == null) return 0;
        return Math.max(0, (until - System.currentTimeMillis()) / 1000);
    }

    public TeamManager teamManager() {
        return teamManager;
    }
}
