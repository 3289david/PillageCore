package com.mingyu.pillage.qol;

import com.mingyu.pillage.data.dao.DeathLocationDao;
import com.mingyu.pillage.tp.TpManager;
import com.mingyu.pillage.util.Msg;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class DeathCommand implements CommandExecutor {

    private final DeathLocationDao deathLocationDao;
    private final TpManager tpManager;

    public DeathCommand(DeathLocationDao deathLocationDao, TpManager tpManager) {
        this.deathLocationDao = deathLocationDao;
        this.tpManager = tpManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        Location location = deathLocationDao.load(player.getUniqueId());
        if (location == null) {
            player.sendMessage(Msg.of("&c기록된 사망 위치가 없습니다."));
            return true;
        }
        tpManager.requestTeleport(player, location);
        return true;
    }
}
