package com.mingyu.pillage.data.dao;

import com.mingyu.pillage.data.Database;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/** Per-instance (each gameplay database has its own copy) so a player's inventory in one
 *  instance never crosses over into another. */
public final class PlayerInventoryDao {

    public record SavedInventory(ItemStack[] contents, ItemStack[] armor, ItemStack offhand) {
    }

    private final Database database;

    public PlayerInventoryDao(Database database) {
        this.database = database;
    }

    public void save(UUID uuid, ItemStack[] contents, ItemStack[] armor, ItemStack offhand) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO player_inventory (uuid, contents, armor, offhand) VALUES (?, ?, ?, ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET contents = excluded.contents, armor = excluded.armor, offhand = excluded.offhand")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, serialize(contents));
            ps.setString(3, serialize(armor));
            ps.setString(4, serialize(new ItemStack[]{offhand}));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("인벤토리 저장에 실패했습니다.", e);
        }
    }

    public Optional<SavedInventory> load(UUID uuid) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT contents, armor, offhand FROM player_inventory WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                ItemStack[] offhand = deserialize(rs.getString("offhand"));
                return Optional.of(new SavedInventory(
                        deserialize(rs.getString("contents")),
                        deserialize(rs.getString("armor")),
                        offhand.length > 0 ? offhand[0] : null));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("인벤토리 로드에 실패했습니다.", e);
        }
    }

    private String serialize(ItemStack[] items) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataStream = new BukkitObjectOutputStream(byteStream)) {
            dataStream.writeInt(items.length);
            for (ItemStack item : items) {
                dataStream.writeObject(item);
            }
            return Base64.getEncoder().encodeToString(byteStream.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("인벤토리 직렬화에 실패했습니다.", e);
        }
    }

    private ItemStack[] deserialize(String raw) {
        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(Base64.getDecoder().decode(raw));
             BukkitObjectInputStream dataStream = new BukkitObjectInputStream(byteStream)) {
            int length = dataStream.readInt();
            ItemStack[] items = new ItemStack[length];
            for (int i = 0; i < length; i++) {
                items[i] = (ItemStack) dataStream.readObject();
            }
            return items;
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("인벤토리 역직렬화에 실패했습니다.", e);
        }
    }
}
