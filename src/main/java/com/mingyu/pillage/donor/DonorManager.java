package com.mingyu.pillage.donor;

import com.mingyu.pillage.data.dao.DonorDao;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DonorManager {

    private static final TextColor GRADIENT_START = TextColor.color(0xFFD700); // gold
    private static final TextColor GRADIENT_END = TextColor.color(0xFF66FF);   // pink/purple
    private static final String DEFAULT_BADGE = "★";

    private final DonorDao donorDao;
    private final Map<UUID, String> donors = new HashMap<>();

    public DonorManager(DonorDao donorDao) {
        this.donorDao = donorDao;
    }

    public void loadAll() {
        donors.clear();
        donors.putAll(donorDao.loadAll());
    }

    public boolean isDonor(UUID uuid) {
        return donors.containsKey(uuid);
    }

    public void add(UUID uuid, String badge, UUID addedBy) {
        String actualBadge = (badge == null || badge.isBlank()) ? DEFAULT_BADGE : badge;
        donorDao.add(uuid, actualBadge, addedBy);
        donors.put(uuid, actualBadge);
    }

    public void remove(UUID uuid) {
        donorDao.remove(uuid);
        donors.remove(uuid);
    }

    public Map<UUID, String> all() {
        return donors;
    }

    public String badge(UUID uuid) {
        return donors.getOrDefault(uuid, DEFAULT_BADGE);
    }

    /** Gradient-colored player name, for chat/tab list. */
    public Component gradientName(String name) {
        Component result = Component.empty();
        int len = name.length();
        for (int i = 0; i < len; i++) {
            float t = len <= 1 ? 0f : (float) i / (len - 1);
            TextColor color = TextColor.lerp(t, GRADIENT_START, GRADIENT_END);
            result = result.append(Component.text(String.valueOf(name.charAt(i)), color));
        }
        return result;
    }

    /** Badge + gradient name, for players who are donors. Falls back to plain white name otherwise. */
    public Component displayName(Player player) {
        if (!isDonor(player.getUniqueId())) {
            return Component.text(player.getName(), TextColor.color(0xFFFFFF));
        }
        return Component.text(badge(player.getUniqueId()) + " ", TextColor.color(0xFFD700))
                .decorate(TextDecoration.BOLD)
                .append(gradientName(player.getName()));
    }
}
