package com.mingyu.pillage.economy;

import com.mingyu.pillage.util.Msg;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class WithdrawCommand implements CommandExecutor {

    private final EconomyManager economyManager;

    public WithdrawCommand(EconomyManager economyManager) {
        this.economyManager = economyManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(Msg.of("&c사용법: /withdraw <수량>"));
            return true;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(Msg.of("&c숫자를 입력하세요."));
            return true;
        }
        if (amount <= 0) {
            player.sendMessage(Msg.of("&c0보다 큰 수량을 입력하세요."));
            return true;
        }
        if (!economyManager.withdraw(player.getUniqueId(), amount)) {
            player.sendMessage(Msg.of("&c잔액이 부족합니다."));
            return true;
        }
        var leftover = player.getInventory().addItem(new ItemStack(Material.EMERALD, amount));
        for (ItemStack extra : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), extra);
        }
        player.sendMessage(Msg.of("&a에메랄드 " + amount + "개를 출금했습니다."));
        return true;
    }
}
