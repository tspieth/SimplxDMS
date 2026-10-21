package database.dao;

import database.model.Leistung;
import database.model.Schwimmer;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Coordinates swimmer and best-time writes so each edit is persisted atomically.
 */
public class SchwimmerService {
    private final Connection connection;

    /**
     * Creates a service that uses the supplied shared database connection.
     *
     * @param connection application database connection
     */
    public SchwimmerService(Connection connection) {
        this.connection = connection;
    }

    /**
     * Saves swimmer fields and inserts or updates one best-time record per discipline atomically.
     * Additional historical results are retained and are not touched by this editor.
     *
     * @param swimmer swimmer data to insert or update
     * @param bestzeiten new results or the currently fastest results being edited
     * @throws SQLException if any database operation fails; all changes are rolled back
     */
    public void save(Schwimmer swimmer, List<Leistung> bestzeiten) throws SQLException {
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            SchwimmerDao schwimmerDao = new SchwimmerDao(connection);
            LeistungDao leistungDao = new LeistungDao(connection);
            if (swimmer.getId() == 0) {
                schwimmerDao.insert(swimmer);
            } else {
                schwimmerDao.update(swimmer);
            }
            for (Leistung leistung : bestzeiten) {
                leistung.setSchwimmerId(swimmer.getId());
                if (leistung.getId() == 0) {
                    leistungDao.insert(leistung);
                } else {
                    leistungDao.update(leistung);
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
}
