package com.mingyu.pillage.reward;

import com.mingyu.pillage.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class EventBoxCommand implements CommandExecutor {

    private final EventBoxManager eventBoxManager;

    public EventBoxCommand(EventBoxManager eventBoxManager) {
        this.eventBoxManager = eventBoxManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 2 || !args[0].equalsIgnoreCase("give")) {
            sender.sendMessage(Msg.of("&c사용법: /eventbox give <player> [수량]"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Msg.of("&c해당 플레이어를 찾을 수 없습니다."));
            return true;
        }
        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Math.max(1, Integer.parseInt(args[2]));
            } catch (NumberFormatException ignored) {
            }
        }
        var box = eventBoxManager.createBox();
        box.setAmount(amount);
        target.getInventory().addItem(box);
        target.sendMessage(Msg.of("&d이벤트 상자를 " + amount + "개 받았습니다!"));
        sender.sendMessage(Msg.of("&a" + target.getName() + " 님에게 이벤트 상자 " + amount + "개를 지급했습니다."));
        return true;
    }
}
