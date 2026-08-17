package com.mingyu.pillage.boss;

import com.mingyu.pillage.data.dao.BossDao;
import com.mingyu.pillage.data.dao.BossRewardDao;
import com.mingyu.pillage.util.Msg;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Pillager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Ravager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** The "최강 보스몹": one always-on Ravager in its own dedicated world with a custom 10,000+ HP
 *  pool (tracked entirely outside vanilla health, since the vanilla max-health attribute is
 *  range-capped well below that) and 10 randomly-picked abilities. Every kill permanently raises
 *  the next spawn's HP ceiling - persisted globally since the boss isn't part of any instance. */
public final class BossManager {

    static final String BOSS_NAME = "약탈의 군주";
    private static final String RIDER_NAME = "약탈 대장";
    private static final String GLOW_TEAM_NAME = "pillage_boss_glow";
    private static final double SCALE = 2.2;

    private final JavaPlugin plugin;
    private final BossDao bossDao;
    private final BossRewardDao rewardDao;
    private final long baseHealth;
    private final long healthIncreasePerKill;
    private final int respawnMinutes;
    private final int attackIntervalSeconds;

    private World world;
    private LivingEntity currentBoss;
    private UUID riderId;
    private long currentMaxHealth;
    private double currentHealth;
    private int killCount;
    private long defeatedAtMillis = 0;
    private double auraAngle = 0;
    private final Map<UUID, Double> damageContribution = new HashMap<>();
    private final Set<UUID> summonedAdds = new HashSet<>();

    private final BossBar bossBar = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.RED, BossBar.Overlay.NOTCHED_20);
    private final Set<UUID> barShown = new HashSet<>();

    public BossManager(JavaPlugin plugin, BossDao bossDao, BossRewardDao rewardDao,
                        long baseHealth, long healthIncreasePerKill, int respawnMinutes, int attackIntervalSeconds) {
        this.plugin = plugin;
        this.bossDao = bossDao;
        this.rewardDao = rewardDao;
        this.baseHealth = baseHealth;
        this.healthIncreasePerKill = healthIncreasePerKill;
        this.respawnMinutes = respawnMinutes;
        this.attackIntervalSeconds = attackIntervalSeconds;
    }

    public void start() {
        world = BossWorld.ensureWorld();
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickBossBar, 20L, 20L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickAura, 5L, 5L);
        long attackIntervalTicks = attackIntervalSeconds * 20L;
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickAttacks, attackIntervalTicks, attackIntervalTicks);
        spawn();
    }

    public World world() {
        return world;
    }

    public Location entryLocation() {
        return BossWorld.ENTRY.clone().toLocation(world);
    }

    public boolean isAlive() {
        return currentBoss != null && currentBoss.isValid();
    }

    public LivingEntity currentBossEntity() {
        return currentBoss;
    }

    public double currentHealth() {
        return currentHealth;
    }

    public long currentMaxHealth() {
        return currentMaxHealth;
    }

    public int killCount() {
        return killCount;
    }

    public long respawnSecondsRemaining() {
        if (isAlive() || defeatedAtMillis == 0) return 0;
        long elapsed = (System.currentTimeMillis() - defeatedAtMillis) / 1000L;
        long total = respawnMinutes * 60L;
        return Math.max(0, total - elapsed);
    }

    public void spawn() {
        if (isAlive()) return;
        BossDao.BossState state = bossDao.load(baseHealth);
        killCount = state.killCount();
        currentMaxHealth = state.maxHealth();
        currentHealth = currentMaxHealth;
        damageContribution.clear();
        despawnAdds();

        Location spawnLoc = BossWorld.CENTER.clone().toLocation(world);
        Ravager ravager = world.spawn(spawnLoc, Ravager.class);
        ravager.setPersistent(true);
        ravager.setRemoveWhenFarAway(false);
        ravager.customName(Msg.of("&4&l" + BOSS_NAME));
        ravager.setCustomNameVisible(true);
        ravager.setGlowing(true);
        applyGlowColor(ravager);
        try {
            var scale = ravager.getAttribute(Attribute.SCALE);
            if (scale != null) scale.setBaseValue(SCALE);
        } catch (Exception ignored) {
        }
        currentBoss = ravager;

        // Ravagers have no equipment render points of their own, so a mounted illager "raid
        // captain" holding a banner - the same trick vanilla raids use - is the one fully
        // vanilla-compatible way to visually set this apart from an ordinary Ravager.
        Pillager rider = world.spawn(spawnLoc, Pillager.class);
        rider.setPersistent(true);
        rider.setRemoveWhenFarAway(false);
        rider.setAI(false);
        rider.setInvulnerable(true);
        rider.setSilent(true);
        rider.setCollidable(false);
        rider.customName(Msg.of("&c&l" + RIDER_NAME));
        rider.setCustomNameVisible(true);
        rider.setGlowing(true);
        applyGlowColor(rider);
        var equipment = rider.getEquipment();
        if (equipment != null) {
            equipment.setItemInMainHand(buildWarBanner());
            equipment.setItemInMainHandDropChance(0f);
        }
        ravager.addPassenger(rider);
        riderId = rider.getUniqueId();

        Bukkit.broadcast(Msg.of("&4&l⚔ " + BOSS_NAME + " &f&l이(가) 보스 월드에 등장했습니다! &7체력 " + currentMaxHealth
                + " &f- &7/boss &f명령어로 이동하세요."));
    }

    private ItemStack buildWarBanner() {
        ItemStack banner = new ItemStack(Material.BLACK_BANNER);
        ItemMeta meta = banner.getItemMeta();
        meta.displayName(Msg.of("&4&l약탈의 깃발"));
        if (meta instanceof BannerMeta bannerMeta) {
            try {
                bannerMeta.addPattern(new Pattern(DyeColor.RED, PatternType.BORDER));
                bannerMeta.addPattern(new Pattern(DyeColor.RED, PatternType.SKULL));
            } catch (Exception ignored) {
            }
        }
        banner.setItemMeta(meta);
        return banner;
    }

    private void applyGlowColor(LivingEntity entity) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam(GLOW_TEAM_NAME);
        if (team == null) {
            team = scoreboard.registerNewTeam(GLOW_TEAM_NAME);
            team.setColor(ChatColor.DARK_RED);
        }
        String entry = entity.getUniqueId().toString();
        if (!team.hasEntry(entry)) {
            team.addEntry(entry);
        }
    }

    private void removeRider() {
        if (riderId == null) return;
        var entity = Bukkit.getEntity(riderId);
        if (entity != null) entity.remove();
        riderId = null;
    }

    void trackAdd(UUID uuid) {
        summonedAdds.add(uuid);
    }

    private void despawnAdds() {
        for (UUID uuid : summonedAdds) {
            var entity = Bukkit.getEntity(uuid);
            if (entity != null) entity.remove();
        }
        summonedAdds.clear();
    }

    public void onBossDamaged(Player attacker, double amount) {
        if (!isAlive() || amount <= 0) return;
        currentHealth = Math.max(0, currentHealth - amount);
        damageContribution.merge(attacker.getUniqueId(), amount, Double::sum);
        if (currentHealth <= 0) {
            defeat();
        }
    }

    private void defeat() {
        killCount++;
        long nextMax = currentMaxHealth + healthIncreasePerKill;
        bossDao.save(killCount, nextMax);

        List<UUID> contributors = damageContribution.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .map(Map.Entry::getKey)
                .toList();
        List<ItemStack> pool = rewardDao.loadAll();
        for (UUID uuid : contributors) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;
            if (pool.isEmpty()) {
                player.sendMessage(Msg.of("&e보스 처치에 기여했지만 관리자가 아직 보상 아이템을 설정하지 않았습니다."));
                continue;
            }
            ItemStack reward = pool.get(ThreadLocalRandom.current().nextInt(pool.size())).clone();
            giveItem(player, reward);
            player.sendMessage(Msg.of("&6&l보스 처치 보상! &f" + reward.getType() + " x" + reward.getAmount() + "을(를) 획득했습니다."));
        }

        Bukkit.broadcast(Msg.of("&4&l" + BOSS_NAME + " &f&l이(가) 쓰러졌습니다! &7(누적 처치 " + killCount + "회, 다음 체력 " + nextMax + ")"));

        despawnAdds();
        removeRider();
        if (currentBoss != null) currentBoss.remove();
        currentBoss = null;
        currentHealth = 0;
        damageContribution.clear();
        defeatedAtMillis = System.currentTimeMillis();

        Bukkit.getScheduler().runTaskLater(plugin, this::spawn, respawnMinutes * 60L * 20L);
    }

    private void giveItem(Player player, ItemStack item) {
        var leftover = player.getInventory().addItem(item);
        for (ItemStack extra : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), extra);
        }
    }

    /** /boss reset confirm - wipes scaling back to the base tier and force-respawns. */
    public void forceReset() {
        despawnAdds();
        removeRider();
        if (currentBoss != null) currentBoss.remove();
        currentBoss = null;
        killCount = 0;
        currentMaxHealth = baseHealth;
        currentHealth = 0;
        damageContribution.clear();
        bossDao.save(0, baseHealth);
        defeatedAtMillis = 0;
        spawn();
    }

    // --- ability helpers (package-private, used by BossAttacks) ------------------------------

    void damagePlayer(Player player, double amount) {
        if (currentBoss == null) return;
        player.damage(amount, currentBoss);
    }

    void knockback(Player player, Location source, double strength) {
        Vector dir = player.getLocation().toVector().subtract(source.toVector());
        if (dir.lengthSquared() < 0.01) dir = new Vector(1, 0, 0);
        dir = dir.normalize();
        dir.setY(Math.max(dir.getY(), 0.35));
        player.setVelocity(dir.multiply(strength));
    }

    void pull(Player player, Location target, double strength) {
        Vector dir = target.toVector().subtract(player.getLocation().toVector());
        if (dir.lengthSquared() < 0.01) return;
        dir = dir.normalize();
        dir.setY(0.25);
        player.setVelocity(dir.multiply(strength));
    }

    void broadcastToWorld(String message) {
        for (Player player : world.getPlayers()) {
            player.sendMessage(Msg.of(message));
        }
    }

    // --- tick / state machine -----------------------------------------------------------------

    private List<Player> nearbyPlayers() {
        List<Player> players = new ArrayList<>();
        for (Player player : world.getPlayers()) {
            if (player.getGameMode() == GameMode.SPECTATOR) continue;
            players.add(player);
        }
        return players;
    }

    private void tickAttacks() {
        if (!isAlive()) return;
        List<Player> nearby = nearbyPlayers();
        if (nearby.isEmpty()) return;
        int idx = ThreadLocalRandom.current().nextInt(BossAttacks.COUNT);
        BossAttacks.execute(idx, this, currentBoss, nearby);
    }

    /** Rotating ring of red dust particles at the boss's feet plus rising flame wisps - a
     *  constant "dark power" aura, entirely vanilla particles, no resource pack needed. */
    private void tickAura() {
        if (!isAlive()) return;
        Location center = currentBoss.getLocation();
        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(200, 20, 20), 1.6f);
        int points = 10;
        double radius = 1.6 * SCALE;
        for (int i = 0; i < points; i++) {
            double angle = auraAngle + (2 * Math.PI * i / points);
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;
            world.spawnParticle(Particle.DUST, x, center.getY() + 0.1, z, 1, 0, 0, 0, 0, dust);
        }
        world.spawnParticle(Particle.FLAME, center.getX(), center.getY() + SCALE, center.getZ(), 3, 0.6, 0.8, 0.6, 0.01);
        auraAngle += 0.35;
    }

    private void tickBossBar() {
        boolean alive = isAlive();
        if (alive) {
            bossBar.name(Msg.of("&4&l" + BOSS_NAME + " &f- &c" + Math.round(currentHealth) + " / " + currentMaxHealth));
            bossBar.progress(clamp((float) (currentHealth / (double) currentMaxHealth)));
        }
        for (Player player : world.getPlayers()) {
            boolean shown = barShown.contains(player.getUniqueId());
            if (alive && !shown) {
                player.showBossBar(bossBar);
                barShown.add(player.getUniqueId());
            } else if (!alive && shown) {
                player.hideBossBar(bossBar);
                barShown.remove(player.getUniqueId());
            }
        }
        barShown.removeIf(uuid -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.getWorld().equals(world)) {
                if (player != null) player.hideBossBar(bossBar);
                return true;
            }
            return false;
        });
    }

    private float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
