package com.mingyu.pillage.data.dao;

import com.mingyu.pillage.data.Database;
import com.mingyu.pillage.data.ItemStackSerialization;
import org.bukkit.inventory.ItemStack;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Admin-configured pool of reward items handed out to every contributor when the boss dies -
 *  populated by holding an item and running /boss reward add, global since the boss itself is
 *  global. */
public final class BossRewardDao {

    private final Database database;

    public BossRewardDao(Database database) {
        this.database = database;
    }

    public void add(ItemStack item) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO boss_rewards (item_data) VALUES (?)")) {
            ps.setString(1, ItemStackSerialization.serialize(new ItemStack[]{item}));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("보스 보상 아이템 저장에 실패했습니다.", e);
        }
    }

    public void clear() {
        try (PreparedStatement ps = database.connection().prepareStatement("DELETE FROM boss_rewards")) {
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("보스 보상 아이템 초기화에 실패했습니다.", e);
        }
    }

    public List<ItemStack> loadAll() {
        List<ItemStack> items = new ArrayList<>();
        try (PreparedStatement ps = database.connection().prepareStatement("SELECT item_data FROM boss_rewards");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ItemStack[] arr = ItemStackSerialization.deserialize(rs.getString("item_data"));
                if (arr.length > 0 && arr[0] != null) items.add(arr[0]);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("보스 보상 아이템 로드에 실패했습니다.", e);
        }
        return items;
    }
}
