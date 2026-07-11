package com.mingyu.pillage.stats;

import com.mingyu.pillage.data.dao.StatsDao;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlaytimeTracker implements Listener {

    private final JavaPlugin plugin;
    private final StatsDao statsDao;
    private final Map<UUID, Long> sessionStart = new HashMap<>();

    public PlaytimeTracker(JavaPlugin plugin, StatsDao statsDao) {
        this.plugin = plugin;
        this.statsDao = statsDao;
    }

    public void start() {
        // Autosave every 5 minutes so long sessions aren't lost on a crash.
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            for (Map.Entry<UUID, Long> entry : sessionStart.entrySet()) {
                long elapsed = (now - entry.getValue()) / 1000;
                if (elapsed > 0) {
                    statsDao.addPlaytime(entry.getKey(), elapsed);
                    sessionStart.put(entry.getKey(), now);
                }
            }
        }, 20L * 60 * 5, 20L * 60 * 5);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        sessionStart.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        flush(event.getPlayer());
    }

    public void flush(Player player) {
        Long start = sessionStart.remove(player.getUniqueId());
        if (start == null) return;
        long elapsed = (System.currentTimeMillis() - start) / 1000;
        if (elapsed > 0) {
            statsDao.addPlaytime(player.getUniqueId(), elapsed);
        }
    }

    public void flushAll() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            flush(player);
        }
    }
}
