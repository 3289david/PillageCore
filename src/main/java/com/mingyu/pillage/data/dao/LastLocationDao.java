package com.mingyu.pillage.data.dao;

import com.mingyu.pillage.data.Database;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public final class LastLocationDao {

    private final Database database;

    public LastLocationDao(Database database) {
        this.database = database;
    }

    public void save(UUID uuid, Location location) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO last_locations (uuid, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET world = excluded.world, x = excluded.x, y = excluded.y, " +
                        "z = excluded.z, yaw = excluded.yaw, pitch = excluded.pitch")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, location.getWorld().getName());
            ps.setDouble(3, location.getX());
            ps.setDouble(4, location.getY());
            ps.setDouble(5, location.getZ());
            ps.setFloat(6, location.getYaw());
            ps.setFloat(7, location.getPitch());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("마지막 위치 저장에 실패했습니다.", e);
        }
    }

    public Location load(UUID uuid) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT * FROM last_locations WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                World world = Bukkit.getWorld(rs.getString("world"));
                if (world == null) return null;
                return new Location(world, rs.getDouble("x"), rs.getDouble("y"),
                        rs.getDouble("z"), rs.getFloat("yaw"), rs.getFloat("pitch"));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("마지막 위치 로드에 실패했습니다.", e);
        }
    }
}
