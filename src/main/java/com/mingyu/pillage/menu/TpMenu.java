package com.mingyu.pillage.menu;

import com.mingyu.pillage.tp.TpManager;
import com.mingyu.pillage.tp.TpRequest;
import com.mingyu.pillage.util.ItemBuilder;
import com.mingyu.pillage.util.Msg;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public final class TpMenu implements PillageMenu {

    private static final int SLOT_SPAWN = 2;
    private static final int SLOT_BACK_LOCATION = 4;
    private static final int SLOT_REQUEST_ACCEPT = 29;
    private static final int SLOT_REQUEST_DENY = 33;
    private static final int SLOT_SET_HOME = 31;
    private static final int SLOT_BACK_MENU = 44;

    private final MenuService menuService;
    private final Inventory inventory;
    private final Map<Integer, String> homeSlots = new HashMap<>();

    public TpMenu(MenuService menuService, Player viewer) {
        this.menuService = menuService;
        this.inventory = Bukkit.createInventory(this, 45, Component.text("텔레포트 메뉴"));

        for (int i = 0; i < 45; i++) {
            inventory.setItem(i, ItemBuilder.filler());
        }

        inventory.setItem(SLOT_SPAWN, new ItemBuilder(Material.BEACON).name("&b스폰으로 이동").build());
        inventory.setItem(SLOT_BACK_LOCATION, new ItemBuilder(Material.CLOCK).name("&e마지막 위치로 (Back)").build());
        inventory.setItem(SLOT_SET_HOME, new ItemBuilder(Material.COMPASS)
                .name("&d현재 위치를 'home' 으로 저장")
                .build());
        inventory.setItem(SLOT_BACK_MENU, new ItemBuilder(Material.ARROW).name("&7뒤로가기").build());

        Map<String, Location> homes = menuService.tpManager().homes(viewer.getUniqueId());
        int slot = 9;
        for (String name : homes.keySet()) {
            if (slot >= 27) break;
            inventory.setItem(slot, new ItemBuilder(Material.RED_BED)
                    .name("&a홈: " + name)
                    .lore("&7좌클릭: 이동", "&c쉬프트클릭: 삭제")
                    .build());
            homeSlots.put(slot, name);
            slot++;
        }

        TpRequest request = menuService.tpManager().peekRequest(viewer.getUniqueId());
        if (request != null) {
            String requesterName = Bukkit.getOfflinePlayer(request.requester()).getName();
            inventory.setItem(SLOT_REQUEST_ACCEPT, new ItemBuilder(Material.LIME_WOOL)
                    .name("&a요청 수락: " + requesterName)
                    .build());
            inventory.setItem(SLOT_REQUEST_DENY, new ItemBuilder(Material.RED_WOOL)
                    .name("&c요청 거절: " + requesterName)
                    .build());
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    @Override
    public void onClick(Player player, int slot, ClickType click) {
        TpManager tpManager = menuService.tpManager();

        if (slot == SLOT_BACK_MENU) {
            menuService.openMain(player);
            return;
        }
        if (slot == SLOT_SPAWN) {
            player.closeInventory();
            tpManager.requestTeleport(player, menuService.spawnService().spawnLocation());
            return;
        }
        if (slot == SLOT_BACK_LOCATION) {
            Location back = tpManager.back(player.getUniqueId());
            player.closeInventory();
            if (back != null) {
                tpManager.requestTeleport(player, back);
            } else {
                player.sendMessage(Msg.of("&c돌아갈 위치가 없습니다."));
            }
            return;
        }
        if (slot == SLOT_SET_HOME) {
            tpManager.setHome(player.getUniqueId(), "home", player.getLocation());
            player.sendMessage(Msg.of("&a홈 'home' 을(를) 저장했습니다."));
            menuService.openTp(player);
            return;
        }
        if (slot == SLOT_REQUEST_ACCEPT) {
            TpRequest request = tpManager.consumeRequest(player.getUniqueId());
            player.closeInventory();
            if (request == null) {
                player.sendMessage(Msg.of("&c받은 텔레포트 요청이 없습니다."));
                return;
            }
            Player requester = Bukkit.getPlayer(request.requester());
            if (requester == null) {
                player.sendMessage(Msg.of("&c요청을 보낸 플레이어가 오프라인입니다."));
                return;
            }
            player.sendMessage(Msg.of("&a" + requester.getName() + " 님의 텔레포트 요청을 수락했습니다."));
            requester.sendMessage(Msg.of("&a" + player.getName() + " 님이 텔레포트 요청을 수락했습니다."));
            tpManager.requestTeleport(requester, player.getLocation());
            return;
        }
        if (slot == SLOT_REQUEST_DENY) {
            tpManager.denyRequest(player.getUniqueId());
            player.sendMessage(Msg.of("&c텔레포트 요청을 거절했습니다."));
            menuService.openTp(player);
            return;
        }

        if (homeSlots.containsKey(slot)) {
            String name = homeSlots.get(slot);
            if (click.isShiftClick()) {
                tpManager.deleteHome(player.getUniqueId(), name);
                player.sendMessage(Msg.of("&c홈 '" + name + "' 을(를) 삭제했습니다."));
                menuService.openTp(player);
            } else {
                Location home = tpManager.homes(player.getUniqueId()).get(name);
                player.closeInventory();
                if (home != null) {
                    tpManager.requestTeleport(player, home);
                }
            }
        }
    }
}
