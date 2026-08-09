package com.mingyu.pillage.instance;

import com.mingyu.pillage.donor.HallOfFameManager;
import com.mingyu.pillage.tp.TpManager;
import com.mingyu.pillage.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class HubCommand implements CommandExecutor {

    private final InstanceManager instanceManager;
    private final TpManager tpManager;
    private final HallOfFameManager hallOfFameManager;

    public HubCommand(InstanceManager instanceManager, TpManager tpManager, HallOfFameManager hallOfFameManager) {
        this.instanceManager = instanceManager;
        this.tpManager = tpManager;
        this.hallOfFameManager = hallOfFameManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("delete")) {
            handleDelete(sender, args);
            return true;
        }

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

    /** Permanently deletes the hub world + its database and turns hub.enabled off in config.yml so
     *  it stays off after a restart too - the command-line equivalent of manually deleting
     *  pillage_hub/hub.db and editing config.yml by hand. Irreversible, so it requires an explicit
     *  "confirm" argument, matching /backup restore's pattern. */
    private void handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("pillage.admin")) {
            sender.sendMessage(Msg.of("&c권한이 없습니다."));
            return;
        }
        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            sender.sendMessage(Msg.of("&c경고: 허브 월드와 데이터를 영구적으로 삭제하며 되돌릴 수 없습니다."));
            sender.sendMessage(Msg.of("&c정말 실행하려면: &f/hub delete confirm"));
            return;
        }
        if (Bukkit.getWorld(InstanceManager.HUB_WORLD_NAME) == null && !instanceManager.isHubEnabled()) {
            sender.sendMessage(Msg.of("&c이미 허브가 없습니다."));
            return;
        }
        // The monument moves out to its own dedicated world first (if it was living in the hub) -
        // this has to happen before the hub world unloads below, since relocating needs to remove
        // the old armor stands/hologram there while the world is still loaded.
        boolean relocatedHallOfFame = hallOfFameManager.origin() != null
                && InstanceManager.HUB_WORLD_NAME.equals(hallOfFameManager.origin().getWorld().getName());
        if (relocatedHallOfFame) {
            hallOfFameManager.relocateToDedicatedWorld();
        }
        instanceManager.deleteHubWorldAndData();
        sender.sendMessage(Msg.of("&a허브 월드와 데이터를 삭제했습니다. config.yml의 hub.enabled 도 false 로 저장되어 재시작해도 다시 생기지 않습니다."));
        if (relocatedHallOfFame) {
            sender.sendMessage(Msg.of("&7명예의 전당은 전용 월드로 옮겨졌습니다. &e/halloffame &7으로 방문할 수 있습니다."));
        }
    }
}
