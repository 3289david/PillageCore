package com.mingyu.pillage.qol;

import com.mingyu.pillage.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class TpsCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        double[] tps = Bukkit.getServer().getTPS();
        sender.sendMessage(Msg.of(String.format("&fTPS (1m/5m/15m): &e%.2f&f, &e%.2f&f, &e%.2f",
                tps[0], tps[1], tps[2])));
        return true;
    }
}
