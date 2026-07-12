package com.mingyu.pillage.reward;

import com.mingyu.pillage.data.dao.RewardDao;
import com.mingyu.pillage.data.dao.StatsDao;
import com.mingyu.pillage.economy.EconomyManager;
import com.mingyu.pillage.stats.PlaytimeTracker;
import com.mingyu.pillage.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.TimeUnit;

public final class RewardManager {

    private final RewardDao rewardDao;
    private final StatsDao statsDao;
    private final EconomyManager economyManager;
    private final PlaytimeTracker playtimeTracker;
    private final long dailyRewardAmount;
    private final int playtimeMilestoneHours;
    private final long playtimeRewardAmount;

    public RewardManager(RewardDao rewardDao, StatsDao statsDao, EconomyManager economyManager,
                          PlaytimeTracker playtimeTracker, long dailyRewardAmount,
                          int playtimeMilestoneHours, long playtimeRewardAmount) {
        this.rewardDao = rewardDao;
        this.statsDao = statsDao;
        this.economyManager = economyManager;
        this.playtimeTracker = playtimeTracker;
        this.dailyRewardAmount = dailyRewardAmount;
        this.playtimeMilestoneHours = playtimeMilestoneHours;
        this.playtimeRewardAmount = playtimeRewardAmount;
    }

    public void startPlaytimeCheck(JavaPlugin plugin) {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                checkPlaytimeMilestone(player);
            }
        }, 20L * 60, 20L * 60);
    }

    public enum ClaimResult { OK, ALREADY_CLAIMED }

    public ClaimResult claimDaily(Player player) {
        long last = rewardDao.lastDailyClaim(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (now - last < TimeUnit.HOURS.toMillis(24)) {
            return ClaimResult.ALREADY_CLAIMED;
        }
        rewardDao.setLastDailyClaim(player.getUniqueId(), now);
        economyManager.deposit(player.getUniqueId(), dailyRewardAmount);
        player.sendMessage(Msg.of("&a일일 보상으로 &e" + dailyRewardAmount + " 에메랄드&a를 받았습니다!"));
        return ClaimResult.OK;
    }

    public long remainingDailyCooldownMillis(Player player) {
        long last = rewardDao.lastDailyClaim(player.getUniqueId());
        long elapsed = System.currentTimeMillis() - last;
        return Math.max(0, TimeUnit.HOURS.toMillis(24) - elapsed);
    }

    private void checkPlaytimeMilestone(Player player) {
        if (playtimeMilestoneHours <= 0) return;
        long savedSeconds = statsDao.get(player.getUniqueId()).playtimeSeconds();
        long liveSeconds = playtimeTracker.liveSessionSeconds(player.getUniqueId());
        long totalHours = (savedSeconds + liveSeconds) / 3600;
        int lastMilestone = rewardDao.lastPlaytimeMilestoneHours(player.getUniqueId());
        if (totalHours >= lastMilestone + playtimeMilestoneHours) {
            int newMilestone = lastMilestone + playtimeMilestoneHours;
            rewardDao.setLastPlaytimeMilestoneHours(player.getUniqueId(), newMilestone);
            economyManager.deposit(player.getUniqueId(), playtimeRewardAmount);
            player.sendMessage(Msg.of("&a플레이타임 " + newMilestone + "시간 달성! &e" + playtimeRewardAmount + " 에메랄드&a를 받았습니다."));
        }
    }
}
