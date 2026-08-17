package com.mingyu.pillage.minigame;

import com.mingyu.pillage.util.Msg;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Coordinates all four minigames - one lightweight {@link MinigameSession} state machine per
 *  type (WAITING for enough players -> COUNTDOWN -> PLAYING -> ENDING -> back to WAITING), ticked
 *  every half-second. Deliberately simple/code-built rather than a full-featured minigame
 *  framework - this covers join/leave, round flow, elimination, and rewards for four specific
 *  games, not an extensible platform for arbitrary future ones. */
public final class MinigameManager {

    private static final int COUNTDOWN_SECONDS = 10;
    private static final int TAG_ROUND_SECONDS = 120;
    private static final long ECONOMY_WIN_REWARD = 30;
    private static final long ECONOMY_PARTICIPATION_REWARD = 5;

    private final JavaPlugin plugin;
    private final com.mingyu.pillage.economy.EconomyManager economyManager;
    private final Map<MinigameType, MinigameSession> sessions = new EnumMap<>(MinigameType.class);
    private World world;
    /** Counts tick() calls (every 10 ticks / 0.5s) since the last spleef floor refill. */
    private int spleefRefillTicks = 0;

    public MinigameManager(JavaPlugin plugin, com.mingyu.pillage.economy.EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        for (MinigameType type : MinigameType.values()) {
            sessions.put(type, new MinigameSession(type));
        }
    }

    public void start() {
        world = MinigameArenas.ensureWorld();
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 10L, 10L);
    }

    boolean isParticipant(UUID uuid) {
        for (MinigameSession session : sessions.values()) {
            if (session.isPlaying(uuid)) return true;
        }
        return false;
    }

    private MinigameSession sessionOf(UUID uuid) {
        for (MinigameSession session : sessions.values()) {
            if (session.isPlaying(uuid)) return session;
        }
        return null;
    }

    // Snow should only be breakable once the round has actually started - MinigameSession.isPlaying()
    // only means "currently joined" (true during WAITING/COUNTDOWN too, on purpose, so waiting
    // participants still get damage protection), so this also has to check the phase.
    public boolean isPlayingSpleef(UUID uuid) {
        MinigameSession session = sessions.get(MinigameType.SPLEEF);
        return session.phase == MinigameSession.Phase.PLAYING && session.isPlaying(uuid);
    }

    /** Dispatches to whichever per-type movement check applies to this player right now (spleef/
     *  TNT run fall-elimination, parkour finish/fall) - a no-op for anyone not in PLAYING, or in
     *  TAG (tag transfer is damage-event-driven, not movement-driven). */
    public void onParticipantMove(Player player) {
        MinigameSession session = sessionOf(player.getUniqueId());
        if (session == null || session.phase != MinigameSession.Phase.PLAYING) return;
        switch (session.type) {
            case SPLEEF -> checkFallElimination(session, player, MinigameArenas.SPLEEF_KILL_Y);
            case TNT_RUN -> checkFallElimination(session, player, MinigameArenas.TNT_RUN_KILL_Y);
            case PARKOUR -> checkParkourFinish(session, player);
            case TAG -> { /* handled by onTagHit() */ }
        }
    }

    /** Called by {@link MinigameListener} whenever one participant damages another - only means
     *  anything for TAG, where it's how a tagger catches a runner. */
    public void onTagHit(Player tagger, Player target) {
        MinigameSession session = sessionOf(tagger.getUniqueId());
        if (session == null || session != sessionOf(target.getUniqueId())) return;
        onTag(session, tagger, target);
    }

    public List<String> statusLines() {
        List<String> lines = new ArrayList<>();
        for (MinigameSession session : sessions.values()) {
            lines.add(session.type.displayName() + ": " + session.phase + " (" + session.participants.size() + "명)");
        }
        return lines;
    }

    public void join(Player player, MinigameType type) {
        if (isParticipant(player.getUniqueId())) {
            player.sendMessage(Msg.of("&c이미 다른 미니게임에 참가 중입니다. &f/minigame leave &c로 먼저 나가세요."));
            return;
        }
        MinigameSession session = sessions.get(type);
        if (session.phase == MinigameSession.Phase.PLAYING || session.phase == MinigameSession.Phase.ENDING) {
            player.sendMessage(Msg.of("&c지금 라운드가 진행 중입니다. 다음 라운드를 기다려주세요."));
            return;
        }

        UUID uuid = player.getUniqueId();
        PlayerInventory inv = player.getInventory();
        session.savedInventory.put(uuid, inv.getContents());
        session.savedArmor.put(uuid, inv.getArmorContents());
        session.savedOffhand.put(uuid, inv.getItemInOffHand());
        session.savedLocation.put(uuid, player.getLocation());
        session.savedGameMode.put(uuid, player.getGameMode());

        inv.clear();
        inv.setArmorContents(new ItemStack[4]);
        inv.setItemInOffHand(null);
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
        player.setFoodLevel(20);

        session.participants.add(uuid);
        if (type == MinigameType.SPLEEF) {
            giveShovel(player);
        }
        player.teleport(waitingSpot(type));

        broadcast(session, Msg.of("&a" + player.getName() + "&f 님이 " + type.displayName() + "에 참가했습니다. &7(" + session.participants.size() + "/" + type.minPlayers() + ")"));
        player.sendMessage(Msg.of("&f/minigame leave &7로 언제든 나갈 수 있습니다."));
    }

    /** Called from /minigame leave, and from a quit-event handler for anyone who disconnects
     *  mid-round - either way this is the only place a participant's real gear comes back, so it
     *  has to run synchronously before the player actually goes offline. */
    void leave(Player player) {
        MinigameSession session = sessionOf(player.getUniqueId());
        if (session == null) return;
        removeParticipant(session, player, true);
    }

    private void removeParticipant(MinigameSession session, Player player, boolean announce) {
        UUID uuid = player.getUniqueId();
        session.participants.remove(uuid);
        session.taggers.remove(uuid);
        session.finished.remove(uuid);

        PlayerInventory inv = player.getInventory();
        inv.setContents(session.savedInventory.remove(uuid));
        inv.setArmorContents(session.savedArmor.remove(uuid));
        inv.setItemInOffHand(session.savedOffhand.remove(uuid));
        GameMode mode = session.savedGameMode.remove(uuid);
        if (mode != null) player.setGameMode(mode);
        Location back = session.savedLocation.remove(uuid);
        if (back != null && back.getWorld() != null) {
            player.teleport(back);
        }

        if (announce && player.isOnline()) {
            player.sendMessage(Msg.of("&7" + session.type.displayName() + "에서 나왔습니다."));
        }
    }

    private void giveShovel(Player player) {
        ItemStack shovel = new ItemStack(Material.DIAMOND_SHOVEL);
        ItemMeta meta = shovel.getItemMeta();
        meta.setUnbreakable(true);
        shovel.setItemMeta(meta);
        player.getInventory().setItemInMainHand(shovel);
    }

    private Location waitingSpot(MinigameType type) {
        return switch (type) {
            case SPLEEF -> MinigameArenas.SPLEEF_ORIGIN.clone().add(0, 1, 0).toLocation(world);
            case TNT_RUN -> MinigameArenas.TNT_RUN_ORIGIN.clone().add(0, 1, 0).toLocation(world);
            case PARKOUR -> MinigameArenas.PARKOUR_START.clone().toLocation(world);
            case TAG -> MinigameArenas.TAG_ORIGIN.clone().add(0, 1, 0).toLocation(world);
        };
    }

    // --- tick / state machine -------------------------------------------------------------

    private void tick() {
        for (MinigameSession session : sessions.values()) {
            switch (session.phase) {
                case WAITING -> tickWaiting(session);
                case COUNTDOWN -> tickCountdown(session);
                case PLAYING -> tickPlaying(session);
                case ENDING -> { /* single-tick transition handled inline in endRound() */ }
            }
        }
    }

    private void tickWaiting(MinigameSession session) {
        if (session.participants.size() >= session.type.minPlayers()) {
            session.phase = MinigameSession.Phase.COUNTDOWN;
            session.countdownSeconds = COUNTDOWN_SECONDS;
            broadcast(session, Msg.of("&e인원이 모여 " + COUNTDOWN_SECONDS + "초 후 " + session.type.displayName() + "이(가) 시작됩니다!"));
        }
    }

    private void tickCountdown(MinigameSession session) {
        if (session.participants.size() < session.type.minPlayers()) {
            session.phase = MinigameSession.Phase.WAITING;
            broadcast(session, Msg.of("&c인원이 부족해 카운트다운이 취소되었습니다."));
            return;
        }
        session.countdownSeconds--;
        if (session.countdownSeconds <= 0) {
            startRound(session);
            return;
        }
        if (session.countdownSeconds <= 5 || session.countdownSeconds % 5 == 0) {
            for (UUID uuid : session.participants) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null) continue;
                player.showTitle(Title.title(Msg.of("&e" + session.countdownSeconds), Component.empty(),
                        Title.Times.times(Duration.ZERO, Duration.ofMillis(900), Duration.ZERO)));
            }
        }
    }

    private void startRound(MinigameSession session) {
        session.phase = MinigameSession.Phase.PLAYING;
        session.finished.clear();
        session.taggers.clear();

        List<UUID> order = new ArrayList<>(session.participants);
        switch (session.type) {
            case SPLEEF -> startSpleef(session, order);
            case TNT_RUN -> startTntRun(session, order);
            case PARKOUR -> startParkour(session, order);
            case TAG -> startTag(session, order);
        }
        broadcast(session, Msg.of("&a&l" + session.type.displayName() + " 시작!"));
    }

    private void startSpleef(MinigameSession session, List<UUID> order) {
        MinigameArenas.buildSpleefFloorFresh(world);
        spleefRefillTicks = 0;
        placeAroundEdge(order, MinigameArenas.SPLEEF_ORIGIN.toLocation(world), MinigameArenas.SPLEEF_ARENA_SIZE);
    }

    private void startTntRun(MinigameSession session, List<UUID> order) {
        MinigameArenas.buildTntRunFloorFresh(world);
        placeAroundEdge(order, MinigameArenas.TNT_RUN_ORIGIN.toLocation(world), MinigameArenas.TNT_RUN_ARENA_SIZE);
    }

    private void startParkour(MinigameSession session, List<UUID> order) {
        for (UUID uuid : order) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) player.teleport(MinigameArenas.PARKOUR_START.toLocation(world));
        }
    }

    private void startTag(MinigameSession session, List<UUID> order) {
        placeAroundEdge(order, MinigameArenas.TAG_ORIGIN.toLocation(world), MinigameArenas.TAG_ARENA_SIZE - 4);
        UUID firstTagger = order.get((int) (Math.random() * order.size()));
        session.taggers.add(firstTagger);
        markTagger(Bukkit.getPlayer(firstTagger), true);
        session.playSecondsRemaining = TAG_ROUND_SECONDS;
        for (UUID uuid : order) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && !uuid.equals(firstTagger)) {
                player.sendMessage(Msg.of("&b" + Bukkit.getOfflinePlayer(firstTagger).getName() + "&f 님이 첫 술래입니다! 도망치세요!"));
            }
        }
    }

    private void placeAroundEdge(List<UUID> order, Location center, int arenaSize) {
        int half = arenaSize / 2;
        int count = order.size();
        for (int i = 0; i < count; i++) {
            Player player = Bukkit.getPlayer(order.get(i));
            if (player == null) continue;
            double angle = (2 * Math.PI * i) / count;
            int x = (int) Math.round(Math.cos(angle) * (half - 1));
            int z = (int) Math.round(Math.sin(angle) * (half - 1));
            Location spot = center.clone().add(x, 2, z);
            player.teleport(spot);
        }
    }

    void markTagger(Player player, boolean tagger) {
        if (player == null) return;
        if (tagger) {
            ItemStack stick = new ItemStack(Material.STICK);
            ItemMeta meta = stick.getItemMeta();
            meta.displayName(Msg.of("&c&l술래봉"));
            meta.setUnbreakable(true);
            stick.setItemMeta(meta);
            player.getInventory().setItemInMainHand(stick);
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 999999, 0, false, false));
        } else {
            player.getInventory().setItemInMainHand(null);
            player.removePotionEffect(org.bukkit.potion.PotionEffectType.SPEED);
        }
    }

    /** Called by {@link MinigameListener} whenever a tagger touches a runner. */
    void onTag(MinigameSession session, Player tagger, Player target) {
        if (session.type != MinigameType.TAG || session.phase != MinigameSession.Phase.PLAYING) return;
        if (!session.taggers.contains(tagger.getUniqueId()) || session.taggers.contains(target.getUniqueId())) return;
        session.taggers.add(target.getUniqueId());
        markTagger(target, true);
        broadcast(session, Msg.of("&c" + target.getName() + "&f 님이 잡혀서 술래가 되었습니다! &7(남은 도망자: "
                + (session.participants.size() - session.taggers.size()) + "명)"));
    }

    /** Called by {@link MinigameListener} on every move while spleef/TNT run is playing. */
    void checkFallElimination(MinigameSession session, Player player, int killY) {
        if (session.phase != MinigameSession.Phase.PLAYING || !session.participants.contains(player.getUniqueId())) return;
        if (player.getLocation().getY() >= killY) return;
        eliminate(session, player);
    }

    private void eliminate(MinigameSession session, Player player) {
        session.participants.remove(player.getUniqueId());
        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(waitingSpot(session.type).clone().add(0, 10, 0));
        broadcast(session, Msg.of("&7" + player.getName() + " 님이 탈락했습니다. &8(남은 인원: " + session.participants.size() + "명)"));
    }

    void checkParkourFinish(MinigameSession session, Player player) {
        if (session.phase != MinigameSession.Phase.PLAYING || !session.participants.contains(player.getUniqueId())) return;
        Location loc = player.getLocation();
        if (loc.getY() < MinigameArenas.PARKOUR_FALL_Y) {
            player.teleport(MinigameArenas.PARKOUR_START.toLocation(world));
            player.sendMessage(Msg.of("&c떨어졌습니다! 처음부터 다시 도전하세요."));
            return;
        }
        if (session.finished.contains(player.getUniqueId())) return;
        Location finish = MinigameArenas.PARKOUR_FINISH.toLocation(world);
        if (loc.distanceSquared(finish) <= 4) {
            session.finished.add(player.getUniqueId());
            int place = session.finished.size();
            broadcast(session, Msg.of("&a" + player.getName() + "&f 님이 " + place + "등으로 완주했습니다!"));
            reward(player, place == 1 ? ECONOMY_WIN_REWARD : ECONOMY_PARTICIPATION_REWARD);
            if (session.finished.containsAll(session.participants)) {
                endRound(session, null);
            }
        }
    }

    private static final int SPLEEF_REFILL_INTERVAL_TICKS = 20; // 20 * 0.5s tick() calls = 10s

    private void tickPlaying(MinigameSession session) {
        switch (session.type) {
            case SPLEEF -> {
                spleefRefillTicks++;
                if (spleefRefillTicks >= SPLEEF_REFILL_INTERVAL_TICKS) {
                    spleefRefillTicks = 0;
                    MinigameArenas.buildSpleefFloorFresh(world);
                }
                if (session.participants.size() <= 1) {
                    endRound(session, session.participants.isEmpty() ? null : session.participants.iterator().next());
                }
            }
            case TNT_RUN -> {
                if (session.participants.size() <= 1) {
                    endRound(session, session.participants.isEmpty() ? null : session.participants.iterator().next());
                }
            }
            case TAG -> {
                session.playSecondsRemaining--;
                int remainingRunners = session.participants.size() - session.taggers.size();
                if (remainingRunners <= 0) {
                    endRound(session, null);
                } else if (session.playSecondsRemaining <= 0) {
                    endRound(session, null);
                }
            }
            case PARKOUR -> {
                // finish/fall handled by checkParkourFinish() on movement; nothing to poll here.
            }
        }
    }

    private void endRound(MinigameSession session, UUID winner) {
        session.phase = MinigameSession.Phase.ENDING;

        switch (session.type) {
            case SPLEEF, TNT_RUN -> {
                if (winner != null) {
                    Player player = Bukkit.getPlayer(winner);
                    broadcast(session, Msg.of("&a&l" + (player != null ? player.getName() : "???") + "&f&l 님 우승!"));
                    if (player != null) reward(player, ECONOMY_WIN_REWARD);
                } else {
                    broadcast(session, Msg.of("&7이번 라운드는 승자 없이 종료되었습니다."));
                }
            }
            case TAG -> {
                int survivors = session.participants.size() - session.taggers.size();
                if (survivors > 0) {
                    broadcast(session, Msg.of("&a도망자 " + survivors + "명이 살아남았습니다! 살아남은 사람 전원 승리!"));
                } else {
                    broadcast(session, Msg.of("&c모두 잡혔습니다! 술래 팀 승리!"));
                }
                for (UUID uuid : session.participants) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null) continue;
                    boolean wasRunner = !session.taggers.contains(uuid);
                    reward(player, wasRunner && survivors > 0 ? ECONOMY_WIN_REWARD : ECONOMY_PARTICIPATION_REWARD);
                    markTagger(player, false);
                }
            }
            case PARKOUR -> broadcast(session, Msg.of("&a모두 완주했습니다! 다음 라운드를 시작합니다."));
        }

        List<UUID> toRemove = new ArrayList<>(session.savedLocation.keySet());
        for (UUID uuid : toRemove) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                removeParticipant(session, player, false);
                player.sendMessage(Msg.of("&7" + session.type.displayName() + " 라운드가 종료되어 원래 위치로 돌아왔습니다."));
            } else {
                session.savedLocation.remove(uuid);
                session.savedInventory.remove(uuid);
                session.savedArmor.remove(uuid);
                session.savedOffhand.remove(uuid);
                session.savedGameMode.remove(uuid);
            }
        }
        session.participants.clear();
        session.taggers.clear();
        session.finished.clear();
        session.phase = MinigameSession.Phase.WAITING;
    }

    private void reward(Player player, long amount) {
        if (economyManager == null || player == null) return;
        economyManager.deposit(player.getUniqueId(), amount);
        player.sendMessage(Msg.of("&e+" + amount + " 에메랄드"));
    }

    private void broadcast(MinigameSession session, Component message) {
        for (UUID uuid : session.participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) player.sendMessage(message);
        }
    }

    /** Handles a disconnect mid-round - must run synchronously in the quit handler so gear comes
     *  back before the player's data is written to disk. */
    public void handleQuit(Player player) {
        MinigameSession session = sessionOf(player.getUniqueId());
        if (session == null) return;
        removeParticipant(session, player, false);
    }
}
