package com.mingyu.pillage.endwar;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class EndWarListener implements Listener {

    private final JavaPlugin plugin;
    private final EndWarManager manager;
    private final Set<UUID> pendingSpectatorRespawn = new HashSet<>();

    public EndWarListener(JavaPlugin plugin, EndWarManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.handleQuit(event.getPlayer());
    }

    // Real death (not a teleport-out like the minigames) since this is meant to be an actual
    // battle - but drops/exp are cleared and gear is restored from the pre-battle snapshot at
    // the end, so nobody permanently loses their real items over it.
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        if (!manager.isBattling(player.getUniqueId())) return;
        event.getDrops().clear();
        event.setDroppedExp(0);
        pendingSpectatorRespawn.add(player.getUniqueId());
        manager.eliminate(player);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!pendingSpectatorRespawn.remove(player.getUniqueId())) return;
        event.setRespawnLocation(manager.spectatorSpot());
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) player.setGameMode(GameMode.SPECTATOR);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!manager.isBattling(player.getUniqueId())) return;
        if (event.getTo() == null) return;
        manager.onMove(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (manager.world() != null && event.getBlock().getWorld().equals(manager.world())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (manager.world() != null && event.getBlock().getWorld().equals(manager.world())) {
            event.setCancelled(true);
        }
    }
}
