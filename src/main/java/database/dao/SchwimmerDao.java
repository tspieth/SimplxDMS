package database.dao;

import database.model.Schwimmer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SchwimmerDao {

    private final Connection connection;

    public SchwimmerDao(Connection connection) {
        this.connection = connection;
    }

    public List<Schwimmer> findAll() throws SQLException {
        List<Schwimmer> result = new ArrayList<>();
        String sql = "SELECT * FROM schwimmer ORDER BY name, vorname";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        return result;
    }

    public List<Schwimmer> findByVerein(int vereinId) throws SQLException {
        List<Schwimmer> result = new ArrayList<>();
        String sql = "SELECT * FROM schwimmer WHERE verein_id = ? ORDER BY name, vorname";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, vereinId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    /**
     * Loads the swimmers of one club filtered by gender and sorted by last and first name.
     *
     * @param vereinId selected club ID
     * @param geschlecht gender value stored as {@code m} or {@code w}
     * @return matching swimmers
     * @throws SQLException if the query fails
     */
    public List<Schwimmer> findByVereinUndGeschlecht(int vereinId, String geschlecht) throws SQLException {
        List<Schwimmer> result = new ArrayList<>();
        String sql = "SELECT * FROM schwimmer WHERE verein_id = ? AND geschlecht = ? ORDER BY name, vorname";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, vereinId);
            ps.setString(2, geschlecht);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    /**
     * Loads club swimmers who have no start (and therefore no team assignment) in any competition.
     */
    public List<Schwimmer> findUnassignedByVereinUndGeschlecht(int vereinId, String geschlecht)
            throws SQLException {
        List<Schwimmer> result = new ArrayList<>();
        String sql = "SELECT sw.* FROM schwimmer sw "
                + "WHERE sw.verein_id = ? AND sw.geschlecht = ? "
                + "AND NOT EXISTS (SELECT 1 FROM start st WHERE st.schwimmer_id = sw.id) "
                + "ORDER BY sw.name, sw.vorname";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, vereinId);
            ps.setString(2, geschlecht);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    /**
     * Returns swimmers from the supplied IDs that gained a team assignment after candidate loading.
     */
    public List<Integer> findAssignedIds(List<Integer> swimmerIds) throws SQLException {
        if (swimmerIds.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(swimmerIds.size(), "?"));
        String sql = "SELECT DISTINCT schwimmer_id FROM start WHERE schwimmer_id IN (" + placeholders + ")";
        List<Integer> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < swimmerIds.size(); i++) {
                ps.setInt(i + 1, swimmerIds.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getInt(1));
                }
            }
        }
        return result;
    }

    public List<Schwimmer> findByGeschlecht(String geschlecht) throws SQLException {
        List<Schwimmer> result = new ArrayList<>();
        String sql = "SELECT * FROM schwimmer WHERE geschlecht = ? ORDER BY name, vorname";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, geschlecht);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    public Schwimmer findById(int id) throws SQLException {
        String sql = "SELECT * FROM schwimmer WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public void insert(Schwimmer schwimmer) throws SQLException {
        String sql = "INSERT INTO schwimmer (name, vorname, geburtsdatum, geschlecht, verein_id) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, schwimmer.getName());
            ps.setString(2, schwimmer.getVorname());
            ps.setDate(3, schwimmer.getGeburtsdatum() != null ? Date.valueOf(schwimmer.getGeburtsdatum()) : null);
            ps.setString(4, schwimmer.getGeschlecht());
            ps.setInt(5, schwimmer.getVereinId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    schwimmer.setId(keys.getInt(1));
                }
            }
        }
    }

    public void update(Schwimmer schwimmer) throws SQLException {
        String sql = "UPDATE schwimmer SET name = ?, vorname = ?, geburtsdatum = ?, geschlecht = ?, verein_id = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, schwimmer.getName());
            ps.setString(2, schwimmer.getVorname());
            ps.setDate(3, schwimmer.getGeburtsdatum() != null ? Date.valueOf(schwimmer.getGeburtsdatum()) : null);
            ps.setString(4, schwimmer.getGeschlecht());
            ps.setInt(5, schwimmer.getVereinId());
            ps.setInt(6, schwimmer.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Counts the team assignments that will be removed together with the swimmer.
     *
     * @param id swimmer database ID
     * @return number of competition starts linked to this swimmer
     * @throws SQLException if the query fails
     */
    public int countTeamAssignments(int id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM start WHERE schwimmer_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /**
     * Removes the swimmer, all known best times, and all competition-team assignments atomically.
     *
     * @param id swimmer database ID
     * @throws SQLException if any dependent data cannot be removed
     */
    public void delete(int id) throws SQLException {
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM start WHERE schwimmer_id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM leistung WHERE schwimmer_id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM schwimmer WHERE id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

    private Schwimmer mapRow(ResultSet rs) throws SQLException {
        Date geburtsdatum = rs.getDate("geburtsdatum");
        return new Schwimmer(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("vorname"),
                geburtsdatum != null ? geburtsdatum.toLocalDate() : null,
                rs.getString("geschlecht"),
                rs.getInt("verein_id")
        );
    }
}
