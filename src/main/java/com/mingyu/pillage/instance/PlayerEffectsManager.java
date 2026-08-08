package com.mingyu.pillage.instance;

import com.mingyu.pillage.data.dao.PlayerEffectDao;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;

/** Bukkit only ever stores one set of active potion effects per player, shared across every
 *  world - this is what makes each instance's effects independent on top of that: {@link #save}
 *  snapshots the current instance's copy before leaving it, {@link #restore} clears whatever is
 *  currently active and swaps in whatever was last saved for the instance being entered (none,
 *  the first time). */
public final class PlayerEffectsManager {

    private final PlayerEffectDao dao;

    public PlayerEffectsManager(PlayerEffectDao dao) {
        this.dao = dao;
    }

    public void save(Player player) {
        dao.save(player.getUniqueId(), player.getActivePotionEffects());
    }

    public void restore(Player player) {
        for (PotionEffect active : new ArrayList<>(player.getActivePotionEffects())) {
            player.removePotionEffect(active.getType());
        }
        for (PotionEffect saved : dao.load(player.getUniqueId())) {
            player.addPotionEffect(saved);
        }
    }
}
