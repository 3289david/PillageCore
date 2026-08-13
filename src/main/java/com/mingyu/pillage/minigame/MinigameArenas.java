package com.mingyu.pillage.minigame;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Slime;

/** Builds (once - each arena checks a marker block before building again on later restarts) the
 *  four minigame arenas inside one small dedicated world, the same "never touches real gameplay
 *  space" pattern the Hall of Fame's dedicated world already uses. Plain code-built platforms,
 *  not hand-crafted structures - functional over decorative, matching the scope of "add 3-4
 *  minigames" rather than "build a full minigame plugin". */
final class MinigameArenas {

    static final String WORLD_NAME = "pillage_minigames";

    static final int SPLEEF_ARENA_SIZE = 13; // odd, so there's a true center
    static final Location SPLEEF_ORIGIN = new Location(null, 0, 100, 0);
    static final int SPLEEF_KILL_Y = 90;

    static final int TNT_RUN_ARENA_SIZE = 13;
    static final Location TNT_RUN_ORIGIN = new Location(null, 100, 100, 0);
    static final int TNT_RUN_KILL_Y = 90;

    static final Location PARKOUR_START = new Location(null, 200, 101, 0);
    static final Location PARKOUR_FINISH = new Location(null, 200, 105, 34);
    static final int PARKOUR_FALL_Y = 90;

    static final int TAG_ARENA_SIZE = 21;
    static final Location TAG_ORIGIN = new Location(null, 300, 100, 0);

    private MinigameArenas() {
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
        world.setTime(6000);
        world.setSpawnFlags(false, false);
        world.getEntitiesByClass(Monster.class).forEach(org.bukkit.entity.Entity::remove);
        world.getEntitiesByClass(Slime.class).forEach(org.bukkit.entity.Entity::remove);
        world.getEntitiesByClass(Animals.class).forEach(org.bukkit.entity.Entity::remove);

        Location marker = new Location(world, 0, 60, 0);
        if (marker.getBlock().getType() != Material.BEDROCK) {
            buildSpleef(world);
            buildTntRun(world);
            buildParkour(world);
            buildTag(world);
            marker.getBlock().setType(Material.BEDROCK);
        }
        return world;
    }

    /** Refills the spleef floor and clears headroom above it - called at world-build time and
     *  again at the start of every round, since the previous round dug the floor apart. */
    static void buildSpleefFloorFresh(World world) {
        buildSpleef(world);
    }

    /** Same idea as {@link #buildSpleefFloorFresh} but for the TNT run floor. */
    static void buildTntRunFloorFresh(World world) {
        buildTntRun(world);
    }

    private static void buildSpleef(World world) {
        int half = SPLEEF_ARENA_SIZE / 2;
        int baseX = SPLEEF_ORIGIN.getBlockX();
        int baseY = SPLEEF_ORIGIN.getBlockY();
        int baseZ = SPLEEF_ORIGIN.getBlockZ();
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                world.getBlockAt(baseX + dx, baseY, baseZ + dz).setType(Material.SNOW_BLOCK);
                for (int y = baseY + 1; y <= baseY + 4; y++) {
                    world.getBlockAt(baseX + dx, y, baseZ + dz).setType(Material.AIR);
                }
            }
        }
    }

    private static void buildTntRun(World world) {
        int half = TNT_RUN_ARENA_SIZE / 2;
        int baseX = TNT_RUN_ORIGIN.getBlockX();
        int baseY = TNT_RUN_ORIGIN.getBlockY();
        int baseZ = TNT_RUN_ORIGIN.getBlockZ();
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                world.getBlockAt(baseX + dx, baseY, baseZ + dz).setType(Material.STONE_BRICKS);
                for (int y = baseY + 1; y <= baseY + 4; y++) {
                    world.getBlockAt(baseX + dx, y, baseZ + dz).setType(Material.AIR);
                }
            }
        }
    }

    private static void buildParkour(World world) {
        int x = PARKOUR_START.getBlockX();
        int y = PARKOUR_START.getBlockY() - 1;
        int z = PARKOUR_START.getBlockZ();
        world.getBlockAt(x, y, z).setType(Material.QUARTZ_BLOCK);
        world.getBlockAt(x, y, z - 1).setType(Material.QUARTZ_BLOCK);
        world.getBlockAt(x, y, z - 2).setType(Material.QUARTZ_BLOCK);

        int steps = 15;
        int direction = 1;
        for (int i = 1; i <= steps; i++) {
            z += 2;
            x += direction * (i % 3 == 0 ? 2 : 1);
            if (i % 4 == 0) direction *= -1;
            if (i % 5 == 0) y += 1;
            world.getBlockAt(x, y, z).setType(Material.QUARTZ_STAIRS);
        }

        int finishX = PARKOUR_FINISH.getBlockX();
        int finishY = PARKOUR_FINISH.getBlockY() - 1;
        int finishZ = PARKOUR_FINISH.getBlockZ();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.getBlockAt(finishX + dx, finishY, finishZ + dz).setType(Material.EMERALD_BLOCK);
            }
        }
    }

    private static void buildTag(World world) {
        int half = TAG_ARENA_SIZE / 2;
        int baseX = TAG_ORIGIN.getBlockX();
        int baseY = TAG_ORIGIN.getBlockY();
        int baseZ = TAG_ORIGIN.getBlockZ();
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                world.getBlockAt(baseX + dx, baseY, baseZ + dz).setType(Material.GRASS_BLOCK);
                for (int y = baseY + 1; y <= baseY + 4; y++) {
                    world.getBlockAt(baseX + dx, y, baseZ + dz).setType(Material.AIR);
                }
                boolean onEdge = Math.abs(dx) == half || Math.abs(dz) == half;
                if (onEdge) {
                    world.getBlockAt(baseX + dx, baseY + 1, baseZ + dz).setType(Material.OAK_FENCE);
                }
            }
        }
        // A handful of scattered waist-high obstacle blocks for varied chase lines.
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) continue;
                int ox = baseX + i * 6;
                int oz = baseZ + j * 6;
                world.getBlockAt(ox, baseY + 1, oz).setType(Material.STONE_BRICKS);
            }
        }
    }
}
