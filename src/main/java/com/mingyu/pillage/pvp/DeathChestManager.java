package com.mingyu.pillage.pvp;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class DeathChestManager implements Listener {

    private final Set<Location> chests = new HashSet<>();

    public void spawn(Location location, List<ItemStack> items, UUID owner) {
        if (items.isEmpty()) return;
        Location origin = location.getBlock().getLocation();

        List<ItemStack> remaining = new ArrayList<>(items);
        int candidateIndex = 0;
        while (!remaining.isEmpty()) {
            Location chestLoc = candidateIndex == 0 ? origin : spiralOffset(origin, candidateIndex);
            candidateIndex++;
            // Never overwrite one of our own still-active death chests - that would destroy whoever's loot is in it.
            if (chests.contains(chestLoc)) continue;

            chestLoc.getBlock().setType(Material.CHEST);
            // getState() (snapshot=true) is a detached copy - writes to its inventory never reach the
            // real tile entity even after update(). getState(false) writes straight into the live block.
            if (!(chestLoc.getBlock().getState(false) instanceof Chest chest)) continue;

            var leftover = chest.getBlockInventory().addItem(remaining.toArray(new ItemStack[0]));
            chests.add(chestLoc);
            remaining = new ArrayList<>(leftover.values());
        }
    }

    /** Square-spiral search around the death spot so overflow items get their own adjacent chest instead of being lost. */
    private Location spiralOffset(Location origin, int index) {
        int x = 0, z = 0;
        int dx = 1, dz = 0;
        int segmentLength = 1, segmentPassed = 0, turns = 0;
        for (int i = 0; i < index; i++) {
            x += dx;
            z += dz;
            segmentPassed++;
            if (segmentPassed == segmentLength) {
                segmentPassed = 0;
                int tmp = dx;
                dx = -dz;
                dz = tmp;
                turns++;
                if (turns % 2 == 0) segmentLength++;
            }
        }
        return origin.clone().add(x, 0, z);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        // Death chests are breakable - vanilla drops their contents on the ground like any other chest.
        chests.remove(event.getBlock().getLocation());
    }
}
