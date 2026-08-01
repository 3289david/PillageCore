package com.mingyu.pillage.data.dao;

import com.mingyu.pillage.data.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DonorDao {

    private final Database database;

    public DonorDao(Database database) {
        this.database = database;
    }

    public void add(UUID uuid, String badge, UUID addedBy) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO donors (uuid, badge, added_by, added_at) VALUES (?, ?, ?, ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET badge = excluded.badge")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, badge);
            ps.setString(3, addedBy == null ? null : addedBy.toString());
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("후원자 등록에 실패했습니다.", e);
        }
    }

    public void remove(UUID uuid) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "DELETE FROM donors WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("후원자 삭제에 실패했습니다.", e);
        }
    }

    public Map<UUID, String> loadAll() {
        Map<UUID, String> donors = new HashMap<>();
        try (PreparedStatement ps = database.connection().prepareStatement("SELECT uuid, badge FROM donors");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                donors.put(UUID.fromString(rs.getString("uuid")), rs.getString("badge"));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("후원자 목록 로드에 실패했습니다.", e);
        }
        return donors;
    }
}
