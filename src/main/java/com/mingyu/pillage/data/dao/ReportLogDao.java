package com.mingyu.pillage.data.dao;

import com.mingyu.pillage.data.Database;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public final class ReportLogDao {

    private final Database database;

    public ReportLogDao(Database database) {
        this.database = database;
    }

    public void log(UUID reporter, UUID target, String reason) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO report_log (reporter, target, reason, timestamp) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, reporter.toString());
            ps.setString(2, target.toString());
            ps.setString(3, reason);
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("신고 로그 기록에 실패했습니다.", e);
        }
    }
}
