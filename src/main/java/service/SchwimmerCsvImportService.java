package service;

import database.dao.DisziplinDao;
import database.dao.LeistungDao;
import database.dao.SchwimmerDao;
import database.model.Disziplin;
import database.model.Leistung;
import database.model.Schwimmer;
import database.model.SchwimmLage;
import database.model.Verein;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Imports swimmer groups and their timed performances from a semicolon-separated file.
 */
public class SchwimmerCsvImportService {
    private static final Pattern TIME_PATTERN =
            Pattern.compile("(?:(\\d+):)?(\\d{1,2})(?:[.,](\\d{1,9}))?");
    private static final Pattern DISCIPLINE_PATTERN = Pattern.compile("^(\\d+)(K|B|R|S|Lg)$");
    private final Connection connection;

    public SchwimmerCsvImportService(Connection connection) {
        this.connection = connection;
    }

    public List<ImportSchwimmer> parse(Path file) throws IOException {
        List<ImportSchwimmerBuilder> result = new ArrayList<>();
        ImportSchwimmerBuilder current = null;
        int rowNumber = 0;

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (rowNumber == 1 && line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }
                if (line.isBlank()) {
                    continue;
                }
                String[] fields = line.split(";", -1);
                if (rowNumber == 1 && fields.length >= 6
                        && fields[0].trim().equalsIgnoreCase("Name")
                        && fields[1].trim().equalsIgnoreCase("Vorname")) {
                    continue;
                }
                if (fields.length != 6) {
                    throw invalidRow(rowNumber, "Es werden genau sechs durch Semikolon getrennte Spalten erwartet.");
                }
                for (int i = 0; i < fields.length; i++) {
                    fields[i] = fields[i].trim();
                }

                boolean hasIdentity = !fields[0].isEmpty() || !fields[1].isEmpty()
                        || !fields[2].isEmpty() || !fields[3].isEmpty();
                if (hasIdentity) {
                    //Birthyear not necessary
                    if (fields[0].isEmpty() || fields[1].isEmpty() || fields[2].isEmpty() ) {
                        throw invalidRow(rowNumber,
                                "Name, Vorname, Geschlecht  müssen gemeinsam angegeben sein.");
                    }

                    current = new ImportSchwimmerBuilder(fields[0], fields[1], parseGender(fields[2], rowNumber),
                            (fields[3] != "" ? parseBirthDate(fields[3], rowNumber): LocalDate.of(1999, 1, 1)));
                    result.add(current);
                } else if (current == null) {
                    throw invalidRow(rowNumber, "Eine Strecke ist angegeben, bevor ein Schwimmer definiert wurde.");
                }

                if (fields[4].isEmpty()) {
                    if (!fields[5].isEmpty()) {
                        throw invalidRow(rowNumber, "Eine Zeit kann nicht ohne Strecke angegeben werden.");
                    }
                    continue;
                }
                Discipline discipline = parseDiscipline(fields[4], rowNumber);
                LocalTime time = fields[5].isEmpty() ? null : parseTime(fields[5], rowNumber);
                if (time != null) {
                    current.addPerformance(discipline, time, rowNumber);
                }
            }
        }

        if (result.isEmpty()) {
            throw new IOException("Die Datei enthält keine Schwimmerdaten.");
        }
        return result.stream().map(ImportSchwimmerBuilder::build).toList();
    }

    public ImportErgebnis importSwimmers(Verein club, List<ImportSchwimmer> swimmers,
                                         Set<Integer> addToExistingSwimmerIds) throws SQLException {
        if (club == null) {
            throw new IllegalArgumentException("Bitte zuerst einen Verein auswählen.");
        }

        boolean oldAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            SchwimmerDao swimmerDao = new SchwimmerDao(connection);
            LeistungDao leistungDao = new LeistungDao(connection);
            DisziplinDao disziplinDao = new DisziplinDao(connection);
            Map<String, Schwimmer> existingByName = new HashMap<>();
            for (Schwimmer existing : swimmerDao.findByVerein(club.getId())) {
                existingByName.putIfAbsent(duplicateKey(existing.getVorname(), existing.getName()), existing);
            }

            int newSwimmers = 0;
            int updatedSwimmers = 0;
            int skippedDuplicates = 0;
            int savedPerformances = 0;
            Map<Discipline, Integer> disciplineIds = new HashMap<>();
            for (ImportSchwimmer imported : swimmers) {
                Schwimmer swimmer = existingByName.get(imported.duplicateKey());
                if (swimmer != null) {
                    if (!addToExistingSwimmerIds.contains(swimmer.getId())) {
                        skippedDuplicates++;
                        continue;
                    }
                    updatedSwimmers++;
                } else {
                    swimmer = new Schwimmer(0, imported.name(), imported.vorname(), imported.geburtsdatum(),
                            imported.geschlecht(), club.getId());
                    swimmerDao.insert(swimmer);
                    existingByName.put(imported.duplicateKey(), swimmer);
                    newSwimmers++;
                }

                for (ImportLeistung performance : imported.leistungen()) {
                    Discipline discipline = new Discipline(performance.lage(), performance.distanz());
                    Integer disciplineId = disciplineIds.get(discipline);
                    if (disciplineId == null) {
                        Disziplin storedDiscipline = disziplinDao.findByLageUndDistanz(
                                performance.lage(), performance.distanz());
                        if (storedDiscipline == null) {
                            throw new SQLException("Disziplin fehlt in der Datenbank: "
                                    + performance.distanz() + "m " + performance.lage());
                        }
                        disciplineId = storedDiscipline.getId();
                        disciplineIds.put(discipline, disciplineId);
                    }
                    leistungDao.insert(new Leistung(0, swimmer.getId(), disciplineId, performance.zeit(), null));
                    savedPerformances++;
                }
            }
            connection.commit();
            return new ImportErgebnis(newSwimmers, updatedSwimmers, skippedDuplicates, savedPerformances);
        } catch (SQLException | RuntimeException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(oldAutoCommit);
        }
    }

    private String parseGender(String value, int rowNumber) throws IOException {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "m" -> "m";
            case "w" -> "w";
            default -> throw invalidRow(rowNumber, "Geschlecht muss 'm' oder 'w' sein.");
        };
    }

    private LocalDate parseBirthDate(String value, int rowNumber) throws IOException {
        //Wenn kein Geburtsjahr angegeben soll dieses Standard geburtsjahr angegeben sein
        try {
            int year = Integer.parseInt(value);
            if (year < 0) {
                throw new NumberFormatException();
            }
            if (value.length() <= 2) {
                int twoDigitYear = year;
                int currentTwoDigitYear = LocalDate.now().getYear() % 100;
                year = twoDigitYear <= currentTwoDigitYear ? 2000 + twoDigitYear : 1900 + twoDigitYear;
            }
            if (year < 1900 || year > LocalDate.now().getYear()) {
                throw new NumberFormatException();
            }
            return LocalDate.of(year, 1, 1);
        } catch (NumberFormatException e) {
            throw invalidRow(rowNumber, "Ungültiges Geburtsjahr: " + value);
        }
    }

    private Discipline parseDiscipline(String value, int rowNumber) throws IOException {
        Matcher matcher = DISCIPLINE_PATTERN.matcher(value);
        if (!matcher.matches()) {
            throw invalidRow(rowNumber, "Unbekannte Strecke: " + value);
        }
        int distance;
        try {
            distance = Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException e) {
            throw invalidRow(rowNumber, "Ungültige Distanz: " + value);
        }
        SchwimmLage stroke = switch (matcher.group(2)) {
            case "K" -> SchwimmLage.FREISTIL;
            case "B" -> SchwimmLage.BRUST;
            case "R" -> SchwimmLage.RUECKEN;
            case "S" -> SchwimmLage.SCHMETTERLING;
            case "Lg" -> SchwimmLage.LAGEN;
            default -> throw invalidRow(rowNumber, "Unbekannte Strecke: " + value);
        };
        return new Discipline(stroke, distance);
    }

    private LocalTime parseTime(String value, int rowNumber) throws IOException {
        Matcher matcher = TIME_PATTERN.matcher(value);
        if (!matcher.matches()) {
            throw invalidRow(rowNumber, "Ungültiges Zeitformat: " + value);
        }
        try {
            int minutes = matcher.group(1) == null ? 0 : Integer.parseInt(matcher.group(1));
            int seconds = Integer.parseInt(matcher.group(2));
            if (seconds > 59) {
                throw new NumberFormatException();
            }
            String fraction = matcher.group(3);
            int nanos = fraction == null ? 0 : Integer.parseInt((fraction + "000000000").substring(0, 9));
            return LocalTime.of(0, minutes, seconds, nanos);
        } catch (NumberFormatException | java.time.DateTimeException e) {
            throw invalidRow(rowNumber, "Ungültige Zeit: " + value);
        }
    }

    private IOException invalidRow(int rowNumber, String message) {
        return new IOException("Zeile " + rowNumber + ": " + message);
    }

    public static String duplicateKey(String firstName, String lastName) {
        return normalize(firstName) + "\u0000" + normalize(lastName);
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public record ImportSchwimmer(String name, String vorname, String geschlecht, LocalDate geburtsdatum,
                                  List<ImportLeistung> leistungen) {
        public ImportSchwimmer {
            leistungen = List.copyOf(leistungen);
        }

        public String duplicateKey() {
            return SchwimmerCsvImportService.duplicateKey(vorname, name);
        }
    }

    public record ImportLeistung(SchwimmLage lage, int distanz, LocalTime zeit) {
    }

    public record ImportErgebnis(int neueSchwimmer, int ergaenzteSchwimmer, int uebersprungeneDubletten,
                                 int gespeicherteLeistungen) {
    }

    private record Discipline(SchwimmLage lage, int distance) {
    }

    private static final class ImportSchwimmerBuilder {
        private final String name;
        private final String firstName;
        private final String gender;
        private final LocalDate birthDate;
        private final List<ImportLeistung> performances = new ArrayList<>();
        private final Set<Discipline> seenDisciplines = new HashSet<>();

        private ImportSchwimmerBuilder(String name, String firstName, String gender, LocalDate birthDate) {
            this.name = name;
            this.firstName = firstName;
            this.gender = gender;
            this.birthDate = birthDate;
        }

        private void addPerformance(Discipline discipline, LocalTime time, int rowNumber) throws IOException {
            if (!seenDisciplines.add(discipline)) {
                throw new IOException("Zeile " + rowNumber + ": Strecke " + discipline.distance()
                        + discipline.lage() + " ist für diesen Schwimmer mehrfach angegeben.");
            }
            performances.add(new ImportLeistung(discipline.lage(), discipline.distance(), time));
        }

        private ImportSchwimmer build() {
            return new ImportSchwimmer(name, firstName, gender, birthDate, performances);
        }
    }
}
