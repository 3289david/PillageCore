package com.mingyu.pillage.reward;

import com.mingyu.pillage.util.ItemBuilder;
import com.mingyu.pillage.util.Msg;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Loot box weighted heavily toward junk (common) with a small chance at real OP gear (rare).
 * Every enchanted item carries 3-4 enchantments, all vanilla-legal combos/levels achievable
 * on an anvil in normal survival - nothing here requires creative mode or commands.
 */
public final class EventBoxManager {

    private record WeightedEntry(int weight, Supplier<ItemStack> item) {
    }

    private final NamespacedKey key;

    private final List<WeightedEntry> pool = List.of(
            // Junk / common - high weight, this is what you'll get most of the time.
            new WeightedEntry(40, () -> new ItemStack(Material.COOKED_BEEF, 32)),
            new WeightedEntry(30, () -> new ItemStack(Material.BREAD, 16)),
            new WeightedEntry(25, () -> new ItemStack(Material.COAL, 16)),
            new WeightedEntry(20, () -> new ItemStack(Material.ROTTEN_FLESH, 8)),
            new WeightedEntry(15, () -> new ItemStack(Material.IRON_INGOT, 4)),

            // OP gear - low weight, this is the rare jackpot.
            new WeightedEntry(3, () -> enchant(new ItemStack(Material.NETHERITE_SWORD), Enchantment.SHARPNESS, 5,
                    Enchantment.LOOTING, 3, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1)),
            new WeightedEntry(3, () -> enchant(new ItemStack(Material.NETHERITE_AXE), Enchantment.SHARPNESS, 5,
                    Enchantment.EFFICIENCY, 5, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1)),
            new WeightedEntry(3, () -> enchant(new ItemStack(Material.NETHERITE_PICKAXE), Enchantment.EFFICIENCY, 5,
                    Enchantment.FORTUNE, 3, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1)),
            new WeightedEntry(3, () -> enchant(new ItemStack(Material.NETHERITE_HELMET), Enchantment.PROTECTION, 4,
                    Enchantment.RESPIRATION, 3, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1)),
            new WeightedEntry(3, () -> enchant(new ItemStack(Material.NETHERITE_CHESTPLATE), Enchantment.PROTECTION, 4,
                    Enchantment.THORNS, 3, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1)),
            new WeightedEntry(3, () -> enchant(new ItemStack(Material.NETHERITE_LEGGINGS), Enchantment.PROTECTION, 4,
                    Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1)),
            new WeightedEntry(3, () -> enchant(new ItemStack(Material.NETHERITE_BOOTS), Enchantment.PROTECTION, 4,
                    Enchantment.DEPTH_STRIDER, 3, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1)),
            new WeightedEntry(3, () -> enchant(new ItemStack(Material.BOW), Enchantment.POWER, 5, Enchantment.PUNCH, 2,
                    Enchantment.FLAME, 1, Enchantment.UNBREAKING, 3)),
            new WeightedEntry(3, () -> enchant(new ItemStack(Material.CROSSBOW), Enchantment.QUICK_CHARGE, 3,
                    Enchantment.MULTISHOT, 1, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1)),
            new WeightedEntry(3, () -> enchant(new ItemStack(Material.TRIDENT), Enchantment.LOYALTY, 3,
                    Enchantment.IMPALING, 5, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1))
    );

    private final int totalWeight;

    public EventBoxManager(JavaPlugin plugin) {
        this.key = new NamespacedKey(plugin, "event_box");
        this.totalWeight = pool.stream().mapToInt(WeightedEntry::weight).sum();
    }

    private static ItemStack enchant(ItemStack item, Object... enchantLevelPairs) {
        ItemMeta meta = item.getItemMeta();
        for (int i = 0; i + 1 < enchantLevelPairs.length; i += 2) {
            meta.addEnchant((Enchantment) enchantLevelPairs[i], (int) enchantLevelPairs[i + 1], true);
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack rollReward() {
        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        int cumulative = 0;
        for (WeightedEntry entry : pool) {
            cumulative += entry.weight();
            if (roll < cumulative) {
                return entry.item().get();
            }
        }
        return pool.get(pool.size() - 1).item().get();
    }

    public ItemStack createBox() {
        // Plain, non-block, non-special item: no placement behavior (unlike CHEST) and no
        // built-in client-side interaction UI of its own (unlike BUNDLE, which pops its own
        // bundle-contents view on right-click and can swallow the click before plugin logic
        // ever sees a normal use). Nether star has zero vanilla interaction behavior, so our
        // PlayerInteractEvent handler is guaranteed to be the only thing responding to the click.
        ItemStack item = new ItemBuilder(Material.NETHER_STAR)
                .name("&d&l✦ 이벤트 상자 ✦")
                .lore("&7우클릭하여 열기", "&7대부분은 평범한 보상, 아주 가끔 OP 아이템")
                .build();
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isEventBox(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return false;
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    /** Consumes one item from the player's main hand (must already be verified as an event box). */
    public void openFromMainHand(Player player) {
        ItemStack reward = rollReward();

        var leftover = player.getInventory().addItem(reward);
        for (ItemStack extra : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), extra);
        }
        player.sendMessage(Msg.of("&d이벤트 상자를 열어 &e" + reward.getAmount() + "x " + reward.getType() + "&d 을(를) 획득했습니다!"));

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            hand.setAmount(hand.getAmount() - 1);
        }
    }
}
