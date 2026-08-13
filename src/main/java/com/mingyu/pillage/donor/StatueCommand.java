package com.mingyu.pillage.donor;

import com.mingyu.pillage.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/** Donor-only cosmetic self-statue - /statue places one at the player's feet, /statue remove
 *  clears every one they've placed in their current world (instance). Placing is on a cooldown
 *  so it can't be spammed to litter the world with armor stands. */
public final class StatueCommand implements CommandExecutor {

    private static final long COOLDOWN_MILLIS = 10_000L;

    private final DonorManager donorManager;
    private final NamespacedKey ownerKey;
    private final Map<UUID, Long> cooldownUntil = new HashMap<>();

    public StatueCommand(JavaPlugin plugin, DonorManager donorManager) {
        this.donorManager = donorManager;
        this.ownerKey = new NamespacedKey(plugin, "statue_owner");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        if (!donorManager.isDonor(player.getUniqueId())) {
            player.sendMessage(Msg.of("&c후원자 전용 명령어입니다."));
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("remove")) {
            handleRemove(player, args);
            return true;
        }

        handleCreate(player);
        return true;
    }

    private void handleCreate(Player player) {
        Long until = cooldownUntil.get(player.getUniqueId());
        if (until != null && until > System.currentTimeMillis() && !player.hasPermission("pillage.admin")) {
            long remaining = (until - System.currentTimeMillis()) / 1000 + 1;
            player.sendMessage(Msg.of("&c동상은 " + remaining + "초 후에 다시 설치할 수 있습니다."));
            return;
        }

        var location = player.getLocation();
        ArmorStand statue = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        statue.setArms(true);
        statue.setBasePlate(true);
        statue.setGravity(false);
        statue.setCustomNameVisible(true);
        statue.customName(donorManager.displayName(player).append(Msg.of(" &7의 동상")));
        statue.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, player.getUniqueId().toString());

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(player);
        head.setItemMeta(meta);
        statue.getEquipment().setHelmet(head);

        cooldownUntil.put(player.getUniqueId(), System.currentTimeMillis() + COOLDOWN_MILLIS);
        player.sendMessage(Msg.of("&a동상을 설치했습니다."));
    }

    /** No args removes the caller's own statues. Admins (pillage.admin) can also target another
     *  player by name, or "all" to clear every self-statue in the world regardless of owner -
     *  both scoped to whichever world (instance) the admin is currently standing in. */
    private void handleRemove(Player player, String[] args) {
        String targetArg = args.length >= 2 ? args[1] : null;

        if (targetArg == null) {
            removeStatues(player, stand -> ownedBy(stand, player.getUniqueId()));
            return;
        }
        if (!player.hasPermission("pillage.admin")) {
            player.sendMessage(Msg.of("&c권한이 없습니다."));
            return;
        }
        if (targetArg.equalsIgnoreCase("all")) {
            removeStatues(player, stand -> true);
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetArg);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            player.sendMessage(Msg.of("&c그런 플레이어를 찾을 수 없습니다."));
            return;
        }
        removeStatues(player, stand -> ownedBy(stand, target.getUniqueId()));
    }

    private boolean ownedBy(ArmorStand stand, UUID uuid) {
        String owner = stand.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        return uuid.toString().equals(owner);
    }

    private void removeStatues(Player remover, Predicate<ArmorStand> matches) {
        int removed = 0;
        for (ArmorStand stand : remover.getWorld().getEntitiesByClass(ArmorStand.class)) {
            if (stand.getPersistentDataContainer().has(ownerKey, PersistentDataType.STRING) && matches.test(stand)) {
                stand.remove();
                removed++;
            }
        }
        remover.sendMessage(removed == 0
                ? Msg.of("&c지금 있는 서버에서 지울 동상을 찾지 못했습니다.")
                : Msg.of("&a동상 " + removed + "개를 제거했습니다."));
    }
}
