package com.mingyu.pillage.donor;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Colors the floating nametag above a donor's head using the server's main scoreboard.
 * Vanilla teams only support one flat color for the name itself (no per-character
 * gradient there), so this gives donors a bold gold badge prefix + a pink-tinted
 * name - the closest a scoreboard team can get to the chat gradient.
 */
public final class DonorNametagManager {

    private static final String TEAM_NAME = "pillage_donor";

    private final DonorManager donorManager;

    public DonorNametagManager(DonorManager donorManager) {
        this.donorManager = donorManager;
    }

    private Team team() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam(TEAM_NAME);
        if (team == null) {
            team = scoreboard.registerNewTeam(TEAM_NAME);
            team.prefix(Component.text("★ ", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
            team.color(NamedTextColor.LIGHT_PURPLE);
        }
        return team;
    }

    public void refresh(Player player) {
        Team team = team();
        if (donorManager.isDonor(player.getUniqueId())) {
            team.addEntry(player.getName());
        } else {
            team.removeEntry(player.getName());
        }
    }

    /** Strips the donor styling from the overhead nametag without touching donor status - used while in combat. */
    public void hideForCombat(Player player) {
        team().removeEntry(player.getName());
    }

    public void applyToOnlinePlayers(JavaPlugin plugin) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            refresh(player);
        }
    }
}
