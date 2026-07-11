package com.mingyu.pillage.combat;

import com.mingyu.pillage.tp.TeleportGuard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class CombatManager implements TeleportGuard {

    private final long tagDurationMillis;
    private final boolean logoutPenalty;

    private final Map<UUID, Long> combatUntil = new HashMap<>();

    public CombatManager(int tagDurationSeconds, boolean logoutPenalty) {
        this.tagDurationMillis = TimeUnit.SECONDS.toMillis(tagDurationSeconds);
        this.logoutPenalty = logoutPenalty;
    }

    public void tag(UUID uuid) {
        combatUntil.put(uuid, System.currentTimeMillis() + tagDurationMillis);
    }

    public boolean isInCombat(UUID uuid) {
        Long until = combatUntil.get(uuid);
        return until != null && until > System.currentTimeMillis();
    }

    public long remainingSeconds(UUID uuid) {
        Long until = combatUntil.get(uuid);
        if (until == null) return 0;
        return Math.max(0, (until - System.currentTimeMillis()) / 1000);
    }

    public void clear(UUID uuid) {
        combatUntil.remove(uuid);
    }

    public boolean logoutPenaltyEnabled() {
        return logoutPenalty;
    }

    @Override
    public String blockReason(org.bukkit.entity.Player player) {
        if (isInCombat(player.getUniqueId())) {
            return "&c전투 중에는 텔레포트를 사용할 수 없습니다. (" + remainingSeconds(player.getUniqueId()) + "초 남음)";
        }
        return null;
    }
}
