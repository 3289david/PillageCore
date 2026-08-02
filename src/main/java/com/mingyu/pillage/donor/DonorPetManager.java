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

/** Cosmetic cat companion for donors. Follows its owner via vanilla tamed-cat AI. */
public final class DonorPetManager {

    private final JavaPlugin plugin;
    private final DonorManager donorManager;
    private final DonorPetDao petDao;
    private final Map<UUID, Cat> activePets = new HashMap<>();

    public DonorPetManager(JavaPlugin plugin, DonorManager donorManager, DonorPetDao petDao) {
        this.plugin = plugin;
        this.donorManager = donorManager;
        this.petDao = petDao;
    }

    public void spawnFor(Player owner) {
        if (!donorManager.isDonor(owner.getUniqueId())) return;
        if (activePets.containsKey(owner.getUniqueId())) return;

        DonorPetDao.PetInfo info = petDao.get(owner.getUniqueId());
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

    /** Called when a player stops being a donor - forgets customization entirely. */
    public void forget(UUID uuid) {
        despawnFor(uuid);
        petDao.remove(uuid);
    }
}
