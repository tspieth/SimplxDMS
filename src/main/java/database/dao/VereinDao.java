package database.dao;

import database.model.Verein;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VereinDao {

    private final Connection connection;

    public VereinDao(Connection connection) {
        this.connection = connection;
    }

    public List<Verein> findAll() throws SQLException {
        List<Verein> result = new ArrayList<>();
        String sql = "SELECT * FROM verein ORDER BY name";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        return result;
    }

    public Verein findById(int id) throws SQLException {
        String sql = "SELECT * FROM verein WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /** Fuegt den Verein ein und traegt die generierte ID direkt ins uebergebene Objekt ein. */
    public void insert(Verein verein) throws SQLException {
        String sql = "INSERT INTO verein (name) VALUES (?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, verein.getName());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    verein.setId(keys.getInt(1));
                }
            }
        }
    }

    public void update(Verein verein) throws SQLException {
        String sql = "UPDATE verein SET name = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, verein.getName());
            ps.setInt(2, verein.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM verein WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Verein mapRow(ResultSet rs) throws SQLException {
        return new Verein(rs.getInt("id"), rs.getString("name"));
    }
}
