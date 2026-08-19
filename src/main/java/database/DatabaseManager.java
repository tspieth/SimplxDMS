package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    private Connection connection;

    public void connect() throws SQLException {
        String dbPath = DatabaseConfig.getDatabasePath();
        // AUTO_SERVER=TRUE erlaubt mehrere Verbindungen gleichzeitig (praktisch beim Debuggen)
        String url = "jdbc:h2:file:" + dbPath + ";AUTO_SERVER=TRUE";

        connection = DriverManager.getConnection(url, "sa", "");
        System.out.println("Datenbank verbunden: " + dbPath);
    }

    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    public Connection getConnection() {
        return connection;
    }
}
