package com.mingyu.pillage.donor;

import com.mingyu.pillage.data.dao.DonorPetDao;
import com.mingyu.pillage.util.Msg;
import org.bukkit.Location;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Cosmetic cat companion for donors. Follows its owner via vanilla tamed-cat AI within one
 *  world, and via {@link #start} - a periodic check that teleports it to the owner whenever it's
 *  in a different world or has fallen far behind - across instance switches (each instance is
 *  its own Bukkit world, so nothing else moves the pet along with a cross-world teleport). */
public final class DonorPetManager {

    private static final double MAX_DISTANCE_SQUARED = 20 * 20;

    private final JavaPlugin plugin;
    private final DonorManager donorManager;
    private final DonorPetDao petDao;
    private final Map<UUID, Cat> activePets = new HashMap<>();

    public DonorPetManager(JavaPlugin plugin, DonorManager donorManager, DonorPetDao petDao) {
        this.plugin = plugin;
        this.donorManager = donorManager;
        this.petDao = petDao;
    }

    /** Idempotent and self-healing: if a pet is already out there following its owner, does
     *  nothing; if the tracked one is stale (dead, or orphaned in a world the owner already left),
     *  cleans it up first. Either way this never results in more than one live cat per owner.
     *  Does nothing at all if the owner has turned their pet off with /pet off. */
    public void spawnFor(Player owner) {
        if (!donorManager.isDonor(owner.getUniqueId())) return;

        DonorPetDao.PetInfo info = petDao.get(owner.getUniqueId());
        if (info != null && !info.enabled()) return;

        Cat existing = activePets.get(owner.getUniqueId());
        if (existing != null) {
            if (!existing.isDead()) {
                return;
            }
            activePets.remove(owner.getUniqueId());
        }

        Location loc = owner.getLocation();
        Cat cat = loc.getWorld().spawn(loc, Cat.class);
        cat.setTamed(true);
        cat.setOwner(owner);
        cat.setInvulnerable(true);
        cat.setSilent(false);
        cat.setCollarColor(org.bukkit.DyeColor.PURPLE);
        try {
            cat.setCatType(Cat.Type.valueOf(info != null && info.variant() != null ? info.variant() : "TABBY"));
        } catch (IllegalArgumentException ignored) {
            cat.setCatType(Cat.Type.TABBY);
        }
        if (info != null && info.name() != null) {
            cat.customName(Msg.of("&d" + info.name()));
            cat.setCustomNameVisible(true);
        } else {
            cat.customName(Msg.of("&d" + owner.getName() + "의 고양이"));
            cat.setCustomNameVisible(true);
        }
        activePets.put(owner.getUniqueId(), cat);
    }

    /** Keeps each donor's pet in the same world (and roughly the same spot) as its owner, since
     *  switching instances teleports the player across worlds but never the pet on its own. */
    public void start() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Map.Entry<UUID, Cat> entry : new HashMap<>(activePets).entrySet()) {
                Player owner = plugin.getServer().getPlayer(entry.getKey());
                Cat cat = entry.getValue();
                if (owner == null || !owner.isOnline()) continue;
                if (cat.isDead()) {
                    activePets.remove(entry.getKey());
                    continue;
                }
                boolean sameWorld = cat.getWorld().equals(owner.getWorld());
                if (!sameWorld || cat.getLocation().distanceSquared(owner.getLocation()) > MAX_DISTANCE_SQUARED) {
                    cat.teleport(owner.getLocation());
                }
            }
        }, 20L, 20L);
    }

    public void despawnFor(UUID uuid) {
        Cat cat = activePets.remove(uuid);
        if (cat != null && !cat.isDead()) {
            cat.remove();
        }
    }

    /** Temporarily removes the pet without touching its saved customization (combat hide). */
    public void hideForCombat(UUID uuid) {
        despawnFor(uuid);
    }

    public void restoreAfterCombat(Player owner) {
        spawnFor(owner);
    }

    public boolean hasPet(UUID uuid) {
        return activePets.containsKey(uuid);
    }

    public void setName(Player owner, String name) {
        petDao.setName(owner.getUniqueId(), name);
        Cat cat = activePets.get(owner.getUniqueId());
        if (cat != null) {
            cat.customName(Msg.of("&d" + name));
            cat.setCustomNameVisible(true);
        }
    }

    public void setVariant(Player owner, Cat.Type type) {
        petDao.setVariant(owner.getUniqueId(), type.name());
        Cat cat = activePets.get(owner.getUniqueId());
        if (cat != null) {
            cat.setCatType(type);
        }
    }

    public boolean isEnabled(UUID uuid) {
        DonorPetDao.PetInfo info = petDao.get(uuid);
        return info == null || info.enabled();
    }

    /** Turns the pet on/off - persists across sessions and instance switches, independent of
     *  the temporary combat hide/restore. */
    public void setEnabled(Player owner, boolean enabled) {
        petDao.setEnabled(owner.getUniqueId(), enabled);
        if (enabled) {
            spawnFor(owner);
        } else {
            despawnFor(owner.getUniqueId());
        }
    }

    /** Called when a player stops being a donor - forgets customization entirely. */
    public void forget(UUID uuid) {
        despawnFor(uuid);
        petDao.remove(uuid);
    }
}
