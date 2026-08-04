package com.mingyu.pillage.data;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Wraps one or more SQLite connections. Most callers only ever connect a single file (the
 * original single-instance behaviour) - but a {@link SchemaKind#GAMEPLAY} database can also
 * {@link #open} additional named connections (one per mini-server instance, each its own file
 * with a fresh copy of the gameplay schema) and {@link #use} switches which one subsequent
 * {@link #connection()} calls return. Every existing DAO already fetches the connection fresh
 * on each call rather than caching it, so this switch is transparent to all of them.
 */
public final class Database {

    public enum SchemaKind { GAMEPLAY, GLOBAL }

    private final JavaPlugin plugin;
    private final SchemaKind schemaKind;
    private final Map<String, Connection> connections = new LinkedHashMap<>();
    private String current;

    public Database(JavaPlugin plugin, SchemaKind schemaKind) {
        this.plugin = plugin;
        this.schemaKind = schemaKind;
    }

    /** Backward-compatible single-connection bootstrap - opens and activates the "main" instance. */
    public void connect(String fileName) {
        open("main", fileName);
    }

    /** Opens (if not already open) a named connection backed by its own file, and creates its schema. */
    public void open(String instanceId, String fileName) {
        if (connections.containsKey(instanceId)) return;
        try {
            Class.forName("org.sqlite.JDBC");
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            File dbFile = new File(dataFolder, fileName);
            plugin.getLogger().info("[DB] " + dbFile.getAbsolutePath() + " exists=" + dbFile.exists()
                    + " size=" + dbFile.length() + " bytes");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement st = conn.createStatement()) {
                // DELETE (not WAL) mode: every write already autocommits individually here, so with
                // WAL a host that force-kills the process (or only backs up the .db file, not the
                // separate -wal journal) loses whatever hadn't been checkpointed yet. DELETE mode
                // writes straight into the main file with no separate journal left lingering after
                // each commit, so nothing can go missing between the plugin's writes and a restart.
                st.execute("PRAGMA journal_mode=DELETE;");
                st.execute("PRAGMA foreign_keys=ON;");
            }
            createSchema(conn);
            connections.put(instanceId, conn);
            if (current == null) {
                current = instanceId;
            }
        } catch (ClassNotFoundException | SQLException e) {
            throw new IllegalStateException("SQLite 데이터베이스 연결에 실패했습니다.", e);
        }
    }

    public boolean hasInstance(String instanceId) {
        return connections.containsKey(instanceId);
    }

    /** Switches which instance {@link #connection()} returns. Must already be {@link #open}. */
    public void use(String instanceId) {
        if (!connections.containsKey(instanceId)) {
            throw new IllegalStateException("연결되지 않은 인스턴스입니다: " + instanceId);
        }
        current = instanceId;
    }

    public String currentInstance() {
        return current;
    }

    /** Closes and forgets one instance's connection (used when a mini-server is deleted). */
    public void closeInstance(String instanceId) {
        Connection conn = connections.remove(instanceId);
        if (conn == null) return;
        try {
            conn.close();
        } catch (SQLException e) {
            plugin.getLogger().warning("데이터베이스를 닫는 중 오류가 발생했습니다: " + e.getMessage());
        }
        if (instanceId.equals(current)) {
            current = connections.isEmpty() ? null : connections.keySet().iterator().next();
        }
    }

    private void createSchema(Connection connection) throws SQLException {
        try (Statement st = connection.createStatement()) {
            if (schemaKind == SchemaKind.GAMEPLAY) {
                createGameplaySchema(st);
            } else {
                createGlobalSchema(st);
            }
        }
    }

    private void createGameplaySchema(Statement st) throws SQLException {
        st.execute("""
            CREATE TABLE IF NOT EXISTS teams (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT UNIQUE NOT NULL,
                leader_uuid TEXT NOT NULL,
                friendly_fire INTEGER NOT NULL DEFAULT 0,
                max_members INTEGER NOT NULL DEFAULT 6,
                home_world TEXT,
                home_x REAL, home_y REAL, home_z REAL, home_yaw REAL, home_pitch REAL,
                kills INTEGER NOT NULL DEFAULT 0,
                loot_score INTEGER NOT NULL DEFAULT 0,
                raids_won INTEGER NOT NULL DEFAULT 0,
                raids_defended INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL
            );
        """);
        st.execute("""
            CREATE TABLE IF NOT EXISTS team_members (
                uuid TEXT PRIMARY KEY,
                team_id INTEGER NOT NULL,
                role TEXT NOT NULL,
                FOREIGN KEY(team_id) REFERENCES teams(id) ON DELETE CASCADE
            );
        """);
        st.execute("""
            CREATE TABLE IF NOT EXISTS homes (
                uuid TEXT NOT NULL,
                name TEXT NOT NULL,
                world TEXT NOT NULL,
                x REAL NOT NULL, y REAL NOT NULL, z REAL NOT NULL,
                yaw REAL NOT NULL, pitch REAL NOT NULL,
                PRIMARY KEY (uuid, name)
            );
        """);
        st.execute("""
            CREATE TABLE IF NOT EXISTS last_locations (
                uuid TEXT PRIMARY KEY,
                world TEXT NOT NULL,
                x REAL NOT NULL, y REAL NOT NULL, z REAL NOT NULL,
                yaw REAL NOT NULL, pitch REAL NOT NULL
            );
        """);
        st.execute("""
            CREATE TABLE IF NOT EXISTS trade_log (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_a TEXT NOT NULL,
                player_b TEXT NOT NULL,
                items_a TEXT NOT NULL,
                items_b TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            );
        """);
        st.execute("""
            CREATE TABLE IF NOT EXISTS kill_log (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                killer TEXT,
                victim TEXT NOT NULL,
                weapon TEXT,
                timestamp INTEGER NOT NULL
            );
        """);
        st.execute("""
            CREATE TABLE IF NOT EXISTS tp_log (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player TEXT NOT NULL,
                kind TEXT NOT NULL,
                world TEXT, x REAL, y REAL, z REAL,
                timestamp INTEGER NOT NULL
            );
        """);
        st.execute("""
            CREATE TABLE IF NOT EXISTS player_stats (
                uuid TEXT PRIMARY KEY,
                kills INTEGER NOT NULL DEFAULT 0,
                deaths INTEGER NOT NULL DEFAULT 0,
                playtime_seconds INTEGER NOT NULL DEFAULT 0,
                blocks_mined INTEGER NOT NULL DEFAULT 0
            );
        """);
        st.execute("""
            CREATE TABLE IF NOT EXISTS death_locations (
                uuid TEXT PRIMARY KEY,
                world TEXT NOT NULL,
                x REAL NOT NULL, y REAL NOT NULL, z REAL NOT NULL,
                yaw REAL NOT NULL, pitch REAL NOT NULL
            );
        """);
        st.execute("""
            CREATE TABLE IF NOT EXISTS daily_rewards (
                uuid TEXT PRIMARY KEY,
                last_claimed INTEGER NOT NULL DEFAULT 0
            );
        """);
        st.execute("""
            CREATE TABLE IF NOT EXISTS playtime_rewards (
                uuid TEXT PRIMARY KEY,
                last_milestone_hours INTEGER NOT NULL DEFAULT 0
            );
        """);
        st.execute("""
            CREATE TABLE IF NOT EXISTS economy (
                uuid TEXT PRIMARY KEY,
                balance INTEGER NOT NULL DEFAULT 0
            );
        """);
        st.execute("""
            CREATE TABLE IF NOT EXISTS shop_offers (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                input_material TEXT NOT NULL,
                input_amount INTEGER NOT NULL,
                output_material TEXT NOT NULL,
                output_amount INTEGER NOT NULL
            );
        """);
        st.execute("""
            CREATE TABLE IF NOT EXISTS player_inventory (
                uuid TEXT PRIMARY KEY,
                contents TEXT NOT NULL,
                armor TEXT NOT NULL,
                offhand TEXT NOT NULL
            );
        """);
        st.execute("""
            CREATE TABLE IF NOT EXISTS player_position (
                uuid TEXT PRIMARY KEY,
                world TEXT NOT NULL,
                x REAL NOT NULL, y REAL NOT NULL, z REAL NOT NULL,
                yaw REAL NOT NULL, pitch REAL NOT NULL
            );
        """);
        st.execute("""
            CREATE TABLE IF NOT EXISTS player_vitals (
                uuid TEXT PRIMARY KEY,
                health REAL NOT NULL,
                food_level INTEGER NOT NULL,
                saturation REAL NOT NULL,
                exhaustion REAL NOT NULL
            );
        """);
    }

    private void createGlobalSchema(Statement st) throws SQLException {
        st.execute("""
            CREATE TABLE IF NOT EXISTS report_log (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                reporter TEXT NOT NULL,
                target TEXT NOT NULL,
                reason TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            );
        """);
        st.execute("""
            CREATE TABLE IF NOT EXISTS ban_log (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                staff TEXT NOT NULL,
                target TEXT NOT NULL,
                reason TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            );
        """);
        st.execute("""
            CREATE TABLE IF NOT EXISTS donors (
                uuid TEXT PRIMARY KEY,
                badge TEXT NOT NULL DEFAULT '★',
                added_by TEXT,
                added_at INTEGER NOT NULL
            );
        """);
        st.execute("""
            CREATE TABLE IF NOT EXISTS donor_pets (
                uuid TEXT PRIMARY KEY,
                name TEXT,
                variant TEXT NOT NULL DEFAULT 'TABBY'
            );
        """);
        st.execute("""
            CREATE TABLE IF NOT EXISTS hall_of_fame (
                uuid TEXT PRIMARY KEY,
                slot INTEGER NOT NULL UNIQUE
            );
        """);
        st.execute("""
            CREATE TABLE IF NOT EXISTS hall_of_fame_meta (
                id INTEGER PRIMARY KEY CHECK (id = 1),
                built INTEGER NOT NULL DEFAULT 0,
                world TEXT,
                origin_x INTEGER,
                origin_y INTEGER,
                origin_z INTEGER
            );
        """);
        st.execute("""
            CREATE TABLE IF NOT EXISTS instances (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                owner_uuid TEXT NOT NULL,
                world_name TEXT NOT NULL,
                created_at INTEGER NOT NULL
            );
        """);
        st.execute("""
            CREATE TABLE IF NOT EXISTS player_last_instance (
                uuid TEXT PRIMARY KEY,
                instance_id TEXT NOT NULL
            );
        """);
    }

    public Connection connection() {
        if (current == null) {
            throw new IllegalStateException("아직 연결된 데이터베이스 인스턴스가 없습니다.");
        }
        return connections.get(current);
    }

    public void close() {
        for (Connection conn : connections.values()) {
            try {
                conn.close();
            } catch (SQLException e) {
                plugin.getLogger().warning("데이터베이스를 닫는 중 오류가 발생했습니다: " + e.getMessage());
            }
        }
        connections.clear();
        current = null;
    }
}
