package com.mingyu.pillage.donor;

import com.mingyu.pillage.combat.CombatTagManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * While a donor is in combat, strip anything that would make them stand out or be
 * easier to track (nametag color, pet). Chat/tab styling is untouched - only the
 * PvP-relevant visuals toggle. Restored automatically once combat ends.
 */
public final class DonorCombatVisibilityManager {

    private final DonorManager donorManager;
    private final CombatTagManager combatTagManager;
    private final DonorNametagManager nametagManager;
    private final DonorPetManager petManager;
    private final Set<UUID> hidden = new HashSet<>();

    public DonorCombatVisibilityManager(DonorManager donorManager, CombatTagManager combatTagManager,
                                         DonorNametagManager nametagManager, DonorPetManager petManager) {
        this.donorManager = donorManager;
        this.combatTagManager = combatTagManager;
        this.nametagManager = nametagManager;
        this.petManager = petManager;
    }

    public void start(JavaPlugin plugin) {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                if (!donorManager.isDonor(uuid)) continue;

                boolean inCombat = combatTagManager.isInCombat(uuid);
                boolean currentlyHidden = hidden.contains(uuid);

                if (inCombat && !currentlyHidden) {
                    nametagManager.hideForCombat(player);
                    petManager.hideForCombat(uuid);
                    hidden.add(uuid);
                } else if (!inCombat && currentlyHidden) {
                    nametagManager.refresh(player);
                    petManager.restoreAfterCombat(player);
                    hidden.remove(uuid);
                }
            }
        }, 20L, 20L);
    }

    public boolean isHidden(UUID uuid) {
        return hidden.contains(uuid);
    }
}
