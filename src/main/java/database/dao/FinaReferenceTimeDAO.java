package database.dao;

import database.model.Disziplin;
import database.model.Geschlecht;
import database.model.SchwimmLage;

import java.sql.*;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;

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

    /**
     * Loads all stored reference times grouped by discipline ID and gender.
     *
     * @return map of discipline IDs to gender/time-in-seconds values
     * @throws SQLException if the database query fails
     */
    public Map<Integer, Map<Geschlecht, Double>> findAll() throws SQLException {
        Map<Integer, Map<Geschlecht, Double>> result = new HashMap<>();
        String sql = "SELECT disziplin_id, geschlecht, fina_time FROM fina_referenzzeit";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int disziplinId = rs.getInt("disziplin_id");
                Geschlecht geschlecht;
                try {
                    geschlecht = Geschlecht.valueOf(rs.getString("geschlecht"));
                } catch (IllegalArgumentException | NullPointerException e) {
                    throw new SQLException("Ungültiges Geschlecht in fina_referenzzeit für Disziplin "
                            + disziplinId, e);
                }
                result.computeIfAbsent(disziplinId, ignored -> new EnumMap<>(Geschlecht.class))
                        .put(geschlecht, rs.getDouble("fina_time"));
            }
        }
        return result;
    }

    public OptionalDouble findTimeSeconds(int disziplinId, Geschlecht geschlecht) throws SQLException {
        String sql = "SELECT fina_time FROM fina_referenzzeit WHERE disziplin_id = ? AND geschlecht = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, disziplinId);
            ps.setString(2, geschlecht.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? OptionalDouble.of(rs.getDouble(1)) : OptionalDouble.empty();
            }
        }
    }

    /**
     * Inserts or updates a reference time for a discipline and gender.
     *
     * @param disziplinId discipline database ID
     * @param geschlecht gender whose time is stored
     * @param sekunden reference time in seconds
     * @throws SQLException if the merge fails
     */
    public void upsert(int disziplinId, Geschlecht geschlecht, double sekunden) throws SQLException {
        String sql = "MERGE INTO fina_referenzzeit (disziplin_id, geschlecht, fina_time) "
                + "KEY (disziplin_id, geschlecht) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, disziplinId);
            ps.setString(2, geschlecht.name());
            ps.setDouble(3, sekunden);
            ps.executeUpdate();
        }
    }

    public void deleteAll() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM fina_referenzzeit");
        }
    }

    /**
     * Updates an existing FINA reference time for backwards compatibility with import callers.
     *
     * @param disziplinId discipline database ID
     * @param gender gender value
     * @param seconds reference time in seconds
     * @throws SQLException if the merge fails
     */
    public void updateRefTime(int disziplinId, Geschlecht gender, double seconds) throws SQLException {
        upsert(disziplinId, gender, seconds);
    }

    /**
     * Updates the time through the older discipline-based API, now using an upsert.
     *
     * @param disziplin discipline whose reference time is stored
     * @param gender gender value
     * @param seconds reference time in seconds
     * @throws SQLException if the merge fails
     */
    public void updateRefTime(Disziplin disziplin, Geschlecht gender, double seconds) throws SQLException {
        upsert(disziplin.getId(), gender, seconds);
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

}
