package com.mingyu.pillage.team;

import com.mingyu.pillage.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class TeamChatService {

    public void sendTeamMessage(Team team, Player sender, String message) {
        String formatted = "&8[&6팀&8] &f" + sender.getName() + "&7: &f" + message;
        for (UUID member : team.members().keySet()) {
            Player online = Bukkit.getPlayer(member);
            if (online != null) {
                online.sendMessage(Msg.of(formatted));
            }
        }
    }
}
