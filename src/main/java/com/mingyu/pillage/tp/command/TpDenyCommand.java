package com.mingyu.pillage.tp.command;

import com.mingyu.pillage.tp.TpManager;
import com.mingyu.pillage.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class TpDenyCommand implements CommandExecutor {

    private final TpManager tpManager;

    public TpDenyCommand(TpManager tpManager) {
        this.tpManager = tpManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        tpManager.denyRequest(player.getUniqueId());
        player.sendMessage(Msg.of("&c텔레포트 요청을 거절했습니다."));
        return true;
    }
}
