package com.mingyu.pillage.tp;

import org.bukkit.entity.Player;

/**
 * Implemented by systems (combat tag, raid timer) that may need to block teleportation.
 */
public interface TeleportGuard {

    /**
     * @return a rejection message if teleportation should be blocked, or null if allowed.
     */
    String blockReason(Player player);
}
