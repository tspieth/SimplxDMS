package service;

import database.dao.LaufDao;
import database.dao.StartDao;
import database.model.Geschlecht;
import database.model.Mannschaft;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds read-only competition grid data and live-calculated team points.
 */
public class WettkampfAnsichtService {
    private final LaufDao laufDao;
    private final StartDao startDao;
    private final PunkteRechner punkteRechner;

    public WettkampfAnsichtService(Connection connection) {
        laufDao = new LaufDao(connection);
        startDao = new StartDao(connection);
        punkteRechner = new PunkteRechner(connection);
    }

    public Ansicht lade(int wettkampfId, Mannschaft mannschaft, Geschlecht geschlecht) throws SQLException {
        var allRuns = laufDao.findByWettkampf(wettkampfId);
        List<LaufZeile> rows = new ArrayList<>();
        long teamPoints = 0;
        for (var run : allRuns) {
            if (run.geschlecht() == null) {
                continue;
            }
            List<StartDao.StartAnzeige> starts = mannschaft == null
                    ? List.of() : startDao.findAnzeigen(run.id(), mannschaft.getId());
            PunkteRechner.PunktErgebnis visiblePoints = null;
            for (StartDao.StartAnzeige start : starts) {
                PunkteRechner.PunktErgebnis points = punkteRechner.berechne(start.schwimmerId(),
                        run.disziplinId(), run.geschlecht(), run.distanz(), run.lage());
                teamPoints += points.punkte();
                if (visiblePoints == null) {
                    visiblePoints = points;
                }
            }
            if (run.geschlecht() != geschlecht) {
                continue;
            }
            String assignment = "–";
            String tooltip = null;
            if (!starts.isEmpty()) {
                StartDao.StartAnzeige start = starts.get(0);
                assignment = start.vorname() + " " + start.name() + " · " + visiblePoints.punkte();
                tooltip = visiblePoints.hinweis();
            }
            rows.add(new LaufZeile(run.abschnitt(), run.wettkampfNummer(),
                    run.distanz(), run.lage().getAnzeigename(), assignment, tooltip));
        }
        List<Integer> sections = rows.stream().map(LaufZeile::abschnitt).distinct().sorted().toList();
        return new Ansicht(sections, rows, mannschaft == null ? null : teamPoints);
    }

    public record LaufZeile(int abschnitt, int wettkampfNummer, int distanz, String lage,
                            String zuordnung, String tooltip) {
    }

    public record Ansicht(List<Integer> abschnitte, List<LaufZeile> laeufe, Long mannschaftspunkte) {
    }
}
