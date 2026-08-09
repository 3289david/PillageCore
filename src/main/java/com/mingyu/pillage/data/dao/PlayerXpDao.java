package com.mingyu.pillage.data.dao;

import com.mingyu.pillage.data.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

/** Per-instance (each gameplay database has its own copy) experience level/progress, so grinding
 *  XP in one instance doesn't carry over into another - Bukkit stores it globally per player
 *  otherwise. */
public final class PlayerXpDao {

    public record Xp(int level, float exp, int totalExperience) {
    }

    private final Database database;

    public PlayerXpDao(Database database) {
        this.database = database;
    }

    public void save(UUID uuid, int level, float exp, int totalExperience) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO player_xp (uuid, level, exp, total_experience) VALUES (?, ?, ?, ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET level = excluded.level, exp = excluded.exp, " +
                        "total_experience = excluded.total_experience")) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, level);
            ps.setFloat(3, exp);
            ps.setInt(4, totalExperience);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("경험치 저장에 실패했습니다.", e);
        }
    }

    public Optional<Xp> load(UUID uuid) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT level, exp, total_experience FROM player_xp WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new Xp(rs.getInt("level"), rs.getFloat("exp"), rs.getInt("total_experience")));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("경험치 조회에 실패했습니다.", e);
        }
    }
}
