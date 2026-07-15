package com.mingyu.pillage.shop;

import com.mingyu.pillage.data.dao.ShopDao;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class ShopManager {

    private final ShopDao shopDao;
    private final List<ShopOffer> offers = new ArrayList<>();

    public ShopManager(ShopDao shopDao) {
        this.shopDao = shopDao;
    }

    public void loadAll() {
        offers.clear();
        offers.addAll(shopDao.loadAll());
    }

    public List<ShopOffer> offers() {
        return offers;
    }

    public ShopOffer addOffer(Material inputMaterial, int inputAmount, Material outputMaterial, int outputAmount) {
        ShopOffer offer = shopDao.addOffer(inputMaterial, inputAmount, outputMaterial, outputAmount);
        offers.add(offer);
        return offer;
    }

    public enum RemoveResult { OK, NOT_FOUND }

    public RemoveResult removeOffer(int id) {
        if (!offers.removeIf(o -> o.id() == id)) {
            return RemoveResult.NOT_FOUND;
        }
        shopDao.removeOffer(id);
        return RemoveResult.OK;
    }

    public enum ExchangeResult { OK, NOT_ENOUGH_ITEMS, INVENTORY_FULL }

    public ExchangeResult exchange(Player player, ShopOffer offer) {
        if (countItems(player, offer.inputMaterial()) < offer.inputAmount()) {
            return ExchangeResult.NOT_ENOUGH_ITEMS;
        }
        // Give the output first and only take the input if it actually fit - never take without paying out.
        var leftover = player.getInventory().addItem(new ItemStack(offer.outputMaterial(), offer.outputAmount()));
        if (!leftover.isEmpty()) {
            // leftover is what didn't fit; roll back only what actually got added, not the leftover amount.
            int notAdded = leftover.values().stream().mapToInt(ItemStack::getAmount).sum();
            int added = offer.outputAmount() - notAdded;
            if (added > 0) {
                player.getInventory().removeItem(new ItemStack(offer.outputMaterial(), added));
            }
            return ExchangeResult.INVENTORY_FULL;
        }
        player.getInventory().removeItem(new ItemStack(offer.inputMaterial(), offer.inputAmount()));
        return ExchangeResult.OK;
    }

    private int countItems(Player player, Material material) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                total += item.getAmount();
            }
        }
        return total;
    }
}
