package com.mingyu.pillage.admin;

import com.mingyu.pillage.instance.InstanceManager;
import com.mingyu.pillage.util.Msg;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.stream.Stream;

/** On-demand snapshot of the current instance's world (chunks/terrain) and every online player's
 *  inventory in it - for an admin who wants a safety net before something risky, not an
 *  automatic/scheduled backup. Synchronous, so a large world means a brief lag spike - that's an
 *  inherent cost of copying live chunk files, the same tradeoff any backup plugin has. */
public final class BackupCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final InstanceManager instanceManager;

    public BackupCommand(JavaPlugin plugin, InstanceManager instanceManager) {
        this.plugin = plugin;
        this.instanceManager = instanceManager;
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

        World world = player.getWorld();
        String instanceId = instanceManager.resolveInstanceId(world);
        String timestamp = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        File backupDir = new File(new File(plugin.getDataFolder(), "backups"), instanceId + "_" + timestamp);

        sender.sendMessage(Msg.of("&e백업을 시작합니다... (월드 크기에 따라 잠깐 서버가 멈출 수 있습니다)"));

        world.save();

        try {
            copyDirectory(world.getWorldFolder().toPath(), new File(backupDir, "world").toPath());
        } catch (IOException e) {
            sender.sendMessage(Msg.of("&c월드 백업에 실패했습니다: " + e.getMessage()));
            plugin.getLogger().warning("[Backup] world copy failed: " + e.getMessage());
            return true;
        }

        File inventoryDir = new File(backupDir, "inventories");
        inventoryDir.mkdirs();
        int savedInventories = 0;
        for (Player online : world.getPlayers()) {
            if (saveInventory(online, inventoryDir)) {
                savedInventories++;
            }
        }

        sender.sendMessage(Msg.of("&a백업 완료! &7(" + backupDir.getName() + ", 인벤토리 " + savedInventories + "명 저장됨)"));
        plugin.getLogger().info("[Backup] " + player.getName() + " backed up instance " + instanceId + " to " + backupDir);
        return true;
    }

    private boolean saveInventory(Player player, File inventoryDir) {
        File file = new File(inventoryDir, player.getUniqueId() + "_" + player.getName() + ".dat");
        PlayerInventory inv = player.getInventory();
        try (FileOutputStream fileOut = new FileOutputStream(file);
             BukkitObjectOutputStream out = new BukkitObjectOutputStream(fileOut)) {
            writeItems(out, inv.getContents());
            writeItems(out, inv.getArmorContents());
            out.writeObject(inv.getItemInOffHand());
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

    private void copyDirectory(Path source, Path target) throws IOException {
        try (Stream<Path> paths = Files.walk(source)) {
            for (Path path : (Iterable<Path>) paths.sorted(Comparator.naturalOrder())::iterator) {
                // The lock file is only meaningful to the live running server - skip it so the
                // backup can't be mistaken for something safe to boot a second server from.
                if (path.getFileName().toString().equals("session.lock")) continue;
                Path destination = target.resolve(source.relativize(path));
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.createDirectories(destination.getParent());
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}
