package com.mingyu.pillage.donor;

import com.mingyu.pillage.data.dao.HallOfFameDao;
import com.mingyu.pillage.data.dao.HallOfFameMetaDao;
import com.mingyu.pillage.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A small monument built once at the spawn location, with one pedestal + statue per donor.
 * Statues are created/removed automatically as /donor add|remove runs.
 */
public final class HallOfFameManager {

    private static final int ROW_LENGTH = 8;
    private static final int SLOT_SPACING_X = 3;
    private static final int ROW_SPACING_Z = 4;
    private static final int PLATFORM_MARGIN = 3;
    private static final int MAX_ROWS = 6;
    private static final int PILLAR_HEIGHT = 5;

    private final JavaPlugin plugin;
    private final DonorManager donorManager;
    private final HallOfFameDao hofDao;
    private final HallOfFameMetaDao metaDao;
    private final NamespacedKey markerKey;

    private Location origin;

    public HallOfFameManager(JavaPlugin plugin, DonorManager donorManager, HallOfFameDao hofDao, HallOfFameMetaDao metaDao) {
        this.plugin = plugin;
        this.donorManager = donorManager;
        this.hofDao = hofDao;
        this.metaDao = metaDao;
        this.markerKey = new NamespacedKey(plugin, "hall_of_fame_owner");
    }

    public NamespacedKey markerKey() {
        return markerKey;
    }

    /** The monument lives in whichever world every player is guaranteed to pass through - the hub
     *  if one exists, otherwise the main server itself - never in a specific mini-server, so it
     *  stays a single global showcase. */
    public void initialize(World world) {
        Location saved = metaDao.loadOrigin();
        boolean firstBuild = saved == null;

        if (firstBuild) {
            origin = computeOrigin(world);
            buildPlatform(origin);
            metaDao.saveOrigin(origin);
            for (UUID uuid : donorManager.all().keySet()) {
                createStatueFor(uuid);
            }
            plugin.getLogger().info("[HallOfFame] Built monument at " + describeLocation(origin));
        } else {
            origin = saved;
        }
    }

    private Location computeOrigin(World world) {
        int groundY = world.getHighestBlockYAt(0, 0);
        return new Location(world, 0, groundY + 1, 0);
    }

    private String describeLocation(Location loc) {
        return loc.getWorld().getName() + " " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private void buildPlatform(Location origin) {
        World world = origin.getWorld();
        int halfWidth = (ROW_LENGTH * SLOT_SPACING_X) / 2 + PLATFORM_MARGIN;
        int depth = MAX_ROWS * ROW_SPACING_Z + PLATFORM_MARGIN;
        int baseY = origin.getBlockY() - 1;
        int roofY = baseY + 1 + PILLAR_HEIGHT;

        // Checkerboard floor, cleared headroom up to the roof, and the flat roof cap (with a
        // 1-block eave overhang) - built in one pass over the full footprint including the
        // overhang margin. The eave itself gets a sloped stair trim afterwards.
        for (int dx = -halfWidth - 1; dx <= halfWidth + 1; dx++) {
            for (int dz = -2; dz <= depth + 1; dz++) {
                int worldX = origin.getBlockX() + dx;
                int worldZ = origin.getBlockZ() + dz;
                if (dx >= -halfWidth && dx <= halfWidth && dz >= -1 && dz <= depth) {
                    Material floorMaterial = ((dx + dz) % 2 == 0) ? Material.SMOOTH_QUARTZ : Material.CHISELED_QUARTZ_BLOCK;
                    world.getBlockAt(worldX, baseY, worldZ).setType(floorMaterial);
                }
                for (int dy = 1; dy < roofY - baseY; dy++) {
                    world.getBlockAt(worldX, baseY + dy, worldZ).setType(Material.AIR);
                }
                world.getBlockAt(worldX, roofY, worldZ).setType(Material.SMOOTH_QUARTZ);
            }
        }

        // Sloped stair cornice around the two long sides and the back (the front stays flat and
        // open so it doesn't read as a wall over the entrance).
        for (int dz = -2; dz <= depth + 1; dz++) {
            placeStairs(world, origin.getBlockX() - halfWidth - 1, roofY, origin.getBlockZ() + dz, BlockFace.WEST);
            placeStairs(world, origin.getBlockX() + halfWidth + 1, roofY, origin.getBlockZ() + dz, BlockFace.EAST);
        }
        for (int dx = -halfWidth - 1; dx <= halfWidth + 1; dx++) {
            placeStairs(world, origin.getBlockX() + dx, roofY, origin.getBlockZ() + depth + 1, BlockFace.SOUTH);
        }

        // Low curb around the platform edge, left open at the front (dz = -1) as the entrance
        // that /spawn drops players straight into, with a potted flower every few blocks.
        for (int dx = -halfWidth; dx <= halfWidth; dx++) {
            world.getBlockAt(origin.getBlockX() + dx, baseY + 1, origin.getBlockZ() + depth)
                    .setType(Material.SMOOTH_QUARTZ_SLAB);
        }
        for (int dz = 0; dz <= depth; dz++) {
            world.getBlockAt(origin.getBlockX() - halfWidth, baseY + 1, origin.getBlockZ() + dz)
                    .setType(Material.SMOOTH_QUARTZ_SLAB);
            world.getBlockAt(origin.getBlockX() + halfWidth, baseY + 1, origin.getBlockZ() + dz)
                    .setType(Material.SMOOTH_QUARTZ_SLAB);
            if (dz % 6 == 2) {
                world.getBlockAt(origin.getBlockX() - halfWidth, baseY + 2, origin.getBlockZ() + dz).setType(Material.POTTED_ALLIUM);
                world.getBlockAt(origin.getBlockX() + halfWidth, baseY + 2, origin.getBlockZ() + dz).setType(Material.POTTED_ALLIUM);
            }
        }

        // Colonnade: pillars along both long edges, each capped with a warm lantern set into the
        // roof and a lit campfire at its base, so the hall reads as an actual open-air pavilion
        // lit by real firelight instead of a bare floor.
        for (int dz = -1; dz <= depth; dz += 6) {
            buildPillar(world, origin.getBlockX() - halfWidth, baseY, origin.getBlockZ() + dz, roofY, 1);
            buildPillar(world, origin.getBlockX() + halfWidth, baseY, origin.getBlockZ() + dz, roofY, -1);
        }
        buildPillar(world, origin.getBlockX() - halfWidth, baseY, origin.getBlockZ() + depth, roofY, 1);
        buildPillar(world, origin.getBlockX() + halfWidth, baseY, origin.getBlockZ() + depth, roofY, -1);

        // A lit campfire flanking each side of the entrance for a welcoming glow on arrival, and
        // a carpet aisle leading straight down the middle to the hologram.
        world.getBlockAt(origin.getBlockX() - halfWidth + 1, baseY + 1, origin.getBlockZ() - 1).setType(Material.CAMPFIRE);
        world.getBlockAt(origin.getBlockX() + halfWidth - 1, baseY + 1, origin.getBlockZ() - 1).setType(Material.CAMPFIRE);
        for (int dz = 0; dz < depth; dz++) {
            world.getBlockAt(origin.getBlockX(), baseY + 1, origin.getBlockZ() + dz).setType(Material.MAGENTA_CARPET);
        }

        spawnHologram(origin.clone().add(0, 4.5, depth - 1), "&e&l✦ 명예의 전당 ✦");
        spawnHologram(origin.clone().add(0, 3.9, depth - 1), "&7후원자들을 기립니다");
    }

    private void placeStairs(World world, int x, int y, int z, BlockFace facing) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(Material.SMOOTH_QUARTZ_STAIRS);
        if (block.getBlockData() instanceof Stairs stairs) {
            stairs.setFacing(facing);
            stairs.setHalf(Bisected.Half.BOTTOM);
            block.setBlockData(stairs);
        }
    }

    private void buildPillar(World world, int x, int baseY, int z, int roofY, int inwardDx) {
        for (int y = baseY + 1; y < roofY; y++) {
            world.getBlockAt(x, y, z).setType(Material.QUARTZ_PILLAR);
        }
        world.getBlockAt(x, roofY, z).setType(Material.LANTERN);
        world.getBlockAt(x + inwardDx, baseY + 1, z).setType(Material.CAMPFIRE);
    }

    private void spawnHologram(Location location, String text) {
        ArmorStand hologram = location.getWorld().spawn(location, ArmorStand.class);
        hologram.setVisible(false);
        hologram.setMarker(true);
        hologram.setGravity(false);
        hologram.setCustomNameVisible(true);
        hologram.customName(Msg.of(text));
        hologram.setInvulnerable(true);
        hologram.setPersistent(true);
    }

    private Location slotLocation(int slot) {
        int row = slot / ROW_LENGTH;
        int col = slot % ROW_LENGTH;
        int offsetFromCenter = col - (ROW_LENGTH - 1) / 2;
        double x = origin.getBlockX() + offsetFromCenter * SLOT_SPACING_X;
        double z = origin.getBlockZ() + row * ROW_SPACING_Z;
        return new Location(origin.getWorld(), x + 0.5, origin.getY(), z + 0.5);
    }

    private int findFreeSlot() {
        Set<Integer> used = new HashSet<>(hofDao.loadAll().values());
        int slot = 0;
        while (used.contains(slot)) {
            slot++;
        }
        return slot;
    }

    public void createStatueFor(UUID uuid) {
        if (origin == null) return;
        removeStatueEntitiesFor(uuid);

        Integer existingSlot = hofDao.loadAll().get(uuid);
        int slot = existingSlot != null ? existingSlot : findFreeSlot();
        hofDao.assignSlot(uuid, slot);

        Location statueLoc = slotLocation(slot);
        World world = statueLoc.getWorld();

        // Pedestal riser so the statue stands a block above the platform floor.
        world.getBlockAt(statueLoc.getBlockX(), statueLoc.getBlockY(), statueLoc.getBlockZ())
                .setType(Material.CHISELED_QUARTZ_BLOCK);

        Location armorStandLoc = statueLoc.clone().add(0, 1, 0);
        ArmorStand statue = world.spawn(armorStandLoc, ArmorStand.class);
        statue.setArms(true);
        statue.setBasePlate(true);
        statue.setInvulnerable(true);
        statue.setPersistent(true);
        statue.setCustomNameVisible(true);
        statue.getPersistentDataContainer().set(markerKey, PersistentDataType.STRING, uuid.toString());

        OfflinePlayer owner = Bukkit.getOfflinePlayer(uuid);
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(owner);
        head.setItemMeta(meta);
        statue.getEquipment().setHelmet(head);

        String name = owner.getName() != null ? owner.getName() : "???";
        statue.customName(Msg.of("&6&l★ &f" + name));
    }

    public void removeStatueFor(UUID uuid) {
        removeStatueEntitiesFor(uuid);
        hofDao.freeSlot(uuid);
    }

    private void removeStatueEntitiesFor(UUID uuid) {
        if (origin == null) return;
        String uuidString = uuid.toString();
        double radius = ROW_LENGTH * SLOT_SPACING_X + MAX_ROWS * ROW_SPACING_Z + 20;
        for (Entity entity : origin.getWorld().getNearbyEntities(origin, radius, radius, radius)) {
            if (entity.getType() != EntityType.ARMOR_STAND) continue;
            String owner = entity.getPersistentDataContainer().get(markerKey, PersistentDataType.STRING);
            if (uuidString.equals(owner)) {
                entity.remove();
            }
        }
    }

    public boolean isOwnedStatue(Entity entity) {
        if (entity.getType() != EntityType.ARMOR_STAND) return false;
        return entity.getPersistentDataContainer().has(markerKey, PersistentDataType.STRING);
    }
}
