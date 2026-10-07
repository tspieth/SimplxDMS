package database.dao;

import database.model.Leistung;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LeistungDao {

    private final Connection connection;

    public LeistungDao(Connection connection) {
        this.connection = connection;
    }

    public List<Leistung> findBySchwimmer(int schwimmerId) throws SQLException {
        List<Leistung> result = new ArrayList<>();
        String sql = "SELECT * FROM leistung WHERE schwimmer_id = ? ORDER BY datum DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, schwimmerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    public List<Leistung> findBySchwimmerUndDisziplin(int schwimmerId, int disziplinId) throws SQLException {
        List<Leistung> result = new ArrayList<>();
        String sql = "SELECT * FROM leistung WHERE schwimmer_id = ? AND disziplin_id = ? ORDER BY zeit ASC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, schwimmerId);
            ps.setInt(2, disziplinId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    public void insert(Leistung leistung) throws SQLException {
        String sql = "INSERT INTO leistung (schwimmer_id, disziplin_id, zeit, datum) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, leistung.getSchwimmerId());
            ps.setInt(2, leistung.getDisziplinId());
            ps.setTime(3, Time.valueOf(leistung.getZeit()));
            ps.setDate(4, leistung.getDatum() != null ? Date.valueOf(leistung.getDatum()) : null);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    leistung.setId(keys.getInt(1));
                }
            }
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM leistung WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Leistung mapRow(ResultSet rs) throws SQLException {
        Date datum = rs.getDate("datum");
        return new Leistung(
                rs.getInt("id"),
                rs.getInt("schwimmer_id"),
                rs.getInt("disziplin_id"),
                rs.getTime("zeit").toLocalTime(),
                datum != null ? datum.toLocalDate() : null
        );
    }
}
