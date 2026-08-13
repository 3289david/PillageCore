package com.mingyu.pillage.minigame;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;

public final class MinigameListener implements Listener {

    private final JavaPlugin plugin;
    private final MinigameManager manager;
    private final Set<Block> pendingTntRunRemoval = new HashSet<>();

    public MinigameListener(JavaPlugin plugin, MinigameManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.handleQuit(event.getPlayer());
    }

    // Games are non-lethal by design (elimination = teleport out, not death) - cancel every
    // damage source for anyone currently playing rather than trying to precisely allow/deny each
    // cause (fall, void, contact, whatever else could fire during a chase or a spleef drop).
    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!manager.isParticipant(player.getUniqueId())) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player target) || !(event.getDamager() instanceof Player tagger)) return;
        if (!manager.isParticipant(tagger.getUniqueId()) || !manager.isParticipant(target.getUniqueId())) return;
        manager.onTagHit(tagger, target);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!manager.isParticipant(player.getUniqueId())) return;
        if (event.getTo() == null) return;
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        manager.onParticipantMove(player);

        // TNT run: whatever solid block the player is standing on disappears shortly after.
        Block under = player.getLocation().clone().subtract(0, 1, 0).getBlock();
        if (under.getType() == Material.STONE_BRICKS && pendingTntRunRemoval.add(under)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (under.getType() == Material.STONE_BRICKS) {
                    under.setType(Material.AIR);
                }
                pendingTntRunRemoval.remove(under);
            }, 10L);
        }
    }

    // Only the spleef floor (snow) is breakable, and only for someone actually playing spleef -
    // everything else in this world is off-limits, same "no building" rule the hub uses.
    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!event.getBlock().getWorld().getName().equals(MinigameArenas.WORLD_NAME)) return;
        Player player = event.getPlayer();
        boolean allowed = event.getBlock().getType() == Material.SNOW_BLOCK
                && manager.isPlayingSpleef(player.getUniqueId());
        if (!allowed) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getBlock().getWorld().getName().equals(MinigameArenas.WORLD_NAME)) {
            event.setCancelled(true);
        }
    }
}
