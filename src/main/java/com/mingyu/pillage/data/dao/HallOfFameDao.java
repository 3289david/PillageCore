package com.mingyu.pillage.data.dao;

import com.mingyu.pillage.data.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class HallOfFameDao {

    private final Database database;

    public HallOfFameDao(Database database) {
        this.database = database;
    }

    public void assignSlot(UUID uuid, int slot) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO hall_of_fame (uuid, slot) VALUES (?, ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET slot = excluded.slot")) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, slot);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("명예의 전당 자리 배정에 실패했습니다.", e);
        }
    }

    public void freeSlot(UUID uuid) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "DELETE FROM hall_of_fame WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("명예의 전당 자리 해제에 실패했습니다.", e);
        }
    }

    /** uuid -> slot */
    public Map<UUID, Integer> loadAll() {
        Map<UUID, Integer> map = new HashMap<>();
        try (PreparedStatement ps = database.connection().prepareStatement("SELECT uuid, slot FROM hall_of_fame");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(UUID.fromString(rs.getString("uuid")), rs.getInt("slot"));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("명예의 전당 목록 로드에 실패했습니다.", e);
        }
        return map;
    }
}
