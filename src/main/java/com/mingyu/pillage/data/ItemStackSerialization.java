package com.mingyu.pillage.data;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/** Shared ItemStack[] <-> Base64 text codec, for every DAO that needs to stash an inventory-
 *  shaped array in a single SQLite TEXT column (player inventory, ender chest, ...). */
public final class ItemStackSerialization {

    private ItemStackSerialization() {
    }

    public static String serialize(ItemStack[] items) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataStream = new BukkitObjectOutputStream(byteStream)) {
            dataStream.writeInt(items.length);
            for (ItemStack item : items) {
                dataStream.writeObject(item);
            }
            return Base64.getEncoder().encodeToString(byteStream.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("아이템 직렬화에 실패했습니다.", e);
        }
    }

    public static ItemStack[] deserialize(String raw) {
        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(Base64.getDecoder().decode(raw));
             BukkitObjectInputStream dataStream = new BukkitObjectInputStream(byteStream)) {
            int length = dataStream.readInt();
            ItemStack[] items = new ItemStack[length];
            for (int i = 0; i < length; i++) {
                items[i] = (ItemStack) dataStream.readObject();
            }
            return items;
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("아이템 역직렬화에 실패했습니다.", e);
        }
    }
}
