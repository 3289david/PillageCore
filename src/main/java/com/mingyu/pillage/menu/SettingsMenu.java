package com.mingyu.pillage.menu;

import com.mingyu.pillage.team.Team;
import com.mingyu.pillage.team.TeamManager;
import com.mingyu.pillage.util.ItemBuilder;
import com.mingyu.pillage.util.Msg;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

public final class SettingsMenu implements PillageMenu {

    private static final int SLOT_FRIENDLY_FIRE = 11;
    private static final int SLOT_MAX_DECREASE = 12;
    private static final int SLOT_MAX_DISPLAY = 13;
    private static final int SLOT_MAX_INCREASE = 14;
    private static final int SLOT_BACK = 22;

    private final MenuService menuService;
    private final Inventory inventory;

    public SettingsMenu(MenuService menuService, Player viewer) {
        this.menuService = menuService;
        this.inventory = Bukkit.createInventory(this, 27, Component.text("팀 설정"));

        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, ItemBuilder.filler());
        }

        Team team = menuService.teamManager().getTeam(viewer.getUniqueId());
        if (team == null) {
            inventory.setItem(13, new ItemBuilder(Material.BARRIER).name("&c팀에 소속되어 있지 않습니다").build());
        } else {
            inventory.setItem(SLOT_FRIENDLY_FIRE, new ItemBuilder(team.friendlyFire() ? Material.RED_DYE : Material.GRAY_DYE)
                    .name("&cFriendly Fire 토글")
                    .lore(team.friendlyFire() ? "&a현재: ON" : "&7현재: OFF", "&7(팀장만 변경 가능)")
                    .build());
            inventory.setItem(SLOT_MAX_DECREASE, new ItemBuilder(Material.RED_CONCRETE).name("&c-1").build());
            inventory.setItem(SLOT_MAX_DISPLAY, new ItemBuilder(Material.PLAYER_HEAD)
                    .name("&f최대 인원: " + team.maxMembers())
                    .lore("&7현재 인원: " + team.size())
                    .build());
            inventory.setItem(SLOT_MAX_INCREASE, new ItemBuilder(Material.LIME_CONCRETE).name("&a+1").build());
        }

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
            return;
        }

        TeamManager teamManager = menuService.teamManager();
        Team team = teamManager.getTeam(player.getUniqueId());
        if (team == null) return;

        if (!team.isLeader(player.getUniqueId())
                && (slot == SLOT_FRIENDLY_FIRE || slot == SLOT_MAX_DECREASE || slot == SLOT_MAX_INCREASE)) {
            player.sendMessage(Msg.of("&c팀장만 변경할 수 있습니다."));
            return;
        }

        if (slot == SLOT_FRIENDLY_FIRE) {
            teamManager.setFriendlyFire(team, !team.friendlyFire());
            menuService.openSettings(player);
        } else if (slot == SLOT_MAX_DECREASE) {
            int value = Math.max(team.size(), team.maxMembers() - 1);
            teamManager.setMaxMembers(team, value);
            menuService.openSettings(player);
        } else if (slot == SLOT_MAX_INCREASE) {
            int cap = teamManager.maxMembersHardCap();
            int value = Math.min(cap, team.maxMembers() + 1);
            teamManager.setMaxMembers(team, value);
            menuService.openSettings(player);
        }
    }
}
