package com.mingyu.pillage.donor;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public final class HallOfFameListener implements Listener {

    private final JavaPlugin plugin;
    private final HallOfFameManager hallOfFameManager;

    public HallOfFameListener(JavaPlugin plugin, HallOfFameManager hallOfFameManager) {
        this.plugin = plugin;
        this.hallOfFameManager = hallOfFameManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (hallOfFameManager.isOwnedStatue(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    // setInvulnerable(true) alone doesn't stop vanilla's /kill command - it's designed to bypass
    // invulnerability outright - so as a last resort, if a statue still dies anyway, rebuild it.
    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (!hallOfFameManager.isOwnedStatue(event.getEntity())) return;
        String uuidString = event.getEntity().getPersistentDataContainer()
                .get(hallOfFameManager.markerKey(), PersistentDataType.STRING);
        if (uuidString == null) return;
        UUID uuid = UUID.fromString(uuidString);
        plugin.getServer().getScheduler().runTask(plugin, () -> hallOfFameManager.createStatueFor(uuid));
    }

    @EventHandler(ignoreCancelled = true)
    public void onManipulate(PlayerArmorStandManipulateEvent event) {
        if (hallOfFameManager.isOwnedStatue(event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    // Block break/place protection for the monument's footprint no longer lives here - it's
    // subsumed by HubBuildGuardListener, which protects the whole hub world (not just the
    // monument) from non-admins.
}
