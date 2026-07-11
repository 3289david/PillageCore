package com.mingyu.pillage.pvp;

import com.mingyu.pillage.util.Msg;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class DeathChestManager implements Listener {

    private final Set<Location> chests = new HashSet<>();

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

        // Chests stay at the death location forever and can be looted by anyone - no team or ownership check.
        chests.add(blockLoc);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.CHEST) return;
        if (chests.contains(event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Msg.of("&c사망 상자는 부술 수 없습니다."));
        }
    }
}
