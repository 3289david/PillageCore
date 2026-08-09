package com.mingyu.pillage.instance;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Every manager/DAO in this plugin just calls {@code database.connection()}/reads the "current"
 * instance without knowing instances exist - this is the one place responsible for making sure
 * that pointer is already correct by the time any of them run. Registered at LOWEST priority so
 * it always fires before any other plugin listener on the same event. Deliberately does NOT
 * handle PlayerTeleportEvent: during a teleport the player's world at event-fire time is still
 * the *old* one, so resolving instance context there would fight with (and undo) the explicit
 * switch InstanceManager's own teleport methods perform - those re-assert the correct instance
 * after the teleport completes instead.
 */
public final class InstanceContextListener implements Listener {

    private final InstanceManager instanceManager;

    public InstanceContextListener(InstanceManager instanceManager) {
        this.instanceManager = instanceManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        instanceManager.enter(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            instanceManager.enter(player);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        instanceManager.enter(event.getPlayer());
    }

    // Right-clicking an entity (the hub NPCs, in particular) is a separate event class from
    // interacting with a block/air - without this, HubNpcListener's teleportToMain() etc. could
    // run with the shared "current instance" pointer still left wherever some other player's last
    // action put it, corrupting whichever instance's inventory save that pointer happened to
    // point at instead of this player's own.
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        instanceManager.enter(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        instanceManager.enter(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        instanceManager.enter(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(PlayerDeathEvent event) {
        instanceManager.enter(event.getEntity());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageByEntityEvent event) {
        instanceManager.enter(event.getEntity().getWorld());
    }

    // HIGH (not LOWEST) so this runs *after* anything else that might set a bed/anchor respawn
    // location - it's a final safety net, not the initial resolution. A bed set in a different
    // instance's world would otherwise respawn the player across instances, which is never
    // legitimate: each instance is meant to be a fully separate world.
    @EventHandler(priority = EventPriority.HIGH)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        World deathWorld = player.getWorld();
        if (!event.getRespawnLocation().getWorld().equals(deathWorld)) {
            event.setRespawnLocation(deathWorld.getSpawnLocation());
        }
        instanceManager.enter(deathWorld);
    }
}
