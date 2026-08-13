package com.mingyu.pillage.announce;

import com.mingyu.pillage.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class AnnounceCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final AnnounceManager announceManager;

    public AnnounceCommand(JavaPlugin plugin, AnnounceManager announceManager) {
        this.plugin = plugin;
        this.announceManager = announceManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("pillage.admin")) {
            sender.sendMessage(Msg.of("&c권한이 없습니다."));
            return true;
        }
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "add" -> handleAdd(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "list" -> handleList(sender);
            case "interval" -> handleInterval(sender, args);
            case "on" -> handleToggle(sender, true);
            case "off" -> handleToggle(sender, false);
            case "test" -> handleTest(sender);
            default -> sendUsage(sender);
        }
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Msg.of("&c사용법: /announce <add|remove|list|interval|on|off|test>"));
    }

    private void handleAdd(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Msg.of("&c사용법: /announce add <메시지>"));
            return;
        }
        String message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        List<String> messages = new ArrayList<>(plugin.getConfig().getStringList("announce.messages"));
        messages.add(message);
        plugin.getConfig().set("announce.messages", messages);
        plugin.saveConfig();
        announceManager.start();
        sender.sendMessage(Msg.of("&a공지 문구를 추가했습니다. &7(현재 " + messages.size() + "개)"));
    }

    private void handleRemove(CommandSender sender, String[] args) {
        List<String> messages = new ArrayList<>(plugin.getConfig().getStringList("announce.messages"));
        if (args.length < 2) {
            sender.sendMessage(Msg.of("&c사용법: /announce remove <번호> &7(/announce list 로 번호 확인)"));
            return;
        }
        int index;
        try {
            index = Integer.parseInt(args[1]) - 1;
        } catch (NumberFormatException e) {
            sender.sendMessage(Msg.of("&c번호는 숫자로 입력하세요."));
            return;
        }
        if (index < 0 || index >= messages.size()) {
            sender.sendMessage(Msg.of("&c그런 번호의 공지 문구가 없습니다."));
            return;
        }
        String removed = messages.remove(index);
        plugin.getConfig().set("announce.messages", messages);
        plugin.saveConfig();
        announceManager.start();
        sender.sendMessage(Msg.of("&a제거했습니다: &7" + removed));
    }

    private void handleList(CommandSender sender) {
        List<String> messages = plugin.getConfig().getStringList("announce.messages");
        boolean enabled = plugin.getConfig().getBoolean("announce.enabled", false);
        long interval = plugin.getConfig().getLong("announce.interval-minutes", 60);
        sender.sendMessage(Msg.of("&e공지 시스템: " + (enabled ? "&a켜짐" : "&c꺼짐") + " &7/ 간격: &f" + interval + "분"));
        if (messages.isEmpty()) {
            sender.sendMessage(Msg.of("&7등록된 공지 문구가 없습니다."));
            return;
        }
        for (int i = 0; i < messages.size(); i++) {
            sender.sendMessage(Msg.of("&7" + (i + 1) + ". &f" + messages.get(i)));
        }
    }

    private void handleInterval(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Msg.of("&c사용법: /announce interval <분>"));
            return;
        }
        int minutes;
        try {
            minutes = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Msg.of("&c분은 숫자로 입력하세요."));
            return;
        }
        if (minutes <= 0) {
            sender.sendMessage(Msg.of("&c1분 이상으로 설정하세요."));
            return;
        }
        plugin.getConfig().set("announce.interval-minutes", minutes);
        plugin.saveConfig();
        announceManager.start();
        sender.sendMessage(Msg.of("&a공지 간격을 " + minutes + "분으로 설정했습니다."));
    }

    private void handleToggle(CommandSender sender, boolean enabled) {
        plugin.getConfig().set("announce.enabled", enabled);
        plugin.saveConfig();
        announceManager.start();
        if (enabled && plugin.getConfig().getStringList("announce.messages").isEmpty()) {
            sender.sendMessage(Msg.of("&e등록된 공지 문구가 없어서 아직 아무것도 방송되지 않습니다. &f/announce add &e로 추가하세요."));
            return;
        }
        sender.sendMessage(enabled ? Msg.of("&a주기적 공지를 켰습니다.") : Msg.of("&c주기적 공지를 껐습니다."));
    }

    private void handleTest(CommandSender sender) {
        List<String> messages = plugin.getConfig().getStringList("announce.messages");
        if (messages.isEmpty()) {
            sender.sendMessage(Msg.of("&c등록된 공지 문구가 없습니다."));
            return;
        }
        announceManager.broadcastNext();
        sender.sendMessage(Msg.of("&a공지를 미리 방송했습니다."));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("add", "remove", "list", "interval", "on", "off", "test");
        }
        return List.of();
    }
}
