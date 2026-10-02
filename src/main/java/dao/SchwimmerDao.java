package dao;

import model.Schwimmer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SchwimmerDao {

    private final Connection connection;

    public SchwimmerDao(Connection connection) {
        this.connection = connection;
    }

    public List<Schwimmer> findAll() throws SQLException {
        List<Schwimmer> result = new ArrayList<>();
        String sql = "SELECT * FROM schwimmer ORDER BY name, vorname";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        return result;
    }

    public List<Schwimmer> findByVerein(int vereinId) throws SQLException {
        List<Schwimmer> result = new ArrayList<>();
        String sql = "SELECT * FROM schwimmer WHERE verein_id = ? ORDER BY name, vorname";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, vereinId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    public List<Schwimmer> findByGeschlecht(String geschlecht) throws SQLException {
        List<Schwimmer> result = new ArrayList<>();
        String sql = "SELECT * FROM schwimmer WHERE geschlecht = ? ORDER BY name, vorname";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, geschlecht);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    public Schwimmer findById(int id) throws SQLException {
        String sql = "SELECT * FROM schwimmer WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public void insert(Schwimmer schwimmer) throws SQLException {
        String sql = "INSERT INTO schwimmer (name, vorname, geburtsdatum, geschlecht, verein_id) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, schwimmer.getName());
            ps.setString(2, schwimmer.getVorname());
            ps.setDate(3, schwimmer.getGeburtsdatum() != null ? Date.valueOf(schwimmer.getGeburtsdatum()) : null);
            ps.setString(4, schwimmer.getGeschlecht());
            ps.setInt(5, schwimmer.getVereinId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    schwimmer.setId(keys.getInt(1));
                }
            }
        }
    }

    public void update(Schwimmer schwimmer) throws SQLException {
        String sql = "UPDATE schwimmer SET name = ?, vorname = ?, geburtsdatum = ?, geschlecht = ?, verein_id = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, schwimmer.getName());
            ps.setString(2, schwimmer.getVorname());
            ps.setDate(3, schwimmer.getGeburtsdatum() != null ? Date.valueOf(schwimmer.getGeburtsdatum()) : null);
            ps.setString(4, schwimmer.getGeschlecht());
            ps.setInt(5, schwimmer.getVereinId());
            ps.setInt(6, schwimmer.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM schwimmer WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Schwimmer mapRow(ResultSet rs) throws SQLException {
        Date geburtsdatum = rs.getDate("geburtsdatum");
        return new Schwimmer(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("vorname"),
                geburtsdatum != null ? geburtsdatum.toLocalDate() : null,
                rs.getString("geschlecht"),
                rs.getInt("verein_id")
        );
    }
}
