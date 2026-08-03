package com.mingyu.pillage.instance;

import com.mingyu.pillage.data.dao.PlayerInventoryDao;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Optional;

/** Bukkit only ever stores one inventory per player, shared across every world - this is what
 *  makes each instance's inventory independent on top of that: {@link #save} snapshots the
 *  current instance's copy before leaving it, {@link #restore} swaps in whatever was last saved
 *  for the instance being entered (or an empty inventory, the first time). */
public final class PlayerInventoryManager {

    private final PlayerInventoryDao dao;

    public PlayerInventoryManager(PlayerInventoryDao dao) {
        this.dao = dao;
    }

    public void save(Player player) {
        PlayerInventory inv = player.getInventory();
        dao.save(player.getUniqueId(), inv.getContents(), inv.getArmorContents(), inv.getItemInOffHand());
    }

    public void restore(Player player) {
        PlayerInventory inv = player.getInventory();
        inv.clear();
        inv.setArmorContents(new ItemStack[4]);
        inv.setItemInOffHand(null);

        Optional<PlayerInventoryDao.SavedInventory> saved = dao.load(player.getUniqueId());
        if (saved.isPresent()) {
            inv.setContents(saved.get().contents());
            inv.setArmorContents(saved.get().armor());
            inv.setItemInOffHand(saved.get().offhand());
        }
    }
}
