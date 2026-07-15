package com.mingyu.pillage.reward;

import com.mingyu.pillage.data.dao.RewardDao;
import com.mingyu.pillage.data.dao.StatsDao;
import com.mingyu.pillage.economy.EconomyManager;
import com.mingyu.pillage.stats.PlaytimeTracker;
import com.mingyu.pillage.util.Msg;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.TimeUnit;

public final class RewardManager {

    private final JavaPlugin plugin;
    private final RewardDao rewardDao;
    private final StatsDao statsDao;
    private final EconomyManager economyManager;
    private final PlaytimeTracker playtimeTracker;
    private final long dailyRewardAmount;
    private final int playtimeMilestoneHours;
    private final long playtimeRewardAmount;

    public RewardManager(JavaPlugin plugin, RewardDao rewardDao, StatsDao statsDao, EconomyManager economyManager,
                          PlaytimeTracker playtimeTracker, long dailyRewardAmount,
                          int playtimeMilestoneHours, long playtimeRewardAmount) {
        this.plugin = plugin;
        this.rewardDao = rewardDao;
        this.statsDao = statsDao;
        this.economyManager = economyManager;
        this.playtimeTracker = playtimeTracker;
        this.dailyRewardAmount = dailyRewardAmount;
        this.playtimeMilestoneHours = playtimeMilestoneHours;
        this.playtimeRewardAmount = playtimeRewardAmount;
    }

    public void startPlaytimeCheck() {
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
        var leftover = player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, (int) dailyRewardAmount));
        // Inventory full: drop what didn't fit at the player's feet instead of silently discarding it.
        leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        player.sendMessage(Msg.of("&a일일 보상으로 &e스테이크 " + dailyRewardAmount + "개&a를 받았습니다!"));
        plugin.getLogger().info("[Reward] " + player.getName() + " claimed daily reward: " + dailyRewardAmount + "x COOKED_BEEF");
        return ClaimResult.OK;
    }

    public long remainingDailyCooldownMillis(Player player) {
        long last = rewardDao.lastDailyClaim(player.getUniqueId());
        long elapsed = System.currentTimeMillis() - last;
        return Math.max(0, TimeUnit.HOURS.toMillis(24) - elapsed);
    }

    /** Clears the daily-claim cooldown and playtime milestone progress so rewards can be tested immediately. */
    public void resetRewards(java.util.UUID uuid) {
        rewardDao.setLastDailyClaim(uuid, 0);
        rewardDao.setLastPlaytimeMilestoneHours(uuid, 0);
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
            plugin.getLogger().info("[Reward] " + player.getName() + " reached " + newMilestone
                    + "h playtime milestone: +" + playtimeRewardAmount + " emerald balance");
        }
    }
}
