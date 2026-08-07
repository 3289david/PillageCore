package com.mingyu.pillage.data.dao;

import com.mingyu.pillage.data.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/** Tracks how many event boxes each player has ever opened, globally (not per-instance) so the
 *  "every 5th is a guaranteed OP item" pity counter follows the player across every server
 *  instead of resetting whenever they switch. */
public final class EventBoxOpenDao {

    private final Database database;

    public EventBoxOpenDao(Database database) {
        this.database = database;
    }

    /** Increments the player's lifetime open count and returns the new total. */
    public int incrementAndGet(UUID uuid) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO event_box_opens (uuid, opened_count) VALUES (?, 1) " +
                        "ON CONFLICT(uuid) DO UPDATE SET opened_count = opened_count + 1")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("이벤트 상자 개봉 횟수 갱신에 실패했습니다.", e);
        }
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT opened_count FROM event_box_opens WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("opened_count") : 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("이벤트 상자 개봉 횟수 조회에 실패했습니다.", e);
        }
    }
}
