package com.mingyu.pillage.data.dao;

import com.mingyu.pillage.data.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TradeLogDao {

    public record Entry(String playerA, String playerB, String itemsA, String itemsB, long timestamp) {
    }

    private final Database database;

    public TradeLogDao(Database database) {
        this.database = database;
    }

    public void log(UUID playerA, UUID playerB, String itemsA, String itemsB) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO trade_log (player_a, player_b, items_a, items_b, timestamp) VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, playerA.toString());
            ps.setString(2, playerB.toString());
            ps.setString(3, itemsA);
            ps.setString(4, itemsB);
            ps.setLong(5, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("거래 로그 기록에 실패했습니다.", e);
        }
    }

    public List<Entry> recent(int limit) {
        List<Entry> entries = new ArrayList<>();
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT * FROM trade_log ORDER BY id DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add(new Entry(rs.getString("player_a"), rs.getString("player_b"),
                            rs.getString("items_a"), rs.getString("items_b"), rs.getLong("timestamp")));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("거래 로그 조회에 실패했습니다.", e);
        }
        return entries;
    }
}
