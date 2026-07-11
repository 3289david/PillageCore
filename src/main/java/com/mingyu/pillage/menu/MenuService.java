package com.mingyu.pillage.menu;

import com.mingyu.pillage.data.dao.StatsDao;
import com.mingyu.pillage.team.TeamChatService;
import com.mingyu.pillage.team.TeamManager;
import com.mingyu.pillage.tp.SpawnService;
import com.mingyu.pillage.tp.TpManager;
import com.mingyu.pillage.trade.TradeManager;
import org.bukkit.entity.Player;

public final class MenuService {

    private final TeamManager teamManager;
    private final TpManager tpManager;
    private final TradeManager tradeManager;
    private final SpawnService spawnService;
    private final TeamChatService teamChatService;
    private final StatsDao statsDao;

    public MenuService(TeamManager teamManager, TpManager tpManager, TradeManager tradeManager,
                        SpawnService spawnService, TeamChatService teamChatService, StatsDao statsDao) {
        this.teamManager = teamManager;
        this.tpManager = tpManager;
        this.tradeManager = tradeManager;
        this.spawnService = spawnService;
        this.teamChatService = teamChatService;
        this.statsDao = statsDao;
    }

    public StatsDao statsDao() {
        return statsDao;
    }

    public TeamManager teamManager() {
        return teamManager;
    }

    public TpManager tpManager() {
        return tpManager;
    }

    public TradeManager tradeManager() {
        return tradeManager;
    }

    public SpawnService spawnService() {
        return spawnService;
    }

    public TeamChatService teamChatService() {
        return teamChatService;
    }

    public void openMain(Player player) {
        player.openInventory(new MainMenu(this).getInventory());
    }

    public void openTeam(Player player) {
        player.openInventory(new TeamMenu(this, player).getInventory());
    }

    public void openTp(Player player) {
        player.openInventory(new TpMenu(this, player).getInventory());
    }

    public void openTrade(Player player) {
        player.openInventory(new TradeMenu(this, player).getInventory());
    }

    public void openSettings(Player player) {
        player.openInventory(new SettingsMenu(this, player).getInventory());
    }

    public void openStats(Player player) {
        player.openInventory(new StatsMenu(this, player).getInventory());
    }
}
