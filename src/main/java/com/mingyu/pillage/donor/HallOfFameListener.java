package com.mingyu.pillage.donor;

import com.mingyu.pillage.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
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

    // Admins can freely build/break in the hub (the monument is the only structure there, so
    // this is effectively "admins can maintain the hub") - everyone else can't touch it at all.
    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getPlayer().hasPermission("pillage.admin")) return;
        if (hallOfFameManager.isWithinHallOfFame(event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Msg.of("&c명예의 전당은 부술 수 없습니다."));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("pillage.admin")) return;
        if (hallOfFameManager.isWithinHallOfFame(event.getBlock().getLocation())) {
            event.setCancelled(true);
            player.sendMessage(Msg.of("&c명예의 전당에는 블록을 설치할 수 없습니다."));
        }
    }
}
