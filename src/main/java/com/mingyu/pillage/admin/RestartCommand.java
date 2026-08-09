package com.mingyu.pillage.admin;

import com.mingyu.pillage.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/** Kicks every player off the whole server (every instance, not just the one the admin is
 *  standing in - a restart is never scoped to one world) and restarts the process. Delegates the
 *  actual kick/world-save/process-restart mechanics to Spigot's own battle-tested
 *  {@code Bukkit.spigot().restart()} rather than reimplementing them - that call already saves
 *  every loaded world and either re-execs the configured restart script or shuts down cleanly if
 *  none is set (most hosting panels auto-restart the process on exit either way). This command
 *  just adds a short warning broadcast first so players aren't kicked with zero notice. */
public final class RestartCommand implements CommandExecutor {

    private final JavaPlugin plugin;

    public RestartCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    @SuppressWarnings("removal") // Bukkit.spigot().restart() has no replacement yet - it's the
                                  // only API that saves worlds and honors a configured restart
                                  // script instead of just exiting the JVM.
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("pillage.admin")) {
            sender.sendMessage(Msg.of("&c권한이 없습니다."));
            return true;
        }

        Bukkit.broadcast(Msg.of("&c서버가 5초 후 재시작됩니다. 잠시 후 다시 접속해 주세요."));
        plugin.getLogger().info("[Restart] " + sender.getName() + "이(가) 서버 재시작을 요청했습니다.");
        Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.spigot().restart(), 100L);
        return true;
    }
}
