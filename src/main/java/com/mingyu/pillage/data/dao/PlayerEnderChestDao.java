package com.mingyu.pillage.data.dao;

import com.mingyu.pillage.data.Database;
import com.mingyu.pillage.data.ItemStackSerialization;
import org.bukkit.inventory.ItemStack;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

/** Per-instance (each gameplay database has its own copy) ender chest, same isolation as the
 *  regular inventory - Bukkit stores it globally per player otherwise. */
public final class PlayerEnderChestDao {

    private final Database database;

    public PlayerEnderChestDao(Database database) {
        this.database = database;
    }

    public void save(UUID uuid, ItemStack[] contents) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO player_enderchest (uuid, contents) VALUES (?, ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET contents = excluded.contents")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, ItemStackSerialization.serialize(contents));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("엔더상자 저장에 실패했습니다.", e);
        }
    }

    public Optional<ItemStack[]> load(UUID uuid) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT contents FROM player_enderchest WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(ItemStackSerialization.deserialize(rs.getString("contents")));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("엔더상자 로드에 실패했습니다.", e);
        }
    }
}
