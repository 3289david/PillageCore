package com.mingyu.pillage.admin;

import com.mingyu.pillage.util.Msg;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/** Kicks every player off the whole server (all instances, not just the one the admin is
 *  standing in - a restart is never scoped to one world) and restarts the process, 5 minutes
 *  after being run: a chat warning every minute, then a big centered title countdown for the
 *  final 10 seconds, then anyone still connected gets force-kicked before the actual restart -
 *  nobody should be caught mid-action by surprise. Delegates the actual world-save/process-restart
 *  mechanics to Spigot's own battle-tested {@code Bukkit.spigot().restart()} rather than
 *  reimplementing them - that call already saves every loaded world and either re-execs the
 *  configured restart script or shuts down cleanly if none is set (most hosting panels
 *  auto-restart the process on exit either way). */
public final class RestartCommand implements CommandExecutor {

    private static final int TOTAL_SECONDS = 5 * 60;
    private static final int COUNTDOWN_SECONDS = 10;

    private final JavaPlugin plugin;
    private final AtomicBoolean restarting = new AtomicBoolean(false);

    public RestartCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("pillage.admin")) {
            sender.sendMessage(Msg.of("&c권한이 없습니다."));
            return true;
        }
        if (!restarting.compareAndSet(false, true)) {
            sender.sendMessage(Msg.of("&c이미 재시작이 예약되어 있습니다."));
            return true;
        }

        plugin.getLogger().info("[Restart] " + sender.getName() + "이(가) 5분 후 서버 재시작을 예약했습니다.");

        new BukkitRunnable() {
            int remaining = TOTAL_SECONDS;

            @Override
            @SuppressWarnings("removal") // Bukkit.spigot().restart() has no replacement yet - it's
                                          // the only API that saves worlds and honors a configured
                                          // restart script instead of just exiting the JVM.
            public void run() {
                if (remaining <= 0) {
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        online.kick(Msg.of("&c서버가 재시작되어 접속이 종료되었습니다."));
                    }
                    cancel();
                    Bukkit.spigot().restart();
                    return;
                }

                if (remaining > COUNTDOWN_SECONDS && remaining % 60 == 0) {
                    Bukkit.broadcast(Msg.of("&e서버가 " + (remaining / 60) + "분 후 재시작됩니다."));
                } else if (remaining <= COUNTDOWN_SECONDS) {
                    Title title = Title.title(
                            Msg.of("&c&l" + remaining),
                            Msg.of("&c초 후 서버가 재시작됩니다"),
                            Title.Times.times(Duration.ZERO, Duration.ofMillis(1100), Duration.ZERO)
                    );
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        online.showTitle(title);
                    }
                }

                remaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        return true;
    }
}
