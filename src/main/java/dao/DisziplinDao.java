package dao;

import model.Disziplin;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DisziplinDao {

    private final Connection connection;

    public DisziplinDao(Connection connection) {
        this.connection = connection;
    }

    public List<Disziplin> findAll() throws SQLException {
        List<Disziplin> result = new ArrayList<>();
        String sql = "SELECT * FROM disziplin ORDER BY distanz, lage";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        return result;
    }

    public Disziplin findById(int id) throws SQLException {
        String sql = "SELECT * FROM disziplin WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public void insert(Disziplin disziplin) throws SQLException {
        String sql = "INSERT INTO disziplin (lage, distanz) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, disziplin.getLage());
            ps.setInt(2, disziplin.getDistanz());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    disziplin.setId(keys.getInt(1));
                }
            }
        }
    }

    public void update(Disziplin disziplin) throws SQLException {
        String sql = "UPDATE disziplin SET lage = ?, distanz = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, disziplin.getLage());
            ps.setInt(2, disziplin.getDistanz());
            ps.setInt(3, disziplin.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM disziplin WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Disziplin mapRow(ResultSet rs) throws SQLException {
        return new Disziplin(rs.getInt("id"), rs.getString("lage"), rs.getInt("distanz"));
    }
}
