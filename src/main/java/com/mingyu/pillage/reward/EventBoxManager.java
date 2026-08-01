package com.mingyu.pillage.reward;

import com.mingyu.pillage.util.ItemBuilder;
import com.mingyu.pillage.util.Msg;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/** Loot box that hands out a random "OP" item instead of currency. */
public final class EventBoxManager {

    private final NamespacedKey key;

    private final List<Supplier<ItemStack>> opItemPool = List.of(
            () -> enchant(new ItemStack(Material.NETHERITE_SWORD), Enchantment.SHARPNESS, 5,
                    Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1),
            () -> enchant(new ItemStack(Material.NETHERITE_HELMET), Enchantment.PROTECTION, 4,
                    Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1),
            () -> enchant(new ItemStack(Material.NETHERITE_CHESTPLATE), Enchantment.PROTECTION, 4,
                    Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1),
            () -> enchant(new ItemStack(Material.NETHERITE_LEGGINGS), Enchantment.PROTECTION, 4,
                    Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1),
            () -> enchant(new ItemStack(Material.NETHERITE_BOOTS), Enchantment.PROTECTION, 4,
                    Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1),
            () -> enchant(new ItemStack(Material.ELYTRA), Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1),
            () -> enchant(new ItemStack(Material.BOW), Enchantment.POWER, 5, Enchantment.INFINITY, 1,
                    Enchantment.UNBREAKING, 3),
            () -> enchant(new ItemStack(Material.TRIDENT), Enchantment.LOYALTY, 3, Enchantment.CHANNELING, 1),
            () -> new ItemStack(Material.TOTEM_OF_UNDYING),
            () -> new ItemStack(Material.NETHERITE_INGOT, 4),
            () -> new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 2)
    );

    public EventBoxManager(JavaPlugin plugin) {
        this.key = new NamespacedKey(plugin, "event_box");
    }

    private ItemStack enchant(ItemStack item, Object... enchantLevelPairs) {
        ItemMeta meta = item.getItemMeta();
        for (int i = 0; i + 1 < enchantLevelPairs.length; i += 2) {
            meta.addEnchant((Enchantment) enchantLevelPairs[i], (int) enchantLevelPairs[i + 1], true);
        }
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createBox() {
        ItemStack item = new ItemBuilder(Material.CHEST)
                .name("&d&l✦ 이벤트 상자 ✦")
                .lore("&7우클릭하여 열기", "&7랜덤 OP 아이템 획득")
                .build();
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isEventBox(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return false;
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    /** Consumes one item from the player's main hand (must already be verified as an event box). */
    public void openFromMainHand(Player player) {
        Supplier<ItemStack> pick = opItemPool.get(ThreadLocalRandom.current().nextInt(opItemPool.size()));
        ItemStack reward = pick.get();

        var leftover = player.getInventory().addItem(reward);
        for (ItemStack extra : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), extra);
        }
        player.sendMessage(Msg.of("&d이벤트 상자를 열어 &e" + reward.getType() + "&d 을(를) 획득했습니다!"));

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            hand.setAmount(hand.getAmount() - 1);
        }
    }
}
