package service;

import database.dao.LaufDao;
import database.dao.LeistungDao;
import database.dao.SchwimmerDao;
import database.dao.StartDao;
import database.model.Geschlecht;
import database.model.Mannschaft;
import database.model.Schwimmer;
import database.model.Wettkampf;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Builds a paired, live-calculated team CSV export without modifying stored results. */
public class MannschaftExportService {
    private final LaufDao laufDao;
    private final StartDao startDao;
    private final SchwimmerDao schwimmerDao;
    private final LeistungDao leistungDao;
    private final PunkteRechner punkteRechner;

    public MannschaftExportService(Connection connection) {
        laufDao = new LaufDao(connection);
        startDao = new StartDao(connection);
        schwimmerDao = new SchwimmerDao(connection);
        leistungDao = new LeistungDao(connection);
        punkteRechner = new PunkteRechner(connection);
    }

    public List<List<String>> ladeZeilen(Mannschaft mannschaft, Wettkampf wettkampf, Geschlecht geschlecht)
            throws SQLException {
        List<LaufDao.LaufMitDisziplin> firstSection = new ArrayList<>();
        List<LaufDao.LaufMitDisziplin> secondSection = new ArrayList<>();
        List<LaufDao.LaufMitDisziplin> runs = laufDao.findByWettkampf(wettkampf.getId()).stream()
                .filter(run -> run.geschlecht() == geschlecht)
                .sorted(Comparator.comparingInt(LaufDao.LaufMitDisziplin::wettkampfNummer))
                .toList();
        for (LaufDao.LaufMitDisziplin run : runs) {
            if (run.abschnitt() == 1) {
                firstSection.add(run);
            } else if (run.abschnitt() == 2) {
                secondSection.add(run);
            }
        }

        List<List<String>> rows = new ArrayList<>();
        long firstPoints = 0;
        long secondPoints = 0;
        boolean hasAssignment = false;
        rows.add(MannschaftCsvFormat.HEADER);
        int rowCount = Math.max(firstSection.size(), secondSection.size());
        for (int index = 0; index < rowCount; index++) {
            List<String> row = new ArrayList<>(8);
            if (index < firstSection.size()) {
                ExportGruppe group = exportGruppe(firstSection.get(index), mannschaft);
                row.addAll(group.values());
                firstPoints += group.points();
                hasAssignment |= group.assigned();
            } else {
                row.addAll(List.of("", "", "", ""));
            }
            if (index < secondSection.size()) {
                ExportGruppe group = exportGruppe(secondSection.get(index), mannschaft);
                row.addAll(group.values());
                secondPoints += group.points();
                hasAssignment |= group.assigned();
            } else {
                row.addAll(List.of("", "", "", ""));
            }
            rows.add(List.copyOf(row));
        }
        if (!hasAssignment) {
            return List.of();
        }

        rows.add(List.of("Summe", "", "", Long.toString(firstPoints), "", "", "",
                Long.toString(secondPoints)));
        rows.add(List.of("Gesamt", "", "", "", "", "", "",
                Long.toString(firstPoints + secondPoints)));
        return List.copyOf(rows);
    }

    private ExportGruppe exportGruppe(LaufDao.LaufMitDisziplin run, Mannschaft mannschaft) throws SQLException {
        List<StartDao.StartAnzeige> assignments = startDao.findAnzeigen(run.id(), mannschaft.getId());
        String discipline = WettkampfDisziplinText.format(run.distanz(), run.lage());
        if (assignments.isEmpty()) {
            return new ExportGruppe(List.of(discipline, "", "", ""), 0, false);
        }

        StartDao.StartAnzeige assignment = assignments.get(0);
        Schwimmer swimmer = schwimmerDao.findById(assignment.schwimmerId());
        if (swimmer == null) {
            throw new SQLException("Der zugeordnete Schwimmer (ID " + assignment.schwimmerId() + ") fehlt.");
        }
        LocalTime bestTime = leistungDao.findBestTime(swimmer.getId(), run.disziplinId());
        long points = punkteRechner.berechne(swimmer.getId(), run.disziplinId(), run.geschlecht(),
                run.distanz(), run.lage()).punkte();
        return new ExportGruppe(List.of(discipline, assignment.vorname() + " " + assignment.name(),
                formatHundredths(bestTime), Long.toString(points)), points, true);
    }

    static String formatHundredths(LocalTime time) {
        if (time == null) {
            return "";
        }
        double secondsValue = time.toSecondOfDay() + time.getNano() / 1_000_000_000.0;
        long hundredths = Math.round(secondsValue * 100);
        long minutes = hundredths / 6000;
        long seconds = hundredths / 100 % 60;
        long centiseconds = hundredths % 100;
        return String.format(Locale.ROOT, "%d:%02d.%02d", minutes, seconds, centiseconds);
    }

    private record ExportGruppe(List<String> values, long points, boolean assigned) {
    }
}
