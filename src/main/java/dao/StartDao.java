package dao;

import model.Start;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StartDao {

    private final Connection connection;

    public StartDao(Connection connection) {
        this.connection = connection;
    }

    /** Alle Starts (Schwimmer-Zuordnungen) fuer einen bestimmten Lauf. */
    public List<Start> findByLauf(int laufId) throws SQLException {
        List<Start> result = new ArrayList<>();
        String sql = "SELECT * FROM start WHERE lauf_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, laufId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    /** Alle Starts einer Mannschaft - Grundlage fuer die Gesamtpunktzahl (SUM). */
    public List<Start> findByMannschaft(int mannschaftId) throws SQLException {
        List<Start> result = new ArrayList<>();
        String sql = "SELECT * FROM start WHERE mannschaft_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, mannschaftId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    /** Berechnet die Gesamtpunktzahl einer Mannschaft direkt in SQL (SUM ueber alle Starts). */
    public double gesamtpunkteFuerMannschaft(int mannschaftId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(punkte), 0) AS summe FROM start WHERE mannschaft_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, mannschaftId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble("summe") : 0.0;
            }
        }
    }

    public void insert(Start start) throws SQLException {
        String sql = "INSERT INTO start (lauf_id, mannschaft_id, schwimmer_id, erzielte_zeit, punkte) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, start.getLaufId());
            ps.setInt(2, start.getMannschaftId());
            ps.setInt(3, start.getSchwimmerId());
            if (start.getErzielteZeit() != null) {
                ps.setTime(4, Time.valueOf(start.getErzielteZeit()));
            } else {
                ps.setNull(4, Types.TIME);
            }
            if (start.getPunkte() != null) {
                ps.setDouble(5, start.getPunkte());
            } else {
                ps.setNull(5, Types.DOUBLE);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    start.setId(keys.getInt(1));
                }
            }
        }
    }

    public void update(Start start) throws SQLException {
        String sql = "UPDATE start SET erzielte_zeit = ?, punkte = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            if (start.getErzielteZeit() != null) {
                ps.setTime(1, Time.valueOf(start.getErzielteZeit()));
            } else {
                ps.setNull(1, Types.TIME);
            }
            if (start.getPunkte() != null) {
                ps.setDouble(2, start.getPunkte());
            } else {
                ps.setNull(2, Types.DOUBLE);
            }
            ps.setInt(3, start.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM start WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Start mapRow(ResultSet rs) throws SQLException {
        Time zeit = rs.getTime("erzielte_zeit");
        double punkteRaw = rs.getDouble("punkte");
        Double punkte = rs.wasNull() ? null : punkteRaw;
        return new Start(
                rs.getInt("id"),
                rs.getInt("lauf_id"),
                rs.getInt("mannschaft_id"),
                rs.getInt("schwimmer_id"),
                zeit != null ? zeit.toLocalTime() : null,
                punkte
        );
    }
}
