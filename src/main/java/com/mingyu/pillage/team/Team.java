package com.mingyu.pillage.team;

import org.bukkit.Location;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class Team {

    private final int id;
    private String name;
    private UUID leader;
    private boolean friendlyFire;
    private int maxMembers;
    private Location home;
    private int kills;
    private int lootScore;
    private int raidsWon;
    private int raidsDefended;
    private long protectedUntil;
    private final long createdAt;

    private final Map<UUID, TeamRole> members = new LinkedHashMap<>();

    public Team(int id, String name, UUID leader, int maxMembers, long createdAt) {
        this.id = id;
        this.name = name;
        this.leader = leader;
        this.maxMembers = maxMembers;
        this.createdAt = createdAt;
        this.members.put(leader, TeamRole.LEADER);
    }

    public int id() {
        return id;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID leader() {
        return leader;
    }

    public void setLeader(UUID leader) {
        this.leader = leader;
    }

    public boolean friendlyFire() {
        return friendlyFire;
    }

    public void setFriendlyFire(boolean friendlyFire) {
        this.friendlyFire = friendlyFire;
    }

    public int maxMembers() {
        return maxMembers;
    }

    public void setMaxMembers(int maxMembers) {
        this.maxMembers = maxMembers;
    }

    public Location home() {
        return home;
    }

    public void setHome(Location home) {
        this.home = home;
    }

    public int kills() {
        return kills;
    }

    public void addKill() {
        this.kills++;
    }

    public int lootScore() {
        return lootScore;
    }

    public void addLootScore(int amount) {
        this.lootScore += amount;
    }

    public int raidsWon() {
        return raidsWon;
    }

    public void addRaidWon() {
        this.raidsWon++;
    }

    public int raidsDefended() {
        return raidsDefended;
    }

    public void addRaidDefended() {
        this.raidsDefended++;
    }

    public long protectedUntil() {
        return protectedUntil;
    }

    public void setProtectedUntil(long protectedUntil) {
        this.protectedUntil = protectedUntil;
    }

    public boolean isProtected() {
        return System.currentTimeMillis() < protectedUntil;
    }

    public long createdAt() {
        return createdAt;
    }

    public Map<UUID, TeamRole> members() {
        return members;
    }

    public boolean isMember(UUID uuid) {
        return members.containsKey(uuid);
    }

    public boolean isLeader(UUID uuid) {
        return leader.equals(uuid);
    }

    public int size() {
        return members.size();
    }
}
