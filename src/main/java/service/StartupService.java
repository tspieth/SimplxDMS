package service;

import database.dao.DisziplinDao;
import database.dao.FinaReferenceTimeDAO;
import database.dao.WettkampfDao;
import database.model.Geschlecht;
import database.model.SchwimmLage;
import importer.DsvParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Imports the bundled sample competition only when the database has none.
 */
public class StartupService {
    private final Connection connection;
    private final WettkampfImportService importService;

    public StartupService(Connection connection) {
        this.connection = connection;
        importService = new WettkampfImportService(connection);
    }

    public int importBundledReferenceTimesIfMissing() throws IOException, SQLException {
        int imported = 0;
        FinaReferenceTimeDAO referenceTimeDao = new FinaReferenceTimeDAO(connection);
        for (BundledReferenceTime referenceTime : readBundledReferenceTimes()) {
            var existing = referenceTimeDao.findTimeSeconds(referenceTime.disciplineId(), referenceTime.gender());
            if (existing.isEmpty() || existing.getAsDouble() <= 0) {
                referenceTimeDao.upsert(referenceTime.disciplineId(), referenceTime.gender(), referenceTime.seconds());
                imported++;
            }
        }
        return imported;
    }

    public int restoreBundledReferenceTimes() throws IOException, SQLException {
        List<BundledReferenceTime> defaults = readBundledReferenceTimes();
        boolean oldAutoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(false);
            FinaReferenceTimeDAO referenceTimeDao = new FinaReferenceTimeDAO(connection);
            referenceTimeDao.deleteAll();
            for (BundledReferenceTime referenceTime : defaults) {
                referenceTimeDao.upsert(referenceTime.disciplineId(), referenceTime.gender(),
                        referenceTime.seconds());
            }
            connection.commit();
            return defaults.size();
        } catch (SQLException | RuntimeException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackError) {
                e.addSuppressed(rollbackError);
            }
            throw e;
        } finally {
            connection.setAutoCommit(oldAutoCommit);
        }
    }

    private List<BundledReferenceTime> readBundledReferenceTimes() throws IOException, SQLException {
        List<BundledReferenceTime> referenceTimes = new ArrayList<>();
        DisziplinDao disziplinDao = new DisziplinDao(connection);
        try (InputStream input = StartupService.class.getResourceAsStream("/data/ref_times.csv")) {
            if (input == null) {
                throw new IOException("Die Referenzzeitdatei /data/ref_times.csv fehlt.");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String header = reader.readLine();
                if (header != null && header.startsWith("\uFEFF")) {
                    header = header.substring(1);
                }
                if (!"Schwimmart;Strecke;Männer_Zeit;Männer_Sekunden;Frauen_Zeit;Frauen_Sekunden"
                        .equals(header)) {
                    throw new IOException("Ungültiger Tabellenkopf in /data/ref_times.csv.");
                }

                String line;
                int lineNumber = 1;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    if (line.isBlank()) {
                        continue;
                    }
                    String[] columns = line.split(";", -1);
                    if (columns.length != 6) {
                        throw invalidReferenceRow(lineNumber, "Es werden sechs Spalten erwartet.");
                    }
                    SchwimmLage lage = parseStroke(columns[0], lineNumber);
                    int distance = parseDistance(columns[1], lineNumber);
                    var discipline = disziplinDao.findByLageUndDistanz(lage, distance);
                    if (discipline == null) {
                        throw invalidReferenceRow(lineNumber,
                                "Keine Disziplin für " + lage + " " + distance + "m vorhanden.");
                    }
                    for (Geschlecht gender : Geschlecht.values()) {
                        int secondsColumn = gender == Geschlecht.MAENNLICH ? 3 : 5;
                        referenceTimes.add(new BundledReferenceTime(discipline.getId(), gender,
                                parseSeconds(columns[secondsColumn], lineNumber)));
                    }
                }
            }
        }
        if (referenceTimes.isEmpty()) {
            throw new IOException("Die Referenzzeitdatei /data/ref_times.csv enthält keine Zeiten.");
        }
        return List.copyOf(referenceTimes);
    }

    private record BundledReferenceTime(int disciplineId, Geschlecht gender, double seconds) {
    }

    public WettkampfImportService.ImportErgebnis importBundledIfEmpty()
            throws IOException, DsvParseException, SQLException {
        if (new WettkampfDao(connection).countAll() != 0) {
            return null;
        }
        try (InputStream input = StartupService.class.getResourceAsStream("/data/DMS2025.dsv7")) {
            if (input == null) {
                throw new IOException("Die mitgelieferte Wettkampfdatei /data/DMS2025.dsv7 fehlt.");
            }
            return importService.importiere("DMS2025", input.readAllBytes());
        }
    }

    private SchwimmLage parseStroke(String value, int lineNumber) throws IOException {
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "freistil" -> SchwimmLage.FREISTIL;
            case "rücken" -> SchwimmLage.RUECKEN;
            case "brust" -> SchwimmLage.BRUST;
            case "schmetterling" -> SchwimmLage.SCHMETTERLING;
            case "lagen" -> SchwimmLage.LAGEN;
            default -> throw invalidReferenceRow(lineNumber, "Unbekannte Schwimmart: " + value);
        };
    }

    private int parseDistance(String value, int lineNumber) throws IOException {
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace("m", "").trim();
        try {
            int distance = Integer.parseInt(normalized);
            if (distance <= 0) {
                throw new NumberFormatException("Distanz muss positiv sein.");
            }
            return distance;
        } catch (NumberFormatException e) {
            throw invalidReferenceRow(lineNumber, "Ungültige Strecke: " + value);
        }
    }

    private double parseSeconds(String value, int lineNumber) throws IOException {
        try {
            double seconds = Double.parseDouble(value.trim().replace(',', '.'));
            if (!Double.isFinite(seconds) || seconds <= 0) {
                throw new NumberFormatException("Zeit muss positiv sein.");
            }
            return seconds;
        } catch (NumberFormatException e) {
            throw invalidReferenceRow(lineNumber, "Ungültige Referenzzeit in Sekunden: " + value);
        }
    }

    private IOException invalidReferenceRow(int lineNumber, String reason) {
        return new IOException("Fehler in /data/ref_times.csv, Zeile " + lineNumber + ": " + reason);
    }
}
