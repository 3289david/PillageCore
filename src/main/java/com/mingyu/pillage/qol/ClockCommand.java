package com.mingyu.pillage.qol;

import com.mingyu.pillage.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class ClockCommand implements CommandExecutor {

    private final ClockManager clockManager;

    public ClockCommand(ClockManager clockManager) {
        this.clockManager = clockManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        boolean nowOn = clockManager.toggle(player.getUniqueId());
        player.sendMessage(nowOn ? Msg.of("&a액션바 시계를 켰습니다.") : Msg.of("&e액션바 시계를 껐습니다."));
        return true;
    }
}
