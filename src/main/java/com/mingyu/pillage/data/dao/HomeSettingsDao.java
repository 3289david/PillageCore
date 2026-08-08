package com.mingyu.pillage.data.dao;

import com.mingyu.pillage.data.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Per-instance (each gameplay database has its own copy) on/off switch for /sethome, /home,
 *  /delhome, so a mini-server's admin can turn the home system off for their own server without
 *  affecting any other instance. */
public final class HomeSettingsDao {

    private final Database database;

    public HomeSettingsDao(Database database) {
        this.database = database;
    }

    public boolean isEnabled() {
        try (Statement st = database.connection().createStatement();
             ResultSet rs = st.executeQuery("SELECT enabled FROM home_settings WHERE id = 1")) {
            return !rs.next() || rs.getInt("enabled") != 0;
        } catch (SQLException e) {
            throw new IllegalStateException("홈 설정 조회에 실패했습니다.", e);
        }
    }

    public void setEnabled(boolean enabled) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO home_settings (id, enabled) VALUES (1, ?) " +
                        "ON CONFLICT(id) DO UPDATE SET enabled = excluded.enabled")) {
            ps.setInt(1, enabled ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("홈 설정 저장에 실패했습니다.", e);
        }
    }
}
