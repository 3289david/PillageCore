package com.mingyu.pillage;

import com.mingyu.pillage.admin.BanCommand;
import com.mingyu.pillage.admin.InspectCommand;
import com.mingyu.pillage.admin.InspectListener;
import com.mingyu.pillage.admin.LogsCommand;
import com.mingyu.pillage.admin.ReportCommand;
import com.mingyu.pillage.admin.StaffCommand;
import com.mingyu.pillage.admin.StaffModeManager;
import com.mingyu.pillage.anticheat.AnticheatManager;
import com.mingyu.pillage.anticheat.AutoClickCheck;
import com.mingyu.pillage.anticheat.CombatChecks;
import com.mingyu.pillage.anticheat.FastBreakCheck;
import com.mingyu.pillage.anticheat.MovementChecks;
import com.mingyu.pillage.anticheat.ScaffoldCheck;
import com.mingyu.pillage.chat.ChatManager;
import com.mingyu.pillage.chat.GlobalChatListener;
import com.mingyu.pillage.chat.MsgCommand;
import com.mingyu.pillage.chat.ReplyCommand;
import com.mingyu.pillage.data.Database;
import com.mingyu.pillage.data.dao.BanLogDao;
import com.mingyu.pillage.data.dao.DeathLocationDao;
import com.mingyu.pillage.data.dao.EconomyDao;
import com.mingyu.pillage.data.dao.HomeDao;
import com.mingyu.pillage.data.dao.KillLogDao;
import com.mingyu.pillage.data.dao.LastLocationDao;
import com.mingyu.pillage.data.dao.ReportLogDao;
import com.mingyu.pillage.data.dao.RewardDao;
import com.mingyu.pillage.data.dao.StatsDao;
import com.mingyu.pillage.data.dao.TeamDao;
import com.mingyu.pillage.data.dao.TpLogDao;
import com.mingyu.pillage.data.dao.ShopDao;
import com.mingyu.pillage.data.dao.TradeLogDao;
import com.mingyu.pillage.economy.BalanceCommand;
import com.mingyu.pillage.economy.DepositCommand;
import com.mingyu.pillage.economy.EconomyManager;
import com.mingyu.pillage.economy.PayCommand;
import com.mingyu.pillage.economy.WithdrawCommand;
import com.mingyu.pillage.help.PillageHelpCommand;
import com.mingyu.pillage.menu.MenuCommand;
import com.mingyu.pillage.menu.MenuListener;
import com.mingyu.pillage.menu.MenuService;
import com.mingyu.pillage.pvp.KillStreakManager;
import com.mingyu.pillage.pvp.PvpListener;
import com.mingyu.pillage.qol.ClockCommand;
import com.mingyu.pillage.qol.ClockManager;
import com.mingyu.pillage.qol.CoordsCommand;
import com.mingyu.pillage.qol.DeathCommand;
import com.mingyu.pillage.qol.PingCommand;
import com.mingyu.pillage.qol.TpsCommand;
import com.mingyu.pillage.raid.RaidListener;
import com.mingyu.pillage.raid.RaidManager;
import com.mingyu.pillage.reward.DailyRewardCommand;
import com.mingyu.pillage.reward.EventBoxCommand;
import com.mingyu.pillage.reward.EventBoxListener;
import com.mingyu.pillage.reward.EventBoxManager;
import com.mingyu.pillage.reward.RewardManager;
import com.mingyu.pillage.shop.ShopCommand;
import com.mingyu.pillage.shop.ShopManager;
import com.mingyu.pillage.stats.MiningTracker;
import com.mingyu.pillage.stats.PlaytimeTracker;
import com.mingyu.pillage.stats.StatsCommand;
import com.mingyu.pillage.team.FriendlyFireListener;
import com.mingyu.pillage.team.TeamChatListener;
import com.mingyu.pillage.team.TeamChatService;
import com.mingyu.pillage.team.TeamManager;
import com.mingyu.pillage.team.command.TeamChatCommand;
import com.mingyu.pillage.team.command.TeamCommand;
import com.mingyu.pillage.tp.SpawnService;
import com.mingyu.pillage.tp.TpManager;
import com.mingyu.pillage.tp.TpMoveListener;
import com.mingyu.pillage.tp.command.BackCommand;
import com.mingyu.pillage.tp.command.DelHomeCommand;
import com.mingyu.pillage.tp.command.HomeCommand;
import com.mingyu.pillage.tp.command.SetHomeCommand;
import com.mingyu.pillage.tp.command.SpawnCommand;
import com.mingyu.pillage.tp.command.TpAcceptCommand;
import com.mingyu.pillage.tp.command.TpDenyCommand;
import com.mingyu.pillage.tp.command.TpaCommand;
import com.mingyu.pillage.trade.TradeListener;
import com.mingyu.pillage.trade.TradeManager;
import com.mingyu.pillage.trade.command.TradeAcceptCommand;
import com.mingyu.pillage.trade.command.TradeCommand;
import com.mingyu.pillage.trade.command.TradeDenyCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;

public final class PillageCore extends JavaPlugin {

    private Database database;
    private TeamManager teamManager;
    private TpManager tpManager;
    private RaidManager raidManager;
    private TradeManager tradeManager;
    private AnticheatManager anticheatManager;
    private MenuService menuService;
    private PlaytimeTracker playtimeTracker;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        database = new Database(this);
        database.connect(getConfig().getString("database.file", "pillage.db"));

        TeamDao teamDao = new TeamDao(database);
        HomeDao homeDao = new HomeDao(database);
        LastLocationDao lastLocationDao = new LastLocationDao(database);
        TradeLogDao tradeLogDao = new TradeLogDao(database);
        KillLogDao killLogDao = new KillLogDao(database);
        ReportLogDao reportLogDao = new ReportLogDao(database);
        BanLogDao banLogDao = new BanLogDao(database);
        TpLogDao tpLogDao = new TpLogDao(database);
        StatsDao statsDao = new StatsDao(database);
        DeathLocationDao deathLocationDao = new DeathLocationDao(database);
        RewardDao rewardDao = new RewardDao(database);
        EconomyDao economyDao = new EconomyDao(database);
        ShopDao shopDao = new ShopDao(database);

        teamManager = new TeamManager(
                teamDao,
                getConfig().getInt("team.default-max-members", 6),
                getConfig().getInt("team.max-members-hard-cap", 12),
                getConfig().getBoolean("team.friendly-fire-default", false));
        teamManager.loadAll();

        tpManager = new TpManager(
                this, homeDao, lastLocationDao,
                getConfig().getInt("tp.countdown-seconds", 5),
                getConfig().getInt("tp.cooldown-seconds", 30),
                getConfig().getInt("tp.request-timeout-seconds", 60),
                getConfig().getBoolean("tp.cancel-on-move", true),
                getConfig().getDouble("tp.cancel-on-move-threshold", 0.3));

        raidManager = new RaidManager(
                this, teamManager,
                getConfig().getInt("raid.raid-duration-minutes", 15),
                getConfig().getString("raid.alert-message", "&c기지가 공격받고 있습니다! (%attacker%)"),
                getConfig().getInt("raid.win-kill-threshold", 3));

        tradeManager = new TradeManager(tradeLogDao);

        TeamChatService teamChatService = new TeamChatService();
        SpawnService spawnService = new SpawnService(this);

        anticheatManager = new AnticheatManager(
                getConfig().getInt("anticheat.alert-violations", 10),
                getConfig().getInt("anticheat.violation-decay-seconds", 30),
                getConfig().getBoolean("anticheat.punish.enabled", false),
                getConfig().getInt("anticheat.punish.kick-violations", 40));

        KillStreakManager killStreakManager = new KillStreakManager();

        playtimeTracker = new PlaytimeTracker(this, statsDao);
        playtimeTracker.start();

        ShopManager shopManager = new ShopManager(shopDao);
        shopManager.loadAll();

        EconomyManager economyManager = new EconomyManager(economyDao);
        RewardManager rewardManager = new RewardManager(
                rewardDao, statsDao, economyManager, playtimeTracker,
                getConfig().getLong("reward.daily-amount", 50),
                getConfig().getInt("reward.playtime-milestone-hours", 1),
                getConfig().getLong("reward.playtime-amount", 20));
        rewardManager.startPlaytimeCheck(this);

        EventBoxManager eventBoxManager = new EventBoxManager(
                this, economyManager,
                getConfig().getLong("reward.event-box-min-reward", 10),
                getConfig().getLong("reward.event-box-max-reward", 100));

        StaffModeManager staffModeManager = new StaffModeManager(this);

        ChatManager chatManager = new ChatManager(
                getConfig().getInt("chat.cooldown-seconds", 2),
                getConfig().getBoolean("chat.profanity-filter-enabled", false),
                Set.copyOf(getConfig().getStringList("chat.banned-words")));

        menuService = new MenuService(teamManager, tpManager, tradeManager, spawnService, teamChatService, statsDao);

        registerCommands(teamChatService, spawnService, killLogDao, reportLogDao, banLogDao, tpLogDao, tradeLogDao,
                statsDao, deathLocationDao, staffModeManager, economyManager, rewardManager, eventBoxManager,
                chatManager, shopManager);
        registerListeners(teamChatService, killLogDao, statsDao, deathLocationDao, killStreakManager,
                staffModeManager, eventBoxManager, chatManager);

        getLogger().info("PillageCore 가 활성화되었습니다.");
    }

    private void registerCommands(TeamChatService teamChatService, SpawnService spawnService,
                                   KillLogDao killLogDao, ReportLogDao reportLogDao, BanLogDao banLogDao,
                                   TpLogDao tpLogDao, TradeLogDao tradeLogDao, StatsDao statsDao,
                                   DeathLocationDao deathLocationDao, StaffModeManager staffModeManager,
                                   EconomyManager economyManager, RewardManager rewardManager,
                                   EventBoxManager eventBoxManager, ChatManager chatManager,
                                   ShopManager shopManager) {
        getCommand("team").setExecutor(new TeamCommand(teamManager, tpManager));
        getCommand("team").setTabCompleter((TeamCommand) getCommand("team").getExecutor());
        getCommand("tc").setExecutor(new TeamChatCommand(teamManager, teamChatService));

        getCommand("tpa").setExecutor(new TpaCommand(tpManager));
        getCommand("tpaccept").setExecutor(new TpAcceptCommand(tpManager));
        getCommand("tpdeny").setExecutor(new TpDenyCommand(tpManager));
        getCommand("back").setExecutor(new BackCommand(tpManager));
        getCommand("spawn").setExecutor(new SpawnCommand(tpManager, spawnService));

        HomeCommand homeCommand = new HomeCommand(tpManager);
        getCommand("home").setExecutor(homeCommand);
        getCommand("home").setTabCompleter(homeCommand);
        getCommand("sethome").setExecutor(new SetHomeCommand(tpManager));
        DelHomeCommand delHomeCommand = new DelHomeCommand(tpManager);
        getCommand("delhome").setExecutor(delHomeCommand);
        getCommand("delhome").setTabCompleter(delHomeCommand);

        getCommand("trade").setExecutor(new TradeCommand(tradeManager));
        getCommand("tradeaccept").setExecutor(new TradeAcceptCommand(tradeManager));
        getCommand("tradedeny").setExecutor(new TradeDenyCommand(tradeManager));

        getCommand("menu").setExecutor(new MenuCommand(menuService));
        getCommand("pillagehelp").setExecutor(new PillageHelpCommand());

        getCommand("stats").setExecutor(new StatsCommand(statsDao));
        getCommand("death").setExecutor(new DeathCommand(deathLocationDao, tpManager));
        getCommand("coords").setExecutor(new CoordsCommand(teamManager, teamChatService));
        getCommand("ping").setExecutor(new PingCommand());
        getCommand("tps").setExecutor(new TpsCommand());
        ClockManager clockManager = new ClockManager();
        clockManager.start(this);
        getCommand("clock").setExecutor(new ClockCommand(clockManager));

        getCommand("dailyreward").setExecutor(new DailyRewardCommand(rewardManager));
        getCommand("balance").setExecutor(new BalanceCommand(economyManager));
        getCommand("pay").setExecutor(new PayCommand(economyManager));
        getCommand("deposit").setExecutor(new DepositCommand(economyManager));
        getCommand("withdraw").setExecutor(new WithdrawCommand(economyManager));
        getCommand("eventbox").setExecutor(new EventBoxCommand(eventBoxManager));
        getCommand("shop").setExecutor(new ShopCommand(shopManager));

        getCommand("msg").setExecutor(new MsgCommand(chatManager));
        getCommand("r").setExecutor(new ReplyCommand(chatManager));

        getCommand("report").setExecutor(new ReportCommand(reportLogDao));
        getCommand("staff").setExecutor(new StaffCommand(staffModeManager));
        getCommand("inspect").setExecutor(new InspectCommand());
        getCommand("logs").setExecutor(new LogsCommand(killLogDao, banLogDao, tpLogDao, tradeLogDao));
        getCommand("pillageban").setExecutor(new BanCommand(banLogDao));
    }

    private void registerListeners(TeamChatService teamChatService, KillLogDao killLogDao, StatsDao statsDao,
                                    DeathLocationDao deathLocationDao, KillStreakManager killStreakManager,
                                    StaffModeManager staffModeManager,
                                    EventBoxManager eventBoxManager, ChatManager chatManager) {
        var pm = getServer().getPluginManager();
        pm.registerEvents(new FriendlyFireListener(teamManager), this);
        pm.registerEvents(new TeamChatListener(teamManager, teamChatService), this);
        pm.registerEvents(new RaidListener(raidManager, teamManager), this);
        pm.registerEvents(new TpMoveListener(tpManager), this);
        pm.registerEvents(new TradeListener(tradeManager), this);
        pm.registerEvents(new MenuListener(), this);
        pm.registerEvents(new PvpListener(killLogDao, statsDao, deathLocationDao, teamManager, raidManager,
                killStreakManager), this);
        pm.registerEvents(new MiningTracker(statsDao), this);
        pm.registerEvents(playtimeTracker, this);
        pm.registerEvents(staffModeManager, this);
        pm.registerEvents(new InspectListener(), this);
        pm.registerEvents(new EventBoxListener(eventBoxManager), this);
        pm.registerEvents(new GlobalChatListener(chatManager, teamManager), this);
        registerAnticheatListeners(pm);
    }

    private void registerAnticheatListeners(org.bukkit.plugin.PluginManager pm) {
        var config = getConfig();

        if (config.getBoolean("anticheat.killaura.enabled", true) || config.getBoolean("anticheat.reach.enabled", true)) {
            pm.registerEvents(new CombatChecks(
                    anticheatManager,
                    config.getBoolean("anticheat.killaura.enabled", true),
                    config.getDouble("anticheat.killaura.max-angle-degrees", 65),
                    config.getBoolean("anticheat.reach.enabled", true),
                    config.getDouble("anticheat.reach.max-distance", 4.2)), this);
        }

        if (config.getBoolean("anticheat.speed.enabled", true) || config.getBoolean("anticheat.fly.enabled", true)) {
            pm.registerEvents(new MovementChecks(
                    anticheatManager,
                    config.getBoolean("anticheat.speed.enabled", true),
                    config.getDouble("anticheat.speed.tolerance-multiplier", 1.7),
                    config.getBoolean("anticheat.fly.enabled", true),
                    config.getInt("anticheat.fly.grace-seconds", 5)), this);
        }

        if (config.getBoolean("anticheat.autoclick.enabled", true)) {
            pm.registerEvents(new AutoClickCheck(anticheatManager, config.getInt("anticheat.autoclick.max-cps", 20)), this);
        }

        if (config.getBoolean("anticheat.scaffold.enabled", true)) {
            pm.registerEvents(new ScaffoldCheck(anticheatManager, config.getDouble("anticheat.scaffold.max-angle-degrees", 75)), this);
        }

        if (config.getBoolean("anticheat.fastbreak.enabled", true)) {
            pm.registerEvents(new FastBreakCheck(
                    anticheatManager,
                    config.getLong("anticheat.fastbreak.min-millis-for-hard-blocks", 90),
                    config.getDouble("anticheat.fastbreak.min-hardness", 0.6)), this);
        }
    }

    @Override
    public void onDisable() {
        if (tradeManager != null) {
            tradeManager.cancelAll("서버가 종료되었습니다");
        }
        if (playtimeTracker != null) {
            playtimeTracker.flushAll();
        }
        if (database != null) {
            database.close();
        }
        getLogger().info("PillageCore 가 비활성화되었습니다.");
    }
}
