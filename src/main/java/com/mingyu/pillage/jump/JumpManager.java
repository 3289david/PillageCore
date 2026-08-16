package com.mingyu.pillage.jump;

import com.mingyu.pillage.data.dao.JumpRecordDao;
import com.mingyu.pillage.util.Msg;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Solo timed checkpoint course - join, climb, fall back to the last checkpoint you reached
 *  (not all the way to the start), finish to record a time and possibly a new personal best. */
public final class JumpManager {

    private record RunState(long startMillis, int checkpointIndex, ItemStack[] savedContents,
                             ItemStack[] savedArmor, ItemStack savedOffhand, Location savedLocation,
                             GameMode savedGameMode) {
        RunState withCheckpoint(int index) {
            return new RunState(startMillis, index, savedContents, savedArmor, savedOffhand, savedLocation, savedGameMode);
        }
    }

    private final JavaPlugin plugin;
    private final JumpRecordDao recordDao;
    private final Map<UUID, RunState> active = new HashMap<>();
    private World world;

    public JumpManager(JavaPlugin plugin, JumpRecordDao recordDao) {
        this.plugin = plugin;
        this.recordDao = recordDao;
    }

    public void start() {
        world = JumpWorld.ensureWorld();
    }

    public World world() {
        return world;
    }

    public boolean isActive(UUID uuid) {
        return active.containsKey(uuid);
    }

    public void startRun(Player player) {
        if (active.containsKey(player.getUniqueId())) {
            player.sendMessage(Msg.of("&c이미 점프맵에 참가 중입니다. &f/jump leave &c로 먼저 나가세요."));
            return;
        }
        PlayerInventory inv = player.getInventory();
        active.put(player.getUniqueId(), new RunState(System.currentTimeMillis(), 0,
                inv.getContents(), inv.getArmorContents(), inv.getItemInOffHand(),
                player.getLocation(), player.getGameMode()));
        inv.clear();
        inv.setArmorContents(new ItemStack[4]);
        inv.setItemInOffHand(null);
        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
        player.setFoodLevel(20);
        player.teleport(JumpWorld.START.clone().toLocation(world).add(0.5, 1, 0.5));
        player.sendMessage(Msg.of("&a점프맵을 시작합니다! &7떨어지면 마지막 체크포인트로 돌아갑니다. &f/jump leave &7로 언제든 나갈 수 있습니다."));
    }

    public void leave(Player player) {
        restore(player, true);
    }

    public void handleQuit(Player player) {
        restore(player, false);
    }

    private void restore(Player player, boolean announce) {
        RunState state = active.remove(player.getUniqueId());
        if (state == null) return;
        PlayerInventory inv = player.getInventory();
        inv.setContents(state.savedContents());
        inv.setArmorContents(state.savedArmor());
        inv.setItemInOffHand(state.savedOffhand());
        player.setGameMode(state.savedGameMode());
        if (state.savedLocation().getWorld() != null) {
            player.teleport(state.savedLocation());
        }
        if (announce && player.isOnline()) {
            player.sendMessage(Msg.of("&7점프맵에서 나왔습니다."));
        }
    }

    public void onMove(Player player) {
        RunState state = active.get(player.getUniqueId());
        if (state == null) return;
        Location loc = player.getLocation();

        int nextIndex = state.checkpointIndex();
        if (nextIndex < JumpWorld.CHECKPOINTS.size()) {
            Location nextCheckpoint = JumpWorld.CHECKPOINTS.get(nextIndex).clone().toLocation(world);
            if (loc.distanceSquared(nextCheckpoint) <= 4) {
                active.put(player.getUniqueId(), state.withCheckpoint(nextIndex + 1));
                player.sendMessage(Msg.of("&a체크포인트! &7(" + (nextIndex + 1) + "/" + JumpWorld.CHECKPOINTS.size() + ")"));
                return;
            }
        }

        Location finish = JumpWorld.FINISH.clone().toLocation(world);
        if (loc.distanceSquared(finish) <= 4) {
            finishRun(player, state);
            return;
        }

        Location lastCheckpoint = lastCheckpointLocation(state);
        if (loc.getY() < lastCheckpoint.getY() - 6) {
            player.teleport(lastCheckpoint.clone().add(0.5, 1, 0.5));
            player.sendMessage(Msg.of("&c떨어졌습니다! 마지막 체크포인트로 돌아갑니다."));
        }
    }

    private Location lastCheckpointLocation(RunState state) {
        if (state.checkpointIndex() == 0) {
            return JumpWorld.START.clone().toLocation(world);
        }
        return JumpWorld.CHECKPOINTS.get(state.checkpointIndex() - 1).clone().toLocation(world);
    }

    private void finishRun(Player player, RunState state) {
        long elapsedMillis = System.currentTimeMillis() - state.startMillis();
        restore(player, false);
        player.sendMessage(Msg.of("&a&l완주! &f기록: " + formatMillis(elapsedMillis)));

        long previousBest = recordDao.bestMillis(player.getUniqueId());
        if (previousBest < 0 || elapsedMillis < previousBest) {
            recordDao.saveBest(player.getUniqueId(), player.getName(), elapsedMillis);
            player.sendMessage(Msg.of("&6&l신기록 갱신!"));
        }
    }

    public List<JumpRecordDao.Record> top(int limit) {
        return recordDao.top(limit);
    }

    public static String formatMillis(long millis) {
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long tenths = (millis % 1000) / 100;
        return String.format("%d:%02d.%d", minutes, seconds, tenths);
    }
}
