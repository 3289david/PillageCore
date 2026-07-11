package com.mingyu.pillage.trade.command;

import com.mingyu.pillage.trade.TradeManager;
import com.mingyu.pillage.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class TradeAcceptCommand implements CommandExecutor {

    private final TradeManager tradeManager;

    public TradeAcceptCommand(TradeManager tradeManager) {
        this.tradeManager = tradeManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        var result = tradeManager.acceptRequest(player);
        switch (result) {
            case OK -> {
            }
            case NO_REQUEST -> player.sendMessage(Msg.of("&c받은 거래 요청이 없습니다."));
            case REQUESTER_OFFLINE -> player.sendMessage(Msg.of("&c요청을 보낸 플레이어가 오프라인입니다."));
            case ALREADY_TRADING -> player.sendMessage(Msg.of("&c이미 거래가 진행 중입니다."));
        }
        return true;
    }
}
