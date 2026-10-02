package dao;

import model.Wettkampf;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WettkampfDao {

    private final Connection connection;

    public WettkampfDao(Connection connection) {
        this.connection = connection;
    }

    public List<Wettkampf> findAll() throws SQLException {
        List<Wettkampf> result = new ArrayList<>();
        String sql = "SELECT * FROM wettkampf ORDER BY datum DESC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        return result;
    }

    public Wettkampf findById(int id) throws SQLException {
        String sql = "SELECT * FROM wettkampf WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public void insert(Wettkampf wettkampf) throws SQLException {
        String sql = "INSERT INTO wettkampf (name, datum) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, wettkampf.getName());
            ps.setDate(2, wettkampf.getDatum() != null ? Date.valueOf(wettkampf.getDatum()) : null);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    wettkampf.setId(keys.getInt(1));
                }
            }
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM wettkampf WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Wettkampf mapRow(ResultSet rs) throws SQLException {
        Date datum = rs.getDate("datum");
        return new Wettkampf(
                rs.getInt("id"),
                rs.getString("name"),
                datum != null ? datum.toLocalDate() : null
        );
    }
}
