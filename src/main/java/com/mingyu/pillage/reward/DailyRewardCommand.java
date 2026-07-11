package com.mingyu.pillage.reward;

import com.mingyu.pillage.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class DailyRewardCommand implements CommandExecutor {

    private final RewardManager rewardManager;

    public DailyRewardCommand(RewardManager rewardManager) {
        this.rewardManager = rewardManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        var result = rewardManager.claimDaily(player);
        if (result == RewardManager.ClaimResult.ALREADY_CLAIMED) {
            long remaining = rewardManager.remainingDailyCooldownMillis(player);
            long hours = remaining / 3_600_000;
            long minutes = (remaining % 3_600_000) / 60_000;
            player.sendMessage(Msg.of("&c오늘의 보상을 이미 받았습니다. " + hours + "시간 " + minutes + "분 후 다시 시도하세요."));
        }
        return true;
    }
}
