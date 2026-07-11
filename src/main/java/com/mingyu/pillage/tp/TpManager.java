package com.mingyu.pillage.tp;

import com.mingyu.pillage.data.dao.HomeDao;
import com.mingyu.pillage.data.dao.LastLocationDao;
import com.mingyu.pillage.util.Msg;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TpManager {

    private final JavaPlugin plugin;
    private final HomeDao homeDao;
    private final LastLocationDao lastLocationDao;
    private final List<TeleportGuard> guards = new ArrayList<>();

    private final int countdownSeconds;
    private final int cooldownSeconds;
    private final int requestTimeoutSeconds;
    private final boolean cancelOnMove;
    private final double cancelOnMoveThreshold;

    private final Map<UUID, TpRequest> incomingRequests = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, PendingTeleport> pendingTeleports = new HashMap<>();

    public TpManager(JavaPlugin plugin, HomeDao homeDao, LastLocationDao lastLocationDao,
                      int countdownSeconds, int cooldownSeconds, int requestTimeoutSeconds,
                      boolean cancelOnMove, double cancelOnMoveThreshold) {
        this.plugin = plugin;
        this.homeDao = homeDao;
        this.lastLocationDao = lastLocationDao;
        this.countdownSeconds = countdownSeconds;
        this.cooldownSeconds = cooldownSeconds;
        this.requestTimeoutSeconds = requestTimeoutSeconds;
        this.cancelOnMove = cancelOnMove;
        this.cancelOnMoveThreshold = cancelOnMoveThreshold;
    }

    public void registerGuard(TeleportGuard guard) {
        guards.add(guard);
    }

    private record PendingTeleport(Location startLocation, Location destination, BukkitTask task) {
    }

    public boolean hasPendingTeleport(UUID uuid) {
        return pendingTeleports.containsKey(uuid);
    }

    public void requestTeleport(Player player, Location destination) {
        for (TeleportGuard guard : guards) {
            String reason = guard.blockReason(player);
            if (reason != null) {
                player.sendMessage(Msg.of(reason));
                return;
            }
        }

        Long cooldownUntil = cooldowns.get(player.getUniqueId());
        if (cooldownUntil != null && cooldownUntil > System.currentTimeMillis() && !player.hasPermission("pillage.admin")) {
            long remaining = (cooldownUntil - System.currentTimeMillis()) / 1000 + 1;
            player.sendMessage(Msg.of("&c텔레포트 쿨타임이 " + remaining + "초 남았습니다."));
            return;
        }

        cancelPendingTeleport(player.getUniqueId(), false);

        if (countdownSeconds <= 0) {
            executeTeleport(player, destination);
            return;
        }

        player.sendMessage(Msg.of("&e" + countdownSeconds + "초 후 이동합니다. 움직이면 취소됩니다."));
        Location start = player.getLocation();
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            pendingTeleports.remove(player.getUniqueId());
            if (!player.isOnline()) return;
            executeTeleport(player, destination);
        }, countdownSeconds * 20L);

        pendingTeleports.put(player.getUniqueId(), new PendingTeleport(start, destination, task));
    }

    private void executeTeleport(Player player, Location destination) {
        lastLocationDao.save(player.getUniqueId(), player.getLocation());
        player.teleport(destination);
        player.sendMessage(Msg.of("&a이동했습니다."));
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + cooldownSeconds * 1000L);
    }

    public void onMove(Player player, Location to) {
        if (!cancelOnMove) return;
        PendingTeleport pending = pendingTeleports.get(player.getUniqueId());
        if (pending == null) return;
        if (pending.startLocation().getWorld() != to.getWorld()
                || pending.startLocation().distanceSquared(to) > cancelOnMoveThreshold * cancelOnMoveThreshold) {
            cancelPendingTeleport(player.getUniqueId(), true);
        }
    }

    public void cancelPendingTeleport(UUID uuid, boolean notify) {
        PendingTeleport pending = pendingTeleports.remove(uuid);
        if (pending != null) {
            pending.task().cancel();
            if (notify) {
                Player player = plugin.getServer().getPlayer(uuid);
                if (player != null) {
                    player.sendMessage(Msg.of("&c이동하여 텔레포트가 취소되었습니다."));
                }
            }
        }
    }

    // --- tpa/tpaccept/tpdeny ---

    public void sendRequest(Player requester, Player target) {
        incomingRequests.put(target.getUniqueId(),
                new TpRequest(requester.getUniqueId(), System.currentTimeMillis() + requestTimeoutSeconds * 1000L));
        target.sendMessage(Msg.of("&e" + requester.getName() + "&f 님이 텔레포트를 요청했습니다. &a/tpaccept &f또는 &c/tpdeny"));
        requester.sendMessage(Msg.of("&f" + target.getName() + " 님에게 텔레포트 요청을 보냈습니다."));
    }

    public TpRequest peekRequest(UUID target) {
        TpRequest request = incomingRequests.get(target);
        if (request == null || request.isExpired()) {
            return null;
        }
        return request;
    }

    public TpRequest consumeRequest(UUID target) {
        TpRequest request = incomingRequests.remove(target);
        if (request == null || request.isExpired()) {
            return null;
        }
        return request;
    }

    public void denyRequest(UUID target) {
        incomingRequests.remove(target);
    }

    // --- homes ---

    public Map<String, Location> homes(UUID uuid) {
        return homeDao.loadHomes(uuid);
    }

    public void setHome(UUID uuid, String name, Location location) {
        homeDao.setHome(uuid, name, location);
    }

    public void deleteHome(UUID uuid, String name) {
        homeDao.deleteHome(uuid, name);
    }

    public Location back(UUID uuid) {
        return lastLocationDao.load(uuid);
    }

    public void recordLastLocation(UUID uuid, Location location) {
        lastLocationDao.save(uuid, location);
    }
}
