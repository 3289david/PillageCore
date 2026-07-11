package com.mingyu.pillage.tp.command;

import com.mingyu.pillage.tp.SpawnService;
import com.mingyu.pillage.tp.TpManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class SpawnCommand implements CommandExecutor {

    private final TpManager tpManager;
    private final SpawnService spawnService;

    public SpawnCommand(TpManager tpManager, SpawnService spawnService) {
        this.tpManager = tpManager;
        this.spawnService = spawnService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        tpManager.requestTeleport(player, spawnService.spawnLocation());
        return true;
    }
}
