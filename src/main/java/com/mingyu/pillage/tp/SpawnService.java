package com.mingyu.pillage.tp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpawnService {

    private final JavaPlugin plugin;

    public SpawnService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public Location spawnLocation() {
        FileConfiguration config = plugin.getConfig();
        World world = Bukkit.getWorld(config.getString("spawn.world", "world"));
        if (world == null) {
            world = Bukkit.getWorlds().get(0);
        }
        double x = config.getDouble("spawn.x");
        double z = config.getDouble("spawn.z");
        // Snap to the highest solid block so players land on the ground instead of floating in the sky.
        int groundY = world.getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z));
        return new Location(world,
                x,
                groundY + 1,
                z,
                (float) config.getDouble("spawn.yaw"),
                (float) config.getDouble("spawn.pitch"));
    }
}
