package com.mingyu.pillage.minigame;

import com.mingyu.pillage.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class MinigameCommand implements CommandExecutor, TabCompleter {

    private final MinigameManager manager;

    public MinigameCommand(MinigameManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "join" -> handleJoin(player, args);
            case "leave" -> {
                manager.leave(player);
            }
            case "list" -> handleList(player);
            default -> sendUsage(player);
        }
        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage(Msg.of("&c사용법: /minigame <join <종류>|leave|list>"));
        player.sendMessage(Msg.of("&7종류: spleef, tntrun, parkour, tag"));
    }

    private void handleJoin(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Msg.of("&c사용법: /minigame join <spleef|tntrun|parkour|tag>"));
            return;
        }
        MinigameType type = MinigameType.fromArg(args[1]);
        if (type == null) {
            player.sendMessage(Msg.of("&c그런 미니게임이 없습니다. &7(spleef, tntrun, parkour, tag)"));
            return;
        }
        manager.join(player, type);
    }

    private void handleList(Player player) {
        player.sendMessage(Msg.of("&6&l===== 미니게임 현황 ====="));
        for (String line : manager.statusLines()) {
            player.sendMessage(Msg.of("&7" + line));
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("join", "leave", "list");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
            return Arrays.stream(MinigameType.values()).map(t -> t.name().toLowerCase()).collect(Collectors.toList());
        }
        return List.of();
    }
}
