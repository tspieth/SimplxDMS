package database.dao;

import database.model.Programmpunkt;
import database.model.ProgrammpunktTyp;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProgrammpunktDao {

    private final Connection connection;

    public ProgrammpunktDao(Connection connection) {
        this.connection = connection;
    }

    /** Alle Programmpunkte eines Wettkampfs, in der korrekten Reihenfolge (Laeufe UND Pausen gemeinsam). */
    public List<Programmpunkt> findByWettkampf(int wettkampfId) throws SQLException {
        List<Programmpunkt> result = new ArrayList<>();
        String sql = "SELECT * FROM programmpunkt WHERE wettkampf_id = ? ORDER BY position";
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

    public void insert(Programmpunkt punkt) throws SQLException {
        String sql = "INSERT INTO programmpunkt (wettkampf_id, position, typ) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, punkt.getWettkampfId());
            ps.setInt(2, punkt.getPosition());
            ps.setString(3, punkt.getTyp().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    punkt.setId(keys.getInt(1));
                }
            }
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM programmpunkt WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Programmpunkt mapRow(ResultSet rs) throws SQLException {
        return new Programmpunkt(
                rs.getInt("id"),
                rs.getInt("wettkampf_id"),
                rs.getInt("position"),
                ProgrammpunktTyp.valueOf(rs.getString("typ"))
        );
    }
}
