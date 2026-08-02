package com.mingyu.pillage.donor;

import com.mingyu.pillage.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;

public final class HallOfFameListener implements Listener {

    private final HallOfFameManager hallOfFameManager;

    public HallOfFameListener(HallOfFameManager hallOfFameManager) {
        this.hallOfFameManager = hallOfFameManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (hallOfFameManager.isOwnedStatue(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onManipulate(PlayerArmorStandManipulateEvent event) {
        if (hallOfFameManager.isOwnedStatue(event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

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
