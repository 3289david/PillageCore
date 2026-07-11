package com.mingyu.pillage.pvp;

import com.mingyu.pillage.team.TeamManager;
import com.mingyu.pillage.util.Msg;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DeathChestManager implements Listener {

    private record ChestRecord(UUID owner, Integer teamId) {
    }

    private final JavaPlugin plugin;
    private final TeamManager teamManager;
    private final int durationSeconds;
    private final Map<Location, ChestRecord> chests = new HashMap<>();

    public DeathChestManager(JavaPlugin plugin, TeamManager teamManager, int durationSeconds) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.durationSeconds = durationSeconds;
    }

    public void spawn(Location location, List<ItemStack> items, UUID owner) {
        if (items.isEmpty()) return;
        Location blockLoc = location.getBlock().getLocation();
        blockLoc.getBlock().setType(Material.CHEST);
        if (!(blockLoc.getBlock().getState() instanceof Chest chest)) {
            return;
        }
        for (ItemStack item : items) {
            chest.getBlockInventory().addItem(item);
        }
        chest.update();

        var team = teamManager.getTeam(owner);
        chests.put(blockLoc, new ChestRecord(owner, team == null ? null : team.id()));

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> expire(blockLoc), durationSeconds * 20L);
    }

    private void expire(Location blockLoc) {
        ChestRecord record = chests.remove(blockLoc);
        if (record == null) return;
        if (blockLoc.getBlock().getType() == Material.CHEST
                && blockLoc.getBlock().getState() instanceof Chest chest) {
            for (ItemStack item : chest.getBlockInventory().getContents()) {
                if (item != null && !item.getType().isAir()) {
                    blockLoc.getWorld().dropItemNaturally(blockLoc, item);
                }
            }
        }
        blockLoc.getBlock().setType(Material.AIR);
    }

    private boolean canOpen(ChestRecord record, Player player) {
        if (record.owner().equals(player.getUniqueId())) return true;
        if (record.teamId() == null) return false;
        var team = teamManager.getTeam(player.getUniqueId());
        return team != null && team.id() == record.teamId();
    }

    @EventHandler(ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (!(event.getInventory().getHolder() instanceof Chest chest)) return;
        ChestRecord record = chests.get(chest.getLocation());
        if (record == null) return;

        Player player = (Player) event.getPlayer();
        if (!canOpen(record, player)) {
            event.setCancelled(true);
            player.sendMessage(Msg.of("&c이 사망 상자는 팀원만 열 수 있습니다."));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.CHEST) return;
        if (chests.containsKey(event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Msg.of("&c사망 상자는 부술 수 없습니다. 시간이 지나면 사라집니다."));
        }
    }
}
