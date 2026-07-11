package com.mingyu.pillage.chat;

import com.mingyu.pillage.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class ReplyCommand implements CommandExecutor {

    private final ChatManager chatManager;

    public ReplyCommand(ChatManager chatManager) {
        this.chatManager = chatManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(Msg.of("&c사용법: /r <메시지>"));
            return true;
        }
        UUID lastFrom = chatManager.lastWhisperFrom(player.getUniqueId());
        if (lastFrom == null) {
            player.sendMessage(Msg.of("&c답장할 대상이 없습니다."));
            return true;
        }
        Player target = Bukkit.getPlayer(lastFrom);
        if (target == null) {
            player.sendMessage(Msg.of("&c해당 플레이어가 오프라인입니다."));
            return true;
        }
        String message = String.join(" ", args);
        String filtered = chatManager.filter(message);

        player.sendMessage(Msg.of("&7[나 -> " + target.getName() + "] &f" + filtered));
        target.sendMessage(Msg.of("&7[" + player.getName() + " -> 나] &f" + filtered));

        chatManager.setLastWhisper(target.getUniqueId(), player.getUniqueId());
        chatManager.setLastWhisper(player.getUniqueId(), target.getUniqueId());
        return true;
    }
}
