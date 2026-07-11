package com.mingyu.pillage.admin;

import com.mingyu.pillage.data.dao.BanLogDao;
import com.mingyu.pillage.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.BanList;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public final class BanCommand implements CommandExecutor {

    private final BanLogDao banLogDao;

    public BanCommand(BanLogDao banLogDao) {
        this.banLogDao = banLogDao;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) {
            sender.sendMessage(Msg.of("&c사용법: /pillageban <player> [사유]"));
            return true;
        }
        String targetName = args[0];
        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "사유 없음";

        Bukkit.getBanList(BanList.Type.NAME).addBan(targetName, reason, (java.util.Date) null, sender.getName());

        Player online = Bukkit.getPlayerExact(targetName);
        if (online != null) {
            online.kick(Msg.of("&c차단되었습니다.\n&f사유: " + reason));
        }

        java.util.UUID staffUuid = sender instanceof Player p ? p.getUniqueId() : new java.util.UUID(0, 0);
        banLogDao.log(staffUuid, targetName, reason);

        sender.sendMessage(Msg.of("&a" + targetName + " 님을 차단했습니다. 사유: " + reason));
        return true;
    }
}
