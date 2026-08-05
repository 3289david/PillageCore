package com.mingyu.pillage.data.dao;

import com.mingyu.pillage.data.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/** Tracks how many event boxes an admin has granted a player that they haven't claimed yet.
 *  Global (not per-instance) so an admin can grant one to a player who's offline or on a
 *  different instance entirely, and the player can claim it later with /eventbox get from
 *  wherever they happen to be. */
public final class EventBoxClaimDao {

    private final Database database;

    public EventBoxClaimDao(Database database) {
        this.database = database;
    }

    public void addPending(UUID uuid, int amount) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO event_box_claims (uuid, pending) VALUES (?, ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET pending = pending + excluded.pending")) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, amount);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("이벤트 상자 지급 예약에 실패했습니다.", e);
        }
    }

    public int getPending(UUID uuid) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT pending FROM event_box_claims WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("pending") : 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("이벤트 상자 예약 조회에 실패했습니다.", e);
        }
    }

    /** Returns however many were pending, then resets it to zero. */
    public int consumePending(UUID uuid) {
        int pending = getPending(uuid);
        if (pending <= 0) return 0;
        try (PreparedStatement ps = database.connection().prepareStatement(
                "UPDATE event_box_claims SET pending = 0 WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("이벤트 상자 수령 처리에 실패했습니다.", e);
        }
        return pending;
    }
}
