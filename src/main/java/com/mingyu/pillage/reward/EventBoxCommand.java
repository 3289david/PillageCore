package com.mingyu.pillage.reward;

import com.mingyu.pillage.data.dao.EventBoxClaimDao;
import com.mingyu.pillage.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class EventBoxCommand implements CommandExecutor {

    private final EventBoxManager eventBoxManager;
    private final EventBoxClaimDao claimDao;

    public EventBoxCommand(EventBoxManager eventBoxManager, EventBoxClaimDao claimDao) {
        this.eventBoxManager = eventBoxManager;
        this.claimDao = claimDao;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("get")) {
            return handleGet(sender);
        }

        if (!sender.hasPermission("pillage.admin")) {
            sender.sendMessage(Msg.of("&c권한이 없습니다."));
            return true;
        }
        if (args.length < 2 || !args[0].equalsIgnoreCase("give")) {
            sender.sendMessage(Msg.of("&c사용법: /eventbox give <player> [수량]"));
            return true;
        }
        return handleGive(sender, args);
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        // Offline-tolerant on purpose: an admin should be able to grant a box to a player who
        // isn't online right now - it just sits pending until they run /eventbox get.
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(Msg.of("&c해당 플레이어를 찾을 수 없습니다."));
            return true;
        }

        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Math.max(1, Integer.parseInt(args[2]));
            } catch (NumberFormatException ignored) {
            }
        }

        claimDao.addPending(target.getUniqueId(), amount);
        String name = target.getName() != null ? target.getName() : args[1];
        sender.sendMessage(Msg.of("&a" + name + " 님에게 이벤트 상자 " + amount + "개 지급을 예약했습니다. "
                + "&7(/eventbox get 으로 직접 수령)"));

        if (target.getPlayer() != null) {
            target.getPlayer().sendMessage(Msg.of("&d이벤트 상자 " + amount + "개가 도착했습니다! &e/eventbox get&d 으로 받으세요."));
        }
        return true;
    }

    private boolean handleGet(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        int pending = claimDao.consumePending(player.getUniqueId());
        if (pending <= 0) {
            player.sendMessage(Msg.of("&7받을 수 있는 이벤트 상자가 없습니다."));
            return true;
        }
        for (int i = 0; i < pending; i++) {
            ItemStack box = eventBoxManager.createBox();
            var leftover = player.getInventory().addItem(box);
            leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        }
        player.sendMessage(Msg.of("&d이벤트 상자 " + pending + "개를 받았습니다!"));
        return true;
    }
}
