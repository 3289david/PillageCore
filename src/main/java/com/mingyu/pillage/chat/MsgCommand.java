package com.mingyu.pillage.chat;

import com.mingyu.pillage.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class MsgCommand implements CommandExecutor {

    private final ChatManager chatManager;

    public MsgCommand(ChatManager chatManager) {
        this.chatManager = chatManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(Msg.of("&c사용법: /msg <player> <메시지>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(Msg.of("&c해당 플레이어를 찾을 수 없습니다."));
            return true;
        }
        if (target.equals(player)) {
            player.sendMessage(Msg.of("&c자신에게 귓속말을 보낼 수 없습니다."));
            return true;
        }
        String message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        String filtered = chatManager.filter(message);

        player.sendMessage(Msg.of("&7[나 -> " + target.getName() + "] &f" + filtered));
        target.sendMessage(Msg.of("&7[" + player.getName() + " -> 나] &f" + filtered));

        chatManager.setLastWhisper(target.getUniqueId(), player.getUniqueId());
        chatManager.setLastWhisper(player.getUniqueId(), target.getUniqueId());
        return true;
    }
}
