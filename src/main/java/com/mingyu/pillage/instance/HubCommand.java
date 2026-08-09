package com.mingyu.pillage.instance;

import com.mingyu.pillage.tp.TpManager;
import com.mingyu.pillage.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class HubCommand implements CommandExecutor {

    private final InstanceManager instanceManager;
    private final TpManager tpManager;

    public HubCommand(InstanceManager instanceManager, TpManager tpManager) {
        this.instanceManager = instanceManager;
        this.tpManager = tpManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        if (!instanceManager.isHubEnabled()) {
            player.sendMessage(Msg.of("&c이 서버는 허브 기능이 비활성화되어 있습니다. &7(&e/main&7, &e/mini&7 사용)"));
            return true;
        }
        if (tpManager.isTeleportBlocked(player)) return true;
        instanceManager.teleportToHub(player);
        player.sendMessage(Msg.of("&a허브로 이동했습니다. &7(전체 서버 / 미니서버 선택은 &e/main&7, &e/mini&7)"));
        return true;
    }
}
