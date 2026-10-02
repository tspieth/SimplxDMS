package database;

/**
 * Class used to detect the distribution-specific storage location of
 * the database file
 */
public class DatabaseConfig {
    public static String getDatabasePath() {
        String userHome = System.getProperty("user.home");
        String os = System.getProperty("os.name").toLowerCase();
        String appDataDir;

        if (os.contains("win")) {
            // Windows: C:\Users\Name\AppData\Roaming\SimplxDMS
            String appData = System.getenv("APPDATA");
            appDataDir = (appData != null ? appData : userHome) + "/SimplxDMS";
        } else if (os.contains("mac")) {
            // macOS: /Users/Name/Library/Application Support/SimplxDMS
            appDataDir = userHome + "/Library/Application Support/SimplxDMS";
        } else {
            // Linux: /home/name/.simplxdms (versteckter Ordner, üblich unter Linux)
            appDataDir = userHome + "/.simplxdms";
        }

        // Ordner anlegen, falls noch nicht vorhanden
        java.io.File dir = new java.io.File(appDataDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        return appDataDir + "/database";
    }
}
