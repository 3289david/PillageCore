package com.mingyu.pillage.endwar;

import com.mingyu.pillage.economy.EconomyManager;
import com.mingyu.pillage.team.Team;
import com.mingyu.pillage.team.TeamManager;
import com.mingyu.pillage.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Monthly (or admin-triggered) large-scale team battle in its own dedicated End-themed world -
 *  completely separate content from the boss mob. Everyone who has /endwar join'd when the
 *  battle starts is grouped by their real team (a player with no team counts as their own
 *  one-person group); groups fight with a provided kit until only one group still has survivors. */
public final class EndWarManager {

    private enum Phase { IDLE, ONGOING }

    private record Saved(ItemStack[] contents, ItemStack[] armor, ItemStack offhand,
                          Location location, GameMode gameMode) {
    }

    private final JavaPlugin plugin;
    private final TeamManager teamManager;
    private final EconomyManager economyManager;
    private final long winRewardPerSurvivor;

    private World world;
    private Phase phase = Phase.IDLE;
    private final Set<UUID> signups = new LinkedHashSet<>();

    private final Map<String, Set<UUID>> aliveByGroup = new HashMap<>();
    private final Map<String, String> groupDisplayName = new HashMap<>();
    private final Map<UUID, String> groupOf = new HashMap<>();
    private final Map<UUID, Saved> saved = new HashMap<>();

    public EndWarManager(JavaPlugin plugin, TeamManager teamManager, EconomyManager economyManager, long winRewardPerSurvivor) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.economyManager = economyManager;
        this.winRewardPerSurvivor = winRewardPerSurvivor;
    }

    public void start() {
        world = EndWarWorld.ensureWorld();
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickSchedule, 1200L, 1200L);
    }

    public World world() {
        return world;
    }

    public boolean isOngoing() {
        return phase == Phase.ONGOING;
    }

    public boolean isBattling(UUID uuid) {
        String groupId = groupOf.get(uuid);
        return groupId != null && aliveByGroup.getOrDefault(groupId, Set.of()).contains(uuid);
    }

    public Location spectatorSpot() {
        return EndWarWorld.SPECTATOR_SPOT.clone().toLocation(world);
    }

    public void join(Player player) {
        if (phase == Phase.ONGOING) {
            player.sendMessage(Msg.of("&c지금은 엔드대전이 진행 중입니다. 다음 회차를 기다려주세요."));
            return;
        }
        if (signups.add(player.getUniqueId())) {
            player.sendMessage(Msg.of("&a엔드대전 참가 명단에 등록되었습니다. &7시작되면 자동으로 이동합니다. (&f/endwar leave&7 로 취소)"));
        } else {
            player.sendMessage(Msg.of("&c이미 참가 신청했습니다."));
        }
    }

    public void leave(Player player) {
        if (signups.remove(player.getUniqueId())) {
            player.sendMessage(Msg.of("&7엔드대전 참가 신청을 취소했습니다."));
        } else {
            player.sendMessage(Msg.of("&c참가 신청 상태가 아닙니다."));
        }
    }

    public int signupCount() {
        return signups.size();
    }

    public String nextScheduleDescription() {
        if (!plugin.getConfig().getBoolean("endwar.enabled", false)) {
            return "관리자가 아직 일정을 설정하지 않았습니다.";
        }
        int day = plugin.getConfig().getInt("endwar.day-of-month", 1);
        int hour = plugin.getConfig().getInt("endwar.hour", 20);
        return String.format("매달 %d일 %02d시 (서버 시각 기준)", day, hour);
    }

    /** Admin /endwar start confirm - ignores the schedule and starts immediately with whoever has
     *  already joined. */
    public void forceStart(Player admin) {
        if (phase == Phase.ONGOING) {
            admin.sendMessage(Msg.of("&c이미 엔드대전이 진행 중입니다."));
            return;
        }
        if (!startBattle()) {
            admin.sendMessage(Msg.of("&c참가 인원(또는 팀 수)이 부족해 시작할 수 없습니다. 서로 다른 2개 팀/개인이 필요합니다."));
        }
    }

    public void adminCancel(Player admin) {
        if (phase != Phase.ONGOING) {
            signups.clear();
            admin.sendMessage(Msg.of("&a참가 신청 명단을 초기화했습니다."));
            return;
        }
        Bukkit.broadcast(Msg.of("&c관리자에 의해 엔드대전이 취소되었습니다."));
        restoreAll();
        resetState();
        admin.sendMessage(Msg.of("&a엔드대전을 취소하고 모두 원래 자리로 되돌렸습니다."));
    }

    private void tickSchedule() {
        if (phase != Phase.IDLE) return;
        if (!plugin.getConfig().getBoolean("endwar.enabled", false)) return;
        int day = plugin.getConfig().getInt("endwar.day-of-month", 1);
        int hour = plugin.getConfig().getInt("endwar.hour", 20);
        String lastRun = plugin.getConfig().getString("endwar.last-run-year-month", "");
        LocalDateTime now = LocalDateTime.now();
        String currentYearMonth = YearMonth.from(now).format(DateTimeFormatter.ofPattern("yyyy-MM"));
        if (currentYearMonth.equals(lastRun)) return;
        int scheduledDay = Math.min(day, now.toLocalDate().lengthOfMonth());
        if (now.getDayOfMonth() != scheduledDay || now.getHour() != hour) return;

        plugin.getConfig().set("endwar.last-run-year-month", currentYearMonth);
        plugin.saveConfig();
        if (!startBattle()) {
            Bukkit.broadcast(Msg.of("&7이번 달 정기 엔드대전은 참가 신청 인원이 부족해 취소되었습니다."));
        }
    }

    private boolean startBattle() {
        Map<String, Set<UUID>> groups = new HashMap<>();
        Map<String, String> names = new HashMap<>();
        for (UUID uuid : signups) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;
            Team team = teamManager.getTeam(uuid);
            String groupId = team != null ? "team:" + team.id() : "solo:" + uuid;
            String groupName = team != null ? team.name() : player.getName();
            groups.computeIfAbsent(groupId, k -> new HashSet<>()).add(uuid);
            names.put(groupId, groupName);
        }
        if (groups.size() < 2) {
            signups.clear();
            return false;
        }

        aliveByGroup.clear();
        aliveByGroup.putAll(groups);
        groupDisplayName.clear();
        groupDisplayName.putAll(names);
        groupOf.clear();
        for (var entry : groups.entrySet()) {
            for (UUID uuid : entry.getValue()) groupOf.put(uuid, entry.getKey());
        }
        phase = Phase.ONGOING;

        List<String> groupIds = new ArrayList<>(groups.keySet());
        int totalPlayers = 0;
        for (int i = 0; i < groupIds.size(); i++) {
            Location spawn = EndWarWorld.spawnPointForGroup(world, i, groupIds.size());
            for (UUID uuid : groups.get(groupIds.get(i))) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null) continue;
                save(player);
                equip(player);
                player.teleport(spawn);
                totalPlayers++;
            }
        }
        signups.clear();
        Bukkit.broadcast(Msg.of("&4&l⚔ 엔드대전 시작! &f" + groups.size() + "개 팀, 총 " + totalPlayers + "명이 참전합니다!"));
        return true;
    }

    private void save(Player player) {
        PlayerInventory inv = player.getInventory();
        saved.put(player.getUniqueId(), new Saved(inv.getContents(), inv.getArmorContents(),
                inv.getItemInOffHand(), player.getLocation(), player.getGameMode()));
    }

    private void equip(Player player) {
        PlayerInventory inv = player.getInventory();
        inv.clear();
        inv.setArmorContents(new ItemStack[4]);
        inv.setItemInOffHand(null);
        inv.setHelmet(new ItemStack(Material.IRON_HELMET));
        inv.setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        inv.setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        inv.setBoots(new ItemStack(Material.IRON_BOOTS));
        inv.addItem(new ItemStack(Material.IRON_SWORD));
        inv.addItem(new ItemStack(Material.BOW));
        inv.addItem(new ItemStack(Material.ARROW, 32));
        inv.addItem(new ItemStack(Material.COOKED_BEEF, 16));
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
        player.setFoodLevel(20);
        player.setFireTicks(0);
    }

    /** Called by {@link EndWarListener} on death, or by {@link #onMove} on falling off the arena. */
    public void eliminate(Player player) {
        String groupId = groupOf.get(player.getUniqueId());
        if (groupId == null) return;
        Set<UUID> alive = aliveByGroup.get(groupId);
        if (alive == null || !alive.remove(player.getUniqueId())) return;
        Bukkit.broadcast(Msg.of("&7" + player.getName() + " 님이 전사했습니다. &8(" + groupDisplayName.get(groupId)
                + " 생존자 " + alive.size() + "명)"));
        checkWinner();
    }

    public void onMove(Player player) {
        if (!isBattling(player.getUniqueId())) return;
        if (player.getLocation().getY() < EndWarWorld.FALL_ELIMINATE_Y) {
            player.teleport(spectatorSpot());
            player.setGameMode(GameMode.SPECTATOR);
            eliminate(player);
        }
    }

    private void checkWinner() {
        List<String> withSurvivors = new ArrayList<>();
        for (var entry : aliveByGroup.entrySet()) {
            if (!entry.getValue().isEmpty()) withSurvivors.add(entry.getKey());
        }
        if (withSurvivors.size() > 1) return;
        endBattle(withSurvivors.isEmpty() ? null : withSurvivors.get(0));
    }

    private void endBattle(String winnerGroupId) {
        if (winnerGroupId != null) {
            Bukkit.broadcast(Msg.of("&6&l🏆 " + groupDisplayName.get(winnerGroupId) + " &6&l팀이 엔드대전에서 승리했습니다!"));
            for (UUID uuid : aliveByGroup.getOrDefault(winnerGroupId, Set.of())) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && economyManager != null) {
                    economyManager.deposit(uuid, winRewardPerSurvivor);
                    player.sendMessage(Msg.of("&e+" + winRewardPerSurvivor + " 에메랄드 (엔드대전 우승)"));
                }
            }
        } else {
            Bukkit.broadcast(Msg.of("&7이번 엔드대전은 생존자 없이 종료되었습니다."));
        }
        restoreAll();
        resetState();
    }

    private void restoreAll() {
        for (UUID uuid : new HashSet<>(groupOf.keySet())) {
            restorePlayer(uuid);
        }
    }

    /** Restores one participant's gear/location - used both at battle end and when a participant
     *  disconnects mid-battle, where it must run synchronously in the quit handler so gear comes
     *  back before their data is written to disk (same requirement as the minigames). */
    public void restorePlayer(UUID uuid) {
        Saved state = saved.remove(uuid);
        if (state == null) return;
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;
        PlayerInventory inv = player.getInventory();
        inv.setContents(state.contents());
        inv.setArmorContents(state.armor());
        inv.setItemInOffHand(state.offhand());
        player.setGameMode(state.gameMode());
        if (state.location().getWorld() != null) {
            player.teleport(state.location());
        }
    }

    public void handleQuit(Player player) {
        UUID uuid = player.getUniqueId();
        if (!groupOf.containsKey(uuid)) {
            signups.remove(uuid);
            return;
        }
        String groupId = groupOf.get(uuid);
        Set<UUID> alive = aliveByGroup.get(groupId);
        if (alive != null) alive.remove(uuid);
        restorePlayer(uuid);
        groupOf.remove(uuid);
        checkWinner();
    }

    private void resetState() {
        phase = Phase.IDLE;
        aliveByGroup.clear();
        groupDisplayName.clear();
        groupOf.clear();
        saved.clear();
    }
}
