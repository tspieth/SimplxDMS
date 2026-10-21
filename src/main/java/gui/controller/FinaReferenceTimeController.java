package gui.controller;

import database.DatabaseManager;
import database.dao.DisziplinDao;
import database.dao.FinaReferenceTimeDAO;
import database.model.Disziplin;
import database.model.Geschlecht;
import database.model.SchwimmLage;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Edits the FINA reference times for male and female swimmers.
 */
public class FinaReferenceTimeController {
    static final List<List<SchwimmLage>> SPALTEN = List.of(
            List.of(SchwimmLage.FREISTIL),
            List.of(SchwimmLage.BRUST, SchwimmLage.RUECKEN),
            List.of(SchwimmLage.SCHMETTERLING, SchwimmLage.LAGEN));

    @FXML private ToggleButton toggleMaennlich;
    @FXML private ToggleButton toggleWeiblich;
    @FXML private ToggleGroup genderToggleGroup;
    @FXML private Button btnAbbrechen;
    @FXML private VBox spalte1;
    @FXML private VBox spalte2;
    @FXML private VBox spalte3;
    @FXML private Button btnSpeichern;

    private final Connection connection = DatabaseManager.getInstance().getConnection();
    private final Map<Integer, TextField> fields = new HashMap<>();
    private final Map<Geschlecht, Map<Integer, String>> valuesByGender = new EnumMap<>(Geschlecht.class);
    private Geschlecht currentGender = Geschlecht.MAENNLICH;
    private Stage stage;
    private boolean ready;

    /**
     * Loads disciplines and stored times, then creates the three columns from database data.
     */
    @FXML
    public void initialize() {
        valuesByGender.put(Geschlecht.MAENNLICH, new HashMap<>());
        valuesByGender.put(Geschlecht.WEIBLICH, new HashMap<>());
        toggleMaennlich.setToggleGroup(genderToggleGroup);
        toggleWeiblich.setToggleGroup(genderToggleGroup);
        toggleMaennlich.setSelected(true);
        toggleMaennlich.setOnAction(event -> {
            if (toggleMaennlich.isSelected()) {
                switchGender(Geschlecht.MAENNLICH);
            } else {
                toggleMaennlich.setSelected(true);
            }
        });
        toggleWeiblich.setOnAction(event -> {
            if (toggleWeiblich.isSelected()) {
                switchGender(Geschlecht.WEIBLICH);
            } else {
                toggleWeiblich.setSelected(true);
            }
        });
        btnSpeichern.setOnAction(event -> save());
        btnAbbrechen.setOnAction(event -> cancel());

        try {
            List<Disziplin> disciplines = new DisziplinDao(connection).findAll();
            Map<Integer, Map<Geschlecht, Double>> storedTimes = new FinaReferenceTimeDAO(connection).findAll();
            populateTimeCache(storedTimes);
            populateColumns(disciplines, storedTimes);
            renderGender(Geschlecht.MAENNLICH);
            ready = true;
        } catch (SQLException | IllegalStateException e) {
            btnSpeichern.setDisable(true);
            showError("Die FINA-Referenzzeiten konnten nicht geladen werden.", e);
        }
    }

    /**
     * Associates the owning window so successful saves and cancellation can close it.
     *
     * @param stage window containing this form
     */
    public void setStage(Stage stage) {
        this.stage = stage;
        this.stage.setOnCloseRequest(event -> captureCurrentFields());
    }

    /**
     * Builds a display cache from reference-time values retrieved from the database.
     *
     * @param storedTimes reference times grouped by discipline ID and gender
     */
    private void populateTimeCache(Map<Integer, Map<Geschlecht, Double>> storedTimes) {
        storedTimes.forEach((disciplineId, byGender) -> byGender.forEach((gender, seconds) ->
                valuesByGender.get(gender).put(disciplineId, formatTime(Math.round(seconds * 100)))));
    }

    /**
     * Creates labels and input fields in the configured stroke groups, sorted by distance.
     *
     * @param disciplines discipline records loaded from the database
     * @param storedTimes reference values, used to detect dangling discipline references
     * @throws IllegalStateException if a database stroke is not assigned to a column
     */
    private void populateColumns(List<Disziplin> disciplines,
                                Map<Integer, Map<Geschlecht, Double>> storedTimes) {
        List<VBox> columns = List.of(spalte1, spalte2, spalte3);
        Map<Integer, Disziplin> disciplinesById = new HashMap<>();
        for (Disziplin discipline : disciplines) {
            int columnIndex = findColumn(discipline.getLage());
            if (columnIndex >= columns.size()) {
                throw new IllegalStateException("Keine FINA-Spalte für " + discipline.getLage() + " vorhanden.");
            }
            disciplinesById.put(discipline.getId(), discipline);
        }

        for (int columnIndex = 0; columnIndex < SPALTEN.size(); columnIndex++) {
            VBox column = columns.get(columnIndex);
            for (SchwimmLage lage : SPALTEN.get(columnIndex)) {
                VBox strokeGroup = new VBox(6);
                javafx.scene.control.Label heading =
                        new javafx.scene.control.Label(lage.getAnzeigename());
                heading.setStyle("-fx-font-weight: bold;");
                strokeGroup.getChildren().add(heading);
                disciplines.stream()
                        .filter(discipline -> discipline.getLage() == lage)
                        .sorted(Comparator.comparingInt(Disziplin::getDistanz))
                        .forEach(discipline -> {
                            TextField field = new TextField();
                            field.setPromptText("m:ss.hh");
                            field.setPrefColumnCount(8);
                            fields.put(discipline.getId(), field);
                            strokeGroup.getChildren().addAll(
                                    new javafx.scene.control.Label(discipline.getDistanz() + " m"), field);
                        });
                column.getChildren().add(strokeGroup);
            }
        }
        for (Integer disciplineId : storedTimes.keySet()) {
            if (!disciplinesById.containsKey(disciplineId)) {
                throw new IllegalStateException("FINA-Zeit verweist auf unbekannte Disziplin " + disciplineId);
            }
        }
    }

    /**
     * Maps a stroke to its configured column, rejecting unsupported database values.
     *
     * @param lage stroke from a database discipline
     * @return zero-based column index
     * @throws IllegalStateException if no column contains the stroke
     */
    private int findColumn(SchwimmLage lage) {
        for (int column = 0; column < SPALTEN.size(); column++) {
            if (SPALTEN.get(column).contains(lage)) {
                return column;
            }
        }
        throw new IllegalStateException("Keine Spalte für Schwimmlage " + lage + " konfiguriert.");
    }

    /**
     * Switches gender while first retaining the current gender's uncommitted field values.
     *
     * @param gender selected gender
     */
    private void switchGender(Geschlecht gender) {
        if (gender == currentGender) {
            return;
        }
        captureCurrentFields();
        currentGender = gender;
        renderGender(gender);
    }

    /**
     * Copies the visible values into their current-gender cache.
     */
    private void captureCurrentFields() {
        Map<Integer, String> values = valuesByGender.get(currentGender);
        fields.forEach((disciplineId, field) -> values.put(disciplineId, field.getText()));
    }

    /**
     * Displays cached values for a gender in the shared discipline fields.
     *
     * @param gender gender whose cached values should be shown
     */
    private void renderGender(Geschlecht gender) {
        Map<Integer, String> values = valuesByGender.get(gender);
        fields.forEach((disciplineId, field) -> field.setText(values.getOrDefault(disciplineId, "")));
    }

    /**
     * Validates and saves all non-empty fields for both genders in one transaction.
     */
    private void save() {
        if (!ready) {
            return;
        }
        captureCurrentFields();
        Map<Geschlecht, Map<Integer, Long>> hundredths = new EnumMap<>(Geschlecht.class);
        try {
            for (Geschlecht gender : Geschlecht.values()) {
                Map<Integer, Long> parsed = new HashMap<>();
                for (Map.Entry<Integer, String> entry : valuesByGender.get(gender).entrySet()) {
                    if (!entry.getValue().isBlank()) {
                        parsed.put(entry.getKey(), parseTime(entry.getValue()));
                    }
                }
                hundredths.put(gender, parsed);
            }
        } catch (IllegalArgumentException e) {
            showError("Bitte prüfe die markierten Zeitfelder. Erwartetes Format: m:ss.hh.", e);
            return;
        }

        boolean autoCommit;
        try {
            autoCommit = connection.getAutoCommit();
        } catch (SQLException e) {
            showError("Der Datenbankstatus konnte nicht geprüft werden.", e);
            return;
        }
        SQLException failure = null;
        try {
            connection.setAutoCommit(false);
            FinaReferenceTimeDAO dao = new FinaReferenceTimeDAO(connection);
            for (Geschlecht gender : Geschlecht.values()) {
                for (Map.Entry<Integer, Long> entry : hundredths.get(gender).entrySet()) {
                    dao.upsert(entry.getKey(), gender, entry.getValue() / 100.0);
                }
            }
            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackError) {
                e.addSuppressed(rollbackError);
            }
            failure = e;
        } finally {
            try {
                connection.setAutoCommit(autoCommit);
            } catch (SQLException restoreError) {
                if (failure == null) {
                    failure = restoreError;
                } else {
                    failure.addSuppressed(restoreError);
                }
            }
        }
        if (failure != null) {
            showError("Die FINA-Referenzzeiten konnten nicht gespeichert werden.", failure);
        } else {
            closeDialog();
        }
    }

    /**
     * Converts a time in m:ss.hh to an integer count of hundredths of a second.
     *
     * @param value time string
     * @return total hundredths
     * @throws IllegalArgumentException if the time is malformed or exceeds the supported range
     */
    private long parseTime(String value) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("^(\\d+):([0-5]\\d)\\.(\\d{2})$")
                .matcher(value.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Ungültiges Zeitformat: " + value);
        }
        try {
            long minutes = Long.parseLong(matcher.group(1));
            long seconds = Long.parseLong(matcher.group(2));
            long hundredths = Long.parseLong(matcher.group(3));
            long total = Math.addExact(Math.addExact(Math.multiplyExact(minutes, 6000L),
                    Math.multiplyExact(seconds, 100L)), hundredths);
            return total;
        } catch (NumberFormatException | ArithmeticException e) {
            throw new IllegalArgumentException("Die eingegebene Zeit ist zu groß.", e);
        }
    }

    /**
     * Formats a rounded integer hundredths count as m:ss.hh.
     *
     * @param hundredths total hundredths of a second
     * @return formatted reference time
     */
    private String formatTime(long hundredths) {
        long minutes = hundredths / 6000;
        long seconds = hundredths / 100 % 60;
        long fraction = hundredths % 100;
        return String.format(Locale.ROOT, "%d:%02d.%02d", minutes, seconds, fraction);
    }

    /**
     * Closes the editor without saving pending edits.
     */
    @FXML
    private void cancel() {
        captureCurrentFields();
        closeDialog();
    }

    /**
     * Closes the owning dialog, if it has already been attached.
     */
    private void closeDialog() {
        if (stage != null) {
            stage.close();
        }
    }

    /**
     * Displays a readable error alert.
     *
     * @param message user-facing error message
     * @param error technical cause
     */
    private void showError(String message, Exception error) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message + "\n" + error.getMessage(), ButtonType.OK);
        alert.setHeaderText("Fehler");
        alert.showAndWait();
    }
}
