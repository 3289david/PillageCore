package com.mingyu.pillage.instance;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

/** Flat worlds (the hub uses one) let slimes spawn at any light level, unlike normal terrain
 *  where they're confined to swamps/slime chunks - without this they'd turn the lobby into a
 *  nuisance. Scoped to the hub only; mini-servers and the main server keep normal mob spawning. */
public final class HubSlimeGuardListener implements Listener {

    private final InstanceManager instanceManager;

    public HubSlimeGuardListener(InstanceManager instanceManager) {
        this.instanceManager = instanceManager;
    }

    @EventHandler
    public void onSpawn(CreatureSpawnEvent event) {
        if (event.getEntityType() == EntityType.SLIME && instanceManager.isHub(event.getLocation().getWorld())) {
            event.setCancelled(true);
        }
    }
}
