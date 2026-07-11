package com.mingyu.pillage.qol;

import com.mingyu.pillage.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class ClockManager {

    private final Set<UUID> enabled = new HashSet<>();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public void start(JavaPlugin plugin) {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (enabled.isEmpty()) return;
            String time = LocalTime.now().format(formatter);
            for (UUID uuid : enabled) {
                Player player = plugin.getServer().getPlayer(uuid);
                if (player != null) {
                    player.sendActionBar(Msg.of("&b🕐 " + time));
                }
            }
        }, 20L, 20L);
    }

    public boolean toggle(UUID uuid) {
        if (!enabled.remove(uuid)) {
            enabled.add(uuid);
            return true;
        }
        return false;
    }

    public void remove(UUID uuid) {
        enabled.remove(uuid);
    }
}
