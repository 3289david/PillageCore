package com.mingyu.pillage.combat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Very small "can't flee" tag: only tracks that a player was recently in a 1v1
 * (player vs player) fight, so ender pearl / chorus fruit teleports can be blocked.
 * Does not touch plugin TP commands, logout penalties, etc. - kept intentionally minimal.
 */
public final class CombatTagManager {

    private final long tagDurationMillis;
    private final Map<UUID, Long> taggedUntil = new HashMap<>();

    public CombatTagManager(int tagDurationSeconds) {
        this.tagDurationMillis = TimeUnit.SECONDS.toMillis(tagDurationSeconds);
    }

    public void tag(UUID uuid) {
        taggedUntil.put(uuid, System.currentTimeMillis() + tagDurationMillis);
    }

    public boolean isInCombat(UUID uuid) {
        Long until = taggedUntil.get(uuid);
        return until != null && until > System.currentTimeMillis();
    }

    public long remainingSeconds(UUID uuid) {
        Long until = taggedUntil.get(uuid);
        if (until == null) return 0;
        return Math.max(0, (until - System.currentTimeMillis()) / 1000);
    }

    public void clear(UUID uuid) {
        taggedUntil.remove(uuid);
    }
}
