package com.mingyu.pillage.instance;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/** Runs last (after other plugins' join handling) and drops the player back into whichever
 *  instance they were last in - or the hub, on a first-ever join or if that instance was
 *  since deleted. */
public final class InstanceJoinListener implements Listener {

    private final InstanceManager instanceManager;

    public InstanceJoinListener(InstanceManager instanceManager) {
        this.instanceManager = instanceManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        instanceManager.sendToLastInstanceOrHub(event.getPlayer());
    }
}
