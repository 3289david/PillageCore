package com.mingyu.pillage.chat;

import com.mingyu.pillage.team.Team;
import com.mingyu.pillage.team.TeamManager;
import com.mingyu.pillage.util.Msg;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class GlobalChatListener implements Listener {

    private final ChatManager chatManager;
    private final TeamManager teamManager;

    public GlobalChatListener(ChatManager chatManager, TeamManager teamManager) {
        this.chatManager = chatManager;
        this.teamManager = teamManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        long remaining = chatManager.remainingCooldownMillis(player.getUniqueId());
        if (remaining > 0 && !player.hasPermission("pillage.admin")) {
            event.setCancelled(true);
            player.sendMessage(Msg.of("&c너무 빠르게 채팅하고 있습니다. " + (remaining / 1000 + 1) + "초 후 다시 시도하세요."));
            return;
        }
        chatManager.markMessaged(player.getUniqueId());

        String plain = PlainTextComponentSerializer.plainText().serialize(event.message());
        String filtered = chatManager.filter(plain);
        Component rendered = Msg.of(filtered);
        event.message(rendered);

        Team team = teamManager.getTeam(player.getUniqueId());
        String prefix = team != null ? "&8[&6" + team.name() + "&8] " : "";
        Component finalMessage = Msg.of(prefix + "&f" + player.getName() + "&7: &f" + filtered);

        event.renderer((source, displayName, message, viewer) -> finalMessage);

        notifyMentions(player, filtered);
    }

    private void notifyMentions(Player sender, String plainMessage) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(sender)) continue;
            if (plainMessage.toLowerCase().contains("@" + online.getName().toLowerCase())) {
                online.sendMessage(Msg.of("&e" + sender.getName() + " 님이 회원님을 언급했습니다!"));
                var soundKey = org.bukkit.Registry.SOUNDS.getKey(org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
                if (soundKey != null) {
                    online.playSound(Sound.sound(soundKey, Sound.Source.PLAYER, 1f, 1.5f));
                }
            }
        }
    }
}
