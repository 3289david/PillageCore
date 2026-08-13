package com.mingyu.pillage.instance;

import com.mingyu.pillage.tp.SpawnService;
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
    private final SpawnService spawnService;

    public MainServerCommand(InstanceManager instanceManager, TpManager tpManager, SpawnService spawnService) {
        this.instanceManager = instanceManager;
        this.tpManager = tpManager;
        this.spawnService = spawnService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("reset")) {
            handleReset(sender, args);
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        if (tpManager.isTeleportBlocked(player)) return true;
        instanceManager.teleportToMain(player);
        player.sendMessage(Msg.of("&a전체 약탈 서버로 이동했습니다."));
        return true;
    }

    /** Wipes the main server's world and gameplay database only - every mini-server and the hub
     *  are untouched. Irreversible, so it requires an explicit "confirm" argument, matching
     *  /hub delete and /backup restore's pattern. */
    private void handleReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("pillage.admin")) {
            sender.sendMessage(Msg.of("&c권한이 없습니다."));
            return;
        }
        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            sender.sendMessage(Msg.of("&c경고: 전체 약탈 서버(메인)의 월드와 데이터를 영구적으로 초기화하며 되돌릴 수 없습니다."));
            sender.sendMessage(Msg.of("&c정말 실행하려면: &f/main reset confirm"));
            return;
        }
        instanceManager.resetMainServer();
        spawnService.applyMainWorldSpawn();
        sender.sendMessage(Msg.of("&a전체 약탈 서버를 초기화했습니다. (허브/미니서버는 영향 없음)"));
    }
}
