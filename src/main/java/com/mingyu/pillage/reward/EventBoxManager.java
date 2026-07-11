package com.mingyu.pillage.reward;

import com.mingyu.pillage.economy.EconomyManager;
import com.mingyu.pillage.util.ItemBuilder;
import com.mingyu.pillage.util.Msg;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ThreadLocalRandom;

public final class EventBoxManager {

    private final NamespacedKey key;
    private final EconomyManager economyManager;
    private final long minReward;
    private final long maxReward;

    public EventBoxManager(JavaPlugin plugin, EconomyManager economyManager, long minReward, long maxReward) {
        this.key = new NamespacedKey(plugin, "event_box");
        this.economyManager = economyManager;
        this.minReward = minReward;
        this.maxReward = maxReward;
    }

    public ItemStack createBox() {
        ItemStack item = new ItemBuilder(Material.CHEST)
                .name("&d&l✦ 이벤트 상자 ✦")
                .lore("&7우클릭하여 열기", "&7랜덤 보상 획득")
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
        long reward = ThreadLocalRandom.current().nextLong(minReward, maxReward + 1);
        economyManager.deposit(player.getUniqueId(), reward);
        player.sendMessage(Msg.of("&d이벤트 상자를 열어 &e" + reward + " 에메랄드&d를 획득했습니다!"));

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            hand.setAmount(hand.getAmount() - 1);
        }
    }
}
