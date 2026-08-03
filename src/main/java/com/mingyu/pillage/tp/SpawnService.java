package com.mingyu.pillage.tp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * "/spawn" always means the current world's own spawn point, whichever instance (main server,
 * a mini-server, the hub) that happens to be - so this generalizes for free to every instance
 * without needing to know about them. The one bit of legacy config (spawn.x/z/yaw/pitch) only
 * customizes the *main* world's spawn, applied once at startup via {@link #applyMainWorldSpawn()}.
 */
public final class SpawnService {

    private final JavaPlugin plugin;

    public SpawnService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public Location spawnLocation(Player player) {
        return player.getWorld().getSpawnLocation();
    }

    /** Applies the legacy config.yml spawn.* coordinates onto the main world's spawn point. */
    public void applyMainWorldSpawn() {
        FileConfiguration config = plugin.getConfig();
        World world = Bukkit.getWorld(config.getString("spawn.world", "world"));
        if (world == null) {
            world = Bukkit.getWorlds().get(0);
        }
        double x = config.getDouble("spawn.x");
        double z = config.getDouble("spawn.z");
        int groundY = world.getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z));
        world.setSpawnLocation(
                (int) Math.floor(x), groundY + 1, (int) Math.floor(z),
                (float) config.getDouble("spawn.yaw"));
    }
}
