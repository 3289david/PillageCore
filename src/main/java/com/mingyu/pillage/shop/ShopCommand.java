package com.mingyu.pillage.shop;

import com.mingyu.pillage.util.Msg;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class ShopCommand implements CommandExecutor {

    private final ShopManager shopManager;

    public ShopCommand(ShopManager shopManager) {
        this.shopManager = shopManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
                return true;
            }
            player.openInventory(new ShopMenu(shopManager).getInventory());
            return true;
        }

        if (!sender.hasPermission("pillage.admin")) {
            sender.sendMessage(Msg.of("&c권한이 없습니다."));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "add" -> add(sender, args);
            case "remove" -> remove(sender, args);
            case "list" -> list(sender);
            default -> sender.sendMessage(Msg.of("&c사용법: /shop add|remove|list"));
        }
        return true;
    }

    private void add(CommandSender sender, String[] args) {
        if (args.length < 5) {
            sender.sendMessage(Msg.of("&c사용법: /shop add <내는아이템> <내는수량> <받는아이템> <받는수량>"));
            return;
        }
        Material input = Material.matchMaterial(args[1]);
        Material output = Material.matchMaterial(args[3]);
        if (input == null || output == null) {
            sender.sendMessage(Msg.of("&c잘못된 아이템 이름입니다."));
            return;
        }
        int inputAmount, outputAmount;
        try {
            inputAmount = Integer.parseInt(args[2]);
            outputAmount = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Msg.of("&c수량은 숫자여야 합니다."));
            return;
        }
        if (inputAmount <= 0 || outputAmount <= 0) {
            sender.sendMessage(Msg.of("&c수량은 1 이상이어야 합니다."));
            return;
        }
        ShopOffer offer = shopManager.addOffer(input, inputAmount, output, outputAmount);
        sender.sendMessage(Msg.of("&a상점 항목 추가됨 (#" + offer.id() + "): " + input + " x" + inputAmount
                + " &f-> &a" + output + " x" + outputAmount));
    }

    private void remove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Msg.of("&c사용법: /shop remove <id>"));
            return;
        }
        int id;
        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Msg.of("&cID는 숫자여야 합니다."));
            return;
        }
        var result = shopManager.removeOffer(id);
        sender.sendMessage(result == ShopManager.RemoveResult.OK
                ? Msg.of("&a상점 항목 #" + id + "을(를) 삭제했습니다.")
                : Msg.of("&c해당 ID의 상점 항목을 찾을 수 없습니다."));
    }

    private void list(CommandSender sender) {
        if (shopManager.offers().isEmpty()) {
            sender.sendMessage(Msg.of("&7등록된 상점 항목이 없습니다."));
            return;
        }
        for (ShopOffer offer : shopManager.offers()) {
            sender.sendMessage(Msg.of("&7#" + offer.id() + " &f" + offer.inputMaterial() + " x" + offer.inputAmount()
                    + " &7-> &a" + offer.outputMaterial() + " x" + offer.outputAmount()));
        }
    }
}
