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
        return new Location(world,
                config.getDouble("spawn.x"),
                config.getDouble("spawn.y"),
                config.getDouble("spawn.z"),
                (float) config.getDouble("spawn.yaw"),
                (float) config.getDouble("spawn.pitch"));
    }
}
