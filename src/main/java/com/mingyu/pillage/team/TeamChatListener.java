package com.mingyu.pillage.team;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class TeamChatListener implements Listener {

    private final TeamManager teamManager;
    private final TeamChatService chatService;

    public TeamChatListener(TeamManager teamManager, TeamChatService chatService) {
        this.teamManager = teamManager;
        this.chatService = chatService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!teamManager.isTeamChatEnabled(player.getUniqueId())) return;

        Team team = teamManager.getTeam(player.getUniqueId());
        if (team == null) return;

        event.setCancelled(true);
        String plain = PlainTextComponentSerializer.plainText().serialize(event.message());
        chatService.sendTeamMessage(team, player, plain);
    }
}
