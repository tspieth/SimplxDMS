package database.dao;

import database.model.Disziplin;
import database.model.FinaReferenceTime;
import database.model.Geschlecht;
import database.model.SchwimmLage;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * FinaReferenceTimeDAO
 * is a Data Access Object to access the World Record Times used
 * to calc Points.
 */
public class FinaReferenceTimeDAO {

    private final Connection connection;

    public FinaReferenceTimeDAO(Connection connection){
        this.connection = connection;
    }

    public List<FinaReferenceTime> findAll() throws SQLException {
        List<FinaReferenceTime> result = new ArrayList<>();
        String sql = "SELECT * FROM fina_referenzzeit ORDER BY disziplin_id";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        return result;
    }

    /**
     * Updates the FINA Reference time for a Given Disziplin and Gender
     *
     * @param disziplin
     * @param gender
     * @param time
     * @throws SQLException
     */
    public void updateRefTime(Disziplin disziplin, Geschlecht gender, double time) throws SQLException {
        String sql = "UPDATE fina_referenzzeit SET fina_time = ? WHERE disziplin_id = ? AND geschlecht = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)){
            ps.setDouble(1, time);
            ps.setInt(2, disziplin.getId());
            ps.setString(3, gender.name());
            ps.executeUpdate();
        }
    }

    /**
     * Method used to save fina times from csv file
     *
     * @param lage
     * @param distanz
     * @param geschlecht
     * @param time
     * @throws SQLException
     */
    public void insertRefTime(SchwimmLage lage, int distanz, Geschlecht geschlecht, double time) throws SQLException {
        String sql = "INSERT INTO fina_referenzzeit (disziplin_id, geschlecht, fina_time) " +
                "SELECT id, ?, ? FROM disziplin WHERE lage = ? AND distanz = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            // Parameter für das SELECT-Konstrukt
            ps.setString(1, geschlecht.name()); // z. B. "MAENNLICH"
            ps.setDouble(2, time);              // z. B. 46.86
            ps.setString(3, lage.name());       // z. B. "FREISTIL"
            ps.setInt(4, distanz);              // z. B. 100

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("Keine passende Disziplin für " + lage + " " + distanz + "m gefunden!");
            }
        }
    }

    /**
     * Maps a ResultSet to an according FinaReferenceTime Object
     *
     * @param rs
     * @return
     * @throws SQLException
     */
    private FinaReferenceTime mapRow(ResultSet rs) throws SQLException{
        return new FinaReferenceTime(rs.getInt("id"),
                rs.getInt("disziplin_id"),
                rs.getString("geschlecht"),
                rs.getInt("fina_time"));
    }
}
