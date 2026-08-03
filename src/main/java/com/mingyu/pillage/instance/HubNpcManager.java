package com.mingyu.pillage.instance;

import com.mingyu.pillage.util.Msg;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/** Three stationary, AI-less villagers standing near the hub spawn that act as a visual menu for
 *  picking a destination - the same three choices "/main", "/mini create", and the mini-server
 *  browse GUI already offer as commands, just discoverable without typing anything. */
public final class HubNpcManager {

    public enum Role { MAIN, MINI_CREATE, MINI_JOIN }

    private final NamespacedKey roleKey;

    public HubNpcManager(JavaPlugin plugin) {
        this.roleKey = new NamespacedKey(plugin, "hub_npc_role");
    }

    public Role roleOf(Entity entity) {
        String value = entity.getPersistentDataContainer().get(roleKey, PersistentDataType.STRING);
        if (value == null) return null;
        try {
            return Role.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Idempotent - does nothing if the NPCs already exist (they're persistent, so normally only
     *  the very first server start needs to actually spawn them). */
    public void spawnIfMissing(Location hubSpawn) {
        boolean alreadySpawned = hubSpawn.getWorld().getEntitiesByClass(Villager.class).stream()
                .anyMatch(v -> roleOf(v) != null);
        if (alreadySpawned) return;

        spawn(hubSpawn.clone().add(-3, 0, 2), Role.MAIN, "&c&l⚔ 전체 약탈 서버", Villager.Profession.WEAPONSMITH);
        spawn(hubSpawn.clone().add(0, 0, 2), Role.MINI_CREATE, "&a&l✦ 미니서버 만들기", Villager.Profession.CARTOGRAPHER);
        spawn(hubSpawn.clone().add(3, 0, 2), Role.MINI_JOIN, "&b&l☰ 미니게임 참가", Villager.Profession.LIBRARIAN);
    }

    private void spawn(Location location, Role role, String name, Villager.Profession profession) {
        location.setYaw(180f);
        Villager villager = location.getWorld().spawn(location, Villager.class);
        villager.setAI(false);
        villager.setInvulnerable(true);
        villager.setPersistent(true);
        villager.setSilent(true);
        villager.setCollidable(false);
        villager.setProfession(profession);
        villager.setCustomNameVisible(true);
        villager.customName(Msg.of(name));
        villager.getPersistentDataContainer().set(roleKey, PersistentDataType.STRING, role.name());
    }
}
