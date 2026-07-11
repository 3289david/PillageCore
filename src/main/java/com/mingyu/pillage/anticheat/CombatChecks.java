package com.mingyu.pillage.anticheat;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

/**
 * KillAura (view-angle) and Reach checks. Both are intentionally forgiving:
 * high angle/distance tolerance and only projectile-free direct hits are checked.
 */
public final class CombatChecks implements Listener {

    private final AnticheatManager anticheatManager;
    private final boolean killAuraEnabled;
    private final double maxAngleDegrees;
    private final boolean reachEnabled;
    private final double maxReachDistance;

    public CombatChecks(AnticheatManager anticheatManager, boolean killAuraEnabled, double maxAngleDegrees,
                         boolean reachEnabled, double maxReachDistance) {
        this.anticheatManager = anticheatManager;
        this.killAuraEnabled = killAuraEnabled;
        this.maxAngleDegrees = maxAngleDegrees;
        this.reachEnabled = reachEnabled;
        this.maxReachDistance = maxReachDistance;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Projectile) return; // only melee is checked
        if (!(event.getDamager() instanceof Player attacker)) return;
        Entity victim = event.getEntity();

        if (killAuraEnabled) {
            double angle = Math.toDegrees(attacker.getEyeLocation().getDirection()
                    .angle(victim.getLocation().add(0, victim.getHeight() / 2.0, 0)
                            .subtract(attacker.getEyeLocation()).toVector()));
            if (angle > maxAngleDegrees) {
                anticheatManager.flag(attacker, CheckType.KILLAURA, "각도 " + (int) angle + "도");
            }
        }

        if (reachEnabled) {
            double distance = distanceToBoundingBox(attacker.getEyeLocation().toVector(), victim.getBoundingBox());
            if (distance > maxReachDistance) {
                anticheatManager.flag(attacker, CheckType.REACH, String.format("%.2f 블럭", distance));
            }
        }
    }

    private double distanceToBoundingBox(Vector point, BoundingBox box) {
        double clampedX = clamp(point.getX(), box.getMinX(), box.getMaxX());
        double clampedY = clamp(point.getY(), box.getMinY(), box.getMaxY());
        double clampedZ = clamp(point.getZ(), box.getMinZ(), box.getMaxZ());
        return point.distance(new Vector(clampedX, clampedY, clampedZ));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
