package gui.controller;

import database.DatabaseManager;
import database.dao.DisziplinDao;
import database.dao.LeistungDao;
import database.dao.SchwimmerDao;
import database.dao.SchwimmerService;
import database.model.Disziplin;
import database.model.Leistung;
import database.model.Schwimmer;
import database.model.SchwimmLage;
import database.model.Verein;
import gui.AppState;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads and edits the gender-specific swimmer list for the globally selected club.
 */
public class SwimmerController {
    @FXML private TableView<Schwimmer> schwimmerTableView;
    @FXML private TableColumn<Schwimmer, String> colSchwimmerName;
    @FXML private TableColumn<Schwimmer, String> colSchwimmerVerein;
    @FXML private TableColumn<Schwimmer, String> colSchwimmerJahrgang;
    @FXML private TableColumn<Schwimmer, Schwimmer> colAktionen;
    @FXML private Button btnSchwimmerNeu;
    @FXML private Label lblHinweis;

    private final Connection connection = DatabaseManager.getInstance().getConnection();
    private List<Disziplin> disciplines = List.of();

    /**
     * Initializes table columns, discipline options, and the current club's swimmer list.
     */
    @FXML
    public void initialize() {
        colSchwimmerName.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getVorname() + " " + cell.getValue().getName()));
        colSchwimmerVerein.setCellValueFactory(cell ->
                new SimpleStringProperty(clubName(AppState.getSelectedClub())));
        colSchwimmerJahrgang.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getGeburtsdatum() == null
                        ? "" : Integer.toString(cell.getValue().getGeburtsdatum().getYear())));
        colAktionen.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        colAktionen.setCellFactory(column -> new TableCell<>() {
            private final Button edit = new Button("Bearbeiten");
            private final Button delete = new Button("Löschen");
            private final HBox buttons = new HBox(6, edit, delete);

            {
                edit.setOnAction(event -> editSwimmer(getTableView().getItems().get(getIndex())));
                delete.setOnAction(event -> deleteSwimmer(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Schwimmer swimmer, boolean empty) {
                super.updateItem(swimmer, empty);
                setGraphic(empty ? null : buttons);
            }
        });
        btnSchwimmerNeu.setOnAction(event -> createSwimmer());
        schwimmerTableView.setPlaceholder(new Label("Keine Schwimmer/innen vorhanden"));
        try {
            disciplines = new DisziplinDao(connection).findAll().stream()
                    .sorted(Comparator.comparingInt((Disziplin d) -> disciplineGroup(d.getLage()))
                            .thenComparingInt(d -> disciplineOrder(d.getLage()))
                            .thenComparingInt(Disziplin::getDistanz))
                    .toList();
            refresh();
        } catch (SQLException e) {
            showError("Die Daten konnten nicht geladen werden.", e);
        }
    }

    /**
     * Returns the gender displayed by this controller; subclasses may specialize the tab.
     *
     * @return gender database value, or {@code null} to show all genders
     */
    protected String getGender() {
        return "m";
    }

    /**
     * Reloads the table from the database for the current club.
     */
    private void refresh() {
        if (schwimmerTableView == null) {
            return;
        }
        Verein club = AppState.getSelectedClub();
        boolean hasClub = club != null;
        btnSchwimmerNeu.setDisable(!hasClub);
        lblHinweis.setText(hasClub ? "" : "Bitte zuerst einen Verein auswählen.");
        schwimmerTableView.setDisable(!hasClub);
        if (!hasClub) {
            schwimmerTableView.getItems().clear();
            return;
        }
        try {
            SchwimmerDao dao = new SchwimmerDao(connection);
            List<Schwimmer> swimmers = getGender() == null
                    ? dao.findByVerein(club.getId())
                    : dao.findByVereinUndGeschlecht(club.getId(), getGender());
            schwimmerTableView.setItems(FXCollections.observableArrayList(swimmers));
        } catch (SQLException e) {
            showError("Die Schwimmerliste konnte nicht geladen werden.", e);
        }
    }

    /**
     * Opens an empty editor and persists a new swimmer after validation.
     */
    @FXML
    private void createSwimmer() {
        openEditor(null);
    }

    /**
     * Opens the editor with the selected swimmer's stored data.
     *
     * @param swimmer swimmer to edit
     */
    private void editSwimmer(Schwimmer swimmer) {
        openEditor(swimmer);
    }

    /**
     * Opens the shared swimmer editor for a swimmer selected from the club overview.
     *
     * @param swimmer swimmer to edit
     */
    public void editFromClub(Schwimmer swimmer) {
        try {
            disciplines = new DisziplinDao(connection).findAll();
            openEditor(swimmer);
        } catch (SQLException e) {
            showError("Die Disziplinen konnten nicht geladen werden.", e);
        }
    }

    /**
     * Displays the shared create/edit dialog and atomically saves the swimmer and best times.
     *
     * @param existing swimmer to edit, or {@code null} for a new swimmer
     */
    private void openEditor(Schwimmer existing) {
        Verein club = AppState.getSelectedClub();
        if (club == null) {
            return;
        }
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Schwimmer/in erstellen" : "Schwimmer/in bearbeiten");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        saveButton.setText("Speichern");

        TextField firstName = new TextField(existing == null ? "" : existing.getVorname());
        TextField lastName = new TextField(existing == null ? "" : existing.getName());
        DatePicker birthDate = new DatePicker(existing == null ? null : existing.getGeburtsdatum());
        ComboBox<String> gender = new ComboBox<>(FXCollections.observableArrayList("m", "w"));
        gender.setValue(existing == null ? getGender() : existing.getGeschlecht());
        gender.setDisable(existing != null);

        Map<Integer, Leistung> fastestTimes = new HashMap<>();
        if (existing != null) {
            try {
                new LeistungDao(connection).findBySchwimmer(existing.getId()).stream()
                        .sorted(Comparator.comparing(Leistung::getZeit))
                        .forEach(leistung -> fastestTimes.putIfAbsent(leistung.getDisziplinId(), leistung));
            } catch (SQLException e) {
                showError("Die Bestzeiten konnten nicht geladen werden.", e);
                return;
            }
        }
        Map<Disziplin, TimeField> timeFields = new HashMap<>();
        GridPane disciplineGrid = new GridPane();
        disciplineGrid.setHgap(8);
        disciplineGrid.setVgap(6);
        disciplineGrid.setPadding(new Insets(8));
        for (int column = 0; column < 3; column++) {
            final int group = column;
            List<Disziplin> groupDisciplines = disciplines.stream()
                    .filter(d -> disciplineGroup(d.getLage()) == group)
                    .sorted(Comparator.comparingInt((Disziplin d) -> disciplineOrder(d.getLage()))
                            .thenComparingInt(Disziplin::getDistanz))
                    .toList();
            VBox disciplineColumn = new VBox(8);
            disciplineColumn.getChildren().add(new Label(switch (group) {
                case 0 -> "Freistil";
                case 1 -> "Rücken / Brust";
                default -> "Schmetterling / Lagen";
            }));
            for (Disziplin discipline : groupDisciplines) {
                TimeField time = new TimeField();
                time.setPromptText("mm:ss,hh");
                time.setPrefColumnCount(8);
                Leistung fastest = fastestTimes.get(discipline.getId());
                if (fastest != null) {
                    String formattedTime = formatTime(fastest.getZeit());
                    time.setText(formattedTime);
                    time.setInputDigits(formattedTime.replaceAll("\\D", ""));
                }
                timeFields.put(discipline, time);
                disciplineColumn.getChildren().add(new VBox(3,
                        new Label(discipline.getDistanz() + "m " + discipline.getLage()), time));
            }
            disciplineGrid.add(disciplineColumn, column, 0);
        }
        GridPane fields = new GridPane();
        fields.setHgap(10);
        fields.setVgap(8);
        fields.addRow(0, new Label("Nachname"), lastName, new Label("Vorname"), firstName);
        fields.addRow(1, new Label("Geburtstag"), birthDate, new Label("Geschlecht"), gender);
        GridPane.setHgrow(lastName, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(firstName, javafx.scene.layout.Priority.ALWAYS);
        VBox bestzeitenBox = new VBox(8, new Label("Bestzeiten"), disciplineGrid);
        bestzeitenBox.setStyle("-fx-border-color: #b0b0b0; -fx-border-radius: 4; -fx-padding: 8;");
        VBox content = new VBox(14, fields, bestzeitenBox);
        content.setPadding(new Insets(12));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(760);
        dialog.getDialogPane().setPrefHeight(620);

        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                List<Leistung> bestzeiten = readBestzeiten(timeFields, fastestTimes);
                LocalDate date = birthDate.getValue();
                if (firstName.getText().isBlank() || lastName.getText().isBlank() || date == null
                        || gender.getValue() == null || date.isAfter(LocalDate.now())) {
                    throw new IllegalArgumentException("Vorname, Nachname, gültiges Geburtsdatum und Geschlecht sind Pflichtfelder.");
                }
                Schwimmer swimmer = existing == null ? new Schwimmer()
                        : new Schwimmer(existing.getId(), existing.getName(), existing.getVorname(),
                        existing.getGeburtsdatum(), existing.getGeschlecht(), existing.getVereinId());
                swimmer.setVorname(firstName.getText().trim());
                swimmer.setName(lastName.getText().trim());
                swimmer.setGeburtsdatum(date);
                swimmer.setVereinId(club.getId());
                if (existing == null) {
                    swimmer.setGeschlecht(gender.getValue());
                    if (isDuplicate(swimmer)) {
                        Alert duplicate = new Alert(Alert.AlertType.CONFIRMATION,
                                "Ein/e Schwimmer/in mit gleichem Namen und Geburtsdatum existiert bereits. Trotzdem speichern?",
                                ButtonType.YES, ButtonType.NO);
                        if (duplicate.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
                            event.consume();
                            return;
                        }
                    }
                }
                new SchwimmerService(connection).save(swimmer, bestzeiten);
                refresh();
            } catch (IllegalArgumentException e) {
                event.consume();
                markInvalidFields(firstName, lastName, birthDate, gender, timeFields);
                showValidation(e.getMessage());
            } catch (SQLException e) {
                event.consume();
                showError("Der/die Schwimmer/in konnte nicht gespeichert werden.", e);
            }
        });
        dialog.showAndWait();
    }

    /**
     * Converts changed time fields to best-time inserts or updates, preserving other history.
     *
     * @param timeFields one field per available discipline
     * @param fastestTimes fastest stored result for each discipline
     * @return newly entered or edited fastest results
     */
    private List<Leistung> readBestzeiten(Map<Disziplin, TimeField> timeFields,
                                          Map<Integer, Leistung> fastestTimes) {
        List<Leistung> result = new ArrayList<>();
        for (Map.Entry<Disziplin, TimeField> entry : timeFields.entrySet()) {
            String value = entry.getValue().getText().trim();
            if (value.isBlank()) {
                continue;
            }
            LocalTime parsedTime = parseTime(value);
            Leistung fastest = fastestTimes.get(entry.getKey().getId());
            if (fastest == null || !fastest.getZeit().equals(parsedTime)) {
                result.add(new Leistung(fastest == null ? 0 : fastest.getId(), 0,
                        entry.getKey().getId(), parsedTime, LocalDate.now()));
            }
        }
        return result;
    }

    /**
     * Parses supported minute/second or hour/minute/second time formats with hundredths.
     *
     * @param value user-entered time
     * @return parsed local time
     * @throws IllegalArgumentException if the time does not match the supported format
     */
    private LocalTime parseTime(String value) {
        Matcher matcher = Pattern.compile("^(?:(\\d{1,2}):)?(\\d{1,2}):(\\d{2}),(\\d{2})$").matcher(value);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Ungültiges Zeitformat. Erwartet wird mm:ss,hh oder hh:mm:ss,hh.");
        }
        try {
            int hours = matcher.group(1) == null ? 0 : Integer.parseInt(matcher.group(1));
            int minutes = Integer.parseInt(matcher.group(2));
            int seconds = Integer.parseInt(matcher.group(3));
            int hundredths = Integer.parseInt(matcher.group(4));
            if (minutes > 59 || seconds > 59) {
                throw new IllegalArgumentException("Minuten und Sekunden müssen zwischen 00 und 59 liegen.");
            }
            return LocalTime.of(hours, minutes, seconds, hundredths * 10_000_000);
        } catch (NumberFormatException | java.time.DateTimeException e) {
            throw new IllegalArgumentException("Die eingegebene Zeit ist ungültig.");
        }
    }

    /**
     * Formats a stored time using the hundredths convention displayed in the editor.
     *
     * @param time time to format
     * @return formatted time string
     */
    private String formatTime(LocalTime time) {
        String fraction = String.format("%02d", time.getNano() / 10_000_000);
        return time.getHour() == 0
                ? String.format("%02d:%02d,%s", time.getMinute(), time.getSecond(), fraction)
                : String.format("%02d:%02d:%02d,%s", time.getHour(), time.getMinute(), time.getSecond(), fraction);
    }

    /**
     * Checks for a likely duplicate within the active club.
     *
     * @param swimmer candidate swimmer
     * @return true when the same first name, last name, and birth date are already stored
     * @throws SQLException if the lookup fails
     */
    private boolean isDuplicate(Schwimmer swimmer) throws SQLException {
        String sql = "SELECT COUNT(*) FROM schwimmer WHERE verein_id = ? AND LOWER(vorname) = LOWER(?) "
                + "AND LOWER(name) = LOWER(?) AND geburtsdatum = ?";
        try (var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, swimmer.getVereinId());
            statement.setString(2, swimmer.getVorname().trim());
            statement.setString(3, swimmer.getName().trim());
            statement.setDate(4, java.sql.Date.valueOf(swimmer.getGeburtsdatum()));
            try (var result = statement.executeQuery()) {
                result.next();
                return result.getInt(1) > 0;
            }
        }
    }

    /**
     * Highlights missing or malformed values so validation feedback is visible in the form.
     *
     * @param firstName first-name input
     * @param lastName last-name input
     * @param birthDate birth-date input
     * @param gender gender input
     * @param timeFields best-time inputs
     */
    private void markInvalidFields(TextField firstName, TextField lastName, DatePicker birthDate,
                                   ComboBox<String> gender, Map<Disziplin, TimeField> timeFields) {
        firstName.setStyle(firstName.getText().isBlank() ? "-fx-border-color: #c62828;" : "");
        lastName.setStyle(lastName.getText().isBlank() ? "-fx-border-color: #c62828;" : "");
        birthDate.setStyle(birthDate.getValue() == null || birthDate.getValue().isAfter(LocalDate.now())
                ? "-fx-border-color: #c62828;" : "");
        gender.setStyle(gender.getValue() == null ? "-fx-border-color: #c62828;" : "");
        for (TimeField time : timeFields.values()) {
            boolean invalidTime = false;
            if (!time.getText().isBlank()) {
                try {
                    parseTime(time.getText().trim());
                } catch (IllegalArgumentException ignored) {
                    invalidTime = true;
                }
            }
            time.setStyle(invalidTime ? "-fx-border-color: #c62828;" : "");
        }
    }

    /**
     * Asks for confirmation and deletes the swimmer and dependent records.
     *
     * @param swimmer swimmer to delete
     */
    private void deleteSwimmer(Schwimmer swimmer) {
        try {
            int assignments = new SchwimmerDao(connection).countTeamAssignments(swimmer.getId());
            String message = "Schwimmer/in " + swimmer.getVorname() + " " + swimmer.getName() + " wirklich löschen?";
            if (assignments > 0) {
                message += "\nDer Eintrag ist noch " + assignments + " Mannschaftsstart(s) zugeordnet "
                        + "und wird dabei aus den Mannschaften entfernt.";
            }
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
            confirmation.setHeaderText("Schwimmer/in löschen");
            if (confirmation.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                new SchwimmerDao(connection).delete(swimmer.getId());
                refresh();
            }
        } catch (SQLException e) {
            showError("Der/die Schwimmer/in konnte nicht gelöscht werden.", e);
        }
    }

    /**
     * Maps swimming disciplines to the three requested visual groups.
     *
     * @param lage discipline stroke
     * @return group position
     */
    private int disciplineGroup(SchwimmLage lage) {
        return switch (lage) {
            case FREISTIL -> 0;
            case RUECKEN, BRUST -> 1;
            case SCHMETTERLING, LAGEN -> 2;
        };
    }

    /**
     * Returns the requested stroke order inside each best-time selection column.
     *
     * @param lage discipline stroke
     * @return order within its selection group
     */
    private int disciplineOrder(SchwimmLage lage) {
        return switch (lage) {
            case FREISTIL -> 0;
            case RUECKEN -> 1;
            case BRUST -> 2;
            case SCHMETTERLING -> 3;
            case LAGEN -> 4;
        };
    }

    /**
     * Returns a display-safe club name.
     *
     * @param club selected club
     * @return club name or empty text when no club is selected
     */
    private String clubName(Verein club) {
        return club == null ? "" : club.getName();
    }

    /**
     * Displays a validation message without closing the editor.
     *
     * @param message validation details
     */
    private void showValidation(String message) {
        new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).showAndWait();
    }

    /**
     * Displays a database error while retaining its technical cause in the alert details.
     *
     * @param message user-friendly operation description
     * @param error database exception
     */
    private void showError(String message, Exception error) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText("Datenbankfehler");
        alert.setContentText(message + "\n" + error.getMessage());
        alert.showAndWait();
    }

    /**
     * Time field that accumulates only typed digits, independently of its formatted display.
     */
    private static final class TimeField extends TextField {
        private String inputDigits = "";

        private TimeField() {
            setTextFormatter(new TextFormatter<String>(change -> {
                if (!change.isContentChange()) {
                    return change;
                }
                String insertedDigits = change.getText().replaceAll("\\D", "");
                boolean replacingWholeValue = change.getRangeStart() == 0
                        && change.getRangeEnd() == change.getControlText().length();
                if (!change.getText().isEmpty() && insertedDigits.isEmpty()) {
                    return null;
                }

                if (!insertedDigits.isEmpty()) {
                    inputDigits = replacingWholeValue ? insertedDigits : inputDigits + insertedDigits;
                    if (inputDigits.length() > 6) {
                        inputDigits = inputDigits.substring(0, 6);
                    }
                } else if (replacingWholeValue) {
                    inputDigits = "";
                } else if (!inputDigits.isEmpty()) {
                    inputDigits = inputDigits.substring(0, inputDigits.length() - 1);
                }

                if (inputDigits.isEmpty()) {
                    change.setRange(0, change.getControlText().length());
                    change.setText("");
                    change.selectRange(0, 0);
                    return change;
                }

                String digits = inputDigits + "0".repeat(6 - inputDigits.length());
                String formatted = digits.substring(0, 2) + ":" + digits.substring(2, 4)
                        + "," + digits.substring(4, 6);
                change.setRange(0, change.getControlText().length());
                change.setText(formatted);
                change.selectRange(formatted.length(), formatted.length());
                return change;
            }));
        }

        /**
         * Seeds the digit buffer when displaying a stored best time.
         *
         * @param digits six formatted time digits
         */
        private void setInputDigits(String digits) {
            inputDigits = digits;
        }
    }
}
