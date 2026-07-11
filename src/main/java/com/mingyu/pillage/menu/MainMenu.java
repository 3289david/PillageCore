package com.mingyu.pillage.menu;

import com.mingyu.pillage.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

public final class MainMenu implements PillageMenu {

    private static final int SLOT_TEAM = 1;
    private static final int SLOT_TP = 2;
    private static final int SLOT_TRADE = 3;
    private static final int SLOT_SETTINGS = 5;
    private static final int SLOT_STATS = 7;

    private final MenuService menuService;
    private final Inventory inventory;

    public MainMenu(MenuService menuService) {
        this.menuService = menuService;
        this.inventory = Bukkit.createInventory(this, 9, Component.text("PillageCore 메뉴"));

        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, ItemBuilder.filler());
        }
        inventory.setItem(SLOT_TEAM, new ItemBuilder(Material.WHITE_BANNER).name("&6팀").lore("&7팀 관리, 채팅, 홈, 랭킹").build());
        inventory.setItem(SLOT_TP, new ItemBuilder(Material.ENDER_PEARL).name("&b텔레포트").lore("&7스폰, 백, 홈, 요청").build());
        inventory.setItem(SLOT_TRADE, new ItemBuilder(Material.EMERALD).name("&a거래").lore("&7플레이어와 물물교환").build());
        inventory.setItem(SLOT_SETTINGS, new ItemBuilder(Material.COMPARATOR).name("&e설정").lore("&7팀 설정 (Friendly Fire, 인원 등)").build());
        inventory.setItem(SLOT_STATS, new ItemBuilder(Material.BOOK).name("&d통계").lore("&7팀 랭킹 및 기록").build());
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    @Override
    public void onClick(Player player, int slot, ClickType click) {
        switch (slot) {
            case SLOT_TEAM -> menuService.openTeam(player);
            case SLOT_TP -> menuService.openTp(player);
            case SLOT_TRADE -> menuService.openTrade(player);
            case SLOT_SETTINGS -> menuService.openSettings(player);
            case SLOT_STATS -> menuService.openStats(player);
            default -> {
            }
        }
    }
}
