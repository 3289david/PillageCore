package com.mingyu.pillage.data.dao;

import com.mingyu.pillage.data.Database;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Per-instance (each gameplay database has its own copy) active potion effects, so a buff or
 *  debuff picked up in one instance doesn't carry over into another. Effects are stored as a
 *  simple delimited string (type:durationTicks:amplifier:ambient:particles:icon per effect,
 *  effects joined by ";") rather than full object serialization, since that's all a
 *  PotionEffect actually needs to be reconstructed. */
public final class PlayerEffectDao {

    private final Database database;

    public PlayerEffectDao(Database database) {
        this.database = database;
    }

    public void save(UUID uuid, Collection<PotionEffect> effects) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO player_effects (uuid, effects) VALUES (?, ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET effects = excluded.effects")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, serialize(effects));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("이펙트 저장에 실패했습니다.", e);
        }
    }

    /** Returns the saved effects, or an empty list if nothing was ever saved (first time in
     *  this instance) or a stored effect type can no longer be resolved. */
    public List<PotionEffect> load(UUID uuid) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT effects FROM player_effects WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return List.of();
                return deserialize(rs.getString("effects"));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("이펙트 조회에 실패했습니다.", e);
        }
    }

    private String serialize(Collection<PotionEffect> effects) {
        StringBuilder sb = new StringBuilder();
        for (PotionEffect effect : effects) {
            if (!sb.isEmpty()) sb.append(';');
            sb.append(effect.getType().getKey().getKey()).append(':')
                    .append(effect.getDuration()).append(':')
                    .append(effect.getAmplifier()).append(':')
                    .append(effect.isAmbient()).append(':')
                    .append(effect.hasParticles()).append(':')
                    .append(effect.hasIcon());
        }
        return sb.toString();
    }

    private List<PotionEffect> deserialize(String raw) {
        List<PotionEffect> effects = new ArrayList<>();
        if (raw == null || raw.isEmpty()) return effects;
        for (String entry : raw.split(";")) {
            String[] parts = entry.split(":");
            if (parts.length != 6) continue;
            PotionEffectType type = PotionEffectType.getByKey(org.bukkit.NamespacedKey.minecraft(parts[0]));
            if (type == null) continue;
            try {
                effects.add(new PotionEffect(type, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
                        Boolean.parseBoolean(parts[3]), Boolean.parseBoolean(parts[4]), Boolean.parseBoolean(parts[5])));
            } catch (NumberFormatException ignored) {
            }
        }
        return effects;
    }
}
