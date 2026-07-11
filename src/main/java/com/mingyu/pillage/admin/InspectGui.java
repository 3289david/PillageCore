package com.mingyu.pillage.admin;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/** Read-only snapshot of a player's inventory + armor, for staff inspection. */
public final class InspectGui implements InventoryHolder {

    private final Inventory inventory;

    public InspectGui(Player target) {
        this.inventory = Bukkit.createInventory(this, 45, Component.text("검사: " + target.getName()));

        var inv = target.getInventory();
        for (int i = 0; i < 36; i++) {
            var item = inv.getItem(i);
            if (item != null) {
                inventory.setItem(i, item.clone());
            }
        }
        setIfPresent(36, inv.getHelmet());
        setIfPresent(37, inv.getChestplate());
        setIfPresent(38, inv.getLeggings());
        setIfPresent(39, inv.getBoots());
        setIfPresent(40, inv.getItemInOffHand());
    }

    private void setIfPresent(int slot, org.bukkit.inventory.ItemStack item) {
        if (item != null && !item.getType().isAir()) {
            inventory.setItem(slot, item.clone());
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
