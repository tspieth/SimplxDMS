package service;

import java.util.List;

/** Fixed CSV layout for one-gender team exports. */
public final class MannschaftCsvFormat {
    public static final String DELIMITER = ";";
    public static final List<String> HEADER =
            List.of("Schwimmart", "Name", "Zeit", "Punkte", "Schwimmart", "Name", "Zeit", "Punkte");

    private MannschaftCsvFormat() {
    }
}
