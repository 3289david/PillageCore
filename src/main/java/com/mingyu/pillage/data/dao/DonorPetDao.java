package com.mingyu.pillage.data.dao;

import com.mingyu.pillage.data.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public final class DonorPetDao {

    public record PetInfo(String name, String variant) {
    }

    private final Database database;

    public DonorPetDao(Database database) {
        this.database = database;
    }

    public PetInfo get(UUID uuid) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT name, variant FROM donor_pets WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new PetInfo(rs.getString("name"), rs.getString("variant"));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("펫 정보 조회에 실패했습니다.", e);
        }
    }

    public void setName(UUID uuid, String name) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO donor_pets (uuid, name, variant) VALUES (?, ?, 'TABBY') " +
                        "ON CONFLICT(uuid) DO UPDATE SET name = excluded.name")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("펫 이름 저장에 실패했습니다.", e);
        }
    }

    public void setVariant(UUID uuid, String variant) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO donor_pets (uuid, variant) VALUES (?, ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET variant = excluded.variant")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, variant);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("펫 종류 저장에 실패했습니다.", e);
        }
    }

    public void remove(UUID uuid) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "DELETE FROM donor_pets WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("펫 정보 삭제에 실패했습니다.", e);
        }
    }
}
