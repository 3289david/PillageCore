package com.mingyu.pillage.data.dao;

import com.mingyu.pillage.data.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class BanLogDao {

    public record Entry(String staff, String target, String reason, long timestamp) {
    }

    private final Database database;

    public BanLogDao(Database database) {
        this.database = database;
    }

    public void log(UUID staff, String targetName, String reason) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO ban_log (staff, target, reason, timestamp) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, staff.toString());
            ps.setString(2, targetName);
            ps.setString(3, reason);
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("밴 로그 기록에 실패했습니다.", e);
        }
    }

    public List<Entry> recent(int limit) {
        List<Entry> entries = new ArrayList<>();
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT * FROM ban_log ORDER BY id DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add(new Entry(rs.getString("staff"), rs.getString("target"),
                            rs.getString("reason"), rs.getLong("timestamp")));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("밴 로그 조회에 실패했습니다.", e);
        }
        return entries;
    }
}
