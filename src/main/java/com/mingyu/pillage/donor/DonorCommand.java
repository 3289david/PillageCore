package com.mingyu.pillage.donor;

import com.mingyu.pillage.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class DonorCommand implements CommandExecutor {

    private final DonorManager donorManager;

    public DonorCommand(DonorManager donorManager) {
        this.donorManager = donorManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) {
            sender.sendMessage(Msg.of("&c사용법: /donor <add|remove> <player> [배지] &f또는 &c/donor list"));
            return true;
        }
        String sub = args[0].toLowerCase();

        if (sub.equals("list")) {
            sender.sendMessage(Msg.of("&6=== 후원자 목록 ==="));
            for (UUID uuid : donorManager.all().keySet()) {
                String name = Bukkit.getOfflinePlayer(uuid).getName();
                sender.sendMessage(Msg.of("&f- " + name + " &7(" + donorManager.badge(uuid) + ")"));
            }
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Msg.of("&c사용법: /donor <add|remove> <player> [배지] &f또는 &c/donor list"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        switch (sub) {
            case "add" -> {
                String badge = args.length >= 3 ? args[2] : null;
                UUID addedBy = sender instanceof Player p ? p.getUniqueId() : null;
                donorManager.add(target.getUniqueId(), badge, addedBy);
                sender.sendMessage(Msg.of("&a" + target.getName() + " 님을 후원자로 등록했습니다."));
                Player online = target.getPlayer();
                if (online != null) {
                    online.sendMessage(Msg.of("&d&l후원자 등급이 적용되었습니다! 감사합니다."));
                }
            }
            case "remove" -> {
                donorManager.remove(target.getUniqueId());
                sender.sendMessage(Msg.of("&e" + target.getName() + " 님의 후원자 등급을 해제했습니다."));
            }
            default -> sender.sendMessage(Msg.of("&c사용법: /donor <add|remove> <player> [배지] &f또는 &c/donor list"));
        }
        return true;
    }
}
