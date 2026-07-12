package com.mingyu.pillage.shop;

import com.mingyu.pillage.menu.PillageMenu;
import com.mingyu.pillage.util.ItemBuilder;
import com.mingyu.pillage.util.Msg;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class ShopMenu implements PillageMenu {

    private final ShopManager shopManager;
    private final Inventory inventory;
    private final List<ShopOffer> offers;

    public ShopMenu(ShopManager shopManager) {
        this.shopManager = shopManager;
        this.offers = shopManager.offers();
        int size = Math.min(54, Math.max(9, ((offers.size() + 8) / 9) * 9));
        this.inventory = Bukkit.createInventory(this, size, Component.text("PillageCore 상점"));
        render();
    }

    private void render() {
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, ItemBuilder.filler());
        }
        for (int i = 0; i < offers.size() && i < inventory.getSize(); i++) {
            ShopOffer offer = offers.get(i);
            inventory.setItem(i, new ItemBuilder(offer.outputMaterial())
                    .amount(offer.outputAmount())
                    .name("&a" + displayName(offer.outputMaterial()) + " x" + offer.outputAmount())
                    .lore("&7비용: " + displayName(offer.inputMaterial()) + " x" + offer.inputAmount(),
                            "&e클릭하여 교환")
                    .build());
        }
    }

    private String displayName(Material material) {
        String[] parts = material.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    @Override
    public void onClick(Player player, int slot, ClickType click) {
        if (slot < 0 || slot >= offers.size()) return;
        ShopOffer offer = offers.get(slot);
        var result = shopManager.exchange(player, offer);
        switch (result) {
            case OK -> player.sendMessage(Msg.of("&a교환 완료: " + displayName(offer.inputMaterial()) + " x" + offer.inputAmount()
                    + " &f-> &a" + displayName(offer.outputMaterial()) + " x" + offer.outputAmount()));
            case NOT_ENOUGH_ITEMS -> player.sendMessage(Msg.of("&c재료가 부족합니다: "
                    + displayName(offer.inputMaterial()) + " x" + offer.inputAmount() + " 필요"));
            case INVENTORY_FULL -> player.sendMessage(Msg.of("&c인벤토리 공간이 부족합니다."));
        }
    }
}
