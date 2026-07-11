package com.mingyu.pillage.reward;

import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public final class EventBoxListener implements Listener {

    private final EventBoxManager eventBoxManager;

    public EventBoxListener(EventBoxManager eventBoxManager) {
        this.eventBoxManager = eventBoxManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() == null || event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;

        var item = event.getPlayer().getInventory().getItemInMainHand();
        if (!eventBoxManager.isEventBox(item)) return;

        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);
        eventBoxManager.openFromMainHand(event.getPlayer());
    }
}
