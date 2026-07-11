package com.mingyu.pillage.tp.command;

import com.mingyu.pillage.tp.TpManager;
import com.mingyu.pillage.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class SetHomeCommand implements CommandExecutor {

    private static final int MAX_HOMES = 5;

    private final TpManager tpManager;

    public SetHomeCommand(TpManager tpManager) {
        this.tpManager = tpManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        String name = args.length > 0 ? args[0].toLowerCase() : "home";
        var homes = tpManager.homes(player.getUniqueId());
        if (!homes.containsKey(name) && homes.size() >= MAX_HOMES && !player.hasPermission("pillage.admin")) {
            player.sendMessage(Msg.of("&c홈은 최대 " + MAX_HOMES + "개까지 설정할 수 있습니다."));
            return true;
        }
        tpManager.setHome(player.getUniqueId(), name, player.getLocation());
        player.sendMessage(Msg.of("&a홈 '" + name + "' 을(를) 설정했습니다."));
        return true;
    }
}
