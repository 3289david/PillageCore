package com.mingyu.pillage.instance;

import com.mingyu.pillage.data.Database;
import com.mingyu.pillage.data.dao.InstanceDao;
import com.mingyu.pillage.data.dao.PlayerInstanceDao;
import com.mingyu.pillage.shop.ShopManager;
import com.mingyu.pillage.team.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;
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

    private final Map<String, InstanceInfo> instancesById = new LinkedHashMap<>();
    private final Map<String, String> instanceIdByWorldName = new LinkedHashMap<>();
    private String mainWorldName;

    public InstanceManager(JavaPlugin plugin, Database gameplayDb, InstanceDao instanceDao,
                            PlayerInstanceDao playerInstanceDao, TeamManager teamManager, ShopManager shopManager) {
        this.plugin = plugin;
        this.gameplayDb = gameplayDb;
        this.instanceDao = instanceDao;
        this.playerInstanceDao = playerInstanceDao;
        this.teamManager = teamManager;
        this.shopManager = shopManager;
    }

    public void initialize() {
        mainWorldName = plugin.getConfig().getString("spawn.world", "world");
        instanceIdByWorldName.put(mainWorldName, MAIN_ID);
        gameplayDb.open(MAIN_ID, "pillage.db");
        gameplayDb.use(MAIN_ID);
        teamManager.loadCurrentInstance();
        shopManager.loadCurrentInstance();

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

        for (InstanceInfo info : instanceDao.loadAll()) {
            loadInstanceWorld(info);
        }
        gameplayDb.use(MAIN_ID);
        plugin.getLogger().info("[Instance] 미니서버 " + instancesById.size() + "개를 불러왔습니다.");
    }

    public World hubWorld() {
        return Bukkit.getWorld(HUB_WORLD_NAME);
    }

    private void loadInstanceWorld(InstanceInfo info) {
        if (Bukkit.getWorld(info.worldName()) == null) {
            new WorldCreator(info.worldName()).environment(World.Environment.NORMAL).createWorld();
        }
        gameplayDb.open(info.id(), "instance-" + info.id() + ".db");
        gameplayDb.use(info.id());
        teamManager.loadCurrentInstance();
        shopManager.loadCurrentInstance();
        instancesById.put(info.id(), info);
        instanceIdByWorldName.put(info.worldName(), info.id());
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
        gameplayDb.use(id);
        teamManager.loadCurrentInstance();
        shopManager.loadCurrentInstance();

        InstanceInfo info = new InstanceInfo(id, name, creator.getUniqueId(), worldName, now);
        instancesById.put(id, info);
        instanceIdByWorldName.put(worldName, id);
        return info;
    }

    /** Kicks everyone inside back to the hub, then unloads and permanently deletes the world + its database. */
    public void delete(InstanceInfo info) {
        World world = Bukkit.getWorld(info.worldName());
        if (world != null) {
            for (Player player : world.getPlayers()) {
                teleportToHub(player);
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

    public void teleportToHub(Player player) {
        gameplayDb.use(HUB_ID);
        player.teleport(hubSpawn());
        playerInstanceDao.set(player.getUniqueId(), HUB_ID);
    }

    public void teleportToMain(Player player) {
        gameplayDb.use(MAIN_ID);
        player.teleport(mainSpawn());
        playerInstanceDao.set(player.getUniqueId(), MAIN_ID);
    }

    public void teleportToInstance(Player player, InstanceInfo info) {
        if (Bukkit.getWorld(info.worldName()) == null) {
            loadInstanceWorld(info);
        }
        World world = Bukkit.getWorld(info.worldName());
        gameplayDb.use(info.id());
        player.teleport(world.getSpawnLocation());
        playerInstanceDao.set(player.getUniqueId(), info.id());
    }

    /** First-ever join (or a deleted last instance) lands in the hub; otherwise resumes exactly where they left off. */
    public void sendToLastInstanceOrHub(Player player) {
        String lastId = playerInstanceDao.get(player.getUniqueId());
        if (lastId == null || HUB_ID.equals(lastId)) {
            teleportToHub(player);
            return;
        }
        if (MAIN_ID.equals(lastId)) {
            teleportToMain(player);
            return;
        }
        InstanceInfo info = instancesById.get(lastId);
        if (info == null) {
            teleportToHub(player);
            return;
        }
        teleportToInstance(player, info);
    }
}
