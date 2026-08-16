package com.mingyu.pillage.endwar;

import com.mingyu.pillage.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class EndWarCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final EndWarManager manager;

    public EndWarCommand(JavaPlugin plugin, EndWarManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        if (args.length == 0) {
            handleInfo(player);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "join" -> manager.join(player);
            case "leave" -> manager.leave(player);
            case "info" -> handleInfo(player);
            case "start" -> handleStart(player, args);
            case "cancel" -> handleCancel(player);
            case "schedule" -> handleSchedule(player, args);
            default -> sendUsage(player);
        }
        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage(Msg.of("&c사용법: /endwar [join|leave|info]"));
        if (player.hasPermission("pillage.admin")) {
            player.sendMessage(Msg.of("&c관리자: /endwar start confirm | /endwar cancel | /endwar schedule <일(1-28)> <시(0-23)>"));
        }
    }

    private void handleInfo(Player player) {
        player.sendMessage(Msg.of("&6&l===== 엔드대전 현황 ====="));
        player.sendMessage(Msg.of("&f다음 정기 일정: &e" + manager.nextScheduleDescription()));
        player.sendMessage(Msg.of("&f현재 진행 중: &e" + (manager.isOngoing() ? "예" : "아니오")));
        if (!manager.isOngoing()) {
            player.sendMessage(Msg.of("&f참가 신청 인원: &e" + manager.signupCount() + "명 &7(/endwar join)"));
        }
    }

    private void handleStart(Player player, String[] args) {
        if (!player.hasPermission("pillage.admin")) {
            player.sendMessage(Msg.of("&c권한이 없습니다."));
            return;
        }
        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            player.sendMessage(Msg.of("&c현재 참가 신청된 인원으로 즉시 시작합니다. &f/endwar start confirm &c으로 확정하세요."));
            return;
        }
        manager.forceStart(player);
    }

    private void handleCancel(Player player) {
        if (!player.hasPermission("pillage.admin")) {
            player.sendMessage(Msg.of("&c권한이 없습니다."));
            return;
        }
        manager.adminCancel(player);
    }

    private void handleSchedule(Player player, String[] args) {
        if (!player.hasPermission("pillage.admin")) {
            player.sendMessage(Msg.of("&c권한이 없습니다."));
            return;
        }
        if (args.length < 3) {
            player.sendMessage(Msg.of("&c사용법: /endwar schedule <일(1-28)> <시(0-23)>"));
            return;
        }
        try {
            int day = Integer.parseInt(args[1]);
            int hour = Integer.parseInt(args[2]);
            if (day < 1 || day > 28 || hour < 0 || hour > 23) {
                player.sendMessage(Msg.of("&c일은 1~28, 시는 0~23 사이여야 합니다."));
                return;
            }
            plugin.getConfig().set("endwar.enabled", true);
            plugin.getConfig().set("endwar.day-of-month", day);
            plugin.getConfig().set("endwar.hour", hour);
            plugin.saveConfig();
            player.sendMessage(Msg.of("&a엔드대전을 매달 " + day + "일 " + hour + "시로 예약했습니다."));
        } catch (NumberFormatException e) {
            player.sendMessage(Msg.of("&c숫자를 입력하세요."));
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("join", "leave", "info", "start", "cancel", "schedule");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            return List.of("confirm");
        }
        return List.of();
    }
}
