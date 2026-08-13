package com.mingyu.pillage;

import com.mingyu.pillage.admin.BackupCommand;
import com.mingyu.pillage.admin.RestartCommand;
import com.mingyu.pillage.admin.BanCommand;
import com.mingyu.pillage.admin.InspectCommand;
import com.mingyu.pillage.admin.InspectListener;
import com.mingyu.pillage.admin.LogsCommand;
import com.mingyu.pillage.admin.ReportCommand;
import com.mingyu.pillage.admin.StaffCommand;
import com.mingyu.pillage.admin.StaffModeManager;
import com.mingyu.pillage.announce.AnnounceCommand;
import com.mingyu.pillage.announce.AnnounceManager;
import com.mingyu.pillage.anticheat.AnticheatManager;
import com.mingyu.pillage.anticheat.ModClientAdminCommand;
import com.mingyu.pillage.anticheat.ModClientGuardListener;
import com.mingyu.pillage.anticheat.AutoClickCheck;
import com.mingyu.pillage.anticheat.CombatChecks;
import com.mingyu.pillage.anticheat.FastBreakCheck;
import com.mingyu.pillage.anticheat.MovementChecks;
import com.mingyu.pillage.anticheat.ScaffoldCheck;
import com.mingyu.pillage.chat.ChatManager;
import com.mingyu.pillage.chat.GlobalChatListener;
import com.mingyu.pillage.chat.MsgCommand;
import com.mingyu.pillage.chat.ReplyCommand;
import com.mingyu.pillage.combat.CombatBossBarManager;
import com.mingyu.pillage.combat.CombatTagListener;
import com.mingyu.pillage.combat.CombatTagManager;
import com.mingyu.pillage.data.Database;
import com.mingyu.pillage.data.dao.BanLogDao;
import com.mingyu.pillage.data.dao.DeathLocationDao;
import com.mingyu.pillage.data.dao.EconomyDao;
import com.mingyu.pillage.data.dao.HomeDao;
import com.mingyu.pillage.data.dao.HomeSettingsDao;
import com.mingyu.pillage.data.dao.InstanceDao;
import com.mingyu.pillage.data.dao.KillLogDao;
import com.mingyu.pillage.data.dao.LastLocationDao;
import com.mingyu.pillage.data.dao.PlayerInstanceDao;
import com.mingyu.pillage.data.dao.PlayerInventoryDao;
import com.mingyu.pillage.data.dao.PlayerPositionDao;
import com.mingyu.pillage.data.dao.PlayerVitalsDao;
import com.mingyu.pillage.data.dao.PlayerEffectDao;
import com.mingyu.pillage.data.dao.PlayerEnderChestDao;
import com.mingyu.pillage.data.dao.PlayerXpDao;
import com.mingyu.pillage.data.dao.PlayerGameModeDao;
import com.mingyu.pillage.data.dao.EventBoxClaimDao;
import com.mingyu.pillage.data.dao.EventBoxOpenDao;
import com.mingyu.pillage.data.dao.MiniServerSettingsDao;
import com.mingyu.pillage.data.dao.ReportLogDao;
import com.mingyu.pillage.data.dao.RewardDao;
import com.mingyu.pillage.data.dao.StatsDao;
import com.mingyu.pillage.data.dao.TeamDao;
import com.mingyu.pillage.data.dao.TpLogDao;
import com.mingyu.pillage.data.dao.ShopDao;
import com.mingyu.pillage.data.dao.DonorDao;
import com.mingyu.pillage.data.dao.DonorPetDao;
import com.mingyu.pillage.data.dao.HallOfFameDao;
import com.mingyu.pillage.data.dao.HallOfFameMetaDao;
import com.mingyu.pillage.data.dao.TradeLogDao;
import com.mingyu.pillage.donor.DonorCombatVisibilityManager;
import com.mingyu.pillage.donor.DonorCommand;
import com.mingyu.pillage.donor.DonorManager;
import com.mingyu.pillage.donor.DonorNametagManager;
import com.mingyu.pillage.donor.DonorParticleTask;
import com.mingyu.pillage.donor.DonorPerksListener;
import com.mingyu.pillage.donor.DonorPetManager;
import com.mingyu.pillage.donor.HallOfFameListener;
import com.mingyu.pillage.donor.HallOfFameCommand;
import com.mingyu.pillage.donor.HallOfFameManager;
import com.mingyu.pillage.donor.PetCommand;
import com.mingyu.pillage.donor.StatueCommand;
import com.mingyu.pillage.economy.BalanceCommand;
import com.mingyu.pillage.economy.DepositCommand;
import com.mingyu.pillage.economy.EconomyManager;
import com.mingyu.pillage.economy.PayCommand;
import com.mingyu.pillage.economy.WithdrawCommand;
import com.mingyu.pillage.help.PillageHelpCommand;
import com.mingyu.pillage.instance.HubBuildGuardListener;
import com.mingyu.pillage.instance.HubCommand;
import com.mingyu.pillage.instance.HubMobGuardListener;
import com.mingyu.pillage.instance.HubNpcListener;
import com.mingyu.pillage.instance.HubNpcManager;
import com.mingyu.pillage.instance.InstanceContextListener;
import com.mingyu.pillage.instance.InstanceJoinListener;
import com.mingyu.pillage.instance.InstanceManager;
import com.mingyu.pillage.instance.MainServerCommand;
import com.mingyu.pillage.instance.MiniServerCommand;
import com.mingyu.pillage.instance.PlayerInventoryManager;
import com.mingyu.pillage.instance.PlayerVitalsManager;
import com.mingyu.pillage.instance.PlayerEffectsManager;
import com.mingyu.pillage.instance.PlayerExtraStateManager;
import com.mingyu.pillage.menu.MenuCommand;
import com.mingyu.pillage.minigame.MinigameCommand;
import com.mingyu.pillage.minigame.MinigameListener;
import com.mingyu.pillage.minigame.MinigameManager;
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
import com.mingyu.pillage.tp.command.HomeAdminCommand;
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
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;

public final class PillageCore extends JavaPlugin {

    private Database gameplayDb;
    private Database globalDb;
    private InstanceManager instanceManager;
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

        // Two independent databases: "gameplay" is a pool of one fully-separate SQLite file per
        // instance (main server, hub, each mini-server) that InstanceManager switches between -
        // this is what makes a brand-new mini-server start with completely empty teams/economy/
        // stats/homes. "global" is a single always-on file for things that must survive an
        // instance switch instead of resetting with it: donor status, ban/report history, and
        // the instance registry itself.
        gameplayDb = new Database(this, Database.SchemaKind.GAMEPLAY);
        globalDb = new Database(this, Database.SchemaKind.GLOBAL);
        globalDb.connect("global.db");

        TeamDao teamDao = new TeamDao(gameplayDb);
        HomeDao homeDao = new HomeDao(gameplayDb);
        HomeSettingsDao homeSettingsDao = new HomeSettingsDao(gameplayDb);
        LastLocationDao lastLocationDao = new LastLocationDao(gameplayDb);
        TradeLogDao tradeLogDao = new TradeLogDao(gameplayDb);
        KillLogDao killLogDao = new KillLogDao(gameplayDb);
        TpLogDao tpLogDao = new TpLogDao(gameplayDb);
        StatsDao statsDao = new StatsDao(gameplayDb);
        DeathLocationDao deathLocationDao = new DeathLocationDao(gameplayDb);
        RewardDao rewardDao = new RewardDao(gameplayDb);
        EconomyDao economyDao = new EconomyDao(gameplayDb);
        ShopDao shopDao = new ShopDao(gameplayDb);
        PlayerInventoryDao playerInventoryDao = new PlayerInventoryDao(gameplayDb);
        PlayerPositionDao playerPositionDao = new PlayerPositionDao(gameplayDb);
        PlayerVitalsDao playerVitalsDao = new PlayerVitalsDao(gameplayDb);
        PlayerEffectDao playerEffectDao = new PlayerEffectDao(gameplayDb);
        PlayerEnderChestDao playerEnderChestDao = new PlayerEnderChestDao(gameplayDb);
        PlayerXpDao playerXpDao = new PlayerXpDao(gameplayDb);
        PlayerGameModeDao playerGameModeDao = new PlayerGameModeDao(gameplayDb);

        // These stay on the global database - donor perks and moderation history must not reset
        // just because someone created or joined a mini-server.
        ReportLogDao reportLogDao = new ReportLogDao(globalDb);
        BanLogDao banLogDao = new BanLogDao(globalDb);
        DonorDao donorDao = new DonorDao(globalDb);
        DonorPetDao donorPetDao = new DonorPetDao(globalDb);
        HallOfFameDao hallOfFameDao = new HallOfFameDao(globalDb);
        HallOfFameMetaDao hallOfFameMetaDao = new HallOfFameMetaDao(globalDb);
        InstanceDao instanceDao = new InstanceDao(globalDb);
        PlayerInstanceDao playerInstanceDao = new PlayerInstanceDao(globalDb);
        // Also global: a box granted to an offline (or different-instance) player must still be
        // there for them to claim with /eventbox get no matter where they log back in.
        EventBoxClaimDao eventBoxClaimDao = new EventBoxClaimDao(globalDb);
        EventBoxOpenDao eventBoxOpenDao = new EventBoxOpenDao(globalDb);
        MiniServerSettingsDao miniServerSettingsDao = new MiniServerSettingsDao(globalDb);

        teamManager = new TeamManager(
                teamDao, gameplayDb,
                getConfig().getInt("team.default-max-members", 6),
                getConfig().getInt("team.max-members-hard-cap", 12),
                getConfig().getBoolean("team.friendly-fire-default", false));

        tpManager = new TpManager(
                this, homeDao, lastLocationDao,
                getConfig().getInt("tp.countdown-seconds", 5),
                getConfig().getInt("tp.cooldown-seconds", 30),
                getConfig().getInt("tp.request-timeout-seconds", 60),
                getConfig().getBoolean("tp.cancel-on-move", true),
                getConfig().getDouble("tp.cancel-on-move-threshold", 0.3));

        raidManager = new RaidManager(
                this, teamManager, gameplayDb,
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
        CombatTagManager combatTagManager = new CombatTagManager(getConfig().getInt("combat.tag-duration-seconds", 30));
        tpManager.setCombatTagManager(combatTagManager);
        CombatBossBarManager combatBossBarManager = new CombatBossBarManager(combatTagManager);
        combatBossBarManager.start(this);

        DonorManager donorManager = new DonorManager(donorDao);
        DonorNametagManager donorNametagManager = new DonorNametagManager(donorManager);
        donorManager.loadAll();
        donorNametagManager.applyToOnlinePlayers(this);
        tpManager.setDonorManager(donorManager);
        new DonorParticleTask(donorManager, combatTagManager).start(this);

        DonorPetManager donorPetManager = new DonorPetManager(this, donorManager, donorPetDao);
        donorPetManager.start();
        HallOfFameManager hallOfFameManager = new HallOfFameManager(this, donorManager, hallOfFameDao, hallOfFameMetaDao);
        new DonorCombatVisibilityManager(donorManager, combatTagManager, donorNametagManager, donorPetManager).start(this);

        ShopManager shopManager = new ShopManager(shopDao, gameplayDb);
        PlayerInventoryManager playerInventoryManager = new PlayerInventoryManager(playerInventoryDao);
        PlayerVitalsManager playerVitalsManager = new PlayerVitalsManager(playerVitalsDao);
        PlayerEffectsManager playerEffectsManager = new PlayerEffectsManager(playerEffectDao);
        PlayerExtraStateManager playerExtraStateManager =
                new PlayerExtraStateManager(playerXpDao, playerEnderChestDao, playerGameModeDao);

        // Provisions the main server + hub + every existing mini-server (each its own world and
        // its own gameplay database file), and loads team/shop state for each of them.
        instanceManager = new InstanceManager(this, gameplayDb, instanceDao, playerInstanceDao, teamManager,
                shopManager, playerInventoryManager, playerPositionDao, playerVitalsManager, playerEffectsManager,
                playerExtraStateManager);
        instanceManager.initialize();
        tpManager.setInstanceManager(instanceManager);
        spawnService.applyMainWorldSpawn();
        // With no hub, the monument gets its own tiny dedicated world instead of the lobby -
        // never touching real main-server terrain or getting in the way of actual play.
        World hallOfFameWorld = instanceManager.isHubEnabled()
                ? instanceManager.hubWorld()
                : hallOfFameManager.ensureDedicatedWorld();
        hallOfFameManager.initialize(hallOfFameWorld);

        HubNpcManager hubNpcManager = new HubNpcManager(this);
        if (instanceManager.isHubEnabled()) {
            hubNpcManager.ensureSpawned(instanceManager.hubSpawn());
        }

        playtimeTracker = new PlaytimeTracker(this, statsDao, instanceManager);
        playtimeTracker.start();

        EconomyManager economyManager = new EconomyManager(economyDao);
        RewardManager rewardManager = new RewardManager(
                this, rewardDao, statsDao, economyManager, playtimeTracker, instanceManager,
                getConfig().getLong("reward.daily-amount", 50),
                getConfig().getInt("reward.playtime-milestone-hours", 1),
                getConfig().getLong("reward.playtime-amount", 20));
        rewardManager.startPlaytimeCheck();

        EventBoxManager eventBoxManager = new EventBoxManager(this, eventBoxOpenDao);

        MinigameManager minigameManager = new MinigameManager(this, economyManager);
        minigameManager.start();
        MinigameCommand minigameCommand = new MinigameCommand(minigameManager);
        getCommand("minigame").setExecutor(minigameCommand);
        getCommand("minigame").setTabCompleter(minigameCommand);
        getServer().getPluginManager().registerEvents(new MinigameListener(this, minigameManager), this);

        StaffModeManager staffModeManager = new StaffModeManager();

        AnnounceManager announceManager = new AnnounceManager(this);
        announceManager.start();
        AnnounceCommand announceCommand = new AnnounceCommand(this, announceManager);
        getCommand("announce").setExecutor(announceCommand);
        getCommand("announce").setTabCompleter(announceCommand);

        ChatManager chatManager = new ChatManager(
                getConfig().getInt("chat.cooldown-seconds", 2),
                getConfig().getBoolean("chat.profanity-filter-enabled", true),
                Set.copyOf(getConfig().getStringList("chat.banned-words")));

        menuService = new MenuService(teamManager, tpManager, tradeManager, spawnService, teamChatService, statsDao);

        registerCommands(teamChatService, spawnService, killLogDao, reportLogDao, banLogDao, tpLogDao, tradeLogDao,
                statsDao, deathLocationDao, staffModeManager, economyManager, rewardManager, eventBoxManager,
                eventBoxClaimDao, chatManager, shopManager, donorManager, donorNametagManager, donorPetManager,
                hallOfFameManager, homeSettingsDao, playerInventoryDao, miniServerSettingsDao);
        registerListeners(teamChatService, killLogDao, statsDao, deathLocationDao, killStreakManager,
                staffModeManager, eventBoxManager, chatManager, combatTagManager, combatBossBarManager, donorManager,
                donorNametagManager, donorPetManager, hallOfFameManager, hubNpcManager);

        getLogger().info("PillageCore 가 활성화되었습니다.");
    }

    private void registerCommands(TeamChatService teamChatService, SpawnService spawnService,
                                   KillLogDao killLogDao, ReportLogDao reportLogDao, BanLogDao banLogDao,
                                   TpLogDao tpLogDao, TradeLogDao tradeLogDao, StatsDao statsDao,
                                   DeathLocationDao deathLocationDao, StaffModeManager staffModeManager,
                                   EconomyManager economyManager, RewardManager rewardManager,
                                   EventBoxManager eventBoxManager, EventBoxClaimDao eventBoxClaimDao,
                                   ChatManager chatManager,
                                   ShopManager shopManager, DonorManager donorManager,
                                   DonorNametagManager donorNametagManager, DonorPetManager donorPetManager,
                                   HallOfFameManager hallOfFameManager, HomeSettingsDao homeSettingsDao,
                                   PlayerInventoryDao playerInventoryDao, MiniServerSettingsDao miniServerSettingsDao) {
        getCommand("team").setExecutor(new TeamCommand(teamManager, tpManager));
        getCommand("team").setTabCompleter((TeamCommand) getCommand("team").getExecutor());
        getCommand("tc").setExecutor(new TeamChatCommand(teamManager, teamChatService));

        getCommand("tpa").setExecutor(new TpaCommand(tpManager));
        getCommand("tpaccept").setExecutor(new TpAcceptCommand(tpManager));
        getCommand("tpdeny").setExecutor(new TpDenyCommand(tpManager));
        getCommand("back").setExecutor(new BackCommand(tpManager));
        getCommand("spawn").setExecutor(new SpawnCommand(tpManager, spawnService));

        HomeCommand homeCommand = new HomeCommand(tpManager, homeSettingsDao);
        getCommand("home").setExecutor(homeCommand);
        getCommand("home").setTabCompleter(homeCommand);
        getCommand("sethome").setExecutor(new SetHomeCommand(tpManager, homeSettingsDao));
        DelHomeCommand delHomeCommand = new DelHomeCommand(tpManager, homeSettingsDao);
        getCommand("delhome").setExecutor(delHomeCommand);
        getCommand("delhome").setTabCompleter(delHomeCommand);
        getCommand("homeadmin").setExecutor(new HomeAdminCommand(homeSettingsDao));
        getCommand("backup").setExecutor(new BackupCommand(this, instanceManager, gameplayDb, playerInventoryDao));
        getCommand("restart").setExecutor(new RestartCommand(this));
        getCommand("modclient").setExecutor(new ModClientAdminCommand(this));
        reclaimBuiltinCommand("restart");

        getCommand("trade").setExecutor(new TradeCommand(tradeManager));
        getCommand("tradeaccept").setExecutor(new TradeAcceptCommand(tradeManager));
        getCommand("tradedeny").setExecutor(new TradeDenyCommand(tradeManager));

        getCommand("menu").setExecutor(new MenuCommand(menuService));
        PillageHelpCommand pillageHelpCommand = new PillageHelpCommand();
        getCommand("pillagehelp").setExecutor(pillageHelpCommand);
        getCommand("help").setExecutor(pillageHelpCommand);

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
        getCommand("eventbox").setExecutor(new EventBoxCommand(eventBoxManager, eventBoxClaimDao));
        getCommand("shop").setExecutor(new ShopCommand(shopManager));

        getCommand("msg").setExecutor(new MsgCommand(chatManager));
        getCommand("r").setExecutor(new ReplyCommand(chatManager));

        getCommand("report").setExecutor(new ReportCommand(reportLogDao));
        getCommand("staff").setExecutor(new StaffCommand(staffModeManager));
        getCommand("inspect").setExecutor(new InspectCommand());
        getCommand("logs").setExecutor(new LogsCommand(killLogDao, banLogDao, tpLogDao, tradeLogDao));
        getCommand("pillageban").setExecutor(new BanCommand(banLogDao));

        getCommand("donor").setExecutor(new DonorCommand(donorManager, donorNametagManager, donorPetManager, hallOfFameManager));
        getCommand("statue").setExecutor(new StatueCommand(this, donorManager));
        getCommand("pet").setExecutor(new PetCommand(donorManager, donorPetManager));

        getCommand("hub").setExecutor(new HubCommand(instanceManager, tpManager, hallOfFameManager));
        getCommand("halloffame").setExecutor(new HallOfFameCommand(tpManager, hallOfFameManager));
        getCommand("main").setExecutor(new MainServerCommand(instanceManager, tpManager, spawnService));
        MiniServerCommand miniServerCommand = new MiniServerCommand(instanceManager, tpManager, miniServerSettingsDao);
        getCommand("mini").setExecutor(miniServerCommand);
        getCommand("mini").setTabCompleter(miniServerCommand);
    }

    /** Spigot registers its own built-in "restart" (and a few other) commands before any plugin
     *  loads, and Bukkit's CommandMap keeps whichever command registered a label FIRST as the
     *  plain, unprefixed owner - a plugin.yml command of the same name normally only ever becomes
     *  reachable as "pillagecore:restart", never plain "/restart". This reaches into the server's
     *  CommandMap after normal registration and forces the plain label back onto our own command,
     *  the same technique other plugins use to reclaim vanilla/built-in command names. */
    private void reclaimBuiltinCommand(String label) {
        try {
            java.lang.reflect.Field commandMapField = getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            org.bukkit.command.CommandMap commandMap = (org.bukkit.command.CommandMap) commandMapField.get(getServer());

            java.lang.reflect.Field knownCommandsField = org.bukkit.command.SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<String, org.bukkit.command.Command> knownCommands =
                    (java.util.Map<String, org.bukkit.command.Command>) knownCommandsField.get(commandMap);
            knownCommands.put(label, getCommand(label));
        } catch (ReflectiveOperationException | ClassCastException e) {
            getLogger().warning("[Command] 내장 \"" + label + "\" 명령어를 재정의하지 못했습니다: " + e.getMessage());
        }
    }

    private void registerListeners(TeamChatService teamChatService, KillLogDao killLogDao, StatsDao statsDao,
                                    DeathLocationDao deathLocationDao, KillStreakManager killStreakManager,
                                    StaffModeManager staffModeManager,
                                    EventBoxManager eventBoxManager, ChatManager chatManager,
                                    CombatTagManager combatTagManager, CombatBossBarManager combatBossBarManager,
                                    DonorManager donorManager,
                                    DonorNametagManager donorNametagManager, DonorPetManager donorPetManager,
                                    HallOfFameManager hallOfFameManager, HubNpcManager hubNpcManager) {
        var pm = getServer().getPluginManager();
        // Registered first, at LOWEST priority internally, so every other listener below sees the
        // correct "current instance" gameplay database already switched in before it runs.
        pm.registerEvents(new InstanceContextListener(instanceManager), this);
        pm.registerEvents(new InstanceJoinListener(instanceManager), this);
        pm.registerEvents(new HubMobGuardListener(instanceManager), this);
        pm.registerEvents(new HubBuildGuardListener(instanceManager, hallOfFameManager), this);
        pm.registerEvents(new HubNpcListener(this, hubNpcManager, instanceManager, tpManager), this);

        pm.registerEvents(new FriendlyFireListener(teamManager), this);
        pm.registerEvents(new TeamChatListener(teamManager, teamChatService), this);
        pm.registerEvents(new RaidListener(raidManager, teamManager), this);
        pm.registerEvents(new TpMoveListener(tpManager), this);
        pm.registerEvents(new TradeListener(tradeManager), this);
        pm.registerEvents(new MenuListener(), this);
        pm.registerEvents(new PvpListener(killLogDao, statsDao, deathLocationDao, teamManager, raidManager,
                killStreakManager, donorManager), this);
        pm.registerEvents(new MiningTracker(statsDao), this);
        pm.registerEvents(playtimeTracker, this);
        pm.registerEvents(new InspectListener(), this);
        pm.registerEvents(new ModClientGuardListener(this), this);
        pm.registerEvents(new EventBoxListener(eventBoxManager), this);
        pm.registerEvents(new GlobalChatListener(chatManager, teamManager, donorManager), this);
        pm.registerEvents(new CombatTagListener(combatTagManager), this);
        pm.registerEvents(combatBossBarManager, this);
        pm.registerEvents(new DonorPerksListener(this, donorManager, donorNametagManager, donorPetManager), this);
        pm.registerEvents(new HallOfFameListener(this, hallOfFameManager), this);
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
        if (gameplayDb != null) {
            gameplayDb.close();
        }
        if (globalDb != null) {
            globalDb.close();
        }
        getLogger().info("PillageCore 가 비활성화되었습니다.");
    }
}
