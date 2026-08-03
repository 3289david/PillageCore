package com.mingyu.pillage.instance;

import com.mingyu.pillage.tp.TpManager;
import com.mingyu.pillage.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

public final class MiniServerCommand implements CommandExecutor, TabCompleter {

    private final InstanceManager instanceManager;
    private final TpManager tpManager;

    public MiniServerCommand(InstanceManager instanceManager, TpManager tpManager) {
        this.instanceManager = instanceManager;
        this.tpManager = tpManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        if (args.length == 0) {
            handleList(player);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(player, args);
            case "join" -> handleJoin(player, args);
            case "list" -> handleList(player);
            case "delete" -> handleDelete(player, args);
            case "info" -> handleInfo(player);
            default -> sendUsage(player);
        }
        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage(Msg.of("&c사용법: /mini <create|join|list|delete|info> [이름]"));
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Msg.of("&c사용법: /mini create <이름>"));
            return;
        }
        if (tpManager.isTeleportBlocked(player)) return;
        String name = args[1];
        try {
            InstanceInfo info = instanceManager.create(player, name);
            instanceManager.teleportToInstance(player, info);
            player.sendMessage(Msg.of("&a미니서버 '" + name + "' 을(를) 생성했습니다! 이 서버의 관리자가 되었습니다."));
        } catch (IllegalArgumentException e) {
            player.sendMessage(Msg.of("&c" + e.getMessage()));
        }
    }

    private void handleJoin(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Msg.of("&c사용법: /mini join <이름>"));
            return;
        }
        InstanceInfo info = instanceManager.findByName(args[1]);
        if (info == null) {
            player.sendMessage(Msg.of("&c해당 이름의 미니서버를 찾을 수 없습니다. /mini list 로 확인하세요."));
            return;
        }
        if (tpManager.isTeleportBlocked(player)) return;
        instanceManager.teleportToInstance(player, info);
        player.sendMessage(Msg.of("&a미니서버 '" + info.name() + "' 에 입장했습니다."));
    }

    private void handleList(Player player) {
        List<InstanceInfo> instances = instanceManager.list();
        if (instances.isEmpty()) {
            player.sendMessage(Msg.of("&7아직 생성된 미니서버가 없습니다. &e/mini create <이름>&7 으로 만들어보세요."));
            return;
        }
        player.sendMessage(Msg.of("&6&l===== 미니서버 목록 ====="));
        for (InstanceInfo info : instances) {
            String ownerName = Bukkit.getOfflinePlayer(info.owner()).getName();
            player.sendMessage(Msg.of("&e" + info.name() + " &7(관리자: " + (ownerName == null ? "???" : ownerName) + ")"));
        }
        player.sendMessage(Msg.of("&8/mini join <이름> 으로 입장할 수 있습니다."));
    }

    private void handleDelete(Player player, String[] args) {
        InstanceInfo target;
        if (args.length >= 2) {
            target = instanceManager.findByName(args[1]);
        } else {
            target = instanceManager.find(instanceManager.resolveInstanceId(player.getWorld()));
        }
        if (target == null) {
            player.sendMessage(Msg.of("&c삭제할 미니서버를 찾을 수 없습니다. (전체 서버/허브는 삭제할 수 없습니다)"));
            return;
        }
        if (!instanceManager.isOwner(player, target)) {
            player.sendMessage(Msg.of("&c이 미니서버를 만든 사람만(또는 관리자만) 삭제할 수 있습니다."));
            return;
        }
        String name = target.name();
        instanceManager.delete(target);
        player.sendMessage(Msg.of("&a미니서버 '" + name + "' 을(를) 삭제했습니다."));
    }

    private void handleInfo(Player player) {
        String id = instanceManager.resolveInstanceId(player.getWorld());
        if (InstanceManager.MAIN_ID.equals(id)) {
            player.sendMessage(Msg.of("&7현재 &e전체 약탈 서버&7에 있습니다."));
            return;
        }
        if (InstanceManager.HUB_ID.equals(id)) {
            player.sendMessage(Msg.of("&7현재 &e허브&7에 있습니다."));
            return;
        }
        InstanceInfo info = instanceManager.find(id);
        if (info == null) {
            player.sendMessage(Msg.of("&7알 수 없는 서버입니다."));
            return;
        }
        String ownerName = Bukkit.getOfflinePlayer(info.owner()).getName();
        player.sendMessage(Msg.of("&e" + info.name() + " &7- 관리자: " + (ownerName == null ? "???" : ownerName)));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("create", "join", "list", "delete", "info");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("join") || args[0].equalsIgnoreCase("delete"))) {
            return instanceManager.list().stream().map(InstanceInfo::name).collect(Collectors.toList());
        }
        return List.of();
    }
}
