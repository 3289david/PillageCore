package com.mingyu.pillage.admin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class StaffModeManager implements Listener {

    private final JavaPlugin plugin;
    private final Set<UUID> vanished = new HashSet<>();

    public StaffModeManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isVanished(UUID uuid) {
        return vanished.contains(uuid);
    }

    public boolean toggle(Player staff) {
        if (vanished.remove(staff.getUniqueId())) {
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                viewer.showPlayer(plugin, staff);
            }
            staff.setInvisible(false);
            return false;
        } else {
            vanished.add(staff.getUniqueId());
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (!viewer.hasPermission("pillage.admin")) {
                    viewer.hidePlayer(plugin, staff);
                }
            }
            staff.setInvisible(true);
            return true;
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (vanished.isEmpty()) return;
        Player joined = event.getPlayer();
        if (joined.hasPermission("pillage.admin")) return;
        for (UUID uuid : vanished) {
            Player staff = Bukkit.getPlayer(uuid);
            if (staff != null) {
                joined.hidePlayer(plugin, staff);
            }
        }
    }
}
