package com.mingyu.pillage.anticheat;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Speed and Fly checks. Both bail out generously on anything that could
 * legitimately explain fast/airborne movement, and require several
 * consecutive offending ticks (not just one) before ever flagging.
 */
public final class MovementChecks implements Listener {

    private static final double BASE_MAX_HORIZONTAL_PER_TICK = 0.35; // generous sprint-jump baseline

    private final AnticheatManager anticheatManager;
    private final boolean speedEnabled;
    private final double speedToleranceMultiplier;
    private final boolean flyEnabled;
    private final int flyGraceTicks;

    private final Map<UUID, Integer> fastTicks = new HashMap<>();
    private final Map<UUID, Integer> airborneTicks = new HashMap<>();

    public MovementChecks(AnticheatManager anticheatManager, boolean speedEnabled, double speedToleranceMultiplier,
                           boolean flyEnabled, int flyGraceSeconds) {
        this.anticheatManager = anticheatManager;
        this.speedEnabled = speedEnabled;
        this.speedToleranceMultiplier = speedToleranceMultiplier;
        this.flyEnabled = flyEnabled;
        this.flyGraceTicks = flyGraceSeconds * 20;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || from.getWorld() != to.getWorld()) return;

        if (isExempt(player)) {
            fastTicks.remove(player.getUniqueId());
            airborneTicks.remove(player.getUniqueId());
            return;
        }

        if (speedEnabled) {
            double horizontal = Math.hypot(to.getX() - from.getX(), to.getZ() - from.getZ());
            double allowed = BASE_MAX_HORIZONTAL_PER_TICK * speedToleranceMultiplier;
            UUID uuid = player.getUniqueId();
            if (horizontal > allowed) {
                int ticks = fastTicks.merge(uuid, 1, Integer::sum);
                if (ticks >= 5) {
                    anticheatManager.flag(player, CheckType.SPEED, String.format("%.2f 블럭/틱", horizontal));
                    fastTicks.put(uuid, 0);
                }
            } else {
                fastTicks.put(uuid, 0);
            }
        }

        if (flyEnabled) {
            UUID uuid = player.getUniqueId();
            double deltaY = to.getY() - from.getY();
            boolean ascendingOrHovering = !isNearGround(to) && deltaY >= -0.08;
            if (ascendingOrHovering) {
                int ticks = airborneTicks.merge(uuid, 1, Integer::sum);
                if (ticks >= flyGraceTicks) {
                    anticheatManager.flag(player, CheckType.FLY, "체공 지속");
                    airborneTicks.put(uuid, 0);
                }
            } else {
                airborneTicks.put(uuid, 0);
            }
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        reset(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        reset(event.getPlayer().getUniqueId());
    }

    private void reset(UUID uuid) {
        fastTicks.remove(uuid);
        airborneTicks.remove(uuid);
    }

    private boolean isNearGround(Location location) {
        for (double dy = 0.0; dy <= 0.5; dy += 0.25) {
            if (location.clone().subtract(0, dy, 0).getBlock().getType().isSolid()) {
                return true;
            }
        }
        return false;
    }

    private boolean isExempt(Player player) {
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return true;
        if (player.isFlying() || player.getAllowFlight()) return true;
        if (player.isGliding()) return true;
        if (player.isInsideVehicle()) return true;
        if (player.isSwimming() || player.isInWater()) return true;
        if (player.isClimbing()) return true;
        if (player.hasPotionEffect(PotionEffectType.SPEED)
                || player.hasPotionEffect(PotionEffectType.JUMP_BOOST)
                || player.hasPotionEffect(PotionEffectType.LEVITATION)
                || player.hasPotionEffect(PotionEffectType.SLOW_FALLING)) {
            return true;
        }
        return player.hasPermission("pillage.admin");
    }
}
