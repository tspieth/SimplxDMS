package importer;

import database.model.Geschlecht;
import database.model.SchwimmLage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reads the individual events and breaks from an EasyWk DSV7 competition definition list.
 */
public class DsvWettkampfParser {
    private static final DateTimeFormatter DSV_DATE = DateTimeFormatter.ofPattern("d.M.uuuu", Locale.ROOT);

    public ParsedWettkampf parse(Reader source) throws IOException, DsvParseException {
        List<ProgrammpunktDaten> items = new ArrayList<>();
        LocalDate date = null;
        int section = 0;
        int skipped = 0;
        boolean validFormat = false;
        boolean inComment = false;

        try (BufferedReader reader = new BufferedReader(source)) {
            String rawLine;
            int lineNumber = 0;
            while ((rawLine = reader.readLine()) != null) {
                lineNumber++;
                StringBuilder normalized = new StringBuilder();
                for (int i = 0; i < rawLine.length(); i++) {
                    if (!inComment && i + 1 < rawLine.length()
                            && rawLine.charAt(i) == '(' && rawLine.charAt(i + 1) == '*') {
                        inComment = true;
                        i++;
                    } else if (inComment && i + 1 < rawLine.length()
                            && rawLine.charAt(i) == '*' && rawLine.charAt(i + 1) == ')') {
                        inComment = false;
                        i++;
                    } else if (!inComment) {
                        normalized.append(rawLine.charAt(i));
                    }
                }
                String line = normalized.toString().trim();
                if (lineNumber == 1 && line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }
                if (line.isEmpty()) {
                    continue;
                }
                if (line.startsWith("FORMAT:")) {
                    validFormat = line.startsWith("FORMAT: Wettkampfdefinitionsliste;7");
                } else if (line.startsWith("ABSCHNITT:")) {
                    String[] values = fields(line);
                    if (values.length < 2) {
                        throw new DsvParseException(lineNumber, "Abschnitt ohne Datum.");
                    }
                    try {
                        section = Integer.parseInt(values[0]);
                        LocalDate sectionDate = LocalDate.parse(values[1], DSV_DATE);
                        if (date == null || section < 2) {
                            date = sectionDate;
                        }
                    } catch (NumberFormatException | DateTimeParseException e) {
                        throw new DsvParseException(lineNumber, "Ungültige Abschnittsnummer oder Datum.", e);
                    }
                } else if (line.startsWith("WETTKAMPF:")) {
                    String[] values = fields(line);
                    if (values.length < 8) {
                        throw new DsvParseException(lineNumber, "WETTKAMPF-Zeile enthält zu wenige Felder.");
                    }
                    try {
                        int number = Integer.parseInt(values[0]);
                        String type = values[1].toUpperCase(Locale.ROOT);
                        int eventSection = Integer.parseInt(values[2]);
                        int distance = Integer.parseInt(values[4]);
                        if (!type.equals("E")) {
                            skipped++;
                            continue;
                        }
                        SchwimmLage stroke = parseStroke(values[5], lineNumber);
                        Geschlecht gender = parseGender(values[7], lineNumber);
                        items.add(ProgrammpunktDaten.lauf(number, eventSection, distance, stroke, gender));
                    } catch (NumberFormatException e) {
                        throw new DsvParseException(lineNumber, "Ungültige Nummer, Abschnittsnummer oder Distanz.", e);
                    }
                } else if (line.startsWith("PAUSE:")) {
                    String[] values = fields(line);
                    String duration = null;
                    for (int index = values.length - 1; index >= 0; index--) {
                        if (!values[index].isBlank()) {
                            duration = values[index].trim();
                            break;
                        }
                    }
                    if (duration == null) {
                        throw new DsvParseException(lineNumber, "Pausenzeile ohne Dauer.");
                    }
                    try {
                        int seconds = parseDuration(duration);
                        if (section < 1) {
                            throw new DsvParseException(lineNumber, "Pause steht vor dem ersten Abschnitt.");
                        }
                        items.add(ProgrammpunktDaten.pause(section, seconds));
                    } catch (NumberFormatException | ArithmeticException e) {
                        throw new DsvParseException(lineNumber,
                                "Ungültige Pausendauer (Sekunden oder mm:ss erwartet).", e);
                    }
                }
            }
        }
        if (!validFormat) {
            throw new DsvParseException(1, "Datei ist keine DSV7-Wettkampfdefinitionsliste.");
        }
        if (items.stream().noneMatch(item -> item.typ() == ProgrammpunktTyp.LAUF)) {
            throw new DsvParseException(0, "Die Datei enthält keine Einzelwettkämpfe.");
        }
        return new ParsedWettkampf(date, items, skipped);
    }

    private String[] fields(String line) {
        int colon = line.indexOf(':');
        String[] values = line.substring(colon + 1).split(";", -1);
        for (int i = 0; i < values.length; i++) {
            values[i] = values[i].trim();
        }
        return values;
    }

    private SchwimmLage parseStroke(String code, int lineNumber) throws DsvParseException {
        return switch (code.toUpperCase(Locale.ROOT)) {
            case "F" -> SchwimmLage.FREISTIL;
            case "B" -> SchwimmLage.BRUST;
            case "R" -> SchwimmLage.RUECKEN;
            case "S" -> SchwimmLage.SCHMETTERLING;
            case "L" -> SchwimmLage.LAGEN;
            default -> throw new DsvParseException(lineNumber, "Unbekannte Schwimmlage: " + code);
        };
    }

    private Geschlecht parseGender(String value, int lineNumber) throws DsvParseException {
        return switch (value.toUpperCase(Locale.ROOT)) {
            case "M" -> Geschlecht.MAENNLICH;
            case "W" -> Geschlecht.WEIBLICH;
            default -> throw new DsvParseException(lineNumber, "Unbekanntes Geschlecht: " + value);
        };
    }

    private int parseDuration(String value) {
        if (value.matches("\\d+:\\d{1,2}")) {
            String[] parts = value.split(":");
            int seconds = Integer.parseInt(parts[1]);
            if (seconds > 59) {
                throw new NumberFormatException("Sekundenanteil muss zwischen 0 und 59 liegen.");
            }
            return Math.addExact(Math.multiplyExact(Integer.parseInt(parts[0]), 60), seconds);
        }
        int seconds = Integer.parseInt(value);
        if (seconds < 0) {
            throw new NumberFormatException("Pausendauer darf nicht negativ sein.");
        }
        return seconds;
    }

    public enum ProgrammpunktTyp {
        LAUF,
        PAUSE
    }

    public record ProgrammpunktDaten(ProgrammpunktTyp typ, int wettkampfNummer, int abschnitt,
                                     int distanz, SchwimmLage lage, Geschlecht geschlecht, int pauseSekunden) {
        static ProgrammpunktDaten lauf(int nr, int abschnitt, int distanz, SchwimmLage lage, Geschlecht geschlecht) {
            return new ProgrammpunktDaten(ProgrammpunktTyp.LAUF, nr, abschnitt, distanz, lage, geschlecht, 0);
        }

        static ProgrammpunktDaten pause(int abschnitt, int sekunden) {
            return new ProgrammpunktDaten(ProgrammpunktTyp.PAUSE, 0, abschnitt, 0, null, null, sekunden);
        }
    }

    public record ParsedWettkampf(LocalDate datum, List<ProgrammpunktDaten> programmpunkte, int uebersprungeneNichtEinzel) {
        public ParsedWettkampf {
            programmpunkte = List.copyOf(programmpunkte);
        }

        public long anzahlLaeufe() {
            return programmpunkte.stream().filter(item -> item.typ() == ProgrammpunktTyp.LAUF).count();
        }

        public long anzahlPausen() {
            return programmpunkte.stream().filter(item -> item.typ() == ProgrammpunktTyp.PAUSE).count();
        }
    }
}
