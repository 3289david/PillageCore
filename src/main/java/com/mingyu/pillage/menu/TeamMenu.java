package com.mingyu.pillage.menu;

import com.mingyu.pillage.team.Team;
import com.mingyu.pillage.team.TeamInvite;
import com.mingyu.pillage.team.TeamManager;
import com.mingyu.pillage.util.ItemBuilder;
import com.mingyu.pillage.util.Msg;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TeamMenu implements PillageMenu {

    private static final int SLOT_BACK = 45;
    private static final int SLOT_CHAT_TOGGLE = 46;
    private static final int SLOT_FRIENDLY_FIRE = 47;
    private static final int SLOT_TEAM_HOME = 48;
    private static final int SLOT_SET_HOME = 49;
    private static final int SLOT_STATS = 50;
    private static final int SLOT_MAX_MEMBERS_INFO = 51;
    private static final int SLOT_LEAVE_DISBAND = 52;

    private static final int SLOT_INVITE_ACCEPT = 21;
    private static final int SLOT_INVITE_DENY = 23;

    private final MenuService menuService;
    private final Inventory inventory;
    private final Map<Integer, UUID> memberSlots = new HashMap<>();

    public TeamMenu(MenuService menuService, Player viewer) {
        this.menuService = menuService;
        TeamManager teamManager = menuService.teamManager();
        Team team = teamManager.getTeam(viewer.getUniqueId());

        this.inventory = Bukkit.createInventory(this, 54,
                Component.text(team != null ? "팀: " + team.name() : "팀 메뉴"));

        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, ItemBuilder.filler());
        }

        if (team == null) {
            renderNoTeam(viewer, teamManager);
        } else {
            renderTeam(viewer, team);
        }

        inventory.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW).name("&7뒤로가기").build());
    }

    private void renderNoTeam(Player viewer, TeamManager teamManager) {
        inventory.setItem(22, new ItemBuilder(Material.PAPER)
                .name("&c팀이 없습니다")
                .lore("&7/team create <이름> 으로", "&7새 팀을 생성하세요.")
                .build());

        teamManager.pendingInvite(viewer.getUniqueId()).ifPresent(invite -> {
            Team invitingTeam = teamManager.allTeams().stream()
                    .filter(t -> t.id() == invite.teamId())
                    .findFirst().orElse(null);
            String teamName = invitingTeam != null ? invitingTeam.name() : "알 수 없음";
            inventory.setItem(SLOT_INVITE_ACCEPT, new ItemBuilder(Material.LIME_WOOL)
                    .name("&a초대 수락: " + teamName)
                    .build());
            inventory.setItem(SLOT_INVITE_DENY, new ItemBuilder(Material.RED_WOOL)
                    .name("&c초대 거절")
                    .build());
        });
    }

    private void renderTeam(Player viewer, Team team) {
        int slot = 0;
        for (var entry : team.members().entrySet()) {
            if (slot >= 36) break;
            UUID uuid = entry.getKey();
            OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
            Player online = Bukkit.getPlayer(uuid);
            String status = online != null ? "&a온라인 (HP " + (int) online.getHealth() + ")" : "&7오프라인";
            inventory.setItem(slot, new ItemBuilder(Material.PLAYER_HEAD)
                    .skullOwner(offline)
                    .name((team.isLeader(uuid) ? "&6[팀장] " : "&f") + offline.getName())
                    .lore(status, team.isLeader(viewer.getUniqueId()) && !uuid.equals(viewer.getUniqueId())
                            ? "&c클릭하여 추방" : "")
                    .build());
            memberSlots.put(slot, uuid);
            slot++;
        }

        inventory.setItem(SLOT_CHAT_TOGGLE, new ItemBuilder(Material.WRITABLE_BOOK)
                .name("&e팀 채팅 토글")
                .lore(menuService.teamManager().isTeamChatEnabled(viewer.getUniqueId()) ? "&a현재: ON" : "&7현재: OFF")
                .build());

        inventory.setItem(SLOT_FRIENDLY_FIRE, new ItemBuilder(team.friendlyFire() ? Material.RED_DYE : Material.GRAY_DYE)
                .name("&cFriendly Fire 토글")
                .lore(team.friendlyFire() ? "&a현재: ON" : "&7현재: OFF", "&7(팀장만 변경 가능)")
                .build());

        inventory.setItem(SLOT_TEAM_HOME, new ItemBuilder(Material.RED_BED)
                .name("&d팀 홈으로 이동")
                .lore(team.home() != null ? "&7설정됨" : "&c설정되지 않음")
                .build());

        inventory.setItem(SLOT_SET_HOME, new ItemBuilder(Material.COMPASS)
                .name("&5팀 홈 설정")
                .lore("&7현재 위치로 설정 (팀장만)")
                .build());

        inventory.setItem(SLOT_STATS, new ItemBuilder(Material.BOOK)
                .name("&b팀 통계 보기")
                .build());

        inventory.setItem(SLOT_MAX_MEMBERS_INFO, new ItemBuilder(Material.PLAYER_HEAD)
                .name("&f인원: " + team.size() + "/" + team.maxMembers())
                .lore("&7/team setmax <숫자> 로 조절")
                .build());

        inventory.setItem(SLOT_LEAVE_DISBAND, new ItemBuilder(Material.BARRIER)
                .name(team.isLeader(viewer.getUniqueId()) ? "&c팀 해체" : "&c팀 탈퇴")
                .build());
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    @Override
    public void onClick(Player player, int slot, ClickType click) {
        TeamManager teamManager = menuService.teamManager();
        Team team = teamManager.getTeam(player.getUniqueId());

        if (slot == SLOT_BACK) {
            menuService.openMain(player);
            return;
        }

        if (team == null) {
            if (slot == SLOT_INVITE_ACCEPT) {
                var result = teamManager.join(player);
                player.sendMessage(Msg.of(result == TeamManager.JoinResult.OK ? "&a팀에 가입했습니다." : "&c가입에 실패했습니다."));
                menuService.openTeam(player);
            } else if (slot == SLOT_INVITE_DENY) {
                teamManager.declineInvite(player.getUniqueId());
                player.closeInventory();
            }
            return;
        }

        if (memberSlots.containsKey(slot)) {
            UUID target = memberSlots.get(slot);
            if (!target.equals(player.getUniqueId()) && team.isLeader(player.getUniqueId())) {
                var result = teamManager.kick(team, player, target);
                if (result == TeamManager.KickResult.OK) {
                    player.sendMessage(Msg.of("&e" + Bukkit.getOfflinePlayer(target).getName() + " 님을 추방했습니다."));
                }
                menuService.openTeam(player);
            }
            return;
        }

        if (slot == SLOT_CHAT_TOGGLE) {
            teamManager.toggleTeamChat(player.getUniqueId());
            menuService.openTeam(player);
        } else if (slot == SLOT_FRIENDLY_FIRE) {
            if (team.isLeader(player.getUniqueId())) {
                teamManager.setFriendlyFire(team, !team.friendlyFire());
            } else {
                player.sendMessage(Msg.of("&c팀장만 변경할 수 있습니다."));
            }
            menuService.openTeam(player);
        } else if (slot == SLOT_TEAM_HOME) {
            if (team.home() != null) {
                player.closeInventory();
                menuService.tpManager().requestTeleport(player, team.home());
            } else {
                player.sendMessage(Msg.of("&c팀 홈이 설정되어 있지 않습니다."));
            }
        } else if (slot == SLOT_SET_HOME) {
            if (team.isLeader(player.getUniqueId())) {
                menuService.teamManager().setHome(team, player.getLocation());
                player.sendMessage(Msg.of("&a팀 홈을 설정했습니다."));
                menuService.openTeam(player);
            } else {
                player.sendMessage(Msg.of("&c팀장만 설정할 수 있습니다."));
            }
        } else if (slot == SLOT_STATS) {
            menuService.openStats(player);
        } else if (slot == SLOT_LEAVE_DISBAND) {
            if (team.isLeader(player.getUniqueId())) {
                if (teamManager.disband(team, player)) {
                    player.sendMessage(Msg.of("&c팀을 해체했습니다."));
                }
            } else {
                teamManager.leave(player);
                player.sendMessage(Msg.of("&e팀을 탈퇴했습니다."));
            }
            player.closeInventory();
        }
    }
}
