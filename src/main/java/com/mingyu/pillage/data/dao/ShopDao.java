package com.mingyu.pillage.data.dao;

import com.mingyu.pillage.data.Database;
import com.mingyu.pillage.shop.ShopOffer;
import org.bukkit.Material;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class ShopDao {

    private final Database database;

    public ShopDao(Database database) {
        this.database = database;
    }

    public ShopOffer addOffer(Material inputMaterial, int inputAmount, Material outputMaterial, int outputAmount) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO shop_offers (input_material, input_amount, output_material, output_amount) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, inputMaterial.name());
            ps.setInt(2, inputAmount);
            ps.setString(3, outputMaterial.name());
            ps.setInt(4, outputAmount);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                int id = keys.getInt(1);
                return new ShopOffer(id, inputMaterial, inputAmount, outputMaterial, outputAmount);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("상점 항목 추가에 실패했습니다.", e);
        }
    }

    public void removeOffer(int id) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "DELETE FROM shop_offers WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("상점 항목 삭제에 실패했습니다.", e);
        }
    }

    public List<ShopOffer> loadAll() {
        List<ShopOffer> offers = new ArrayList<>();
        try (Statement st = database.connection().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM shop_offers ORDER BY id")) {
            while (rs.next()) {
                offers.add(new ShopOffer(
                        rs.getInt("id"),
                        Material.valueOf(rs.getString("input_material")),
                        rs.getInt("input_amount"),
                        Material.valueOf(rs.getString("output_material")),
                        rs.getInt("output_amount")));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("상점 목록 로드에 실패했습니다.", e);
        }
        return offers;
    }
}
