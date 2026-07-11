package com.mingyu.pillage.economy;

import com.mingyu.pillage.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class PayCommand implements CommandExecutor {

    private final EconomyManager economyManager;

    public PayCommand(EconomyManager economyManager) {
        this.economyManager = economyManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(Msg.of("&c사용법: /pay <player> <금액>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(Msg.of("&c해당 플레이어를 찾을 수 없습니다."));
            return true;
        }
        if (target.equals(player)) {
            player.sendMessage(Msg.of("&c자신에게 보낼 수 없습니다."));
            return true;
        }
        long amount;
        try {
            amount = Long.parseLong(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(Msg.of("&c숫자를 입력하세요."));
            return true;
        }
        if (amount <= 0) {
            player.sendMessage(Msg.of("&c0보다 큰 금액을 입력하세요."));
            return true;
        }
        if (!economyManager.pay(player.getUniqueId(), target.getUniqueId(), amount)) {
            player.sendMessage(Msg.of("&c잔액이 부족합니다."));
            return true;
        }
        player.sendMessage(Msg.of("&a" + target.getName() + " 님에게 " + amount + " 에메랄드를 보냈습니다."));
        target.sendMessage(Msg.of("&a" + player.getName() + " 님에게 " + amount + " 에메랄드를 받았습니다."));
        return true;
    }
}
