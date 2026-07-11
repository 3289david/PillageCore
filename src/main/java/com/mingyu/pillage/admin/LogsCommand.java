package com.mingyu.pillage.admin;

import com.mingyu.pillage.data.dao.BanLogDao;
import com.mingyu.pillage.data.dao.KillLogDao;
import com.mingyu.pillage.data.dao.TpLogDao;
import com.mingyu.pillage.data.dao.TradeLogDao;
import com.mingyu.pillage.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;

public final class LogsCommand implements CommandExecutor {

    private final KillLogDao killLogDao;
    private final BanLogDao banLogDao;
    private final TpLogDao tpLogDao;
    private final TradeLogDao tradeLogDao;
    private final SimpleDateFormat format = new SimpleDateFormat("MM-dd HH:mm");

    public LogsCommand(KillLogDao killLogDao, BanLogDao banLogDao, TpLogDao tpLogDao, TradeLogDao tradeLogDao) {
        this.killLogDao = killLogDao;
        this.banLogDao = banLogDao;
        this.tpLogDao = tpLogDao;
        this.tradeLogDao = tradeLogDao;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) {
            sender.sendMessage(Msg.of("&c사용법: /logs <kill|ban|tp> [개수]"));
            return true;
        }
        int limit = args.length >= 2 ? parseIntOr(args[1], 10) : 10;

        switch (args[0].toLowerCase()) {
            case "kill" -> {
                sender.sendMessage(Msg.of("&6=== 최근 킬 로그 ==="));
                for (var e : killLogDao.recent(limit)) {
                    sender.sendMessage(Msg.of("&7[" + format.format(new Date(e.timestamp())) + "] &f"
                            + (e.killer() == null ? "환경" : e.killer()) + " &7-> &f" + e.victim()
                            + " &7(" + (e.weapon() == null ? "-" : e.weapon()) + ")"));
                }
            }
            case "ban" -> {
                sender.sendMessage(Msg.of("&6=== 최근 밴 로그 ==="));
                for (var e : banLogDao.recent(limit)) {
                    sender.sendMessage(Msg.of("&7[" + format.format(new Date(e.timestamp())) + "] &f"
                            + e.staff() + " &7-> &f" + e.target() + " &7: &f" + e.reason()));
                }
            }
            case "tp" -> {
                sender.sendMessage(Msg.of("&6=== 최근 TP 로그 ==="));
                for (var e : tpLogDao.recent(limit)) {
                    sender.sendMessage(Msg.of("&7[" + format.format(new Date(e.timestamp())) + "] &f"
                            + e.player() + " &7(" + e.kind() + ") -> &f"
                            + e.world() + " " + (int) e.x() + "," + (int) e.y() + "," + (int) e.z()));
                }
            }
            case "trade" -> {
                sender.sendMessage(Msg.of("&6=== 최근 거래 로그 ==="));
                for (var e : tradeLogDao.recent(limit)) {
                    sender.sendMessage(Msg.of("&7[" + format.format(new Date(e.timestamp())) + "] &f"
                            + e.playerA() + " &7<-> &f" + e.playerB()));
                    sender.sendMessage(Msg.of("&7  " + e.playerA() + ": " + e.itemsA()));
                    sender.sendMessage(Msg.of("&7  " + e.playerB() + ": " + e.itemsB()));
                }
            }
            default -> sender.sendMessage(Msg.of("&c종류는 kill, ban, tp, trade 중 하나여야 합니다."));
        }
        return true;
    }

    private int parseIntOr(String s, int fallback) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
