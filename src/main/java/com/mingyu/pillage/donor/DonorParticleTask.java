package com.mingyu.pillage.donor;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Small trailing particle under donors while they walk around. */
public final class DonorParticleTask {

    private final DonorManager donorManager;
    private final Map<UUID, Location> lastLocations = new HashMap<>();

    public DonorParticleTask(DonorManager donorManager) {
        this.donorManager = donorManager;
    }

    public void start(JavaPlugin plugin) {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (donorManager.all().isEmpty()) return;
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (!donorManager.isDonor(player.getUniqueId())) continue;

                Location current = player.getLocation();
                Location last = lastLocations.put(player.getUniqueId(), current);
                if (last != null && last.getWorld() == current.getWorld() && last.distanceSquared(current) > 0.05) {
                    player.getWorld().spawnParticle(Particle.END_ROD, current.clone().add(0, 0.1, 0), 3, 0.2, 0.05, 0.2, 0.01);
                }
            }
        }, 10L, 10L);
    }
}
