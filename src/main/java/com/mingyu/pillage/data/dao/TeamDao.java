package com.mingyu.pillage.data.dao;

import com.mingyu.pillage.data.Database;
import com.mingyu.pillage.team.Team;
import com.mingyu.pillage.team.TeamRole;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TeamDao {

    private final Database database;

    public TeamDao(Database database) {
        this.database = database;
    }

    public Team createTeam(String name, UUID leader, int maxMembers) {
        Connection conn = database.connection();
        long now = System.currentTimeMillis();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO teams (name, leader_uuid, max_members, created_at) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, leader.toString());
            ps.setInt(3, maxMembers);
            ps.setLong(4, now);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                int id = keys.getInt(1);
                addMember(id, leader, TeamRole.LEADER);
                return new Team(id, name, leader, maxMembers, now);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("팀 생성에 실패했습니다.", e);
        }
    }

    public void addMember(int teamId, UUID uuid, TeamRole role) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO team_members (uuid, team_id, role) VALUES (?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, teamId);
            ps.setString(3, role.name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("팀원 추가에 실패했습니다.", e);
        }
    }

    public void removeMember(UUID uuid) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "DELETE FROM team_members WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("팀원 제거에 실패했습니다.", e);
        }
    }

    public void updateRole(UUID uuid, TeamRole role) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "UPDATE team_members SET role = ? WHERE uuid = ?")) {
            ps.setString(1, role.name());
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("팀원 역할 변경에 실패했습니다.", e);
        }
    }

    public void updateLeader(int teamId, UUID leader) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "UPDATE teams SET leader_uuid = ? WHERE id = ?")) {
            ps.setString(1, leader.toString());
            ps.setInt(2, teamId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("팀장 변경에 실패했습니다.", e);
        }
    }

    public void updateFriendlyFire(int teamId, boolean friendlyFire) {
        setColumn(teamId, "friendly_fire", friendlyFire ? 1 : 0);
    }

    public void updateMaxMembers(int teamId, int maxMembers) {
        setColumn(teamId, "max_members", maxMembers);
    }

    public void addKill(int teamId) {
        incrementColumn(teamId, "kills");
    }

    public void addLootScore(int teamId, int amount) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "UPDATE teams SET loot_score = loot_score + ? WHERE id = ?")) {
            ps.setInt(1, amount);
            ps.setInt(2, teamId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("약탈 점수 갱신에 실패했습니다.", e);
        }
    }

    public void addRaidWon(int teamId) {
        incrementColumn(teamId, "raids_won");
    }

    public void addRaidDefended(int teamId) {
        incrementColumn(teamId, "raids_defended");
    }

    public void updateHome(int teamId, Location location) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "UPDATE teams SET home_world = ?, home_x = ?, home_y = ?, home_z = ?, home_yaw = ?, home_pitch = ? WHERE id = ?")) {
            ps.setString(1, location.getWorld().getName());
            ps.setDouble(2, location.getX());
            ps.setDouble(3, location.getY());
            ps.setDouble(4, location.getZ());
            ps.setFloat(5, location.getYaw());
            ps.setFloat(6, location.getPitch());
            ps.setInt(7, teamId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("팀 홈 설정에 실패했습니다.", e);
        }
    }

    public void deleteTeam(int teamId) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "DELETE FROM teams WHERE id = ?")) {
            ps.setInt(1, teamId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("팀 삭제에 실패했습니다.", e);
        }
    }

    public void renameTeam(int teamId, String name) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "UPDATE teams SET name = ? WHERE id = ?")) {
            ps.setString(1, name);
            ps.setInt(2, teamId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("팀 이름 변경에 실패했습니다.", e);
        }
    }

    private void setColumn(int teamId, String column, Object value) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "UPDATE teams SET " + column + " = ? WHERE id = ?")) {
            ps.setObject(1, value);
            ps.setInt(2, teamId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("팀 정보 갱신에 실패했습니다.", e);
        }
    }

    private void incrementColumn(int teamId, String column) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "UPDATE teams SET " + column + " = " + column + " + 1 WHERE id = ?")) {
            ps.setInt(1, teamId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("팀 정보 갱신에 실패했습니다.", e);
        }
    }

    public List<Team> loadAll() {
        List<Team> teams = new ArrayList<>();
        try (Statement st = database.connection().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM teams")) {
            while (rs.next()) {
                teams.add(mapTeam(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("팀 목록 로드에 실패했습니다.", e);
        }

        try (Statement st = database.connection().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM team_members")) {
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                int teamId = rs.getInt("team_id");
                TeamRole role = TeamRole.valueOf(rs.getString("role"));
                for (Team team : teams) {
                    if (team.id() == teamId && !uuid.equals(team.leader())) {
                        team.members().put(uuid, role);
                    }
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("팀원 목록 로드에 실패했습니다.", e);
        }
        return teams;
    }

    private Team mapTeam(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        UUID leader = UUID.fromString(rs.getString("leader_uuid"));
        int maxMembers = rs.getInt("max_members");
        long createdAt = rs.getLong("created_at");

        Team team = new Team(id, name, leader, maxMembers, createdAt);
        team.setFriendlyFire(rs.getInt("friendly_fire") == 1);

        for (int i = 0; i < rs.getInt("kills"); i++) team.addKill();
        team.addLootScore(rs.getInt("loot_score"));
        for (int i = 0; i < rs.getInt("raids_won"); i++) team.addRaidWon();
        for (int i = 0; i < rs.getInt("raids_defended"); i++) team.addRaidDefended();

        String world = rs.getString("home_world");
        if (world != null) {
            World bukkitWorld = Bukkit.getWorld(world);
            if (bukkitWorld != null) {
                team.setHome(new Location(bukkitWorld, rs.getDouble("home_x"), rs.getDouble("home_y"),
                        rs.getDouble("home_z"), rs.getFloat("home_yaw"), rs.getFloat("home_pitch")));
            }
        }
        return team;
    }
}
