package com.mingyu.pillage.data.dao;

import com.mingyu.pillage.data.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class KillLogDao {

    public record Entry(String killer, String victim, String weapon, long timestamp) {
    }

    private final Database database;

    public KillLogDao(Database database) {
        this.database = database;
    }

    public void log(UUID killer, UUID victim, String weapon) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO kill_log (killer, victim, weapon, timestamp) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, killer == null ? null : killer.toString());
            ps.setString(2, victim.toString());
            ps.setString(3, weapon);
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("킬 로그 기록에 실패했습니다.", e);
        }
    }

    public List<Entry> recent(int limit) {
        List<Entry> entries = new ArrayList<>();
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT * FROM kill_log ORDER BY id DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add(new Entry(rs.getString("killer"), rs.getString("victim"),
                            rs.getString("weapon"), rs.getLong("timestamp")));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("킬 로그 조회에 실패했습니다.", e);
        }
        return entries;
    }
}
