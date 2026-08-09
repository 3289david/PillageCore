package com.mingyu.pillage.instance;

import com.mingyu.pillage.data.dao.PlayerEnderChestDao;
import com.mingyu.pillage.data.dao.PlayerGameModeDao;
import com.mingyu.pillage.data.dao.PlayerXpDao;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Bukkit only ever stores one copy each of XP, ender chest, and game mode per player, shared
 *  across every world - same story as inventory/vitals/effects, just bundled into one manager
 *  since none of the three need much logic of their own. New instance = level 0, empty ender
 *  chest, survival mode, the same "fresh start" the rest of per-instance state already gets. */
public final class PlayerExtraStateManager {

    private final PlayerXpDao xpDao;
    private final PlayerEnderChestDao enderChestDao;
    private final PlayerGameModeDao gameModeDao;

    public PlayerExtraStateManager(PlayerXpDao xpDao, PlayerEnderChestDao enderChestDao, PlayerGameModeDao gameModeDao) {
        this.xpDao = xpDao;
        this.enderChestDao = enderChestDao;
        this.gameModeDao = gameModeDao;
    }

    public void save(Player player) {
        xpDao.save(player.getUniqueId(), player.getLevel(), player.getExp(), player.getTotalExperience());
        enderChestDao.save(player.getUniqueId(), player.getEnderChest().getContents());
        gameModeDao.save(player.getUniqueId(), player.getGameMode().name());
    }

    public void restore(Player player) {
        var xp = xpDao.load(player.getUniqueId());
        if (xp.isPresent()) {
            player.setLevel(xp.get().level());
            player.setExp(xp.get().exp());
            player.setTotalExperience(xp.get().totalExperience());
        } else {
            player.setLevel(0);
            player.setExp(0f);
            player.setTotalExperience(0);
        }

        int enderChestSize = player.getEnderChest().getSize();
        ItemStack[] enderChest = enderChestDao.load(player.getUniqueId()).orElse(new ItemStack[enderChestSize]);
        if (enderChest.length == enderChestSize) {
            player.getEnderChest().setContents(enderChest);
        }

        GameMode mode = GameMode.SURVIVAL;
        String savedMode = gameModeDao.load(player.getUniqueId());
        if (savedMode != null) {
            try {
                mode = GameMode.valueOf(savedMode);
            } catch (IllegalArgumentException ignored) {
            }
        }
        player.setGameMode(mode);
    }
}
