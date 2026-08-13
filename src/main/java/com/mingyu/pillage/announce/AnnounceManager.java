package com.mingyu.pillage.announce;

import com.mingyu.pillage.util.Msg;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.List;

/** Server-wide periodic announcement, broadcast to chat and shown as a big centered title, at an
 *  admin-configured interval - cycling through however many messages are configured one at a
 *  time. Config-driven (announce.enabled/interval-minutes/messages) rather than DB-backed since
 *  it's a single global setting, same as hub.enabled. */
public final class AnnounceManager {

    private final JavaPlugin plugin;
    private BukkitTask task;
    private int nextIndex = 0;

    public AnnounceManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** (Re)starts the periodic task from whatever is currently in config.yml - call this once at
     *  startup and again after any /announce command changes a setting. */
    public void start() {
        stop();
        if (!plugin.getConfig().getBoolean("announce.enabled", false)) return;
        long intervalMinutes = plugin.getConfig().getLong("announce.interval-minutes", 60);
        if (intervalMinutes <= 0) return;
        List<String> messages = plugin.getConfig().getStringList("announce.messages");
        if (messages.isEmpty()) return;

        long intervalTicks = intervalMinutes * 60L * 20L;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::broadcastNext, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    /** Broadcasts whichever message is next in rotation immediately, without waiting for the
     *  timer - used by /announce test so an admin can preview it. */
    public void broadcastNext() {
        List<String> messages = plugin.getConfig().getStringList("announce.messages");
        if (messages.isEmpty()) return;
        String raw = messages.get(nextIndex % messages.size());
        nextIndex++;

        Component chatMessage = Msg.of("&6&l[공지] &f" + raw);
        Title title = Title.title(
                Msg.of("&6&l공지"),
                Msg.of("&f" + raw),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(4), Duration.ofMillis(500))
        );
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(chatMessage);
            player.showTitle(title);
        }
    }
}
