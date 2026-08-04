package com.mingyu.pillage.data.dao;

import com.mingyu.pillage.data.Database;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/** Per-instance (each gameplay database has its own copy) last-known position, so switching back
 *  into an instance resumes exactly where the player left it instead of always the world spawn. */
public final class PlayerPositionDao {

    private final Database database;

    public PlayerPositionDao(Database database) {
        this.database = database;
    }

    public void save(Player player) {
        Location loc = player.getLocation();
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO player_position (uuid, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET world = excluded.world, x = excluded.x, y = excluded.y, " +
                        "z = excluded.z, yaw = excluded.yaw, pitch = excluded.pitch")) {
            ps.setString(1, player.getUniqueId().toString());
            ps.setString(2, loc.getWorld().getName());
            ps.setDouble(3, loc.getX());
            ps.setDouble(4, loc.getY());
            ps.setDouble(5, loc.getZ());
            ps.setFloat(6, loc.getYaw());
            ps.setFloat(7, loc.getPitch());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("위치 저장에 실패했습니다.", e);
        }
    }

    /** Returns the saved location, or null if nothing was ever saved (first time in this instance)
     *  or the saved world can't be resolved. */
    public Location load(UUID uuid) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT world, x, y, z, yaw, pitch FROM player_position WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                World world = Bukkit.getWorld(rs.getString("world"));
                if (world == null) return null;
                return new Location(world, rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                        rs.getFloat("yaw"), rs.getFloat("pitch"));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("위치 조회에 실패했습니다.", e);
        }
    }
}
