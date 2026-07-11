package com.mingyu.pillage.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.InventoryHolder;

public interface PillageMenu extends InventoryHolder {

    void onClick(Player player, int slot, ClickType click);

    default void onClose(Player player) {
    }
}
