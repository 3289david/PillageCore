package com.mingyu.pillage.tp.command;

import com.mingyu.pillage.tp.TpManager;
import com.mingyu.pillage.util.Msg;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public final class HomeCommand implements CommandExecutor, TabCompleter {

    private final TpManager tpManager;

    public HomeCommand(TpManager tpManager) {
        this.tpManager = tpManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        Map<String, Location> homes = tpManager.homes(player.getUniqueId());
        String name = args.length > 0 ? args[0].toLowerCase() : "home";
        Location location = homes.get(name);
        if (location == null) {
            if (homes.isEmpty()) {
                player.sendMessage(Msg.of("&c설정된 홈이 없습니다. /sethome 으로 홈을 설정하세요."));
            } else {
                player.sendMessage(Msg.of("&c해당 이름의 홈이 없습니다. 보유한 홈: &f" + String.join(", ", homes.keySet())));
            }
            return true;
        }
        tpManager.requestTeleport(player, location);
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
