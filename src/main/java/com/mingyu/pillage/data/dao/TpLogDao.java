package com.mingyu.pillage.data.dao;

import com.mingyu.pillage.data.Database;
import org.bukkit.Location;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TpLogDao {

    public record Entry(String player, String kind, String world, double x, double y, double z, long timestamp) {
    }

    private final Database database;

    public TpLogDao(Database database) {
        this.database = database;
    }

    public void log(UUID player, String kind, Location location) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO tp_log (player, kind, world, x, y, z, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, player.toString());
            ps.setString(2, kind);
            ps.setString(3, location.getWorld().getName());
            ps.setDouble(4, location.getX());
            ps.setDouble(5, location.getY());
            ps.setDouble(6, location.getZ());
            ps.setLong(7, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("TP 로그 기록에 실패했습니다.", e);
        }
    }

    public List<Entry> recent(int limit) {
        List<Entry> entries = new ArrayList<>();
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT * FROM tp_log ORDER BY id DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add(new Entry(rs.getString("player"), rs.getString("kind"), rs.getString("world"),
                            rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"), rs.getLong("timestamp")));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("TP 로그 조회에 실패했습니다.", e);
        }
        return entries;
    }
}
