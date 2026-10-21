package database.dao;

import database.model.Verein;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VereinDao {

    private final Connection connection;

    public VereinDao(Connection connection) {
        this.connection = connection;
    }

    public List<Verein> findAll() throws SQLException {
        List<Verein> result = new ArrayList<>();
        String sql = "SELECT * FROM verein ORDER BY name";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        return result;
    }

    public Verein findById(int id) throws SQLException {
        String sql = "SELECT * FROM verein WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /**
     * Adds a club and stores its generated database ID in the supplied model.
     *
     * @param verein club to insert
     * @throws SQLException if the insert fails
     */
    public void insert(Verein verein) throws SQLException {
        String sql = "INSERT INTO verein (name, farbe) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, verein.getName());
            ps.setString(2, verein.getFarbe());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    verein.setId(keys.getInt(1));
                }
            }
        }
    }

    public void update(Verein verein) throws SQLException {
        String sql = "UPDATE verein SET name = ?, farbe = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, verein.getName());
            ps.setString(2, verein.getFarbe());
            ps.setInt(3, verein.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Deletes a club and its dependent teams, starts, swimmers, and best times atomically.
     * Competitions themselves are retained because they are not owned by a single club.
     *
     * @param id club database ID
     * @throws SQLException if any dependent data cannot be removed
     */
    public void delete(int id) throws SQLException {
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            String[] statements = {
                    "DELETE FROM start WHERE mannschaft_id IN (SELECT id FROM mannschaft WHERE verein_id = ?)",
                    "DELETE FROM start WHERE schwimmer_id IN (SELECT id FROM schwimmer WHERE verein_id = ?)",
                    "DELETE FROM leistung WHERE schwimmer_id IN (SELECT id FROM schwimmer WHERE verein_id = ?)",
                    "DELETE FROM mannschaft WHERE verein_id = ?",
                    "DELETE FROM schwimmer WHERE verein_id = ?",
                    "DELETE FROM verein WHERE id = ?"
            };
            for (String sql : statements) {
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
            }
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

    private Verein mapRow(ResultSet rs) throws SQLException {
        return new Verein(rs.getInt("id"), rs.getString("name"), rs.getString("farbe"));
    }
}
