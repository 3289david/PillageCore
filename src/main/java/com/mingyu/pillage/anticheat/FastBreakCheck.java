package com.mingyu.pillage.anticheat;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Only catches genuinely impossible "instant nuker" style breaks. Anything a real
 * tool/enchant/haste combo could plausibly achieve is intentionally left alone.
 */
public final class FastBreakCheck implements Listener {

    private final AnticheatManager anticheatManager;
    private final long minMillisForHardBlocks;
    private final double minHardness;

    private final Map<UUID, Long> breakStartedAt = new HashMap<>();

    public FastBreakCheck(AnticheatManager anticheatManager, long minMillisForHardBlocks, double minHardness) {
        this.anticheatManager = anticheatManager;
        this.minMillisForHardBlocks = minMillisForHardBlocks;
        this.minHardness = minHardness;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(BlockDamageEvent event) {
        breakStartedAt.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;
        if (event.getBlock().getType().getHardness() < minHardness) return;

        Long started = breakStartedAt.remove(player.getUniqueId());
        if (started == null) return;

        long elapsed = System.currentTimeMillis() - started;
        if (elapsed < minMillisForHardBlocks) {
            anticheatManager.flag(player, CheckType.FASTBREAK, elapsed + "ms");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        breakStartedAt.remove(event.getPlayer().getUniqueId());
    }
}
