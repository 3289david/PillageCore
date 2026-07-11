package com.mingyu.pillage.admin;

import com.mingyu.pillage.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class StaffCommand implements CommandExecutor {

    private final StaffModeManager staffModeManager;

    public StaffCommand(StaffModeManager staffModeManager) {
        this.staffModeManager = staffModeManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        boolean nowVanished = staffModeManager.toggle(player);
        player.sendMessage(nowVanished
                ? Msg.of("&8[관리자모드] &f투명화 상태로 전환되었습니다.")
                : Msg.of("&8[관리자모드] &f투명화가 해제되었습니다."));
        return true;
    }
}
