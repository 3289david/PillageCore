package com.mingyu.pillage.tp.command;

import com.mingyu.pillage.tp.TpManager;
import com.mingyu.pillage.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class DelHomeCommand implements CommandExecutor, TabCompleter {

    private final TpManager tpManager;

    public DelHomeCommand(TpManager tpManager) {
        this.tpManager = tpManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        String name = args.length > 0 ? args[0].toLowerCase() : "home";
        if (!tpManager.homes(player.getUniqueId()).containsKey(name)) {
            player.sendMessage(Msg.of("&c해당 이름의 홈이 없습니다."));
            return true;
        }
        tpManager.deleteHome(player.getUniqueId(), name);
        player.sendMessage(Msg.of("&a홈 '" + name + "' 을(를) 삭제했습니다."));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1 && sender instanceof Player player) {
            return tpManager.homes(player.getUniqueId()).keySet().stream().toList();
        }
        return List.of();
    }
}
