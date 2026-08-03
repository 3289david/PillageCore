package com.mingyu.pillage.data.dao;

import com.mingyu.pillage.data.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/** Tracks which instance (main / hub / a mini-server id) each player was last inside, so a
 *  reconnecting player can be dropped straight back into it instead of the hub. */
public final class PlayerInstanceDao {

    private final Database database;

    public PlayerInstanceDao(Database database) {
        this.database = database;
    }

    public void set(UUID uuid, String instanceId) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO player_last_instance (uuid, instance_id) VALUES (?, ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET instance_id = excluded.instance_id")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, instanceId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("마지막 접속 서버 저장에 실패했습니다.", e);
        }
    }

    /** Returns the saved instance id, or null if the player has never been tracked. */
    public String get(UUID uuid) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT instance_id FROM player_last_instance WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("instance_id") : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("마지막 접속 서버 조회에 실패했습니다.", e);
        }
    }
}
