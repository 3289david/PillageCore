package com.mingyu.pillage.data.dao;

import com.mingyu.pillage.data.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public final class RewardDao {

    private final Database database;

    public RewardDao(Database database) {
        this.database = database;
    }

    public long lastDailyClaim(UUID uuid) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT last_claimed FROM daily_rewards WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("last_claimed") : 0L;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("일일보상 조회에 실패했습니다.", e);
        }
    }

    public void setLastDailyClaim(UUID uuid, long timestamp) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO daily_rewards (uuid, last_claimed) VALUES (?, ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET last_claimed = excluded.last_claimed")) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, timestamp);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("일일보상 기록에 실패했습니다.", e);
        }
    }

    public int lastPlaytimeMilestoneHours(UUID uuid) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT last_milestone_hours FROM playtime_rewards WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("last_milestone_hours") : 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("플레이타임 보상 조회에 실패했습니다.", e);
        }
    }

    public void setLastPlaytimeMilestoneHours(UUID uuid, int hours) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO playtime_rewards (uuid, last_milestone_hours) VALUES (?, ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET last_milestone_hours = excluded.last_milestone_hours")) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, hours);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("플레이타임 보상 기록에 실패했습니다.", e);
        }
    }
}
