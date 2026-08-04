package com.mingyu.pillage.instance;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

/** The hub is a lobby, not a place to fight off zombies or round up cows - blocks every
 *  non-plugin-triggered mob spawn there (natural, spawner, flat-world slimes, village
 *  reinforcements, etc.), while still letting an admin /summon something on purpose (SpawnReason
 *  COMMAND) or a future feature spawn something intentionally (CUSTOM). Scoped to the hub only;
 *  mini-servers and the main server keep normal mob spawning. */
public final class HubMobGuardListener implements Listener {

    private final InstanceManager instanceManager;

    public HubMobGuardListener(InstanceManager instanceManager) {
        this.instanceManager = instanceManager;
    }

    @EventHandler
    public void onSpawn(CreatureSpawnEvent event) {
        if (!instanceManager.isHub(event.getLocation().getWorld())) return;
        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
        if (reason == CreatureSpawnEvent.SpawnReason.COMMAND || reason == CreatureSpawnEvent.SpawnReason.CUSTOM) {
            return;
        }
        event.setCancelled(true);
    }
}
