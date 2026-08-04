package com.mingyu.pillage.instance;

import com.mingyu.pillage.util.Msg;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;
import java.util.Map;

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

    /** Idempotent per role - only (re)spawns whichever of the three is currently missing, so this
     *  doubles as a watchdog: safe to call again after one NPC dies to some damage source that
     *  slips past {@code HubNpcListener}'s cancellation, without duplicating the survivors. */
    public void ensureSpawned(Location hubSpawn) {
        World world = hubSpawn.getWorld();
        Map<Role, Boolean> present = new EnumMap<>(Role.class);
        for (Role role : Role.values()) {
            present.put(role, false);
        }
        for (Villager villager : world.getEntitiesByClass(Villager.class)) {
            Role role = roleOf(villager);
            if (role != null) {
                present.put(role, true);
            }
        }

        if (!present.get(Role.MAIN)) {
            spawn(hubSpawn.clone().add(-3, 0, 2), Role.MAIN, "&c&l⚔ 전체 약탈 서버", Villager.Profession.WEAPONSMITH);
        }
        if (!present.get(Role.MINI_CREATE)) {
            spawn(hubSpawn.clone().add(0, 0, 2), Role.MINI_CREATE, "&a&l✦ 미니서버 만들기", Villager.Profession.CARTOGRAPHER);
        }
        if (!present.get(Role.MINI_JOIN)) {
            spawn(hubSpawn.clone().add(3, 0, 2), Role.MINI_JOIN, "&b&l☰ 미니게임 참가", Villager.Profession.LIBRARIAN);
        }
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
