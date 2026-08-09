package com.mingyu.pillage.instance;

import com.mingyu.pillage.donor.HallOfFameManager;
import com.mingyu.pillage.util.Msg;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/** The hub is a shared lobby, not a place for regular players to build or dig - blocks every
 *  break/place anywhere in the hub world (not just the Hall of Fame monument's footprint) for
 *  anyone without pillage.admin. The dedicated Hall of Fame world (used instead of the hub when
 *  hub.enabled: false) gets the same lockdown, since it's just as much a showcase-only space. */
public final class HubBuildGuardListener implements Listener {

    private final InstanceManager instanceManager;
    private final HallOfFameManager hallOfFameManager;

    public HubBuildGuardListener(InstanceManager instanceManager, HallOfFameManager hallOfFameManager) {
        this.instanceManager = instanceManager;
        this.hallOfFameManager = hallOfFameManager;
    }

    private boolean isGuardedWorld(World world) {
        return instanceManager.isHub(world) || hallOfFameManager.isDedicatedWorld(world);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("pillage.admin")) return;
        if (!isGuardedWorld(event.getBlock().getWorld())) return;
        event.setCancelled(true);
        player.sendMessage(Msg.of("&c이곳에서는 블록을 부술 수 없습니다."));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("pillage.admin")) return;
        if (!isGuardedWorld(event.getBlock().getWorld())) return;
        event.setCancelled(true);
        player.sendMessage(Msg.of("&c이곳에서는 블록을 설치할 수 없습니다."));
    }
}
