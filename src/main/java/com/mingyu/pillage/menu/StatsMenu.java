package com.mingyu.pillage.menu;

import com.mingyu.pillage.data.dao.StatsDao;
import com.mingyu.pillage.team.Team;
import com.mingyu.pillage.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class StatsMenu implements PillageMenu {

    private static final int SLOT_MY_TEAM = 4;
    private static final int SLOT_MY_PERSONAL = 22;
    private static final int SLOT_BACK = 26;

    private final MenuService menuService;
    private final Inventory inventory;

    public StatsMenu(MenuService menuService, Player viewer) {
        this.menuService = menuService;
        this.inventory = Bukkit.createInventory(this, 27, Component.text("통계 / 랭킹"));

        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, ItemBuilder.filler());
        }

        Team myTeam = menuService.teamManager().getTeam(viewer.getUniqueId());
        if (myTeam != null) {
            inventory.setItem(SLOT_MY_TEAM, new ItemBuilder(Material.SHIELD)
                    .name("&6내 팀: " + myTeam.name())
                    .lore(
                            "&f킬: " + myTeam.kills(),
                            "&f약탈 점수: " + myTeam.lootScore(),
                            "&f레이드 방어: " + myTeam.raidsDefended(),
                            "&f레이드 성공: " + myTeam.raidsWon())
                    .build());
        } else {
            inventory.setItem(SLOT_MY_TEAM, new ItemBuilder(Material.BARRIER).name("&c팀에 소속되어 있지 않습니다").build());
        }

        List<Team> topKills = menuService.teamManager().topByKills(5);
        int slot = 10;
        for (int i = 0; i < topKills.size(); i++) {
            Team team = topKills.get(i);
            inventory.setItem(slot + i, new ItemBuilder(Material.IRON_SWORD)
                    .name("&e" + (i + 1) + "위 " + team.name())
                    .lore("&7킬 " + team.kills())
                    .build());
        }

        List<Team> topLoot = menuService.teamManager().topByLootScore(5);
        slot = 15;
        for (int i = 0; i < topLoot.size(); i++) {
            Team team = topLoot.get(i);
            inventory.setItem(slot + i, new ItemBuilder(Material.GOLD_INGOT)
                    .name("&e" + (i + 1) + "위 " + team.name())
                    .lore("&7약탈 점수 " + team.lootScore())
                    .build());
        }

        StatsDao.Stats personal = menuService.statsDao().get(viewer.getUniqueId());
        long hours = personal.playtimeSeconds() / 3600;
        long minutes = (personal.playtimeSeconds() % 3600) / 60;
        inventory.setItem(SLOT_MY_PERSONAL, new ItemBuilder(Material.PLAYER_HEAD)
                .skullOwner(viewer)
                .name("&b내 개인 통계")
                .lore(
                        "&f킬: " + personal.kills() + " / 데스: " + personal.deaths(),
                        String.format("&fK/D: %.2f", personal.kd()),
                        "&f플레이 시간: " + hours + "시간 " + minutes + "분",
                        "&f채굴량: " + personal.blocksMined() + " 블록")
                .build());

        inventory.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW).name("&7뒤로가기").build());
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    @Override
    public void onClick(Player player, int slot, ClickType click) {
        if (slot == SLOT_BACK) {
            menuService.openMain(player);
        }
    }
}
