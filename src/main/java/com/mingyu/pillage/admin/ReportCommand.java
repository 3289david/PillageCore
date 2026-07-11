package com.mingyu.pillage.admin;

import com.mingyu.pillage.data.dao.ReportLogDao;
import com.mingyu.pillage.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class ReportCommand implements CommandExecutor {

    private final ReportLogDao reportLogDao;

    public ReportCommand(ReportLogDao reportLogDao) {
        this.reportLogDao = reportLogDao;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player reporter)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        if (args.length < 2) {
            reporter.sendMessage(Msg.of("&c사용법: /report <player> <사유>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            reporter.sendMessage(Msg.of("&c해당 플레이어를 찾을 수 없습니다."));
            return true;
        }
        String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        reportLogDao.log(reporter.getUniqueId(), target.getUniqueId(), reason);

        reporter.sendMessage(Msg.of("&a신고가 접수되었습니다. 감사합니다."));
        String staffMessage = "&8[&c신고&8] &f" + reporter.getName() + " &7-> &f" + target.getName() + " &7: &f" + reason;
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("pillage.admin")) {
                staff.sendMessage(Msg.of(staffMessage));
            }
        }
        return true;
    }
}
