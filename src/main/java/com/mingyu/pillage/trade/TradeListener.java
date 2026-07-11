package com.mingyu.pillage.trade;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public final class TradeListener implements Listener {

    private final TradeManager tradeManager;

    public TradeListener(TradeManager tradeManager) {
        this.tradeManager = tradeManager;
    }

    private TradeSession sessionOf(Inventory topInventory) {
        InventoryHolder holder = topInventory.getHolder();
        return holder instanceof TradeSession session ? session : null;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        TradeSession session = sessionOf(event.getView().getTopInventory());
        if (session == null) return;

        Player player = (Player) event.getWhoClicked();
        UUID uuid = player.getUniqueId();

        boolean clickedTop = event.getClickedInventory() != null
                && event.getClickedInventory().equals(event.getView().getTopInventory());

        if (!clickedTop) {
            if (event.isShiftClick()) {
                event.setCancelled(true);
            }
            return;
        }

        int slot = event.getSlot();

        if (slot == session.toggleSlotOf(uuid)) {
            event.setCancelled(true);
            session.setReady(uuid, !session.isReady(uuid));
            tradeManager.refreshToggleIcon(session, uuid);
            if (session.bothReady()) {
                tradeManager.completeTrade(session);
            }
            return;
        }

        if (isOwnSlot(session, uuid, slot)) {
            if (session.isReady(uuid)) {
                event.setCancelled(true);
                player.sendMessage(com.mingyu.pillage.util.Msg.of("&c확인을 취소해야 아이템을 변경할 수 있습니다."));
            }
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        TradeSession session = sessionOf(event.getView().getTopInventory());
        if (session == null) return;

        Player player = (Player) event.getWhoClicked();
        UUID uuid = player.getUniqueId();
        int topSize = event.getView().getTopInventory().getSize();

        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= topSize) continue;
            if (session.isReady(uuid) || !isOwnSlot(session, uuid, rawSlot)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        TradeSession session = sessionOf(event.getInventory());
        if (session == null || session.isFinished()) return;
        tradeManager.cancelTrade(session, "상대방이 거래 창을 닫았습니다");
    }

    private boolean isOwnSlot(TradeSession session, UUID uuid, int slot) {
        for (int s : session.rowOf(uuid)) {
            if (s == slot) return true;
        }
        return false;
    }
}
