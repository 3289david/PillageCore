package com.mingyu.pillage.economy;

import com.mingyu.pillage.util.Msg;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class DepositCommand implements CommandExecutor {

    private final EconomyManager economyManager;

    public DepositCommand(EconomyManager economyManager) {
        this.economyManager = economyManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        int held = countEmeralds(player);
        if (held == 0) {
            player.sendMessage(Msg.of("&c인벤토리에 에메랄드가 없습니다."));
            return true;
        }
        int amount = held;
        if (args.length >= 1) {
            try {
                amount = Math.min(held, Integer.parseInt(args[0]));
            } catch (NumberFormatException e) {
                player.sendMessage(Msg.of("&c숫자를 입력하세요."));
                return true;
            }
        }
        if (amount <= 0) {
            player.sendMessage(Msg.of("&c0보다 큰 수량을 입력하세요."));
            return true;
        }
        removeEmeralds(player, amount);
        economyManager.deposit(player.getUniqueId(), amount);
        player.sendMessage(Msg.of("&a에메랄드 " + amount + "개를 입금했습니다. 잔액: " + economyManager.balance(player.getUniqueId())));
        return true;
    }

    private int countEmeralds(Player player) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.EMERALD) {
                total += item.getAmount();
            }
        }
        return total;
    }

    private void removeEmeralds(Player player, int amount) {
        player.getInventory().removeItem(new ItemStack(Material.EMERALD, amount));
    }
}
