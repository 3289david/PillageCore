package com.mingyu.pillage.anticheat;

import com.mingyu.pillage.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;

/** Kicks anyone connecting with a modded client (Forge, Fabric, NeoForge, Quilt, and anything
 *  else matching config.yml's anti-mod-client.blocked-keywords) - both PvP-affecting client mods
 *  (freecam, hitbox/reach changes, X-ray-adjacent resource packs forced through mod loaders) and
 *  simple unfairness between modded and vanilla players are the concern, not any specific mod.
 *
 *  <p>Two independent signals, since neither alone is fully reliable: a mod loader's handshake
 *  registers a plugin channel (e.g. "fml:handshake" for Forge/NeoForge) within the very first
 *  few packets of a connection - checking that catches loaders immediately, before they can do
 *  anything. Not every loader necessarily registers a channel we'd recognize, though, so shortly
 *  after join this also checks the client's self-reported "brand" string as a broader fallback. */
public final class ModClientGuardListener implements Listener {

    private final JavaPlugin plugin;

    public ModClientGuardListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("anti-mod-client.enabled", true);
    }

    private List<String> blockedKeywords() {
        return plugin.getConfig().getStringList("anti-mod-client.blocked-keywords");
    }

    private boolean matchesBlockedKeyword(String value) {
        if (value == null) return false;
        String lower = value.toLowerCase(Locale.ROOT);
        for (String keyword : blockedKeywords()) {
            if (lower.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onRegisterChannel(PlayerRegisterChannelEvent event) {
        if (!enabled()) return;
        Player player = event.getPlayer();
        if (player.hasPermission("pillage.admin")) return;
        if (matchesBlockedKeyword(event.getChannel())) {
            kick(player, "channel:" + event.getChannel());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled()) return;
        Player player = event.getPlayer();
        if (player.hasPermission("pillage.admin")) return;

        // The brand packet isn't guaranteed to have arrived by the time PlayerJoinEvent fires -
        // give the client a couple seconds, then check whatever it reported (if anything).
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline() || player.hasPermission("pillage.admin")) return;
            String brand = player.getClientBrandName();
            if (matchesBlockedKeyword(brand)) {
                kick(player, "brand:" + brand);
            }
        }, 40L);
    }

    private void kick(Player player, String detectedVia) {
        String message = plugin.getConfig().getString("anti-mod-client.kick-message",
                "&c이 서버는 모드 클라이언트의 접속을 허용하지 않습니다.");
        player.kick(Msg.of(message));
        plugin.getLogger().info("[ModClientGuard] " + player.getName() + " 접속을 모드 클라이언트로 감지해 차단했습니다. (" + detectedVia + ")");
    }
}
