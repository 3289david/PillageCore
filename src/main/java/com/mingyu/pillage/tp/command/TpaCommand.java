package com.mingyu.pillage.tp.command;

import com.mingyu.pillage.tp.TpManager;
import com.mingyu.pillage.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class TpaCommand implements CommandExecutor {

    private final TpManager tpManager;

    public TpaCommand(TpManager tpManager) {
        this.tpManager = tpManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(Msg.of("&c사용법: /tpa <player>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(Msg.of("&c해당 플레이어를 찾을 수 없습니다."));
            return true;
        }
        if (target.equals(player)) {
            player.sendMessage(Msg.of("&c자신에게 텔레포트 요청을 보낼 수 없습니다."));
            return true;
        }
        tpManager.sendRequest(player, target);
        return true;
    }
}
