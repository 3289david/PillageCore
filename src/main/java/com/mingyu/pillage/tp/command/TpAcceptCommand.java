package com.mingyu.pillage.tp.command;

import com.mingyu.pillage.tp.TpManager;
import com.mingyu.pillage.tp.TpRequest;
import com.mingyu.pillage.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class TpAcceptCommand implements CommandExecutor {

    private final TpManager tpManager;

    public TpAcceptCommand(TpManager tpManager) {
        this.tpManager = tpManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        TpRequest request = tpManager.consumeRequest(player.getUniqueId());
        if (request == null) {
            player.sendMessage(Msg.of("&c받은 텔레포트 요청이 없습니다."));
            return true;
        }
        Player requester = Bukkit.getPlayer(request.requester());
        if (requester == null || !requester.isOnline()) {
            player.sendMessage(Msg.of("&c요청을 보낸 플레이어가 오프라인입니다."));
            return true;
        }
        player.sendMessage(Msg.of("&a" + requester.getName() + " 님의 텔레포트 요청을 수락했습니다."));
        requester.sendMessage(Msg.of("&a" + player.getName() + " 님이 텔레포트 요청을 수락했습니다."));
        tpManager.requestTeleport(requester, player.getLocation());
        return true;
    }
}
