package com.mingyu.pillage.team;

import com.mingyu.pillage.data.dao.TeamDao;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class TeamManager {

    private final TeamDao teamDao;
    private final int defaultMaxMembers;
    private final int maxMembersHardCap;
    private final boolean friendlyFireDefault;

    private final Map<Integer, Team> teamsById = new HashMap<>();
    private final Map<UUID, Integer> teamIdByMember = new HashMap<>();
    private final Map<String, Integer> teamIdByNameLower = new HashMap<>();
    private final Map<UUID, TeamInvite> pendingInvites = new HashMap<>();
    private final Map<UUID, Boolean> teamChatToggle = new HashMap<>();

    public TeamManager(TeamDao teamDao, int defaultMaxMembers, int maxMembersHardCap,
                        boolean friendlyFireDefault) {
        this.teamDao = teamDao;
        this.defaultMaxMembers = defaultMaxMembers;
        this.maxMembersHardCap = maxMembersHardCap;
        this.friendlyFireDefault = friendlyFireDefault;
    }

    public void loadAll() {
        for (Team team : teamDao.loadAll()) {
            index(team);
        }
    }

    private void index(Team team) {
        teamsById.put(team.id(), team);
        teamIdByNameLower.put(team.name().toLowerCase(), team.id());
        for (UUID member : team.members().keySet()) {
            teamIdByMember.put(member, team.id());
        }
    }

    public enum CreateResult { OK, NAME_TAKEN, ALREADY_IN_TEAM }

    public CreateResult createTeam(String name, Player leader) {
        if (teamIdByMember.containsKey(leader.getUniqueId())) {
            return CreateResult.ALREADY_IN_TEAM;
        }
        if (teamIdByNameLower.containsKey(name.toLowerCase())) {
            return CreateResult.NAME_TAKEN;
        }
        Team team = teamDao.createTeam(name, leader.getUniqueId(), defaultMaxMembers);
        team.setFriendlyFire(friendlyFireDefault);
        teamDao.updateFriendlyFire(team.id(), friendlyFireDefault);
        index(team);
        return CreateResult.OK;
    }

    public enum InviteResult { OK, TARGET_ALREADY_IN_TEAM, NOT_LEADER, TEAM_FULL, ALREADY_INVITED }

    public InviteResult invite(Team team, Player inviter, Player target) {
        if (!team.isLeader(inviter.getUniqueId())) {
            return InviteResult.NOT_LEADER;
        }
        if (teamIdByMember.containsKey(target.getUniqueId())) {
            return InviteResult.TARGET_ALREADY_IN_TEAM;
        }
        if (team.size() >= team.maxMembers()) {
            return InviteResult.TEAM_FULL;
        }
        TeamInvite existing = pendingInvites.get(target.getUniqueId());
        if (existing != null && existing.teamId() == team.id() && !existing.isExpired()) {
            return InviteResult.ALREADY_INVITED;
        }
        pendingInvites.put(target.getUniqueId(), new TeamInvite(team.id(), inviter.getUniqueId(),
                System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(2)));
        return InviteResult.OK;
    }

    public void declineInvite(UUID target) {
        pendingInvites.remove(target);
    }

    public Optional<TeamInvite> pendingInvite(UUID target) {
        TeamInvite invite = pendingInvites.get(target);
        if (invite == null || invite.isExpired()) {
            pendingInvites.remove(target);
            return Optional.empty();
        }
        return Optional.of(invite);
    }

    public enum JoinResult { OK, NO_INVITE, TEAM_FULL, ALREADY_IN_TEAM }

    public JoinResult join(Player player) {
        if (teamIdByMember.containsKey(player.getUniqueId())) {
            return JoinResult.ALREADY_IN_TEAM;
        }
        Optional<TeamInvite> inviteOpt = pendingInvite(player.getUniqueId());
        if (inviteOpt.isEmpty()) {
            return JoinResult.NO_INVITE;
        }
        Team team = teamsById.get(inviteOpt.get().teamId());
        if (team == null) {
            return JoinResult.NO_INVITE;
        }
        if (team.size() >= team.maxMembers()) {
            return JoinResult.TEAM_FULL;
        }
        team.members().put(player.getUniqueId(), TeamRole.MEMBER);
        teamIdByMember.put(player.getUniqueId(), team.id());
        teamDao.addMember(team.id(), player.getUniqueId(), TeamRole.MEMBER);
        pendingInvites.remove(player.getUniqueId());
        return JoinResult.OK;
    }

    public enum LeaveResult { OK, NOT_IN_TEAM, LEADER_MUST_DISBAND }

    public LeaveResult leave(Player player) {
        Team team = getTeam(player.getUniqueId());
        if (team == null) {
            return LeaveResult.NOT_IN_TEAM;
        }
        if (team.isLeader(player.getUniqueId())) {
            return LeaveResult.LEADER_MUST_DISBAND;
        }
        team.members().remove(player.getUniqueId());
        teamIdByMember.remove(player.getUniqueId());
        teamDao.removeMember(player.getUniqueId());
        return LeaveResult.OK;
    }

    public enum KickResult { OK, NOT_LEADER, TARGET_NOT_IN_TEAM, CANNOT_KICK_SELF }

    public KickResult kick(Team team, Player leader, UUID target) {
        if (!team.isLeader(leader.getUniqueId())) {
            return KickResult.NOT_LEADER;
        }
        if (target.equals(leader.getUniqueId())) {
            return KickResult.CANNOT_KICK_SELF;
        }
        if (!team.isMember(target)) {
            return KickResult.TARGET_NOT_IN_TEAM;
        }
        team.members().remove(target);
        teamIdByMember.remove(target);
        teamDao.removeMember(target);
        return KickResult.OK;
    }

    public boolean disband(Team team, Player leader) {
        if (!team.isLeader(leader.getUniqueId())) {
            return false;
        }
        for (UUID member : new ArrayList<>(team.members().keySet())) {
            teamIdByMember.remove(member);
        }
        teamsById.remove(team.id());
        teamIdByNameLower.remove(team.name().toLowerCase());
        teamDao.deleteTeam(team.id());
        return true;
    }

    public Team getTeam(UUID uuid) {
        Integer id = teamIdByMember.get(uuid);
        return id == null ? null : teamsById.get(id);
    }

    public Team getTeamByName(String name) {
        Integer id = teamIdByNameLower.get(name.toLowerCase());
        return id == null ? null : teamsById.get(id);
    }

    public boolean isSameTeam(UUID a, UUID b) {
        Integer teamA = teamIdByMember.get(a);
        Integer teamB = teamIdByMember.get(b);
        return teamA != null && teamA.equals(teamB);
    }

    public int maxMembersHardCap() {
        return maxMembersHardCap;
    }

    public void setFriendlyFire(Team team, boolean value) {
        team.setFriendlyFire(value);
        teamDao.updateFriendlyFire(team.id(), value);
    }

    public void setMaxMembers(Team team, int value) {
        team.setMaxMembers(value);
        teamDao.updateMaxMembers(team.id(), value);
    }

    public void setHome(Team team, org.bukkit.Location location) {
        team.setHome(location);
        teamDao.updateHome(team.id(), location);
    }

    public void addKill(Team team) {
        team.addKill();
        teamDao.addKill(team.id());
    }

    public void addLootScore(Team team, int amount) {
        team.addLootScore(amount);
        teamDao.addLootScore(team.id(), amount);
    }

    public void addRaidWon(Team team) {
        team.addRaidWon();
        teamDao.addRaidWon(team.id());
    }

    public void addRaidDefended(Team team) {
        team.addRaidDefended();
        teamDao.addRaidDefended(team.id());
    }

    public boolean isTeamChatEnabled(UUID uuid) {
        return teamChatToggle.getOrDefault(uuid, false);
    }

    public boolean toggleTeamChat(UUID uuid) {
        boolean next = !isTeamChatEnabled(uuid);
        teamChatToggle.put(uuid, next);
        return next;
    }

    public List<Team> topByKills(int limit) {
        return teamsById.values().stream()
                .sorted(Comparator.comparingInt(Team::kills).reversed())
                .limit(limit)
                .toList();
    }

    public List<Team> topByLootScore(int limit) {
        return teamsById.values().stream()
                .sorted(Comparator.comparingInt(Team::lootScore).reversed())
                .limit(limit)
                .toList();
    }

    public List<Team> allTeams() {
        return new ArrayList<>(teamsById.values());
    }
}
