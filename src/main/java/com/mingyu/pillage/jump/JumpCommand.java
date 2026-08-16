package com.mingyu.pillage.jump;

import com.mingyu.pillage.data.dao.JumpRecordDao;
import com.mingyu.pillage.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class JumpCommand implements CommandExecutor, TabCompleter {

    private final JumpManager manager;

    public JumpCommand(JumpManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        String sub = args.length == 0 ? "start" : args[0].toLowerCase();
        switch (sub) {
            case "start" -> manager.startRun(player);
            case "leave" -> manager.leave(player);
            case "top" -> handleTop(player);
            default -> player.sendMessage(Msg.of("&c사용법: /jump [start|leave|top]"));
        }
        return true;
    }

    private void handleTop(Player player) {
        player.sendMessage(Msg.of("&6&l===== 점프맵 기록 순위 ====="));
        List<JumpRecordDao.Record> top = manager.top(10);
        if (top.isEmpty()) {
            player.sendMessage(Msg.of("&7아직 기록이 없습니다."));
            return;
        }
        int rank = 1;
        for (JumpRecordDao.Record record : top) {
            player.sendMessage(Msg.of("&e" + rank + "위 &f" + record.name() + " &7- &a" + JumpManager.formatMillis(record.millis())));
            rank++;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("start", "leave", "top");
        }
        return List.of();
    }
}
