package com.mingyu.pillage.data.dao;

import com.mingyu.pillage.data.Database;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class HallOfFameMetaDao {

    private final Database database;

    public HallOfFameMetaDao(Database database) {
        this.database = database;
    }

    /** Returns the saved origin, or null if the monument hasn't been built yet. */
    public Location loadOrigin() {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT * FROM hall_of_fame_meta WHERE id = 1 AND built = 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                World world = Bukkit.getWorld(rs.getString("world"));
                if (world == null) return null;
                return new Location(world, rs.getInt("origin_x"), rs.getInt("origin_y"), rs.getInt("origin_z"));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("명예의 전당 위치 조회에 실패했습니다.", e);
        }
    }

    public void saveOrigin(Location origin) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO hall_of_fame_meta (id, built, world, origin_x, origin_y, origin_z) VALUES (1, 1, ?, ?, ?, ?) " +
                        "ON CONFLICT(id) DO UPDATE SET built = 1, world = excluded.world, " +
                        "origin_x = excluded.origin_x, origin_y = excluded.origin_y, origin_z = excluded.origin_z")) {
            ps.setString(1, origin.getWorld().getName());
            ps.setInt(2, origin.getBlockX());
            ps.setInt(3, origin.getBlockY());
            ps.setInt(4, origin.getBlockZ());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("명예의 전당 위치 저장에 실패했습니다.", e);
        }
    }
}
