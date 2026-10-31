package service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Reads semicolon-separated records with standard quoted-field escaping. */
final class SemicolonCsvReader {
    private SemicolonCsvReader() {
    }

    static List<List<String>> readAll(Path file) throws IOException {
        List<List<String>> records = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            List<String> row = new ArrayList<>();
            StringBuilder field = new StringBuilder();
            boolean quoted = false;
            boolean firstCharacter = true;
            int current;
            while ((current = reader.read()) != -1) {
                char character = (char) current;
                if (firstCharacter) {
                    firstCharacter = false;
                    if (character == '\uFEFF') {
                        continue;
                    }
                }

                if (quoted) {
                    if (character == '"') {
                        reader.mark(1);
                        int next = reader.read();
                        if (next == '"') {
                            field.append('"');
                        } else {
                            quoted = false;
                            if (next != -1) {
                                reader.reset();
                            }
                        }
                    } else {
                        field.append(character);
                    }
                    continue;
                }

                if (character == '"') {
                    if (field.length() != 0) {
                        throw new IOException("Ungültiges Anführungszeichen in der CSV-Datei.");
                    }
                    quoted = true;
                } else if (character == ';') {
                    row.add(field.toString());
                    field.setLength(0);
                } else if (character == '\n' || character == '\r') {
                    if (character == '\r') {
                        reader.mark(1);
                        if (reader.read() != '\n') {
                            reader.reset();
                        }
                    }
                    row.add(field.toString());
                    records.add(List.copyOf(row));
                    row.clear();
                    field.setLength(0);
                } else {
                    field.append(character);
                }
            }
            if (quoted) {
                throw new IOException("Ein CSV-Feld wurde nicht geschlossen.");
            }
            if (!row.isEmpty() || field.length() > 0) {
                row.add(field.toString());
                records.add(List.copyOf(row));
            }
        }
        return records;
    }
}
