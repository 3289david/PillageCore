package com.mingyu.pillage.tp;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class TpMoveListener implements Listener {

    private final TpManager tpManager;

    public TpMoveListener(TpManager tpManager) {
        this.tpManager = tpManager;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!tpManager.hasPendingTeleport(event.getPlayer().getUniqueId())) {
            return;
        }
        if (event.getTo() == null) return;
        tpManager.onMove(event.getPlayer(), event.getTo());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        tpManager.cancelPendingTeleport(event.getPlayer().getUniqueId(), false);
    }
}
