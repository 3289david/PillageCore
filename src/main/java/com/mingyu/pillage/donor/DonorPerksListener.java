package com.mingyu.pillage.donor;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ThreadLocalRandom;

public final class DonorPerksListener implements Listener {

    private final JavaPlugin plugin;
    private final DonorManager donorManager;
    private final DonorNametagManager nametagManager;
    private final DonorPetManager petManager;
    private final NamespacedKey decorativeFireworkKey;

    public DonorPerksListener(JavaPlugin plugin, DonorManager donorManager, DonorNametagManager nametagManager,
                               DonorPetManager petManager) {
        this.plugin = plugin;
        this.donorManager = donorManager;
        this.nametagManager = nametagManager;
        this.petManager = petManager;
        this.decorativeFireworkKey = new NamespacedKey(plugin, "decorative_firework");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        nametagManager.refresh(player);
        if (!donorManager.isDonor(player.getUniqueId())) return;

        petManager.spawnFor(player);

        Component message = Component.text("✦ ")
                .append(donorManager.gradientName(player.getName()))
                .append(Component.text(" 님이 화려하게 입장했습니다! ✦"));
        event.joinMessage(message);

        Location loc = player.getLocation();
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(0, 1, 0), 40, 0.5, 1, 0.5, 0.1);
        launchFirework(loc);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        petManager.despawnFor(player.getUniqueId());
        if (!donorManager.isDonor(player.getUniqueId())) return;

        Component message = Component.text("✦ ")
                .append(donorManager.gradientName(player.getName()))
                .append(Component.text(" 님이 퇴장했습니다. ✦"));
        event.quitMessage(message);
    }

    // Purely decorative - a firework detonating this close to the player would otherwise deal
    // it explosion damage like any other firework rocket.
    @EventHandler(ignoreCancelled = true)
    public void onFireworkDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Firework firework
                && firework.getPersistentDataContainer().has(decorativeFireworkKey, PersistentDataType.BYTE)) {
            event.setCancelled(true);
        }
    }

    private void launchFirework(Location location) {
        Firework firework = location.getWorld().spawn(location, Firework.class);
        firework.getPersistentDataContainer().set(decorativeFireworkKey, PersistentDataType.BYTE, (byte) 1);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .withColor(org.bukkit.Color.fromRGB(255, 215, 0), org.bukkit.Color.fromRGB(255, 102, 255))
                .withFade(org.bukkit.Color.WHITE)
                .with(FireworkEffect.Type.BALL_LARGE)
                .trail(true)
                .flicker(true)
                .build());
        meta.setPower(1);
        firework.setFireworkMeta(meta);
        Bukkit.getScheduler().runTaskLater(plugin, firework::detonate,
                ThreadLocalRandom.current().nextInt(5, 15));
    }
}
