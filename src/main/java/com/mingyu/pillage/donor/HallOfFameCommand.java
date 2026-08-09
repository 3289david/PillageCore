package com.mingyu.pillage.donor;

import com.mingyu.pillage.tp.TpManager;
import com.mingyu.pillage.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** Visits the monument - open to everyone, not just donors, since it's a showcase meant to be
 *  seen. Only meaningful when running without a hub (hub.enabled: false), where the monument
 *  lives in its own dedicated world instead of somewhere players already pass through; with a
 *  hub, {@code /hub} already gets you there. Goes through the normal teleport pipeline
 *  (countdown, cooldown, /back support) exactly like /spawn does. */
public final class HallOfFameCommand implements CommandExecutor {

    private final TpManager tpManager;
    private final HallOfFameManager hallOfFameManager;

    public HallOfFameCommand(TpManager tpManager, HallOfFameManager hallOfFameManager) {
        this.tpManager = tpManager;
        this.hallOfFameManager = hallOfFameManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        if (hallOfFameManager.origin() == null) {
            player.sendMessage(Msg.of("&c명예의 전당이 아직 준비되지 않았습니다."));
            return true;
        }
        tpManager.requestTeleport(player, hallOfFameManager.origin());
        return true;
    }
}
