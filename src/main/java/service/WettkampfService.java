package service;

import database.dao.MannschaftDao;
import database.dao.WettkampfDao;
import database.model.Mannschaft;
import database.model.Wettkampf;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Read and transactional deletion operations for competitions and teams.
 */
public class WettkampfService {
    private final Connection connection;
    private final WettkampfDao wettkampfDao;
    private final MannschaftDao mannschaftDao;

    public WettkampfService(Connection connection) {
        this.connection = connection;
        wettkampfDao = new WettkampfDao(connection);
        mannschaftDao = new MannschaftDao(connection);
    }

    public List<Wettkampf> ladeWettkaempfe() throws SQLException {
        return wettkampfDao.findAll();
    }

    public List<Mannschaft> ladeMannschaften(int wettkampfId, int vereinId) throws SQLException {
        return mannschaftDao.findByWettkampfUndVerein(wettkampfId, vereinId);
    }

    public long anzahlStarts(Mannschaft mannschaft) throws SQLException {
        return mannschaftDao.countStarts(mannschaft.getId());
    }

    public void loescheMannschaft(Mannschaft mannschaft) throws SQLException {
        inTransaction(() -> mannschaftDao.delete(mannschaft.getId()));
    }

    public LoeschInfo loeschInfo(Wettkampf wettkampf) throws SQLException {
        return new LoeschInfo(wettkampfDao.countLaeufe(wettkampf.getId()),
                wettkampfDao.countMannschaften(wettkampf.getId()));
    }

    public void loescheWettkampf(Wettkampf wettkampf) throws SQLException {
        inTransaction(() -> wettkampfDao.delete(wettkampf.getId()));
    }

    private void inTransaction(SqlOperation operation) throws SQLException {
        boolean autoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(false);
            operation.run();
            connection.commit();
        } catch (SQLException | RuntimeException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackError) {
                e.addSuppressed(rollbackError);
            }
            throw e;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

    @FunctionalInterface
    private interface SqlOperation {
        void run() throws SQLException;
    }

    public record LoeschInfo(long laeufe, long mannschaften) {
    }
}
