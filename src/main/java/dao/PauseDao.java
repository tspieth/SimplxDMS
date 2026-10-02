package dao;

import model.Pause;

import java.sql.*;

public class PauseDao {

    private final Connection connection;

    public PauseDao(Connection connection) {
        this.connection = connection;
    }

    public Pause findByProgrammpunkt(int programmpunktId) throws SQLException {
        String sql = "SELECT * FROM pause WHERE programmpunkt_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, programmpunktId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public void insert(Pause pause) throws SQLException {
        String sql = "INSERT INTO pause (programmpunkt_id, dauer) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, pause.getProgrammpunktId());
            ps.setInt(2, pause.getDauerSekunden());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    pause.setId(keys.getInt(1));
                }
            }
        }
    }

    private Pause mapRow(ResultSet rs) throws SQLException {
        return new Pause(rs.getInt("id"), rs.getInt("programmpunkt_id"), rs.getInt("dauer"));
    }
}
