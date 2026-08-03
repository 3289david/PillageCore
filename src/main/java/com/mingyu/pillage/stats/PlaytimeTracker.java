package com.mingyu.pillage.stats;

import com.mingyu.pillage.data.dao.StatsDao;
import com.mingyu.pillage.instance.InstanceManager;
import org.bukkit.Bukkit;
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
    private final InstanceManager instanceManager;
    private final Map<UUID, Long> sessionStart = new HashMap<>();

    public PlaytimeTracker(JavaPlugin plugin, StatsDao statsDao, InstanceManager instanceManager) {
        this.plugin = plugin;
        this.statsDao = statsDao;
        this.instanceManager = instanceManager;
    }

    public void start() {
        // Autosave every 5 minutes so long sessions aren't lost on a crash. Each player may be in
        // a different instance, so the gameplay database has to be switched per-player here rather
        // than relying on whatever the last command/click happened to leave it pointed at.
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            for (Map.Entry<UUID, Long> entry : sessionStart.entrySet()) {
                long elapsed = (now - entry.getValue()) / 1000;
                if (elapsed > 0) {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player != null) {
                        instanceManager.enter(player);
                    }
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
            instanceManager.enter(player);
            statsDao.addPlaytime(player.getUniqueId(), elapsed);
        }
    }

    public void flushAll() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            flush(player);
        }
    }

    /** Seconds elapsed in the current session that haven't been saved to the database yet. */
    public long liveSessionSeconds(UUID uuid) {
        Long start = sessionStart.get(uuid);
        if (start == null) return 0;
        return (System.currentTimeMillis() - start) / 1000;
    }
}
