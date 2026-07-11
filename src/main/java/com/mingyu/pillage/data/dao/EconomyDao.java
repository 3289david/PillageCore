package com.mingyu.pillage.data.dao;

import com.mingyu.pillage.data.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public final class EconomyDao {

    private final Database database;

    public EconomyDao(Database database) {
        this.database = database;
    }

    private void ensureRow(UUID uuid) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT OR IGNORE INTO economy (uuid, balance) VALUES (?, 0)")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("경제 데이터 초기화에 실패했습니다.", e);
        }
    }

    public long balance(UUID uuid) {
        ensureRow(uuid);
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT balance FROM economy WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("balance") : 0L;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("잔액 조회에 실패했습니다.", e);
        }
    }

    public void add(UUID uuid, long amount) {
        ensureRow(uuid);
        try (PreparedStatement ps = database.connection().prepareStatement(
                "UPDATE economy SET balance = balance + ? WHERE uuid = ?")) {
            ps.setLong(1, amount);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("잔액 갱신에 실패했습니다.", e);
        }
    }

    public boolean subtract(UUID uuid, long amount) {
        if (balance(uuid) < amount) return false;
        add(uuid, -amount);
        return true;
    }
}
