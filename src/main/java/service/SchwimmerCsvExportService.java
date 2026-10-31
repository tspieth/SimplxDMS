package service;

import database.dao.DisziplinDao;
import database.dao.LeistungDao;
import database.dao.SchwimmerDao;
import database.model.Disziplin;
import database.model.Leistung;
import database.model.Schwimmer;
import database.model.Verein;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Builds the import-compatible CSV rows for all swimmers in a club. */
public class SchwimmerCsvExportService {
    private final SchwimmerDao schwimmerDao;
    private final DisziplinDao disziplinDao;
    private final LeistungDao leistungDao;

    public SchwimmerCsvExportService(Connection connection) {
        schwimmerDao = new SchwimmerDao(connection);
        disziplinDao = new DisziplinDao(connection);
        leistungDao = new LeistungDao(connection);
    }

    public List<List<String>> ladeZeilen(Verein verein) throws SQLException {
        List<Schwimmer> swimmers = schwimmerDao.findByVerein(verein.getId());
        Map<Integer, Disziplin> disciplines = new HashMap<>();
        for (Disziplin discipline : disziplinDao.findAll()) {
            disciplines.put(discipline.getId(), discipline);
        }

        List<List<String>> rows = new ArrayList<>();
        rows.add(SchwimmerCsvFormat.HEADER);
        for (Schwimmer swimmer : swimmers) {
            rows.add(List.of(swimmer.getName(), swimmer.getVorname(), swimmer.getGeschlecht(),
                    SchwimmerCsvFormat.formatBirthDate(swimmer.getGeburtsdatum()), "", ""));

            Map<Integer, Leistung> bestByDiscipline = new HashMap<>();
            for (Leistung performance : leistungDao.findBySchwimmer(swimmer.getId())) {
                bestByDiscipline.merge(performance.getDisziplinId(), performance,
                        (first, second) -> first.getZeit().compareTo(second.getZeit()) <= 0 ? first : second);
            }
            List<Leistung> performances = bestByDiscipline.values().stream()
                    .sorted(Comparator.comparing((Leistung performance) -> {
                        Disziplin discipline = disciplines.get(performance.getDisziplinId());
                        return discipline == null ? "" : discipline.getLage().name();
                    }).thenComparingInt(performance -> {
                        Disziplin discipline = disciplines.get(performance.getDisziplinId());
                        return discipline == null ? 0 : discipline.getDistanz();
                    })).toList();
            for (Leistung performance : performances) {
                Disziplin discipline = disciplines.get(performance.getDisziplinId());
                if (discipline == null) {
                    throw new SQLException("Zu einer Bestzeit fehlt die Disziplin (ID "
                            + performance.getDisziplinId() + ").");
                }
                String event = discipline.getDistanz() + SchwimmerCsvFormat.strokeCode(discipline.getLage());
                rows.add(List.of("", "", "", "", event,
                        SchwimmerCsvFormat.formatTime(performance.getZeit())));
            }
        }
        return rows;
    }
}
