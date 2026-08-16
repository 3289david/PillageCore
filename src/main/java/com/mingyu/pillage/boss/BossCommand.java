package com.mingyu.pillage.boss;

import com.mingyu.pillage.data.dao.BossRewardDao;
import com.mingyu.pillage.tp.TpManager;
import com.mingyu.pillage.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class BossCommand implements CommandExecutor, TabCompleter {

    private final BossManager bossManager;
    private final BossRewardDao rewardDao;
    private final TpManager tpManager;

    public BossCommand(BossManager bossManager, BossRewardDao rewardDao, TpManager tpManager) {
        this.bossManager = bossManager;
        this.rewardDao = rewardDao;
        this.tpManager = tpManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        if (args.length == 0) {
            tpManager.requestTeleport(player, bossManager.entryLocation());
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "info" -> handleInfo(player);
            case "spawn" -> handleSpawn(player);
            case "reset" -> handleReset(player, args);
            case "reward" -> handleReward(player, args);
            default -> sendUsage(player);
        }
        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage(Msg.of("&c사용법: /boss [info|spawn|reset confirm|reward <add|clear|list>]"));
    }

    private void handleInfo(Player player) {
        if (bossManager.isAlive()) {
            player.sendMessage(Msg.of("&6&l===== 보스 현황 ====="));
            player.sendMessage(Msg.of("&f체력: &c" + Math.round(bossManager.currentHealth()) + " / " + bossManager.currentMaxHealth()));
            player.sendMessage(Msg.of("&f누적 처치: &e" + bossManager.killCount() + "회"));
        } else {
            long remaining = bossManager.respawnSecondsRemaining();
            player.sendMessage(Msg.of("&7현재 보스가 없습니다. &f(" + remaining + "초 후 재등장, 누적 처치 " + bossManager.killCount() + "회)"));
        }
    }

    private void handleSpawn(Player player) {
        if (!player.hasPermission("pillage.admin")) {
            player.sendMessage(Msg.of("&c권한이 없습니다."));
            return;
        }
        if (bossManager.isAlive()) {
            player.sendMessage(Msg.of("&c이미 보스가 살아있습니다."));
            return;
        }
        bossManager.spawn();
        player.sendMessage(Msg.of("&a보스를 강제 소환했습니다."));
    }

    private void handleReset(Player player, String[] args) {
        if (!player.hasPermission("pillage.admin")) {
            player.sendMessage(Msg.of("&c권한이 없습니다."));
            return;
        }
        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            player.sendMessage(Msg.of("&c보스의 체력 스케일링과 처치 기록이 초기화됩니다. &f/boss reset confirm &c으로 확정하세요."));
            return;
        }
        bossManager.forceReset();
        player.sendMessage(Msg.of("&a보스 상태를 초기화하고 다시 소환했습니다."));
    }

    private void handleReward(Player player, String[] args) {
        if (!player.hasPermission("pillage.admin")) {
            player.sendMessage(Msg.of("&c권한이 없습니다."));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(Msg.of("&c사용법: /boss reward <add|clear|list>"));
            return;
        }
        switch (args[1].toLowerCase()) {
            case "add" -> {
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand.getType().isAir()) {
                    player.sendMessage(Msg.of("&c손에 보상으로 등록할 아이템을 들고 사용하세요."));
                    return;
                }
                rewardDao.add(hand.clone());
                player.sendMessage(Msg.of("&a손에 든 아이템을 보스 처치 보상 목록에 추가했습니다."));
            }
            case "clear" -> {
                rewardDao.clear();
                player.sendMessage(Msg.of("&a보스 처치 보상 목록을 비웠습니다."));
            }
            case "list" -> {
                List<ItemStack> items = rewardDao.loadAll();
                player.sendMessage(Msg.of("&6&l===== 보스 보상 목록 (" + items.size() + "개) ====="));
                for (ItemStack item : items) {
                    player.sendMessage(Msg.of("&7- &f" + item.getType() + " x" + item.getAmount()));
                }
            }
            default -> player.sendMessage(Msg.of("&c사용법: /boss reward <add|clear|list>"));
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("info", "spawn", "reset", "reward");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("reward")) {
            return List.of("add", "clear", "list");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
            return List.of("confirm");
        }
        return List.of();
    }
}
