package com.mingyu.pillage.menu;

import com.mingyu.pillage.trade.TradeManager;
import com.mingyu.pillage.util.ItemBuilder;
import com.mingyu.pillage.util.Msg;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TradeMenu implements PillageMenu {

    private static final int SLOT_REQUEST_ACCEPT = 48;
    private static final int SLOT_REQUEST_DENY = 50;
    private static final int SLOT_BACK = 49;

    private final MenuService menuService;
    private final Inventory inventory;
    private final Map<Integer, UUID> playerSlots = new HashMap<>();

    public TradeMenu(MenuService menuService, Player viewer) {
        this.menuService = menuService;
        this.inventory = Bukkit.createInventory(this, 54, Component.text("거래 - 플레이어 선택"));

        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, ItemBuilder.filler());
        }

        int slot = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(viewer)) continue;
            if (slot >= 45) break;
            inventory.setItem(slot, new ItemBuilder(Material.PLAYER_HEAD)
                    .skullOwner(online)
                    .name("&f" + online.getName())
                    .lore("&7클릭하여 거래 요청 보내기")
                    .build());
            playerSlots.put(slot, online.getUniqueId());
            slot++;
        }

        inventory.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW).name("&7뒤로가기").build());

        UUID requester = menuService.tradeManager().peekRequester(viewer.getUniqueId());
        if (requester != null) {
            String name = Bukkit.getOfflinePlayer(requester).getName();
            inventory.setItem(SLOT_REQUEST_ACCEPT, new ItemBuilder(Material.LIME_WOOL).name("&a요청 수락: " + name).build());
            inventory.setItem(SLOT_REQUEST_DENY, new ItemBuilder(Material.RED_WOOL).name("&c요청 거절: " + name).build());
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    @Override
    public void onClick(Player player, int slot, ClickType click) {
        TradeManager tradeManager = menuService.tradeManager();

        if (slot == SLOT_BACK) {
            menuService.openMain(player);
            return;
        }

        UUID requester = tradeManager.peekRequester(player.getUniqueId());
        if (requester != null) {
            if (slot == SLOT_REQUEST_ACCEPT) {
                player.closeInventory();
                var result = tradeManager.acceptRequest(player);
                if (result != TradeManager.AcceptResult.OK) {
                    player.sendMessage(Msg.of("&c거래를 수락할 수 없습니다: " + result));
                }
                return;
            }
            if (slot == SLOT_REQUEST_DENY) {
                tradeManager.denyRequest(player.getUniqueId());
                player.sendMessage(Msg.of("&c거래 요청을 거절했습니다."));
                menuService.openTrade(player);
                return;
            }
        }

        UUID target = playerSlots.get(slot);
        if (target == null || target.equals(player.getUniqueId())) return;

        Player targetPlayer = Bukkit.getPlayer(target);
        if (targetPlayer == null) return;

        if (tradeManager.isInTrade(player.getUniqueId()) || tradeManager.isInTrade(target)) {
            player.sendMessage(Msg.of("&c이미 거래가 진행 중입니다."));
            return;
        }

        tradeManager.sendRequest(player, targetPlayer);
        player.closeInventory();
    }
}
