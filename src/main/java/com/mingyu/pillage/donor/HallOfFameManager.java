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

    public void initialize() {
        Location saved = metaDao.loadOrigin();
        boolean firstBuild = saved == null;

        if (firstBuild) {
            origin = computeOriginFromSpawnConfig();
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

    private Location computeOriginFromSpawnConfig() {
        var config = plugin.getConfig();
        World world = Bukkit.getWorld(config.getString("spawn.world", "world"));
        if (world == null) {
            world = Bukkit.getWorlds().get(0);
        }
        double x = config.getDouble("spawn.x");
        double z = config.getDouble("spawn.z");
        int groundY = world.getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z));
        return new Location(world, Math.floor(x), groundY + 1, Math.floor(z));
    }

    private String describeLocation(Location loc) {
        return loc.getWorld().getName() + " " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private void buildPlatform(Location origin) {
        World world = origin.getWorld();
        int halfWidth = (ROW_LENGTH * SLOT_SPACING_X) / 2 + PLATFORM_MARGIN;
        int depth = MAX_ROWS * ROW_SPACING_Z + PLATFORM_MARGIN;
        int baseY = origin.getBlockY() - 1;

        for (int dx = -halfWidth; dx <= halfWidth; dx++) {
            for (int dz = -1; dz <= depth; dz++) {
                world.getBlockAt(origin.getBlockX() + dx, baseY, origin.getBlockZ() + dz)
                        .setType(Material.SMOOTH_QUARTZ);
                // Clear headroom above the floor so the monument doesn't get buried in existing terrain.
                for (int dy = 0; dy <= 3; dy++) {
                    world.getBlockAt(origin.getBlockX() + dx, baseY + 1 + dy, origin.getBlockZ() + dz)
                            .setType(Material.AIR);
                }
            }
        }

        spawnHologram(origin.clone().add(0, 2.5, depth - 1), "&e&l✦ 명예의 전당 ✦");
        spawnHologram(origin.clone().add(0, 1.9, depth - 1), "&7후원자들을 기립니다");
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

    /** Whether a location falls inside the monument's protected footprint (platform + statues). */
    public boolean isWithinHallOfFame(Location location) {
        if (origin == null || location.getWorld() != origin.getWorld()) return false;
        int halfWidth = (ROW_LENGTH * SLOT_SPACING_X) / 2 + PLATFORM_MARGIN;
        int depth = MAX_ROWS * ROW_SPACING_Z + PLATFORM_MARGIN;
        int dx = location.getBlockX() - origin.getBlockX();
        int dz = location.getBlockZ() - origin.getBlockZ();
        int dy = location.getBlockY() - (origin.getBlockY() - 1);
        return dx >= -halfWidth && dx <= halfWidth && dz >= -1 && dz <= depth && dy >= 0 && dy <= 4;
    }
}
