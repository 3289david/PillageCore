package com.mingyu.pillage.instance;

import com.mingyu.pillage.data.Database;
import com.mingyu.pillage.data.dao.InstanceDao;
import com.mingyu.pillage.data.dao.PlayerInstanceDao;
import com.mingyu.pillage.data.dao.PlayerPositionDao;
import com.mingyu.pillage.shop.ShopManager;
import com.mingyu.pillage.team.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Every "instance" (the original main server, the hub lobby, and each player-created
 * mini-server) is one Bukkit World paired with one fully independent gameplay SQLite file, so a
 * new mini-server starts with completely empty teams/economy/stats/homes - no data ever leaks
 * between instances. Donor status, bans, and the instance registry itself live in the plugin's
 * separate always-global database instead (see PillageCore's wiring), since those must survive
 * instance switches rather than reset with them.
 */
public final class InstanceManager {

    public static final String MAIN_ID = "main";
    public static final String HUB_ID = "hub";
    public static final String HUB_WORLD_NAME = "pillage_hub";

    private final JavaPlugin plugin;
    private final Database gameplayDb;
    private final InstanceDao instanceDao;
    private final PlayerInstanceDao playerInstanceDao;
    private final TeamManager teamManager;
    private final ShopManager shopManager;
    private final PlayerInventoryManager playerInventoryManager;
    private final PlayerPositionDao playerPositionDao;
    private final PlayerVitalsManager playerVitalsManager;
    private final PlayerEffectsManager playerEffectsManager;
    private final PlayerExtraStateManager playerExtraStateManager;

    private final Map<String, InstanceInfo> instancesById = new LinkedHashMap<>();
    private final Map<String, String> instanceIdByWorldName = new LinkedHashMap<>();
    private String mainWorldName;
    private boolean hubEnabled;

    public InstanceManager(JavaPlugin plugin, Database gameplayDb, InstanceDao instanceDao,
                            PlayerInstanceDao playerInstanceDao, TeamManager teamManager, ShopManager shopManager,
                            PlayerInventoryManager playerInventoryManager, PlayerPositionDao playerPositionDao,
                            PlayerVitalsManager playerVitalsManager, PlayerEffectsManager playerEffectsManager,
                            PlayerExtraStateManager playerExtraStateManager) {
        this.plugin = plugin;
        this.gameplayDb = gameplayDb;
        this.instanceDao = instanceDao;
        this.playerInstanceDao = playerInstanceDao;
        this.teamManager = teamManager;
        this.shopManager = shopManager;
        this.playerInventoryManager = playerInventoryManager;
        this.playerPositionDao = playerPositionDao;
        this.playerVitalsManager = playerVitalsManager;
        this.playerEffectsManager = playerEffectsManager;
        this.playerExtraStateManager = playerExtraStateManager;
    }

    public void initialize() {
        hubEnabled = plugin.getConfig().getBoolean("hub.enabled", true);

        mainWorldName = plugin.getConfig().getString("spawn.world", "world");
        instanceIdByWorldName.put(mainWorldName, MAIN_ID);
        gameplayDb.open(MAIN_ID, "pillage.db");
        gameplayDb.use(MAIN_ID);
        teamManager.loadCurrentInstance();
        shopManager.loadCurrentInstance();

        if (hubEnabled) {
            World hubWorld = Bukkit.getWorld(HUB_WORLD_NAME);
            if (hubWorld == null) {
                hubWorld = new WorldCreator(HUB_WORLD_NAME)
                        .type(WorldType.FLAT)
                        .environment(World.Environment.NORMAL)
                        .createWorld();
            }
            int groundY = hubWorld.getHighestBlockYAt(0, 0);
            hubWorld.setSpawnLocation(0, groundY + 1, 0);
            instanceIdByWorldName.put(HUB_WORLD_NAME, HUB_ID);
            gameplayDb.open(HUB_ID, "hub.db");
            gameplayDb.use(HUB_ID);
            teamManager.loadCurrentInstance();
            shopManager.loadCurrentInstance();

            // The hub is a lobby, not a place to fight monsters or wrangle animals: always daytime,
            // no natural mob spawning at all (HubMobGuardListener backs this up event-by-event too,
            // since setSpawnFlags alone doesn't stop every edge case like flat-world slimes).
            hubWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            hubWorld.setTime(6000);
            hubWorld.setSpawnFlags(false, false);
            hubWorld.getEntitiesByClass(Monster.class).forEach(org.bukkit.entity.Entity::remove);
            hubWorld.getEntitiesByClass(Slime.class).forEach(org.bukkit.entity.Entity::remove);
            hubWorld.getEntitiesByClass(Animals.class).forEach(org.bukkit.entity.Entity::remove);
        }

        for (InstanceInfo info : instanceDao.loadAll()) {
            loadInstanceWorld(info);
        }
        gameplayDb.use(MAIN_ID);
        plugin.getLogger().info("[Instance] 미니서버 " + instancesById.size() + "개를 불러왔습니다.");
    }

    public boolean isHubEnabled() {
        return hubEnabled;
    }

    public World hubWorld() {
        return Bukkit.getWorld(HUB_WORLD_NAME);
    }

    private void loadInstanceWorld(InstanceInfo info) {
        if (Bukkit.getWorld(info.worldName()) == null) {
            new WorldCreator(info.worldName()).environment(World.Environment.NORMAL).createWorld();
        }
        gameplayDb.open(info.id(), "instance-" + info.id() + ".db");
        loadTeamAndShopFor(info.id());
        instancesById.put(info.id(), info);
        instanceIdByWorldName.put(info.worldName(), info.id());
    }

    /** Loads team/shop state for one instance without disturbing whatever instance the gameplay
     *  database was already pointed at - callers like {@link #teleportToInstance} rely on
     *  "current" still meaning "the instance the player is switching FROM" once this returns. */
    private void loadTeamAndShopFor(String instanceId) {
        String previous = gameplayDb.currentInstance();
        gameplayDb.use(instanceId);
        teamManager.loadCurrentInstance();
        shopManager.loadCurrentInstance();
        if (previous != null) {
            gameplayDb.use(previous);
        }
    }

    public String resolveInstanceId(World world) {
        if (world == null) return MAIN_ID;
        return instanceIdByWorldName.getOrDefault(world.getName(), MAIN_ID);
    }

    /** Switches the active gameplay database to whichever instance this player is currently standing in. */
    public void enter(Player player) {
        gameplayDb.use(resolveInstanceId(player.getWorld()));
    }

    public void enter(World world) {
        gameplayDb.use(resolveInstanceId(world));
    }

    public boolean isHub(World world) {
        return HUB_ID.equals(resolveInstanceId(world));
    }

    public Location hubSpawn() {
        return hubWorld().getSpawnLocation();
    }

    public Location mainSpawn() {
        return Bukkit.getWorld(mainWorldName).getSpawnLocation();
    }

    public List<InstanceInfo> list() {
        return instancesById.values().stream()
                .sorted(Comparator.comparingLong(InstanceInfo::createdAt))
                .collect(Collectors.toList());
    }

    public InstanceInfo find(String id) {
        return instancesById.get(id);
    }

    public InstanceInfo findByName(String name) {
        return instancesById.values().stream()
                .filter(info -> info.name().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public boolean isOwner(Player player, InstanceInfo info) {
        return info.owner().equals(player.getUniqueId()) || player.hasPermission("pillage.admin");
    }

    /** Creates a brand-new mini-server world + its own empty gameplay database. The creator becomes its admin. */
    public InstanceInfo create(Player creator, String name) {
        if (findByName(name) != null) {
            throw new IllegalArgumentException("이미 같은 이름의 미니서버가 있습니다.");
        }
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String worldName = "pillage_mini_" + id;
        new WorldCreator(worldName).environment(World.Environment.NORMAL).createWorld();

        long now = System.currentTimeMillis();
        instanceDao.insert(id, name, creator.getUniqueId(), worldName, now);
        gameplayDb.open(id, "instance-" + id + ".db");
        loadTeamAndShopFor(id);

        InstanceInfo info = new InstanceInfo(id, name, creator.getUniqueId(), worldName, now);
        instancesById.put(id, info);
        instanceIdByWorldName.put(worldName, id);
        return info;
    }

    /** Kicks everyone inside back to the hub (or the main server, if running without one), then
     *  unloads and permanently deletes the world + its database. */
    public void delete(InstanceInfo info) {
        World world = Bukkit.getWorld(info.worldName());
        if (world != null) {
            for (Player player : world.getPlayers()) {
                sendToLobbyOrMain(player);
            }
        }
        gameplayDb.closeInstance(info.id());
        instanceDao.delete(info.id());
        instancesById.remove(info.id());
        instanceIdByWorldName.remove(info.worldName());

        if (world != null) {
            Bukkit.unloadWorld(world, false);
        }
        deleteWorldFolder(new File(Bukkit.getWorldContainer(), info.worldName()));
        new File(plugin.getDataFolder(), "instance-" + info.id() + ".db").delete();
    }

    private void deleteWorldFolder(File folder) {
        if (!folder.exists()) return;
        try (Stream<Path> paths = Files.walk(folder.toPath())) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException e) {
            plugin.getLogger().warning("월드 폴더 삭제 실패: " + folder + " (" + e.getMessage() + ")");
        }
    }

    /** Saves the inventory + exact position under whichever instance is currently active (the
     *  "from"), switches the database, restores whatever was last saved for the instance being
     *  entered - the exact spot they left it at, or {@code fallbackSpawn} the first time - then
     *  teleports and records the new instance as current. This is the only place instance state
     *  actually swaps - resuming the same instance you were already in (see
     *  {@link #sendToLastInstanceOrHub}) deliberately skips this so it doesn't churn the save
     *  for no reason (Bukkit already restored that exact position/inventory from disk untouched). */
    private void switchTo(Player player, String toInstanceId, Location fallbackSpawn) {
        playerInventoryManager.save(player);
        playerPositionDao.save(player);
        playerVitalsManager.save(player);
        playerEffectsManager.save(player);
        playerExtraStateManager.save(player);
        gameplayDb.use(toInstanceId);
        playerInventoryManager.restore(player);
        playerVitalsManager.restore(player);
        playerEffectsManager.restore(player);
        playerExtraStateManager.restore(player);
        Location destination = playerPositionDao.load(player.getUniqueId());
        // A saved position table only ever legitimately holds locations in that instance's own
        // world - if it doesn't match (a corrupted leftover row from an earlier bug, or the
        // instance's world having been recreated), ignore it rather than teleporting the player
        // into whatever world it happens to point at.
        if (destination == null || !destination.getWorld().equals(fallbackSpawn.getWorld())) {
            destination = fallbackSpawn;
        }
        player.teleport(destination);
        gameplayDb.use(toInstanceId);
        playerInstanceDao.set(player.getUniqueId(), toInstanceId);
    }

    public void teleportToHub(Player player) {
        switchTo(player, HUB_ID, hubSpawn());
    }

    public void teleportToMain(Player player) {
        switchTo(player, MAIN_ID, mainSpawn());
    }

    public void teleportToInstance(Player player, InstanceInfo info) {
        if (Bukkit.getWorld(info.worldName()) == null) {
            loadInstanceWorld(info);
        }
        switchTo(player, info.id(), Bukkit.getWorld(info.worldName()).getSpawnLocation());
    }

    /** First-ever join (or a deleted last instance) lands in the hub with a swapped-in inventory;
     *  otherwise this is a resume - Bukkit already restored the player's position and inventory
     *  from disk exactly as they left it, so all that's needed is pointing the gameplay database
     *  at the right instance - but only if the player is actually physically standing in that
     *  instance's world. If the recorded instance doesn't match reality (state can drift out of
     *  sync from a bug, or from editing the database by hand), this forces a real switchTo() to
     *  reconcile everything instead of silently trusting a stale record. */
    public void sendToLastInstanceOrHub(Player player) {
        String lastId = playerInstanceDao.get(player.getUniqueId());
        if (lastId == null) {
            sendToLobbyOrMain(player);
            return;
        }

        String actualId = resolveInstanceId(player.getWorld());
        if (HUB_ID.equals(lastId)) {
            if (hubEnabled && actualId.equals(HUB_ID)) {
                gameplayDb.use(HUB_ID);
            } else {
                // Either they need the usual resync, or the hub they last stood in has since been
                // turned off entirely (hub.enabled: false) - either way there's no hub to return
                // to, so this falls back the same way a deleted mini-server would.
                sendToLobbyOrMain(player);
            }
            return;
        }
        if (MAIN_ID.equals(lastId)) {
            if (actualId.equals(MAIN_ID)) {
                gameplayDb.use(MAIN_ID);
            } else {
                switchTo(player, MAIN_ID, mainSpawn());
            }
            return;
        }
        InstanceInfo info = instancesById.get(lastId);
        if (info == null) {
            // Their mini-server was deleted while they were offline - fall back to the lobby.
            sendToLobbyOrMain(player);
            return;
        }
        if (actualId.equals(lastId)) {
            gameplayDb.use(lastId);
        } else {
            switchTo(player, lastId, Bukkit.getWorld(info.worldName()).getSpawnLocation());
        }
    }

    /** Wherever "send them to the hub" was previously an unconditional fallback destination - a
     *  brand-new player's first spawn, a deleted mini-server's leftover occupants - this falls
     *  back to the main server instead when running with hub.enabled: false, since there's no
     *  lobby world to land them in. */
    public void sendToLobbyOrMain(Player player) {
        if (hubEnabled) {
            switchTo(player, HUB_ID, hubSpawn());
        } else {
            switchTo(player, MAIN_ID, mainSpawn());
        }
    }
}
