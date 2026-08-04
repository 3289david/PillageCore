package com.mingyu.pillage.instance;

import com.mingyu.pillage.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/** The hub is a shared lobby, not a place for regular players to build or dig - blocks every
 *  break/place anywhere in the hub world (not just the Hall of Fame monument's footprint) for
 *  anyone without pillage.admin. */
public final class HubBuildGuardListener implements Listener {

    private final InstanceManager instanceManager;

    public HubBuildGuardListener(InstanceManager instanceManager) {
        this.instanceManager = instanceManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("pillage.admin")) return;
        if (!instanceManager.isHub(event.getBlock().getWorld())) return;
        event.setCancelled(true);
        player.sendMessage(Msg.of("&c허브에서는 블록을 부술 수 없습니다."));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("pillage.admin")) return;
        if (!instanceManager.isHub(event.getBlock().getWorld())) return;
        event.setCancelled(true);
        player.sendMessage(Msg.of("&c허브에서는 블록을 설치할 수 없습니다."));
    }
}
