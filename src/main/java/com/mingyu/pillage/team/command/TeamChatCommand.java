package com.mingyu.pillage.team.command;

import com.mingyu.pillage.team.Team;
import com.mingyu.pillage.team.TeamChatService;
import com.mingyu.pillage.team.TeamManager;
import com.mingyu.pillage.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class TeamChatCommand implements CommandExecutor {

    private final TeamManager teamManager;
    private final TeamChatService chatService;

    public TeamChatCommand(TeamManager teamManager, TeamChatService chatService) {
        this.teamManager = teamManager;
        this.chatService = chatService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        Team team = teamManager.getTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(Msg.of("&c팀에 소속되어 있지 않습니다."));
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(Msg.of("&c사용법: /tc <메시지>"));
            return true;
        }
        chatService.sendTeamMessage(team, player, String.join(" ", args));
        return true;
    }
}
