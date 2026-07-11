package com.mingyu.pillage.stats;

import com.mingyu.pillage.data.dao.StatsDao;
import com.mingyu.pillage.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class StatsCommand implements CommandExecutor {

    private final StatsDao statsDao;

    public StatsCommand(StatsDao statsDao) {
        this.statsDao = statsDao;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        OfflinePlayer target;
        if (args.length >= 1) {
            target = Bukkit.getOfflinePlayer(args[0]);
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(Msg.of("&c콘솔에서는 대상 플레이어를 지정하세요: /stats <player>"));
            return true;
        }

        StatsDao.Stats stats = statsDao.get(target.getUniqueId());
        long hours = stats.playtimeSeconds() / 3600;
        long minutes = (stats.playtimeSeconds() % 3600) / 60;

        sender.sendMessage(Msg.of("&6=== " + target.getName() + " 통계 ==="));
        sender.sendMessage(Msg.of("&f킬: " + stats.kills() + "  &f데스: " + stats.deaths()
                + "  &fK/D: " + String.format("%.2f", stats.kd())));
        sender.sendMessage(Msg.of("&f플레이 시간: " + hours + "시간 " + minutes + "분"));
        sender.sendMessage(Msg.of("&f채굴량: " + stats.blocksMined() + " 블록"));
        return true;
    }
}
