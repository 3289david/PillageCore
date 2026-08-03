package com.mingyu.pillage.instance;

import com.mingyu.pillage.tp.TpManager;
import com.mingyu.pillage.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class MainServerCommand implements CommandExecutor {

    private final InstanceManager instanceManager;
    private final TpManager tpManager;

    public MainServerCommand(InstanceManager instanceManager, TpManager tpManager) {
        this.instanceManager = instanceManager;
        this.tpManager = tpManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        if (tpManager.isTeleportBlocked(player)) return true;
        instanceManager.teleportToMain(player);
        player.sendMessage(Msg.of("&a전체 약탈 서버로 이동했습니다."));
        return true;
    }
}
