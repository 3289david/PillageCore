package com.mingyu.pillage.minigame;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Per-type mutable game state - one of these exists per {@link MinigameType} for the whole
 *  plugin's lifetime, reused across rounds (cleared back to WAITING after each ENDING). */
final class MinigameSession {

    enum Phase { WAITING, COUNTDOWN, PLAYING, ENDING }

    final MinigameType type;
    Phase phase = Phase.WAITING;
    int countdownSeconds;
    int playSecondsRemaining;

    /** Everyone still active in the current round (alive for spleef/TNT run, still racing for
     *  parkour, not yet finished). Removed on elimination/finish/leave. */
    final Set<UUID> participants = new LinkedHashSet<>();
    /** TAG only - the subset of participants who are currently "it". */
    final Set<UUID> taggers = new HashSet<>();
    /** PARKOUR only - finish order, first to last. */
    final Set<UUID> finished = new LinkedHashSet<>();

    final Map<UUID, ItemStack[]> savedInventory = new HashMap<>();
    final Map<UUID, ItemStack[]> savedArmor = new HashMap<>();
    final Map<UUID, ItemStack> savedOffhand = new HashMap<>();
    final Map<UUID, Location> savedLocation = new HashMap<>();
    final Map<UUID, GameMode> savedGameMode = new HashMap<>();

    MinigameSession(MinigameType type) {
        this.type = type;
    }

    boolean isPlaying(UUID uuid) {
        return savedLocation.containsKey(uuid);
    }
}
