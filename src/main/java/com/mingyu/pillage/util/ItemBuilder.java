package com.mingyu.pillage.util;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ItemBuilder {

    private final ItemStack item;
    private final List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
    private String name;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
    }

    public ItemBuilder name(String name) {
        this.name = name;
        return this;
    }

    public ItemBuilder lore(String... lines) {
        for (String line : lines) {
            if (line == null || line.isEmpty()) continue;
            lore.add(Msg.of(line));
        }
        return this;
    }

    public ItemBuilder lore(List<String> lines) {
        for (String line : lines) {
            lore.add(Msg.of(line));
        }
        return this;
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(Math.max(1, amount));
        return this;
    }

    public ItemBuilder skullOwner(OfflinePlayer owner) {
        if (item.getItemMeta() instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(owner);
            item.setItemMeta(skullMeta);
        }
        return this;
    }

    public ItemStack build() {
        ItemMeta meta = item.getItemMeta();
        if (name != null) {
            meta.displayName(Msg.of(name));
        }
        if (!lore.isEmpty()) {
            meta.lore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack filler() {
        return new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("&7").build();
    }
}
