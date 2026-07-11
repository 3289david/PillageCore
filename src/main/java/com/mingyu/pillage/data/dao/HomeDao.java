package com.mingyu.pillage.data.dao;

import com.mingyu.pillage.data.Database;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class HomeDao {

    private final Database database;

    public HomeDao(Database database) {
        this.database = database;
    }

    public void setHome(UUID uuid, String name, Location location) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO homes (uuid, name, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                        "ON CONFLICT(uuid, name) DO UPDATE SET world = excluded.world, x = excluded.x, y = excluded.y, " +
                        "z = excluded.z, yaw = excluded.yaw, pitch = excluded.pitch")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name.toLowerCase());
            ps.setString(3, location.getWorld().getName());
            ps.setDouble(4, location.getX());
            ps.setDouble(5, location.getY());
            ps.setDouble(6, location.getZ());
            ps.setFloat(7, location.getYaw());
            ps.setFloat(8, location.getPitch());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("홈 설정에 실패했습니다.", e);
        }
    }

    public void deleteHome(UUID uuid, String name) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "DELETE FROM homes WHERE uuid = ? AND name = ?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name.toLowerCase());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("홈 삭제에 실패했습니다.", e);
        }
    }

    public Map<String, Location> loadHomes(UUID uuid) {
        Map<String, Location> homes = new LinkedHashMap<>();
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT * FROM homes WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    World world = Bukkit.getWorld(rs.getString("world"));
                    if (world == null) continue;
                    Location loc = new Location(world, rs.getDouble("x"), rs.getDouble("y"),
                            rs.getDouble("z"), rs.getFloat("yaw"), rs.getFloat("pitch"));
                    homes.put(rs.getString("name"), loc);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("홈 목록 로드에 실패했습니다.", e);
        }
        return homes;
    }
}
