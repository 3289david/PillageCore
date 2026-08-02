package com.mingyu.pillage.reward;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class EventBoxListener implements Listener {

    private final EventBoxManager eventBoxManager;

    public EventBoxListener(EventBoxManager eventBoxManager) {
        this.eventBoxManager = eventBoxManager;
    }

    // Not ignoreCancelled: some clients send RIGHT_CLICK_BLOCK against a nearby block first,
    // which other logic may cancel for unrelated reasons (e.g. a protected block) - that must
    // never stop the event box itself from opening.
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        // The client fires this event once per hand for the same physical click; only react to
        // the main-hand copy so we don't double-open when the off-hand event also comes through.
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!eventBoxManager.isEventBox(item)) {
            return;
        }

        event.setCancelled(true);
        eventBoxManager.openFromMainHand(player);
    }
}
