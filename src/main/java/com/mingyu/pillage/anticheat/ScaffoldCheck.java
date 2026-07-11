package com.mingyu.pillage.anticheat;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.util.Vector;

/**
 * Detects "no-look" auto-bridging: placing the block directly beneath your own
 * feet while looking far away from it. Very lenient angle tolerance.
 */
public final class ScaffoldCheck implements Listener {

    private final AnticheatManager anticheatManager;
    private final double maxAngleDegrees;

    public ScaffoldCheck(AnticheatManager anticheatManager, double maxAngleDegrees) {
        this.anticheatManager = anticheatManager;
        this.maxAngleDegrees = maxAngleDegrees;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Location placed = event.getBlockPlaced().getLocation();
        Location feet = player.getLocation();

        boolean directlyBelowFeet = placed.getBlockX() == feet.getBlockX()
                && placed.getBlockZ() == feet.getBlockZ()
                && placed.getBlockY() == feet.getBlockY() - 1;
        if (!directlyBelowFeet) return;

        Vector toBlock = placed.toCenterLocation().subtract(player.getEyeLocation()).toVector();
        double angle = Math.toDegrees(player.getEyeLocation().getDirection().angle(toBlock));

        if (angle > maxAngleDegrees) {
            anticheatManager.flag(player, CheckType.SCAFFOLD, "각도 " + (int) angle + "도");
        }
    }
}
