package com.mingyu.pillage.data.dao;

import com.mingyu.pillage.data.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Global - the jump map is a dedicated world outside the instance system, so best times must
 *  be comparable across the whole server regardless of which mini-server anyone is in. */
public final class JumpRecordDao {

    public record Record(String name, long millis) {
    }

    private final Database database;

    public JumpRecordDao(Database database) {
        this.database = database;
    }

    /** -1 if the player has no recorded time yet. */
    public long bestMillis(UUID uuid) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT best_millis FROM jump_records WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("best_millis") : -1;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("점프맵 기록 조회에 실패했습니다.", e);
        }
    }

    public void saveBest(UUID uuid, String name, long millis) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO jump_records (uuid, name, best_millis) VALUES (?, ?, ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET name = excluded.name, best_millis = excluded.best_millis")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setLong(3, millis);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("점프맵 기록 저장에 실패했습니다.", e);
        }
    }

    public List<Record> top(int limit) {
        List<Record> records = new ArrayList<>();
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT name, best_millis FROM jump_records ORDER BY best_millis ASC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(new Record(rs.getString("name"), rs.getLong("best_millis")));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("점프맵 순위 조회에 실패했습니다.", e);
        }
        return records;
    }
}
