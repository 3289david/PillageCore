package com.mingyu.pillage.boss;

import com.mingyu.pillage.data.dao.BossDao;
import com.mingyu.pillage.data.dao.BossRewardDao;
import com.mingyu.pillage.util.Msg;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Ravager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
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

    private final JavaPlugin plugin;
    private final BossDao bossDao;
    private final BossRewardDao rewardDao;
    private final long baseHealth;
    private final long healthIncreasePerKill;
    private final int respawnMinutes;
    private final int attackIntervalSeconds;

    private World world;
    private LivingEntity currentBoss;
    private long currentMaxHealth;
    private double currentHealth;
    private int killCount;
    private long defeatedAtMillis = 0;
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
        try {
            var scale = ravager.getAttribute(Attribute.SCALE);
            if (scale != null) scale.setBaseValue(1.8);
        } catch (Exception ignored) {
        }
        currentBoss = ravager;

        Bukkit.broadcast(Msg.of("&4&l⚔ " + BOSS_NAME + " &f&l이(가) 보스 월드에 등장했습니다! &7체력 " + currentMaxHealth
                + " &f- &7/boss &f명령어로 이동하세요."));
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
