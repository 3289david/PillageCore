package com.mingyu.pillage.data;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {

    private final JavaPlugin plugin;
    private Connection connection;

    public Database(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void connect(String fileName) {
        try {
            Class.forName("org.sqlite.JDBC");
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            File dbFile = new File(dataFolder, fileName);
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA journal_mode=WAL;");
                st.execute("PRAGMA foreign_keys=ON;");
            }
            createSchema();
        } catch (ClassNotFoundException | SQLException e) {
            throw new IllegalStateException("SQLite 데이터베이스 연결에 실패했습니다.", e);
        }
    }

    private void createSchema() throws SQLException {
        try (Statement st = connection.createStatement()) {
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
                    protected_until INTEGER NOT NULL DEFAULT 0,
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
        }
    }

    public Connection connection() {
        return connection;
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().warning("데이터베이스를 닫는 중 오류가 발생했습니다: " + e.getMessage());
            }
        }
    }
}
