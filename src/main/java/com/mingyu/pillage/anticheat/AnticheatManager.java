package com.mingyu.pillage.anticheat;

import com.mingyu.pillage.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Very lenient anticheat: high thresholds, alerts staff only by default.
 * Punishment (kick) is opt-in via config and only after many repeated violations.
 */
public final class AnticheatManager {

    private final int alertViolations;
    private final long violationDecayMillis;
    private final boolean punishEnabled;
    private final int kickViolations;

    private final Map<UUID, Map<CheckType, Integer>> violationCounts = new HashMap<>();
    private final Map<UUID, Map<CheckType, Long>> lastViolationAt = new HashMap<>();

    public AnticheatManager(int alertViolations, int violationDecaySeconds, boolean punishEnabled, int kickViolations) {
        this.alertViolations = alertViolations;
        this.violationDecayMillis = TimeUnit.SECONDS.toMillis(violationDecaySeconds);
        this.punishEnabled = punishEnabled;
        this.kickViolations = kickViolations;
    }

    public void flag(Player player, CheckType type, String detail) {
        UUID uuid = player.getUniqueId();
        Map<CheckType, Long> lastMap = lastViolationAt.computeIfAbsent(uuid, k -> new EnumMap<>(CheckType.class));
        Map<CheckType, Integer> countMap = violationCounts.computeIfAbsent(uuid, k -> new EnumMap<>(CheckType.class));

        long now = System.currentTimeMillis();
        Long last = lastMap.get(type);
        int count = (last == null || now - last > violationDecayMillis) ? 0 : countMap.getOrDefault(type, 0);
        count++;
        countMap.put(type, count);
        lastMap.put(type, now);

        if (count == alertViolations) {
            alertStaff(player, type, detail, count);
        }

        if (punishEnabled && count >= kickViolations) {
            countMap.put(type, 0);
            player.kick(Msg.of("&c비정상적인 행동이 감지되어 접속이 종료되었습니다. (" + type.displayName() + ")"));
            alertStaff(player, type, detail + " (킥 처리됨)", count);
        }
    }

    private void alertStaff(Player player, CheckType type, String detail, int count) {
        String message = "&8[&cAC&8] &f" + player.getName() + " &7- &e" + type.displayName()
                + " &7의심 (" + count + "회) &8" + (detail == null ? "" : detail);
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("pillage.admin")) {
                staff.sendMessage(Msg.of(message));
            }
        }
        Bukkit.getLogger().info("[PillageCore-AC] " + player.getName() + " - " + type.displayName() + " x" + count);
    }

    public void clear(UUID uuid) {
        violationCounts.remove(uuid);
        lastViolationAt.remove(uuid);
    }
}
