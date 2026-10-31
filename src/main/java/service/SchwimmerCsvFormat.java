package service;

import database.model.SchwimmLage;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

/** Shared column order and value formats for swimmer CSV import and export. */
public final class SchwimmerCsvFormat {
    public static final String DELIMITER = ";";
    public static final List<String> HEADER =
            List.of("Name", "Vorname", "Geschlecht", "Geburtsjahr", "Strecke", "Zeit");
    public static final int COLUMN_COUNT = 6;
    public static final int NAME = 0;
    public static final int FIRST_NAME = 1;
    public static final int GENDER = 2;
    public static final int BIRTH_YEAR = 3;
    public static final int DISCIPLINE = 4;
    public static final int TIME = 5;

    private SchwimmerCsvFormat() {
    }

    public static String strokeCode(SchwimmLage lage) {
        return switch (lage) {
            case FREISTIL -> "K";
            case BRUST -> "B";
            case RUECKEN -> "R";
            case SCHMETTERLING -> "S";
            case LAGEN -> "Lg";
        };
    }

    public static SchwimmLage strokeFromCode(String code) {
        return switch (code) {
            case "K" -> SchwimmLage.FREISTIL;
            case "B" -> SchwimmLage.BRUST;
            case "R" -> SchwimmLage.RUECKEN;
            case "S" -> SchwimmLage.SCHMETTERLING;
            case "Lg" -> SchwimmLage.LAGEN;
            default -> null;
        };
    }

    public static String formatBirthDate(LocalDate date) {
        return date == null ? "" : date.toString();
    }

    public static String formatTime(LocalTime time) {
        if (time == null) {
            return "";
        }
        int minutes = time.toSecondOfDay() / 60;
        int seconds = time.getSecond();
        int milliseconds = time.getNano() / 1_000_000;
        String result = String.format(Locale.ROOT, "%d:%02d", minutes, seconds);
        if (milliseconds > 0) {
            String fraction = String.format(Locale.ROOT, "%03d", milliseconds);
            while (fraction.endsWith("0")) {
                fraction = fraction.substring(0, fraction.length() - 1);
            }
            result += "." + fraction;
        }
        return result;
    }
}
