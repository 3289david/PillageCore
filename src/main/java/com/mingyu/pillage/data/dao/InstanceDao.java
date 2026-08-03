package com.mingyu.pillage.data.dao;

import com.mingyu.pillage.data.Database;
import com.mingyu.pillage.instance.InstanceInfo;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class InstanceDao {

    private final Database database;

    public InstanceDao(Database database) {
        this.database = database;
    }

    public void insert(String id, String name, UUID owner, String worldName, long createdAt) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO instances (id, name, owner_uuid, world_name, created_at) VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, id);
            ps.setString(2, name);
            ps.setString(3, owner.toString());
            ps.setString(4, worldName);
            ps.setLong(5, createdAt);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("미니서버 등록에 실패했습니다.", e);
        }
    }

    public void delete(String id) {
        try (PreparedStatement ps = database.connection().prepareStatement("DELETE FROM instances WHERE id = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("미니서버 삭제에 실패했습니다.", e);
        }
    }

    public Optional<InstanceInfo> find(String id) {
        try (PreparedStatement ps = database.connection().prepareStatement("SELECT * FROM instances WHERE id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("미니서버 조회에 실패했습니다.", e);
        }
    }

    public Optional<InstanceInfo> findByName(String name) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT * FROM instances WHERE LOWER(name) = LOWER(?)")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("미니서버 조회에 실패했습니다.", e);
        }
    }

    public List<InstanceInfo> loadAll() {
        List<InstanceInfo> instances = new ArrayList<>();
        try (PreparedStatement ps = database.connection().prepareStatement("SELECT * FROM instances ORDER BY created_at ASC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                instances.add(map(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("미니서버 목록 로드에 실패했습니다.", e);
        }
        return instances;
    }

    private InstanceInfo map(ResultSet rs) throws SQLException {
        return new InstanceInfo(
                rs.getString("id"),
                rs.getString("name"),
                UUID.fromString(rs.getString("owner_uuid")),
                rs.getString("world_name"),
                rs.getLong("created_at"));
    }
}
