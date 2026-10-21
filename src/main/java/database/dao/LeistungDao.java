package database.dao;

import database.model.Leistung;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

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

    public OptionalDouble findBestTimeSeconds(int schwimmerId, int disziplinId) throws SQLException {
        String sql = "SELECT MIN(zeit) AS bestzeit FROM leistung WHERE schwimmer_id = ? AND disziplin_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, schwimmerId);
            ps.setInt(2, disziplinId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return OptionalDouble.empty();
                }
                java.time.LocalTime time = rs.getObject("bestzeit", java.time.LocalTime.class);
                return time == null ? OptionalDouble.empty()
                        : OptionalDouble.of(time.toSecondOfDay() + time.getNano() / 1_000_000_000.0);
            }
        }
    }

    public void insert(Leistung leistung) throws SQLException {
        String sql = "INSERT INTO leistung (schwimmer_id, disziplin_id, zeit, datum) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, leistung.getSchwimmerId());
            ps.setInt(2, leistung.getDisziplinId());
            ps.setObject(3, leistung.getZeit());
            ps.setDate(4, leistung.getDatum() != null ? Date.valueOf(leistung.getDatum()) : null);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    leistung.setId(keys.getInt(1));
                }
            }
        }
    }

    /**
     * Updates the discipline, time, and date of an existing best time.
     *
     * @param leistung updated best-time data
     * @throws SQLException if the update fails
     */
    public void update(Leistung leistung) throws SQLException {
        String sql = "UPDATE leistung SET disziplin_id = ?, zeit = ?, datum = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, leistung.getDisziplinId());
            ps.setObject(2, leistung.getZeit());
            ps.setDate(3, leistung.getDatum() != null ? Date.valueOf(leistung.getDatum()) : null);
            ps.setInt(4, leistung.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Deletes every best time belonging to a swimmer.
     *
     * @param schwimmerId swimmer database ID
     * @throws SQLException if the delete fails
     */
    public void deleteBySchwimmer(int schwimmerId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM leistung WHERE schwimmer_id = ?")) {
            ps.setInt(1, schwimmerId);
            ps.executeUpdate();
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
                rs.getObject("zeit", java.time.LocalTime.class),
                datum != null ? datum.toLocalDate() : null
        );
    }
}
