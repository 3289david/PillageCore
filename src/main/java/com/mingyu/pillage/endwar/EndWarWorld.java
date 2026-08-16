package com.mingyu.pillage.endwar;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Slime;

/** Builds (once - marker-block gated, same pattern as every other dedicated world in this
 *  plugin) an End-themed battle arena. Deliberately a reskinned NORMAL/FLAT world rather than the
 *  real End dimension - same reasoning as the rest of this plugin's dedicated worlds: it needs to
 *  be a small, safe, code-built platform, not drag in vanilla End generation, the dragon fight,
 *  or an exit portal. Falling off the edge into open air below the platform is a deliberate
 *  "ring out" elimination, handled in {@link EndWarManager#onMove}. */
final class EndWarWorld {

    static final String WORLD_NAME = "pillage_endwar";
    static final int ARENA_HALF_SIZE = 35;
    static final Location CENTER = new Location(null, 0, 101, 0);
    static final Location SPECTATOR_SPOT = new Location(null, 0, 140, 0);
    static final int FALL_ELIMINATE_Y = 85;

    private EndWarWorld() {
    }

    static World ensureWorld() {
        World world = Bukkit.getWorld(WORLD_NAME);
        if (world == null) {
            world = new WorldCreator(WORLD_NAME)
                    .type(WorldType.FLAT)
                    .environment(World.Environment.NORMAL)
                    .createWorld();
        }
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setTime(18000);
        world.setSpawnFlags(false, false);
        world.getEntitiesByClass(Monster.class).forEach(Entity::remove);
        world.getEntitiesByClass(Slime.class).forEach(Entity::remove);
        world.getEntitiesByClass(Animals.class).forEach(Entity::remove);

        Location marker = new Location(world, 0, 60, 0);
        if (marker.getBlock().getType() != Material.BEDROCK) {
            buildArena(world);
            marker.getBlock().setType(Material.BEDROCK);
        }
        return world;
    }

    private static void buildArena(World world) {
        int half = ARENA_HALF_SIZE;
        int baseY = CENTER.getBlockY() - 1;
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                boolean border = Math.abs(dx) >= half - 1 || Math.abs(dz) >= half - 1;
                world.getBlockAt(dx, baseY, dz).setType(border ? Material.OBSIDIAN : Material.END_STONE);
                for (int y = baseY + 1; y <= baseY + 6; y++) {
                    world.getBlockAt(dx, y, dz).setType(Material.AIR);
                }
            }
        }
        int ringRadius = half - half / 3;
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                double dist = Math.sqrt((double) dx * dx + (double) dz * dz);
                if (Math.abs(dist - ringRadius) < 0.7) {
                    world.getBlockAt(dx, baseY, dz).setType(Material.PURPUR_BLOCK);
                }
            }
        }
    }

    static Location spawnPointForGroup(World world, int index, int totalGroups) {
        int half = ARENA_HALF_SIZE;
        double angle = (2 * Math.PI * index) / Math.max(1, totalGroups);
        int x = (int) Math.round(Math.cos(angle) * (half - 4));
        int z = (int) Math.round(Math.sin(angle) * (half - 4));
        Location spot = new Location(world, x + 0.5, CENTER.getBlockY(), z + 0.5);
        spot.setDirection(CENTER.clone().toLocation(world).toVector().subtract(spot.toVector()));
        return spot;
    }
}
