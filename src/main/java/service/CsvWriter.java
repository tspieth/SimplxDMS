package service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;

/** Writes semicolon-separated UTF-8 CSV files without exposing partial output. */
public final class CsvWriter {
    public static final boolean CSV_WRITE_BOM = false;
    public static final String DELIMITER = ";";

    private CsvWriter() {
    }

    public static void writeAtomically(Path target, List<List<String>> rows) throws IOException {
        Path absoluteTarget = target.toAbsolutePath().normalize();
        Path parent = absoluteTarget.getParent();
        if (parent == null || !Files.isDirectory(parent)) {
            throw new IOException("Der Exportordner ist nicht verfügbar.");
        }

        Path temporary = Files.createTempFile(parent, ".csv-export-", ".tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                if (CSV_WRITE_BOM) {
                    writer.write('\uFEFF');
                }
                for (List<String> row : rows) {
                    for (int column = 0; column < row.size(); column++) {
                        if (column > 0) {
                            writer.write(DELIMITER);
                        }
                        writer.write(escape(row.get(column)));
                    }
                    writer.write("\n");
                }
            }
            try {
                Files.move(temporary, absoluteTarget, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, absoluteTarget, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | RuntimeException e) {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException cleanupError) {
                e.addSuppressed(cleanupError);
            }
            throw e;
        }
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(DELIMITER) || value.contains("\"")
                || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
