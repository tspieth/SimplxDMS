package database.dao;

import database.model.Lauf;
import database.model.Geschlecht;
import database.model.SchwimmLage;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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

    public List<LaufMitDisziplin> findByWettkampf(int wettkampfId) throws SQLException {
        List<LaufMitDisziplin> result = new ArrayList<>();
        String sql = "SELECT l.id, l.programmpunkt_id, l.disziplin_id, l.geschlecht, "
                + "l.wettkampf_nr, l.abschnitt, d.lage, d.distanz "
                + "FROM lauf l JOIN programmpunkt p ON p.id = l.programmpunkt_id "
                + "JOIN disziplin d ON d.id = l.disziplin_id "
                + "WHERE p.wettkampf_id = ? ORDER BY l.abschnitt, l.wettkampf_nr";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, wettkampfId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new LaufMitDisziplin(
                            rs.getInt("id"),
                            rs.getInt("disziplin_id"),
                            rs.getInt("wettkampf_nr"),
                            rs.getInt("abschnitt"),
                            readGender(rs),
                            SchwimmLage.fromDbValue(rs.getString("lage")),
                            rs.getInt("distanz")));
                }
            }
        }
        return result;
    }

    public void insert(Lauf lauf) throws SQLException {
        String sql = "INSERT INTO lauf (programmpunkt_id, disziplin_id, geschlecht, wettkampf_nr, abschnitt) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, lauf.getProgrammpunktId());
            ps.setInt(2, lauf.getDisziplinId());
            ps.setString(3, lauf.getGeschlecht().name());
            ps.setInt(4, lauf.getWettkampfNummer());
            ps.setInt(5, lauf.getAbschnitt());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    lauf.setId(keys.getInt(1));
                }
            }
        }
    }

    private Lauf mapRow(ResultSet rs) throws SQLException {
        return new Lauf(rs.getInt("id"), rs.getInt("programmpunkt_id"), rs.getInt("disziplin_id"),
                readGender(rs), rs.getInt("wettkampf_nr"), rs.getInt("abschnitt"));
    }

    private Geschlecht readGender(ResultSet rs) throws SQLException {
        String gender = rs.getString("geschlecht");
        return gender == null ? null : Geschlecht.valueOf(gender);
    }

    public record LaufMitDisziplin(int id, int disziplinId, int wettkampfNummer, int abschnitt,
                                   Geschlecht geschlecht, SchwimmLage lage, int distanz) {
    }
}
