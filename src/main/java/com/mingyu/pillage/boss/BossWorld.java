package com.mingyu.pillage.boss;

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

/** Builds (once - marker-block gated, same as the minigame/Hall of Fame worlds) the dedicated
 *  boss arena: a walled-in flat platform that never touches real gameplay space. */
final class BossWorld {

    static final String WORLD_NAME = "pillage_boss";
    static final int ARENA_HALF_SIZE = 30;
    static final Location CENTER = new Location(null, 0, 101, 0);
    static final Location ENTRY = new Location(null, 0, 101, 26, 180f, 0f);

    private BossWorld() {
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
                world.getBlockAt(dx, baseY, dz).setType(Material.BLACKSTONE);
                boolean edge = Math.abs(dx) == half || Math.abs(dz) == half;
                for (int y = baseY + 1; y <= baseY + 14; y++) {
                    world.getBlockAt(dx, y, dz).setType(edge ? Material.BARRIER : Material.AIR);
                }
            }
        }
    }
}
