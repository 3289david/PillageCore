package com.mingyu.pillage.combat;

import com.mingyu.pillage.util.Msg;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Shows a countdown boss bar to a player for as long as they're combat-tagged - the exact same
 *  30-second (configurable) window that blocks their teleports and kills them on combat-log,
 *  including any extension from being hit again. Only ever driven by {@link CombatTagManager},
 *  which itself is only ever tagged by player-vs-player hits - mobs never start or extend it. */
public final class CombatBossBarManager implements Listener {

    private final CombatTagManager combatTagManager;
    private final Map<UUID, BossBar> activeBars = new HashMap<>();

    public CombatBossBarManager(CombatTagManager combatTagManager) {
        this.combatTagManager = combatTagManager;
    }

    public void start(JavaPlugin plugin) {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                if (combatTagManager.isInCombat(uuid)) {
                    show(player, combatTagManager.remainingSeconds(uuid));
                } else {
                    hide(player);
                }
            }
        }, 20L, 20L);
    }

    private void show(Player player, long remainingSeconds) {
        float progress = clamp((float) remainingSeconds / combatTagManager.tagDurationSeconds());
        Component title = Msg.of("&c&l⚔ 전투 중 &f- &e" + remainingSeconds + "초 후 해제");
        BossBar bar = activeBars.get(player.getUniqueId());
        if (bar == null) {
            bar = BossBar.bossBar(title, progress, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
            activeBars.put(player.getUniqueId(), bar);
            player.showBossBar(bar);
        } else {
            bar.name(title);
            bar.progress(progress);
        }
    }

    private void hide(Player player) {
        BossBar bar = activeBars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    private float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        hide(event.getPlayer());
    }
}
