package com.mingyu.pillage.admin;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Admin-only "staff mode" (pillage.admin, gated in plugin.yml) - toggles real spectator gamemode
 *  rather than faking invisibility with hidePlayer()/setInvisible(). Manually hiding a player from
 *  every viewer only fights symptoms (still solid, still takes hits, still needs onJoin bookkeeping
 *  for anyone who connects afterward) and is easy to get wrong; actual GameMode.SPECTATOR already
 *  gives free noclip flight and is invisible to every non-spectator viewer natively at the
 *  protocol level, with zero extra bookkeeping required. */
public final class StaffModeManager {

    private final Map<UUID, GameMode> previousGameMode = new HashMap<>();

    public boolean isVanished(UUID uuid) {
        return previousGameMode.containsKey(uuid);
    }

    public boolean toggle(Player staff) {
        UUID uuid = staff.getUniqueId();
        if (previousGameMode.containsKey(uuid)) {
            GameMode restore = previousGameMode.remove(uuid);
            staff.setGameMode(restore);
            return false;
        } else {
            previousGameMode.put(uuid, staff.getGameMode());
            staff.setGameMode(GameMode.SPECTATOR);
            return true;
        }
    }
}
