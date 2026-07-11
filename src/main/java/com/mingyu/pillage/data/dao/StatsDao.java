package com.mingyu.pillage.data.dao;

import com.mingyu.pillage.data.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public final class StatsDao {

    public record Stats(int kills, int deaths, long playtimeSeconds, int blocksMined) {
        public double kd() {
            return deaths == 0 ? kills : (double) kills / deaths;
        }
    }

    private final Database database;

    public StatsDao(Database database) {
        this.database = database;
    }

    private void ensureRow(UUID uuid) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT OR IGNORE INTO player_stats (uuid) VALUES (?)")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("통계 초기화에 실패했습니다.", e);
        }
    }

    public Stats get(UUID uuid) {
        ensureRow(uuid);
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT * FROM player_stats WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return new Stats(0, 0, 0, 0);
                return new Stats(rs.getInt("kills"), rs.getInt("deaths"),
                        rs.getLong("playtime_seconds"), rs.getInt("blocks_mined"));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("통계 조회에 실패했습니다.", e);
        }
    }

    public void addKill(UUID uuid) {
        increment(uuid, "kills", 1);
    }

    public void addDeath(UUID uuid) {
        increment(uuid, "deaths", 1);
    }

    public void addPlaytime(UUID uuid, long seconds) {
        increment(uuid, "playtime_seconds", seconds);
    }

    public void addBlockMined(UUID uuid) {
        increment(uuid, "blocks_mined", 1);
    }

    private void increment(UUID uuid, String column, long amount) {
        ensureRow(uuid);
        try (PreparedStatement ps = database.connection().prepareStatement(
                "UPDATE player_stats SET " + column + " = " + column + " + ? WHERE uuid = ?")) {
            ps.setLong(1, amount);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("통계 갱신에 실패했습니다.", e);
        }
    }
}
