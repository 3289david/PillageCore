package com.mingyu.pillage.qol;

import com.mingyu.pillage.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class PingCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length >= 1) {
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(Msg.of("&c해당 플레이어를 찾을 수 없습니다."));
                return true;
            }
            sender.sendMessage(Msg.of("&f" + target.getName() + " 님의 핑: &e" + target.getPing() + "ms"));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("콘솔에서는 /ping <player> 를 사용하세요.");
            return true;
        }
        player.sendMessage(Msg.of("&f내 핑: &e" + player.getPing() + "ms"));
        return true;
    }
}
