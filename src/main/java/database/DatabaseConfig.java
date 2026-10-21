package database;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Class used to detect the distribution-specific storage location of
 * the database file
 */
public class DatabaseConfig {

    /**
     * Returns the H2 database path in the current user's platform-appropriate application directory.
     *
     * @return path to the database file without its H2-specific extension
     */
    public static String getDatabasePath() {
        String userHome = System.getProperty("user.home");
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        Path appDataDir;

        if (os.startsWith("windows")) {
            // Windows: C:\Users\Name\AppData\Roaming\SimplxDMS
            String appData = System.getenv("APPDATA");
            appDataDir = Path.of(appData != null && !appData.isBlank() ? appData : userHome, "SimplxDMS");
        } else if (os.contains("mac") || os.contains("darwin")) {
            // macOS: /Users/Name/Library/Application Support/SimplxDMS
            appDataDir = Path.of(userHome, "Library", "Application Support", "SimplxDMS");
        } else {
            // Linux: /home/name/.simplxdms (versteckter Ordner, üblich unter Linux)
            appDataDir = Path.of(userHome, ".simplxdms");
        }

        try {
            Files.createDirectories(appDataDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Datenbankverzeichnis kann nicht angelegt werden: " + appDataDir, e);
        }

        return appDataDir.resolve("database").toString();
    }
}
