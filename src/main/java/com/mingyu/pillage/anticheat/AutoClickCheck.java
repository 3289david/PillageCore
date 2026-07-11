package com.mingyu.pillage.anticheat;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * CPS check. Very lenient: only counts sustained periods above the cap, not single spikes.
 */
public final class AutoClickCheck implements Listener {

    private final AnticheatManager anticheatManager;
    private final int maxCps;

    private final Map<UUID, Deque<Long>> clickTimestamps = new HashMap<>();
    private final Map<UUID, Integer> overCapSeconds = new HashMap<>();
    private final Map<UUID, Long> lastSampleAt = new HashMap<>();

    public AutoClickCheck(AnticheatManager anticheatManager, int maxCps) {
        this.anticheatManager = anticheatManager;
        this.maxCps = maxCps;
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwing(PlayerAnimationEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        Deque<Long> timestamps = clickTimestamps.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        timestamps.addLast(now);
        while (!timestamps.isEmpty() && now - timestamps.peekFirst() > 1000) {
            timestamps.pollFirst();
        }

        Long lastSample = lastSampleAt.get(uuid);
        if (lastSample != null && now - lastSample < 1000) {
            return; // only sample once per second so "sustained" means real seconds, not swing count
        }
        lastSampleAt.put(uuid, now);

        int cps = timestamps.size();
        if (cps > maxCps) {
            int seconds = overCapSeconds.merge(uuid, 1, Integer::sum);
            if (seconds >= 5) {
                anticheatManager.flag(player, CheckType.AUTOCLICK, cps + " CPS");
                overCapSeconds.put(uuid, 0);
            }
        } else {
            overCapSeconds.put(uuid, 0);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        clickTimestamps.remove(uuid);
        overCapSeconds.remove(uuid);
    }
}
