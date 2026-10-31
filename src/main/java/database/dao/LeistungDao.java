package database.dao;

import database.model.Leistung;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

public class LeistungDao {

    private final Connection connection;

    public LeistungDao(Connection connection) {
        this.connection = connection;
    }

    public List<Leistung> findBySchwimmer(int schwimmerId) throws SQLException {
        List<Leistung> result = new ArrayList<>();
        String sql = "SELECT * FROM leistung WHERE schwimmer_id = ? ORDER BY datum DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, schwimmerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    public List<Leistung> findBySchwimmerUndDisziplin(int schwimmerId, int disziplinId) throws SQLException {
        List<Leistung> result = new ArrayList<>();
        String sql = "SELECT * FROM leistung WHERE schwimmer_id = ? AND disziplin_id = ? ORDER BY zeit ASC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, schwimmerId);
            ps.setInt(2, disziplinId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    public java.time.LocalTime findBestTime(int schwimmerId, int disziplinId) throws SQLException {
        String sql = "SELECT MIN(zeit) AS bestzeit FROM leistung WHERE schwimmer_id = ? AND disziplin_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, schwimmerId);
            ps.setInt(2, disziplinId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return rs.getObject("bestzeit", java.time.LocalTime.class);
            }
        }
    }

    public OptionalDouble findBestTimeSeconds(int schwimmerId, int disziplinId) throws SQLException {
        String sql = "SELECT MIN(zeit) AS bestzeit FROM leistung WHERE schwimmer_id = ? AND disziplin_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, schwimmerId);
            ps.setInt(2, disziplinId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return OptionalDouble.empty();
                }
                java.time.LocalTime time = rs.getObject("bestzeit", java.time.LocalTime.class);
                return time == null ? OptionalDouble.empty()
                        : OptionalDouble.of(time.toSecondOfDay() + time.getNano() / 1_000_000_000.0);
            }
        }
    }

    /**
     * Loads the fastest time for each requested swimmer and discipline in batches.
     *
     * @param swimmerIds swimmer IDs to include
     * @param disciplineIds discipline IDs to include
     * @return best times grouped by swimmer ID and discipline ID, in seconds
     * @throws SQLException if the query fails
     */
    public Map<Integer, Map<Integer, Double>> findBestTimesSeconds(List<Integer> swimmerIds,
                                                                    List<Integer> disciplineIds)
            throws SQLException {
        Map<Integer, Map<Integer, Double>> result = new HashMap<>();
        if (swimmerIds.isEmpty() || disciplineIds.isEmpty()) {
            return result;
        }

        int swimmerBatchSize = 500;
        for (int start = 0; start < swimmerIds.size(); start += swimmerBatchSize) {
            List<Integer> swimmerBatch = swimmerIds.subList(start,
                    Math.min(start + swimmerBatchSize, swimmerIds.size()));
            String swimmerParameters = String.join(",", java.util.Collections.nCopies(swimmerBatch.size(), "?"));
            String disciplineParameters = String.join(",", java.util.Collections.nCopies(disciplineIds.size(), "?"));
            String sql = "SELECT schwimmer_id, disziplin_id, MIN(zeit) AS bestzeit FROM leistung "
                    + "WHERE schwimmer_id IN (" + swimmerParameters + ") AND disziplin_id IN ("
                    + disciplineParameters + ") GROUP BY schwimmer_id, disziplin_id";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                int parameterIndex = 1;
                for (int swimmerId : swimmerBatch) {
                    ps.setInt(parameterIndex++, swimmerId);
                }
                for (int disciplineId : disciplineIds) {
                    ps.setInt(parameterIndex++, disciplineId);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        java.time.LocalTime time = rs.getObject("bestzeit", java.time.LocalTime.class);
                        if (time != null) {
                            result.computeIfAbsent(rs.getInt("schwimmer_id"), ignored -> new HashMap<>())
                                    .put(rs.getInt("disziplin_id"),
                                            time.toSecondOfDay() + time.getNano() / 1_000_000_000.0);
                        }
                    }
                }
            }
        }
        return result;
    }

    public void insert(Leistung leistung) throws SQLException {
        String sql = "INSERT INTO leistung (schwimmer_id, disziplin_id, zeit, datum) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, leistung.getSchwimmerId());
            ps.setInt(2, leistung.getDisziplinId());
            ps.setObject(3, leistung.getZeit());
            ps.setDate(4, leistung.getDatum() != null ? Date.valueOf(leistung.getDatum()) : null);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    leistung.setId(keys.getInt(1));
                }
            }
        }
    }

    /**
     * Updates the discipline, time, and date of an existing best time.
     *
     * @param leistung updated best-time data
     * @throws SQLException if the update fails
     */
    public void update(Leistung leistung) throws SQLException {
        String sql = "UPDATE leistung SET disziplin_id = ?, zeit = ?, datum = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, leistung.getDisziplinId());
            ps.setObject(2, leistung.getZeit());
            ps.setDate(3, leistung.getDatum() != null ? Date.valueOf(leistung.getDatum()) : null);
            ps.setInt(4, leistung.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Deletes every best time belonging to a swimmer.
     *
     * @param schwimmerId swimmer database ID
     * @throws SQLException if the delete fails
     */
    public void deleteBySchwimmer(int schwimmerId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM leistung WHERE schwimmer_id = ?")) {
            ps.setInt(1, schwimmerId);
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM leistung WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Leistung mapRow(ResultSet rs) throws SQLException {
        Date datum = rs.getDate("datum");
        return new Leistung(
                rs.getInt("id"),
                rs.getInt("schwimmer_id"),
                rs.getInt("disziplin_id"),
                rs.getObject("zeit", java.time.LocalTime.class),
                datum != null ? datum.toLocalDate() : null
        );
    }
}
