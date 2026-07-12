package com.mingyu.pillage.team.command;

import com.mingyu.pillage.team.Team;
import com.mingyu.pillage.team.TeamManager;
import com.mingyu.pillage.tp.TpManager;

import com.mingyu.pillage.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public final class TeamCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of(
            "create", "invite", "join", "leave", "kick", "disband",
            "chat", "ff", "sethome", "home", "setmax", "list", "info", "top");

    private final TeamManager teamManager;
    private final TpManager tpManager;

    public TeamCommand(TeamManager teamManager, TpManager tpManager) {
        this.teamManager = teamManager;
        this.tpManager = tpManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Msg.of("&c사용법: /team <create|invite|join|leave|kick|disband|chat|ff|sethome|home|setmax|list|info|top>"));
            return true;
        }

        String sub = args[0].toLowerCase();
        String[] rest = args.length > 1 ? java.util.Arrays.copyOfRange(args, 1, args.length) : new String[0];

        return switch (sub) {
            case "create" -> create(sender, rest);
            case "invite" -> invite(sender, rest);
            case "join" -> join(sender, rest);
            case "leave" -> leave(sender);
            case "kick" -> kick(sender, rest);
            case "disband" -> disband(sender);
            case "chat" -> chat(sender);
            case "ff" -> friendlyFire(sender, rest);
            case "sethome" -> setHome(sender);
            case "home" -> home(sender);
            case "setmax" -> setMax(sender, rest);
            case "list" -> list(sender);
            case "info" -> info(sender, rest);
            case "top" -> top(sender, rest);
            default -> {
                sender.sendMessage(Msg.of("&c알 수 없는 하위 명령어입니다."));
                yield true;
            }
        };
    }

    private boolean requirePlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return false;
        }
        return true;
    }

    private boolean create(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) return true;
        Player player = (Player) sender;
        if (args.length < 1) {
            player.sendMessage(Msg.of("&c사용법: /team create <팀 이름>"));
            return true;
        }
        String name = args[0];
        if (name.length() < 2 || name.length() > 16) {
            player.sendMessage(Msg.of("&c팀 이름은 2~16자여야 합니다."));
            return true;
        }
        var result = teamManager.createTeam(name, player);
        switch (result) {
            case OK -> player.sendMessage(Msg.of("&a팀 '" + name + "' 을(를) 생성했습니다."));
            case NAME_TAKEN -> player.sendMessage(Msg.of("&c이미 사용 중인 팀 이름입니다."));
            case ALREADY_IN_TEAM -> player.sendMessage(Msg.of("&c이미 팀에 소속되어 있습니다."));
        }
        return true;
    }

    private boolean invite(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) return true;
        Player player = (Player) sender;
        Team team = teamManager.getTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(Msg.of("&c팀에 소속되어 있지 않습니다."));
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(Msg.of("&c사용법: /team invite <player>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(Msg.of("&c해당 플레이어를 찾을 수 없습니다."));
            return true;
        }
        var result = teamManager.invite(team, player, target);
        switch (result) {
            case OK -> {
                player.sendMessage(Msg.of("&a" + target.getName() + " 님을 팀에 초대했습니다."));
                target.sendMessage(Msg.of("&e" + player.getName() + " 님이 팀 '" + team.name() + "' 에 초대했습니다. &a/team join " + team.name()));
            }
            case NOT_LEADER -> player.sendMessage(Msg.of("&c팀장만 초대할 수 있습니다."));
            case TARGET_ALREADY_IN_TEAM -> player.sendMessage(Msg.of("&c해당 플레이어는 이미 다른 팀에 소속되어 있습니다."));
            case TEAM_FULL -> player.sendMessage(Msg.of("&c팀 최대 인원에 도달했습니다."));
            case ALREADY_INVITED -> player.sendMessage(Msg.of("&c이미 초대를 보냈습니다."));
        }
        return true;
    }

    private boolean join(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) return true;
        Player player = (Player) sender;
        var result = teamManager.join(player);
        switch (result) {
            case OK -> {
                Team team = teamManager.getTeam(player.getUniqueId());
                player.sendMessage(Msg.of("&a팀 '" + team.name() + "' 에 가입했습니다."));
                for (UUID member : team.members().keySet()) {
                    if (member.equals(player.getUniqueId())) continue;
                    Player online = Bukkit.getPlayer(member);
                    if (online != null) online.sendMessage(Msg.of("&e" + player.getName() + " 님이 팀에 가입했습니다."));
                }
            }
            case NO_INVITE -> player.sendMessage(Msg.of("&c받은 팀 초대가 없습니다."));
            case TEAM_FULL -> player.sendMessage(Msg.of("&c팀 최대 인원에 도달했습니다."));
            case ALREADY_IN_TEAM -> player.sendMessage(Msg.of("&c이미 팀에 소속되어 있습니다."));
        }
        return true;
    }

    private boolean leave(CommandSender sender) {
        if (!requirePlayer(sender)) return true;
        Player player = (Player) sender;
        var result = teamManager.leave(player);
        switch (result) {
            case OK -> player.sendMessage(Msg.of("&e팀을 탈퇴했습니다."));
            case NOT_IN_TEAM -> player.sendMessage(Msg.of("&c팀에 소속되어 있지 않습니다."));
            case LEADER_MUST_DISBAND -> player.sendMessage(Msg.of("&c팀장은 탈퇴할 수 없습니다. /team disband 를 사용하세요."));
        }
        return true;
    }

    private boolean kick(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) return true;
        Player player = (Player) sender;
        Team team = teamManager.getTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(Msg.of("&c팀에 소속되어 있지 않습니다."));
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(Msg.of("&c사용법: /team kick <player>"));
            return true;
        }
        OfflinePlayerLookup lookup = OfflinePlayerLookup.byName(args[0]);
        if (lookup == null) {
            player.sendMessage(Msg.of("&c해당 플레이어를 찾을 수 없습니다."));
            return true;
        }
        var result = teamManager.kick(team, player, lookup.uuid());
        switch (result) {
            case OK -> player.sendMessage(Msg.of("&e" + lookup.name() + " 님을 팀에서 추방했습니다."));
            case NOT_LEADER -> player.sendMessage(Msg.of("&c팀장만 추방할 수 있습니다."));
            case TARGET_NOT_IN_TEAM -> player.sendMessage(Msg.of("&c해당 플레이어는 팀원이 아닙니다."));
            case CANNOT_KICK_SELF -> player.sendMessage(Msg.of("&c자기 자신은 추방할 수 없습니다."));
        }
        return true;
    }

    private boolean disband(CommandSender sender) {
        if (!requirePlayer(sender)) return true;
        Player player = (Player) sender;
        Team team = teamManager.getTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(Msg.of("&c팀에 소속되어 있지 않습니다."));
            return true;
        }
        if (teamManager.disband(team, player)) {
            for (UUID member : team.members().keySet()) {
                Player online = Bukkit.getPlayer(member);
                if (online != null) online.sendMessage(Msg.of("&c팀 '" + team.name() + "' 이(가) 해체되었습니다."));
            }
        } else {
            player.sendMessage(Msg.of("&c팀장만 팀을 해체할 수 있습니다."));
        }
        return true;
    }

    private boolean chat(CommandSender sender) {
        if (!requirePlayer(sender)) return true;
        Player player = (Player) sender;
        if (teamManager.getTeam(player.getUniqueId()) == null) {
            player.sendMessage(Msg.of("&c팀에 소속되어 있지 않습니다."));
            return true;
        }
        boolean enabled = teamManager.toggleTeamChat(player.getUniqueId());
        player.sendMessage(enabled ? Msg.of("&a팀 채팅 모드를 켰습니다.") : Msg.of("&e팀 채팅 모드를 껐습니다."));
        return true;
    }

    private boolean friendlyFire(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) return true;
        Player player = (Player) sender;
        Team team = teamManager.getTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(Msg.of("&c팀에 소속되어 있지 않습니다."));
            return true;
        }
        if (!team.isLeader(player.getUniqueId())) {
            player.sendMessage(Msg.of("&c팀장만 설정할 수 있습니다."));
            return true;
        }
        boolean next = !team.friendlyFire();
        if (args.length >= 1) {
            next = args[0].equalsIgnoreCase("on");
        }
        teamManager.setFriendlyFire(team, next);
        player.sendMessage(next ? Msg.of("&aFriendly Fire 를 켰습니다.") : Msg.of("&eFriendly Fire 를 껐습니다."));
        return true;
    }

    private boolean setHome(CommandSender sender) {
        if (!requirePlayer(sender)) return true;
        Player player = (Player) sender;
        Team team = teamManager.getTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(Msg.of("&c팀에 소속되어 있지 않습니다."));
            return true;
        }
        if (!team.isLeader(player.getUniqueId())) {
            player.sendMessage(Msg.of("&c팀장만 팀 홈을 설정할 수 있습니다."));
            return true;
        }
        teamManager.setHome(team, player.getLocation());
        player.sendMessage(Msg.of("&a팀 홈을 설정했습니다."));
        return true;
    }

    private boolean home(CommandSender sender) {
        if (!requirePlayer(sender)) return true;
        Player player = (Player) sender;
        Team team = teamManager.getTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(Msg.of("&c팀에 소속되어 있지 않습니다."));
            return true;
        }
        if (team.home() == null) {
            player.sendMessage(Msg.of("&c팀 홈이 설정되어 있지 않습니다."));
            return true;
        }
        tpManager.requestTeleport(player, team.home());
        return true;
    }

    private boolean setMax(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) return true;
        Player player = (Player) sender;
        Team team = teamManager.getTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(Msg.of("&c팀에 소속되어 있지 않습니다."));
            return true;
        }
        if (!team.isLeader(player.getUniqueId())) {
            player.sendMessage(Msg.of("&c팀장만 설정할 수 있습니다."));
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(Msg.of("&c사용법: /team setmax <인원수>"));
            return true;
        }
        int value;
        try {
            value = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(Msg.of("&c숫자를 입력하세요."));
            return true;
        }
        int cap = teamManager.maxMembersHardCap();
        if (value < team.size() || (value > cap && !player.hasPermission("pillage.admin"))) {
            player.sendMessage(Msg.of("&c인원 수는 현재 인원 이상, 최대 " + cap + " 이하여야 합니다."));
            return true;
        }
        teamManager.setMaxMembers(team, value);
        player.sendMessage(Msg.of("&a팀 최대 인원을 " + value + " 명으로 설정했습니다."));
        return true;
    }

    private boolean list(CommandSender sender) {
        if (!requirePlayer(sender)) return true;
        Player player = (Player) sender;
        Team team = teamManager.getTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(Msg.of("&c팀에 소속되어 있지 않습니다."));
            return true;
        }
        player.sendMessage(Msg.of("&6=== " + team.name() + " (" + team.size() + "/" + team.maxMembers() + ") ==="));
        for (var entry : team.members().entrySet()) {
            Player online = Bukkit.getPlayer(entry.getKey());
            String name = online != null ? online.getName() : Bukkit.getOfflinePlayer(entry.getKey()).getName();
            String status = online != null
                    ? " &a(온라인, HP " + (int) online.getHealth() + ")"
                    : " &7(오프라인)";
            player.sendMessage(Msg.of("&f- " + name + " &7[" + entry.getValue() + "]" + status));
        }
        return true;
    }

    private boolean info(CommandSender sender, String[] args) {
        Team team;
        if (args.length >= 1) {
            team = teamManager.getTeamByName(args[0]);
        } else {
            if (!requirePlayer(sender)) return true;
            team = teamManager.getTeam(((Player) sender).getUniqueId());
        }
        if (team == null) {
            sender.sendMessage(Msg.of("&c팀을 찾을 수 없습니다."));
            return true;
        }
        sender.sendMessage(Msg.of("&6=== " + team.name() + " 정보 ==="));
        sender.sendMessage(Msg.of("&f인원: " + team.size() + "/" + team.maxMembers()));
        sender.sendMessage(Msg.of("&fFriendly Fire: " + (team.friendlyFire() ? "ON" : "OFF")));
        sender.sendMessage(Msg.of("&f킬: " + team.kills() + " / 약탈 점수: " + team.lootScore()));
        sender.sendMessage(Msg.of("&f레이드 방어: " + team.raidsDefended() + " / 레이드 성공: " + team.raidsWon()));
        return true;
    }

    private boolean top(CommandSender sender, String[] args) {
        boolean loot = args.length >= 1 && args[0].equalsIgnoreCase("loot");
        List<Team> top = loot ? teamManager.topByLootScore(10) : teamManager.topByKills(10);
        sender.sendMessage(Msg.of(loot ? "&6=== 약탈 점수 랭킹 ===" : "&6=== 팀 킬 랭킹 ==="));
        int rank = 1;
        for (Team team : top) {
            int value = loot ? team.lootScore() : team.kills();
            sender.sendMessage(Msg.of("&f" + rank++ + ". " + team.name() + " &7- " + value));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS;
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("kick"))) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("ff")) {
            return List.of("on", "off");
        }
        return List.of();
    }

    private record OfflinePlayerLookup(UUID uuid, String name) {
        static OfflinePlayerLookup byName(String name) {
            Player online = Bukkit.getPlayerExact(name);
            if (online != null) {
                return new OfflinePlayerLookup(online.getUniqueId(), online.getName());
            }
            var offline = Bukkit.getOfflinePlayer(name);
            if (offline.hasPlayedBefore()) {
                return new OfflinePlayerLookup(offline.getUniqueId(), offline.getName());
            }
            return null;
        }
    }
}
