package com.mingyu.pillage.donor;

import com.mingyu.pillage.util.Msg;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

public final class StatueCommand implements CommandExecutor {

    private final DonorManager donorManager;

    public StatueCommand(DonorManager donorManager) {
        this.donorManager = donorManager;
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

        var location = player.getLocation();
        ArmorStand statue = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        statue.setArms(true);
        statue.setBasePlate(true);
        statue.setGravity(false);
        statue.setCustomNameVisible(true);
        statue.customName(donorManager.displayName(player).append(Msg.of(" &7의 동상")));

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(player);
        head.setItemMeta(meta);
        statue.getEquipment().setHelmet(head);

        player.sendMessage(Msg.of("&a동상을 설치했습니다."));
        return true;
    }
}
