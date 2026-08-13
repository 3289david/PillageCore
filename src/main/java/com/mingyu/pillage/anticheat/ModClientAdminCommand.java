package com.mingyu.pillage.anticheat;

import com.mingyu.pillage.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/** /modclient on|off - toggles anti-mod-client.enabled at runtime, no restart needed since
 *  {@link ModClientGuardListener} reads the config fresh on every join/channel-register check. */
public final class ModClientAdminCommand implements CommandExecutor {

    private final JavaPlugin plugin;

    public ModClientAdminCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("pillage.admin")) {
            sender.sendMessage(Msg.of("&c권한이 없습니다."));
            return true;
        }
        if (args.length < 1 || !(args[0].equalsIgnoreCase("on") || args[0].equalsIgnoreCase("off"))) {
            boolean enabled = plugin.getConfig().getBoolean("anti-mod-client.enabled", true);
            sender.sendMessage(Msg.of("&c사용법: /modclient <on|off> &7(현재: " + (enabled ? "&aon" : "&coff") + "&7)"));
            return true;
        }
        boolean enabled = args[0].equalsIgnoreCase("on");
        plugin.getConfig().set("anti-mod-client.enabled", enabled);
        plugin.saveConfig();
        sender.sendMessage(enabled
                ? Msg.of("&a모드 클라이언트 차단을 켰습니다.")
                : Msg.of("&c모드 클라이언트 차단을 껐습니다."));
        return true;
    }
}
