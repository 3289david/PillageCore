package com.mingyu.pillage.data.dao;

import com.mingyu.pillage.data.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/** Per-instance (each gameplay database has its own copy) game mode, so switching a player to
 *  creative in one instance (e.g. a mini-server admin building) never carries over into another
 *  - Bukkit stores it globally per player otherwise. */
public final class PlayerGameModeDao {

    private final Database database;

    public PlayerGameModeDao(Database database) {
        this.database = database;
    }

    public void save(UUID uuid, String gameMode) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO player_gamemode (uuid, mode) VALUES (?, ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET mode = excluded.mode")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, gameMode);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("게임 모드 저장에 실패했습니다.", e);
        }
    }

    /** Returns the saved game mode name, or null if nothing was ever saved (first time in this
     *  instance - defaults to survival). */
    public String load(UUID uuid) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT mode FROM player_gamemode WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("mode") : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("게임 모드 조회에 실패했습니다.", e);
        }
    }
}
