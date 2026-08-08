package com.mingyu.pillage.pvp;

import com.mingyu.pillage.util.Msg;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class KillStreakManager {

    private static final int[] MILESTONES = {5, 10, 20};

    private final Map<UUID, Integer> streaks = new HashMap<>();

    public void onKill(Player killer) {
        int streak = streaks.merge(killer.getUniqueId(), 1, Integer::sum);
        for (int milestone : MILESTONES) {
            if (streak == milestone) {
                // Instance-local, not server-wide - see PvpListener's kill feed for why.
                var message = Msg.of("&c&l" + killer.getName() + " &6님이 &e" + milestone + "연킬&6을 달성했습니다!");
                for (Player online : killer.getWorld().getPlayers()) {
                    online.sendMessage(message);
                }
            }
        }
    }

    public void onDeath(Player victim) {
        streaks.remove(victim.getUniqueId());
    }

    public int streakOf(UUID uuid) {
        return streaks.getOrDefault(uuid, 0);
    }
}
