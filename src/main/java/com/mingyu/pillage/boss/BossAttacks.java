package com.mingyu.pillage.boss;

import com.mingyu.pillage.util.Msg;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Vindicator;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** The 10 randomly-picked boss abilities. Every ability deals damage through the normal player
 *  damage pipeline (real damage, real death messages, real knockback) - only the boss's OWN
 *  incoming damage is intercepted into the custom HP pool by {@link BossListener}, never its
 *  outgoing damage. Deliberately no vanilla explosions/block placement anywhere here, so the
 *  arena never takes incidental damage. */
final class BossAttacks {

    static final int COUNT = 10;

    private BossAttacks() {
    }

    static void execute(int index, BossManager manager, LivingEntity boss, List<Player> nearby) {
        switch (index) {
            case 0 -> fireball(manager, boss, nearby);
            case 1 -> summon(manager, boss);
            case 2 -> slam(manager, boss, nearby);
            case 3 -> poisonCloud(manager, boss, nearby);
            case 4 -> teleportStrike(manager, boss, nearby);
            case 5 -> lightning(manager, boss, nearby);
            case 6 -> gravityPull(manager, boss, nearby);
            case 7 -> fireField(manager, boss, nearby);
            case 8 -> shockwave(manager, boss, nearby);
            case 9 -> blindCurse(manager, boss, nearby);
        }
    }

    private static Player randomTarget(List<Player> nearby) {
        return nearby.get(ThreadLocalRandom.current().nextInt(nearby.size()));
    }

    private static void fireball(BossManager manager, LivingEntity boss, List<Player> nearby) {
        Player target = randomTarget(nearby);
        World world = boss.getWorld();
        Location origin = boss.getEyeLocation();
        Vector dir = target.getEyeLocation().toVector().subtract(origin.toVector()).normalize();
        world.spawn(origin, SmallFireball.class, fb -> {
            fb.setShooter(boss);
            fb.setDirection(dir);
            fb.setYield(0f);
            fb.setIsIncendiary(false);
        });
        world.playSound(origin, Sound.ENTITY_GHAST_SHOOT, 1.5f, 0.6f);
        manager.broadcastToWorld("&c" + BossManager.BOSS_NAME + "&f이(가) 화염구를 발사합니다!");
    }

    private static void summon(BossManager manager, LivingEntity boss) {
        World world = boss.getWorld();
        for (int i = 0; i < 3; i++) {
            Location loc = boss.getLocation().clone().add(
                    ThreadLocalRandom.current().nextInt(-4, 5), 0, ThreadLocalRandom.current().nextInt(-4, 5));
            Vindicator vindicator = world.spawn(loc, Vindicator.class);
            vindicator.setRemoveWhenFarAway(true);
            manager.trackAdd(vindicator.getUniqueId());
        }
        world.playSound(boss.getLocation(), Sound.ENTITY_VINDICATOR_CELEBRATE, 1.5f, 1f);
        manager.broadcastToWorld("&5" + BossManager.BOSS_NAME + "&f이(가) 소환수를 불러냅니다!");
    }

    private static void slam(BossManager manager, LivingEntity boss, List<Player> nearby) {
        Location center = boss.getLocation();
        boss.getWorld().spawnParticle(Particle.EXPLOSION, center, 1);
        boss.getWorld().playSound(center, Sound.ENTITY_RAVAGER_ATTACK, 2f, 0.7f);
        for (Player player : nearby) {
            if (player.getLocation().distance(center) <= 6) {
                manager.damagePlayer(player, 6.0);
                manager.knockback(player, center, 1.4);
            }
        }
        manager.broadcastToWorld("&c" + BossManager.BOSS_NAME + "&f이(가) 강력하게 내려찍습니다!");
    }

    private static void poisonCloud(BossManager manager, LivingEntity boss, List<Player> nearby) {
        Location center = boss.getLocation();
        boss.getWorld().spawnParticle(Particle.WITCH, center, 60, 5, 1, 5, 0.05);
        for (Player player : nearby) {
            if (player.getLocation().distance(center) <= 8) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 1));
            }
        }
        manager.broadcastToWorld("&2" + BossManager.BOSS_NAME + "&f이(가) 독구름을 뿜습니다!");
    }

    private static void teleportStrike(BossManager manager, LivingEntity boss, List<Player> nearby) {
        Player target = randomTarget(nearby);
        Location targetLoc = target.getLocation();
        Location strikeSpot = targetLoc.clone().add(1.5, 0, 0);
        strikeSpot.setDirection(targetLoc.toVector().subtract(strikeSpot.toVector()));
        boss.teleport(strikeSpot);
        boss.getWorld().spawnParticle(Particle.PORTAL, strikeSpot, 40, 0.5, 1, 0.5, 0.2);
        manager.damagePlayer(target, 8.0);
        manager.knockback(target, strikeSpot, 1.2);
        manager.broadcastToWorld("&d" + BossManager.BOSS_NAME + "&f이(가) &e" + target.getName() + "&f 곁으로 순간이동해 강타합니다!");
    }

    private static void lightning(BossManager manager, LivingEntity boss, List<Player> nearby) {
        Player target = randomTarget(nearby);
        Location strike = target.getLocation();
        boss.getWorld().strikeLightningEffect(strike);
        for (Player player : nearby) {
            if (player.getLocation().distance(strike) <= 4) {
                manager.damagePlayer(player, 7.0);
            }
        }
        manager.broadcastToWorld("&e" + BossManager.BOSS_NAME + "&f이(가) 벼락을 소환합니다!");
    }

    private static void gravityPull(BossManager manager, LivingEntity boss, List<Player> nearby) {
        Location center = boss.getLocation();
        for (Player player : nearby) {
            if (player.getLocation().distance(center) <= 15) {
                manager.pull(player, center, 1.1);
            }
        }
        boss.getWorld().spawnParticle(Particle.SWEEP_ATTACK, center, 10, 2, 1, 2, 0);
        manager.broadcastToWorld("&8" + BossManager.BOSS_NAME + "&f이(가) 중력장을 일으켜 끌어당깁니다!");
    }

    private static void fireField(BossManager manager, LivingEntity boss, List<Player> nearby) {
        for (Player player : nearby) {
            if (player.getLocation().distance(boss.getLocation()) <= 20 && ThreadLocalRandom.current().nextBoolean()) {
                player.setFireTicks(60);
                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 30, 0.4, 0.6, 0.4, 0.02);
            }
        }
        manager.broadcastToWorld("&6" + BossManager.BOSS_NAME + "&f이(가) 화염을 흩뿌립니다!");
    }

    private static void shockwave(BossManager manager, LivingEntity boss, List<Player> nearby) {
        Location center = boss.getLocation();
        boss.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
        boss.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2f, 1f);
        for (Player player : nearby) {
            if (player.getLocation().distance(center) <= 12) {
                manager.damagePlayer(player, 4.0);
                manager.knockback(player, center, 1.8);
            }
        }
        manager.broadcastToWorld("&4" + BossManager.BOSS_NAME + "&f이(가) 광역 충격파를 일으킵니다!");
    }

    private static void blindCurse(BossManager manager, LivingEntity boss, List<Player> nearby) {
        Location center = boss.getLocation();
        for (Player player : nearby) {
            if (player.getLocation().distance(center) <= 10) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 70, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1));
            }
        }
        manager.broadcastToWorld("&0" + BossManager.BOSS_NAME + "&f이(가) 저주를 겁니다! 시야가 흐려집니다...");
    }
}
