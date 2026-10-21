package database;

import database.dao.DisziplinDao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Singleton Class for Databasemanagement
 *
 * Typicall Singleton Structure
 */
public class DatabaseManager {

    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        try {
            connect();
        } catch (SQLException e) {
            throw new RuntimeException("Datenbankverbindung fehlgeschlagen", e);
        }
    }

    /**
     * getInstance() Singleton Method
     *
     * @return
     */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * This method sets the connection to the database
     * with the values defined in the DatabaseConfig class.
     *
     * Furthermore calls initScheme() to init the Schematic of the database
     *
     * @throws SQLException
     */
    private void connect() throws SQLException {
        String dbPath = DatabaseConfig.getDatabasePath();
        String url = "jdbc:h2:file:" + dbPath + ";AUTO_SERVER=TRUE";
        connection = DriverManager.getConnection(url, "sa", "");
        initSchema();
        new DisziplinDao(connection).initDefaultDisziplinen();
    }

    /**
     * Initialises the Scheme of the database.
     * The Scheme is decided via the schema.sql file
     *
     * Resource Path without src/main/resources because mavn puts it all
     * in the root.
     *
     * @throws SQLException
     */
    private void initSchema() throws SQLException {
        String sql = readResourceFile("/db/schema.sql");
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    /**
     * Helper Method to read ResourceFile
     *
     * Returns the String representation of the file in the path
     *
     * @param path
     * @return
     */
    private String readResourceFile(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) throw new RuntimeException("Schema-Datei nicht gefunden: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Fehler beim Lesen der Schema-Datei", e);
        }
    }

    /**
     * Standard getter for the Connection object
     * @return
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Method to close() the Databaseconnection
     */
    public void close() throws SQLException{
        if (connection != null && !connection.isClosed()){
            connection.close();
        }
    }
}