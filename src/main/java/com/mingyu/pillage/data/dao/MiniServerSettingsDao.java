package com.mingyu.pillage.data.dao;

import com.mingyu.pillage.data.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Server-wide (global database, not per-instance) on/off switch for creating new mini-servers via
 *  /mini create. Existing mini-servers stay joinable either way - this only stops new growth. */
public final class MiniServerSettingsDao {

    private final Database database;

    public MiniServerSettingsDao(Database database) {
        this.database = database;
    }

    public boolean isCreationEnabled() {
        try (Statement st = database.connection().createStatement();
             ResultSet rs = st.executeQuery("SELECT creation_enabled FROM mini_server_settings WHERE id = 1")) {
            return !rs.next() || rs.getInt("creation_enabled") != 0;
        } catch (SQLException e) {
            throw new IllegalStateException("미니서버 설정 조회에 실패했습니다.", e);
        }
    }

    public void setCreationEnabled(boolean enabled) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO mini_server_settings (id, creation_enabled) VALUES (1, ?) " +
                        "ON CONFLICT(id) DO UPDATE SET creation_enabled = excluded.creation_enabled")) {
            ps.setInt(1, enabled ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("미니서버 설정 저장에 실패했습니다.", e);
        }
    }
}
