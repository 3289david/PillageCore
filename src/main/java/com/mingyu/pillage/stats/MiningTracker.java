package com.mingyu.pillage.stats;

import com.mingyu.pillage.data.dao.StatsDao;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public final class MiningTracker implements Listener {

    private final StatsDao statsDao;

    public MiningTracker(StatsDao statsDao) {
        this.statsDao = statsDao;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) return;
        statsDao.addBlockMined(event.getPlayer().getUniqueId());
    }
}
