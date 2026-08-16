package com.mingyu.pillage.jump;

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

import java.util.ArrayList;
import java.util.List;

/** Builds (once - marker-block gated, same pattern as every other dedicated world in this
 *  plugin) a long checkpointed parkour course in its own world, entirely separate from the
 *  live-race PARKOUR minigame - this is a solo, timed, checkpoint-recoverable course with a
 *  persistent leaderboard instead. */
final class JumpWorld {

    static final String WORLD_NAME = "pillage_jump";
    static final int TOTAL_STEPS = 60;
    static final int CHECKPOINT_INTERVAL = 5;
    static final Location START = new Location(null, 0, 101, 0);
    static final List<Location> CHECKPOINTS;
    static final Location FINISH;

    static {
        List<int[]> path = computePath();
        List<Location> checkpoints = new ArrayList<>();
        for (int i = 0; i < path.size(); i++) {
            int step = i + 1;
            if (step != TOTAL_STEPS && step % CHECKPOINT_INTERVAL == 0) {
                int[] p = path.get(i);
                checkpoints.add(new Location(null, p[0], p[1], p[2]));
            }
        }
        CHECKPOINTS = List.copyOf(checkpoints);
        int[] last = path.get(path.size() - 1);
        FINISH = new Location(null, last[0], last[1], last[2]);
    }

    private JumpWorld() {
    }

    private static List<int[]> computePath() {
        List<int[]> path = new ArrayList<>();
        int x = START.getBlockX();
        int y = START.getBlockY();
        int z = START.getBlockZ();
        int direction = 1;
        for (int i = 1; i <= TOTAL_STEPS; i++) {
            z += 2;
            x += direction * (i % 3 == 0 ? 2 : 1);
            if (i % 5 == 0) direction *= -1;
            if (i % 4 == 0) y += 1;
            path.add(new int[]{x, y, z});
        }
        return path;
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
        world.getEntitiesByClass(Monster.class).forEach(Entity::remove);
        world.getEntitiesByClass(Slime.class).forEach(Entity::remove);
        world.getEntitiesByClass(Animals.class).forEach(Entity::remove);

        Location marker = new Location(world, 0, 60, 0);
        if (marker.getBlock().getType() != Material.BEDROCK) {
            buildCourse(world);
            marker.getBlock().setType(Material.BEDROCK);
        }
        return world;
    }

    private static void buildCourse(World world) {
        world.getBlockAt(START.getBlockX(), START.getBlockY() - 1, START.getBlockZ()).setType(Material.QUARTZ_BLOCK);
        List<int[]> path = computePath();
        for (int i = 0; i < path.size(); i++) {
            int step = i + 1;
            int[] p = path.get(i);
            boolean finish = step == TOTAL_STEPS;
            boolean checkpoint = !finish && step % CHECKPOINT_INTERVAL == 0;
            if (finish || checkpoint) {
                world.getBlockAt(p[0], p[1] - 1, p[2]).setType(finish ? Material.DIAMOND_BLOCK : Material.EMERALD_BLOCK);
            } else {
                world.getBlockAt(p[0], p[1], p[2]).setType(Material.QUARTZ_STAIRS);
            }
        }
    }
}
