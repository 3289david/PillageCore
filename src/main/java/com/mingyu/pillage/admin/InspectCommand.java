package com.mingyu.pillage.admin;

import com.mingyu.pillage.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class InspectCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player staff)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        if (args.length < 1) {
            staff.sendMessage(Msg.of("&c사용법: /inspect <player>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            staff.sendMessage(Msg.of("&c해당 플레이어를 찾을 수 없습니다."));
            return true;
        }
        staff.openInventory(new InspectGui(target).getInventory());
        return true;
    }
}
