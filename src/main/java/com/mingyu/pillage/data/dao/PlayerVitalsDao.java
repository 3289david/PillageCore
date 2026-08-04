package com.mingyu.pillage.data.dao;

import com.mingyu.pillage.data.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

/** Per-instance (each gameplay database has its own copy) health/hunger, so getting hurt or
 *  starving in one instance never carries over into another. */
public final class PlayerVitalsDao {

    public record Vitals(double health, int foodLevel, float saturation, float exhaustion) {
    }

    private final Database database;

    public PlayerVitalsDao(Database database) {
        this.database = database;
    }

    public void save(UUID uuid, double health, int foodLevel, float saturation, float exhaustion) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO player_vitals (uuid, health, food_level, saturation, exhaustion) VALUES (?, ?, ?, ?, ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET health = excluded.health, food_level = excluded.food_level, " +
                        "saturation = excluded.saturation, exhaustion = excluded.exhaustion")) {
            ps.setString(1, uuid.toString());
            ps.setDouble(2, health);
            ps.setInt(3, foodLevel);
            ps.setFloat(4, saturation);
            ps.setFloat(5, exhaustion);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("체력/배고픔 저장에 실패했습니다.", e);
        }
    }

    /** Returns the saved vitals, or empty if nothing was ever saved (first time in this instance). */
    public Optional<Vitals> load(UUID uuid) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT health, food_level, saturation, exhaustion FROM player_vitals WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new Vitals(
                        rs.getDouble("health"), rs.getInt("food_level"),
                        rs.getFloat("saturation"), rs.getFloat("exhaustion")));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("체력/배고픔 조회에 실패했습니다.", e);
        }
    }
}
