package com.mingyu.pillage.admin;

import com.mingyu.pillage.data.Database;
import com.mingyu.pillage.data.dao.PlayerInventoryDao;
import com.mingyu.pillage.instance.InstanceManager;
import com.mingyu.pillage.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/** On-demand snapshot (and restore) of the current instance's world (chunks/terrain) and every
 *  online player's inventory in it - for an admin who wants a safety net before something risky,
 *  not an automatic/scheduled backup. Stored as a single compressed .zip (world files compress
 *  well, and raw uncompressed copies were eating hosting disk quotas fast) rather than a loose
 *  directory tree. Synchronous, so a large world means a brief lag spike - that's an inherent cost
 *  of copying live chunk files, the same tradeoff any backup plugin has. */
public final class BackupCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final InstanceManager instanceManager;
    private final Database gameplayDb;
    private final PlayerInventoryDao playerInventoryDao;

    public BackupCommand(JavaPlugin plugin, InstanceManager instanceManager, Database gameplayDb,
                          PlayerInventoryDao playerInventoryDao) {
        this.plugin = plugin;
        this.instanceManager = instanceManager;
        this.gameplayDb = gameplayDb;
        this.playerInventoryDao = playerInventoryDao;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("pillage.admin")) {
            sender.sendMessage(Msg.of("&c권한이 없습니다."));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("이 서버(월드)에 서 있는 상태에서 사용해야 하는 명령어라 콘솔에서는 쓸 수 없습니다.");
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("list")) {
            list(player);
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("restore")) {
            if (args.length < 2) {
                sender.sendMessage(Msg.of("&c사용법: /backup restore <백업이름> [confirm]"));
                return true;
            }
            restore(player, args[1], args.length >= 3 && args[2].equalsIgnoreCase("confirm"));
            return true;
        }

        create(player);
        return true;
    }

    private File backupsRoot() {
        return new File(plugin.getDataFolder(), "backups");
    }

    private void create(Player player) {
        World world = player.getWorld();
        String instanceId = instanceManager.resolveInstanceId(world);
        String timestamp = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        backupsRoot().mkdirs();
        File zipFile = new File(backupsRoot(), instanceId + "_" + timestamp + ".zip");

        player.sendMessage(Msg.of("&e백업을 시작합니다... (월드 크기에 따라 잠깐 서버가 멈출 수 있습니다)"));

        world.save();

        int savedInventories;
        try (FileOutputStream fileOut = new FileOutputStream(zipFile);
             ZipOutputStream zip = new ZipOutputStream(fileOut)) {
            zip.setLevel(Deflater.BEST_SPEED);
            zipDirectory(world.getWorldFolder().toPath(), "world", zip);
            savedInventories = 0;
            for (Player online : world.getPlayers()) {
                if (zipInventory(online, zip)) {
                    savedInventories++;
                }
            }
        } catch (IOException e) {
            zipFile.delete();
            player.sendMessage(Msg.of("&c월드 백업에 실패했습니다: " + e.getMessage()));
            plugin.getLogger().warning("[Backup] world backup failed: " + e.getMessage());
            return;
        }

        String sizeText = String.format("%.1fMB", zipFile.length() / 1024.0 / 1024.0);
        player.sendMessage(Msg.of("&a백업 완료! &7(" + zipFile.getName() + ", " + sizeText
                + ", 인벤토리 " + savedInventories + "명 저장됨)"));
        plugin.getLogger().info("[Backup] " + player.getName() + " backed up instance " + instanceId + " to " + zipFile.getName());
    }

    private void list(Player player) {
        String instanceId = instanceManager.resolveInstanceId(player.getWorld());
        File[] entries = backupsRoot().listFiles();
        if (entries == null) entries = new File[0];
        String prefix = instanceId + "_";
        String[] names = Arrays.stream(entries)
                .filter(File::isFile)
                .map(File::getName)
                .filter(name -> name.endsWith(".zip"))
                .map(name -> name.substring(0, name.length() - 4))
                .filter(name -> name.startsWith(prefix))
                .sorted(Comparator.reverseOrder())
                .toArray(String[]::new);

        if (names.length == 0) {
            player.sendMessage(Msg.of("&7이 서버(현재 인스턴스)의 백업이 없습니다."));
            return;
        }
        player.sendMessage(Msg.of("&e이 서버의 백업 목록 (" + names.length + "개):"));
        for (String name : names) {
            player.sendMessage(Msg.of("&7- " + name));
        }
    }

    /** Overwrites the current instance's live world with a backup, and queues each backed-up
     *  player's inventory to be handed back the next time they enter this instance (via the
     *  existing switchTo() save/restore flow - no need to special-case "currently online"). This
     *  destroys whatever is currently in the world, so it requires an explicit "confirm" argument
     *  and always kicks everyone present out to the hub (or main, if running without one) first. */
    private void restore(Player admin, String backupName, boolean confirmed) {
        String instanceId = instanceManager.resolveInstanceId(admin.getWorld());
        String zipName = backupName.endsWith(".zip") ? backupName : backupName + ".zip";
        String displayName = zipName.substring(0, zipName.length() - 4);
        File zipFile = new File(backupsRoot(), zipName);

        if (!zipFile.isFile()) {
            admin.sendMessage(Msg.of("&c그런 백업을 찾을 수 없습니다: " + displayName));
            return;
        }
        if (!displayName.startsWith(instanceId + "_")) {
            admin.sendMessage(Msg.of("&c이 백업은 현재 서버(인스턴스)의 것이 아닙니다. 복원하려는 서버에 직접 들어가서 실행하세요."));
            return;
        }
        if (!confirmed) {
            admin.sendMessage(Msg.of("&c경고: 이 작업은 현재 서버의 월드를 백업 시점으로 되돌리며 되돌릴 수 없습니다."));
            admin.sendMessage(Msg.of("&c정말 실행하려면: &f/backup restore " + displayName + " confirm"));
            return;
        }

        World world = admin.getWorld();
        String worldName = world.getName();

        boolean hasHub = instanceManager.isHubEnabled();
        admin.sendMessage(Msg.of("&e복원을 시작합니다... 이 서버에 있던 플레이어는 모두 " + (hasHub ? "허브" : "메인 서버") + "로 이동합니다."));
        for (Player online : world.getPlayers()) {
            online.sendMessage(Msg.of("&e관리자가 이 서버를 백업 시점으로 복원하고 있어 " + (hasHub ? "허브" : "메인 서버") + "로 이동합니다."));
            instanceManager.sendToLobbyOrMain(online);
        }

        if (!Bukkit.unloadWorld(world, false)) {
            admin.sendMessage(Msg.of("&c월드를 언로드하지 못해 복원을 중단했습니다."));
            return;
        }

        File liveWorldFolder = new File(Bukkit.getWorldContainer(), worldName);
        deleteDirectory(liveWorldFolder);

        int restoredInventories;
        try (ZipFile zip = new ZipFile(zipFile)) {
            restoredInventories = extractBackup(zip, liveWorldFolder);
        } catch (IOException e) {
            admin.sendMessage(Msg.of("&c월드 복원 중 오류: " + e.getMessage() + " (월드가 손상되었을 수 있습니다!)"));
            plugin.getLogger().severe("[Backup] restore extract failed for " + worldName + ": " + e.getMessage());
            new WorldCreator(worldName).createWorld();
            return;
        }

        new WorldCreator(worldName).createWorld();

        // The world reload doesn't touch the gameplay database at all - re-assert the instance
        // pointer anyway since sendToLobbyOrMain() above may have switched it away.
        gameplayDb.use(instanceId);

        admin.sendMessage(Msg.of("&a복원 완료! &7(" + displayName + ", 인벤토리 " + restoredInventories
                + "명분 예약됨 - 해당 플레이어가 이 서버에 다음에 들어올 때 적용됩니다)"));
        plugin.getLogger().info("[Backup] " + admin.getName() + " restored instance " + instanceId + " from " + displayName);
    }

    private int extractBackup(ZipFile zip, File liveWorldFolder) throws IOException {
        int restoredInventories = 0;
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory()) continue;

            if (entry.getName().startsWith("world/")) {
                String relative = entry.getName().substring("world/".length());
                File dest = new File(liveWorldFolder, relative);
                Files.createDirectories(dest.getParentFile().toPath());
                try (InputStream in = zip.getInputStream(entry);
                     FileOutputStream out = new FileOutputStream(dest)) {
                    in.transferTo(out);
                }
            } else if (entry.getName().startsWith("inventories/")) {
                String fileName = entry.getName().substring("inventories/".length());
                try (InputStream in = zip.getInputStream(entry)) {
                    if (restoreInventory(fileName, in)) {
                        restoredInventories++;
                    }
                }
            }
        }
        return restoredInventories;
    }

    private boolean restoreInventory(String fileName, InputStream rawIn) {
        int underscore = fileName.indexOf('_');
        if (underscore <= 0) return false;
        UUID uuid;
        try {
            uuid = UUID.fromString(fileName.substring(0, underscore));
        } catch (IllegalArgumentException e) {
            return false;
        }

        try (BukkitObjectInputStream in = new BukkitObjectInputStream(rawIn)) {
            ItemStack[] contents = readItems(in);
            ItemStack[] armor = readItems(in);
            ItemStack offhand = (ItemStack) in.readObject();
            playerInventoryDao.save(uuid, contents, armor, offhand);
            return true;
        } catch (IOException | ClassNotFoundException e) {
            plugin.getLogger().warning("[Backup] inventory restore failed for " + fileName + ": " + e.getMessage());
            return false;
        }
    }

    private ItemStack[] readItems(BukkitObjectInputStream in) throws IOException, ClassNotFoundException {
        int length = in.readInt();
        ItemStack[] items = new ItemStack[length];
        for (int i = 0; i < length; i++) {
            items[i] = (ItemStack) in.readObject();
        }
        return items;
    }

    private boolean zipInventory(Player player, ZipOutputStream zip) {
        String entryName = "inventories/" + player.getUniqueId() + "_" + player.getName() + ".dat";
        PlayerInventory inv = player.getInventory();
        try {
            zip.putNextEntry(new ZipEntry(entryName));
            // BukkitObjectOutputStream must NOT be closed here - closing it would close the
            // shared ZipOutputStream underneath it and abort every entry written after this one.
            BukkitObjectOutputStream out = new BukkitObjectOutputStream(zip);
            writeItems(out, inv.getContents());
            writeItems(out, inv.getArmorContents());
            out.writeObject(inv.getItemInOffHand());
            out.flush();
            zip.closeEntry();
            return true;
        } catch (IOException e) {
            plugin.getLogger().warning("[Backup] inventory save failed for " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }

    private void writeItems(BukkitObjectOutputStream out, ItemStack[] items) throws IOException {
        out.writeInt(items.length);
        for (ItemStack item : items) {
            out.writeObject(item);
        }
    }

    private void zipDirectory(Path source, String entryPrefix, ZipOutputStream zip) throws IOException {
        try (Stream<Path> paths = Files.walk(source)) {
            for (Path path : (Iterable<Path>) paths.sorted(Comparator.naturalOrder())::iterator) {
                if (Files.isDirectory(path)) continue;
                // The lock file is only meaningful to the live running server - skip it so the
                // backup can't be mistaken for something safe to boot a second server from.
                if (path.getFileName().toString().equals("session.lock")) continue;

                String relative = source.relativize(path).toString().replace(File.separatorChar, '/');
                zip.putNextEntry(new ZipEntry(entryPrefix + "/" + relative));
                Files.copy(path, zip);
                zip.closeEntry();
            }
        }
    }

    private void deleteDirectory(File folder) {
        if (!folder.exists()) return;
        try (Stream<Path> paths = Files.walk(folder.toPath())) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException e) {
            plugin.getLogger().warning("[Backup] 월드 폴더 삭제 실패: " + folder + " (" + e.getMessage() + ")");
        }
    }
}
