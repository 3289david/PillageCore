package com.mingyu.pillage.instance;

import com.mingyu.pillage.data.dao.PlayerVitalsDao;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

/** Bukkit only ever stores one health/hunger state per player, shared across every world - this
 *  is what makes each instance's vitals independent on top of that: {@link #save} snapshots the
 *  current instance's copy before leaving it, {@link #restore} swaps in whatever was last saved
 *  for the instance being entered (full health and hunger, the first time). */
public final class PlayerVitalsManager {

    private final PlayerVitalsDao dao;

    public PlayerVitalsManager(PlayerVitalsDao dao) {
        this.dao = dao;
    }

    public void save(Player player) {
        dao.save(player.getUniqueId(), player.getHealth(), player.getFoodLevel(),
                player.getSaturation(), player.getExhaustion());
    }

    public void restore(Player player) {
        var saved = dao.load(player.getUniqueId());
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH) != null
                ? player.getAttribute(Attribute.MAX_HEALTH).getValue() : 20.0;
        if (saved.isPresent()) {
            player.setHealth(Math.min(saved.get().health(), maxHealth));
            player.setFoodLevel(saved.get().foodLevel());
            player.setSaturation(saved.get().saturation());
            player.setExhaustion(saved.get().exhaustion());
        } else {
            player.setHealth(maxHealth);
            player.setFoodLevel(20);
            player.setSaturation(5.0f);
            player.setExhaustion(0.0f);
        }
    }
}
