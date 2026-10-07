package database.dao;

import database.model.Lauf;

import java.sql.*;

public class LaufDao {

    private final Connection connection;

    public LaufDao(Connection connection) {
        this.connection = connection;
    }

    public Lauf findByProgrammpunkt(int programmpunktId) throws SQLException {
        String sql = "SELECT * FROM lauf WHERE programmpunkt_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, programmpunktId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public void insert(Lauf lauf) throws SQLException {
        String sql = "INSERT INTO lauf (programmpunkt_id, disziplin_id) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, lauf.getProgrammpunktId());
            ps.setInt(2, lauf.getDisziplinId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    lauf.setId(keys.getInt(1));
                }
            }
        }
    }

    private Lauf mapRow(ResultSet rs) throws SQLException {
        return new Lauf(rs.getInt("id"), rs.getInt("programmpunkt_id"), rs.getInt("disziplin_id"));
    }
}
