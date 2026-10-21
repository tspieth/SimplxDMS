package service;

import database.dao.FinaReferenceTimeDAO;
import database.dao.LeistungDao;
import database.model.Geschlecht;
import database.model.SchwimmLage;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.OptionalDouble;

/**
 * Calculates DMS points from the swimmer's best time and the FINA reference time.
 */
public class PunkteRechner {
    private final LeistungDao leistungDao;
    private final FinaReferenceTimeDAO finaDao;

    public PunkteRechner(Connection connection) {
        this.leistungDao = new LeistungDao(connection);
        this.finaDao = new FinaReferenceTimeDAO(connection);
    }

    public PunktErgebnis berechne(int schwimmerId, int disziplinId, Geschlecht geschlecht,
                                  int distanz, SchwimmLage lage) throws SQLException {
        OptionalDouble leistung = leistungDao.findBestTimeSeconds(schwimmerId, disziplinId);
        if (leistung.isEmpty() || leistung.getAsDouble() <= 0) {
            return new PunktErgebnis(0, "Keine Leistung für " + distanz + " m " + lage.getAnzeigename());
        }
        OptionalDouble fina = finaDao.findTimeSeconds(disziplinId, geschlecht);
        if (fina.isEmpty() || fina.getAsDouble() <= 0) {
            return new PunktErgebnis(0, "Keine FINA-Zeit hinterlegt");
        }
        int punkte = (int) (Math.pow(fina.getAsDouble() / leistung.getAsDouble(), 3) * 1000);
        return new PunktErgebnis(punkte, null);
    }

    public record PunktErgebnis(long punkte, String hinweis) {
    }
}
