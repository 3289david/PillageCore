package com.mingyu.pillage.trade.command;

import com.mingyu.pillage.trade.TradeManager;
import com.mingyu.pillage.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class TradeDenyCommand implements CommandExecutor {

    private final TradeManager tradeManager;

    public TradeDenyCommand(TradeManager tradeManager) {
        this.tradeManager = tradeManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        tradeManager.denyRequest(player.getUniqueId());
        player.sendMessage(Msg.of("&c거래 요청을 거절했습니다."));
        return true;
    }
}
