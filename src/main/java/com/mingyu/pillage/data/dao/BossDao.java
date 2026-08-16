package com.mingyu.pillage.data.dao;

import com.mingyu.pillage.data.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Global (not per-instance) - the boss lives in its own dedicated world outside the instance
 *  system entirely, so its kill count and scaled health must survive regardless of which mini
 *  server anyone is standing in. */
public final class BossDao {

    public record BossState(int killCount, long maxHealth) {
    }

    private final Database database;

    public BossDao(Database database) {
        this.database = database;
    }

    public BossState load(long defaultMaxHealth) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT kill_count, max_health FROM boss_state WHERE id = 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return new BossState(0, defaultMaxHealth);
                return new BossState(rs.getInt("kill_count"), rs.getLong("max_health"));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("보스 상태 로드에 실패했습니다.", e);
        }
    }

    public void save(int killCount, long maxHealth) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO boss_state (id, kill_count, max_health) VALUES (1, ?, ?) " +
                        "ON CONFLICT(id) DO UPDATE SET kill_count = excluded.kill_count, max_health = excluded.max_health")) {
            ps.setInt(1, killCount);
            ps.setLong(2, maxHealth);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("보스 상태 저장에 실패했습니다.", e);
        }
    }
}
