package database.dao;

import database.model.Mannschaft;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MannschaftDao {

    private final Connection connection;

    public MannschaftDao(Connection connection) {
        this.connection = connection;
    }

    public List<Mannschaft> findByWettkampf(int wettkampfId) throws SQLException {
        List<Mannschaft> result = new ArrayList<>();
        String sql = "SELECT * FROM mannschaft WHERE wettkampf_id = ? ORDER BY bezeichnung";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, wettkampfId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    public List<Mannschaft> findByWettkampfUndVerein(int wettkampfId, int vereinId) throws SQLException {
        List<Mannschaft> result = new ArrayList<>();
        String sql = "SELECT * FROM mannschaft WHERE wettkampf_id = ? AND verein_id = ? ORDER BY bezeichnung";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, wettkampfId);
            ps.setInt(2, vereinId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    public long countStarts(int mannschaftId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM start WHERE mannschaft_id = ?")) {
            ps.setInt(1, mannschaftId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public void insert(Mannschaft mannschaft) throws SQLException {
        String sql = "INSERT INTO mannschaft (verein_id, wettkampf_id, bezeichnung) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, mannschaft.getVereinId());
            ps.setInt(2, mannschaft.getWettkampfId());
            ps.setString(3, mannschaft.getBezeichnung());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    mannschaft.setId(keys.getInt(1));
                }
            }
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM mannschaft WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Mannschaft mapRow(ResultSet rs) throws SQLException {
        return new Mannschaft(
                rs.getInt("id"),
                rs.getInt("verein_id"),
                rs.getInt("wettkampf_id"),
                rs.getString("bezeichnung")
        );
    }
}
