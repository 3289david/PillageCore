package com.mingyu.pillage.tp.command;

import com.mingyu.pillage.data.dao.HomeSettingsDao;
import com.mingyu.pillage.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/** Admin on/off switch for /sethome, /home, /delhome - scoped to whichever instance the admin is
 *  currently standing in (each instance has its own gameplay database, so this only affects
 *  that one server). */
public final class HomeAdminCommand implements CommandExecutor {

    private final HomeSettingsDao homeSettingsDao;

    public HomeAdminCommand(HomeSettingsDao homeSettingsDao) {
        this.homeSettingsDao = homeSettingsDao;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("pillage.admin")) {
            sender.sendMessage(Msg.of("&c권한이 없습니다."));
            return true;
        }
        if (args.length < 1 || !(args[0].equalsIgnoreCase("on") || args[0].equalsIgnoreCase("off"))) {
            sender.sendMessage(Msg.of("&c사용법: /homeadmin <on|off> &7(현재 서버 기준, 현재: "
                    + (homeSettingsDao.isEnabled() ? "&aON" : "&cOFF") + "&7)"));
            return true;
        }
        boolean enabled = args[0].equalsIgnoreCase("on");
        homeSettingsDao.setEnabled(enabled);
        sender.sendMessage(enabled
                ? Msg.of("&a이 서버에서 홈 기능을 켰습니다.")
                : Msg.of("&c이 서버에서 홈 기능을 껐습니다. (/sethome, /home, /delhome 사용 불가)"));
        return true;
    }
}
