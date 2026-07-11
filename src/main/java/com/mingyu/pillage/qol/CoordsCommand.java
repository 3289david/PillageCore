package com.mingyu.pillage.qol;

import com.mingyu.pillage.team.Team;
import com.mingyu.pillage.team.TeamChatService;
import com.mingyu.pillage.team.TeamManager;
import com.mingyu.pillage.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class CoordsCommand implements CommandExecutor {

    private final TeamManager teamManager;
    private final TeamChatService teamChatService;

    public CoordsCommand(TeamManager teamManager, TeamChatService teamChatService) {
        this.teamManager = teamManager;
        this.teamChatService = teamChatService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        var loc = player.getLocation();
        String coords = loc.getWorld().getName() + " " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();

        Team team = teamManager.getTeam(player.getUniqueId());
        if (team != null) {
            teamChatService.sendTeamMessage(team, player, "&e좌표 공유: &f" + coords);
        } else {
            player.sendMessage(Msg.of("&f현재 좌표: &e" + coords));
        }
        return true;
    }
}
