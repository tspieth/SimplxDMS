package gui.controller;

import database.DatabaseManager;
import database.dao.SchwimmerDao;
import database.dao.VereinDao;
import database.model.Schwimmer;
import database.model.Verein;
import gui.AppState;
import javafx.stage.FileChooser;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.scene.paint.Color;
import service.SchwimmerCsvImportService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages club selection and the overview of all swimmers belonging to that club.
 */
public class ClubController {
    @FXML private ListView<Schwimmer> vereinSchwimmerListView;
    @FXML private ListView<Verein> vereinAuswahlListView;
    @FXML private Button btnVereinBearbeiten;
    @FXML private Button btnSchwimmerAusDatei;
    @FXML private Label lblVereinSchwimmerHinweis;
    @FXML private Label lblVereineHinweis;

    private final Connection connection = DatabaseManager.getInstance().getConnection();

    /**
     * Loads clubs, connects selection changes to global application state, and renders rows.
     */
    @FXML
    public void initialize() {
        vereinAuswahlListView.setCellFactory(list -> new ListCell<>() {
            private final Button edit = new Button("Bearbeiten");
            private final Button delete = new Button("Löschen");
            private final HBox actions = new HBox(5, edit, delete);
            private final VBox content = new VBox(5);

            {
                edit.setOnAction(event -> editClub(getItem()));
                delete.setOnAction(event -> deleteClub(getItem()));
                content.getChildren().addAll(new Label(), actions);
            }

            @Override
            protected void updateItem(Verein club, boolean empty) {
                super.updateItem(club, empty);
                if (empty || club == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    ((Label) content.getChildren().get(0)).setText(club.getName());
                    setGraphic(content);
                    setText(null);
                }
            }
        });
        vereinAuswahlListView.getSelectionModel().selectedItemProperty().addListener((observable, oldClub, newClub) -> {
            AppState.setSelectedClub(newClub);
            refreshSwimmers();
            updateButtons();
        });
        vereinSchwimmerListView.setCellFactory(list -> new ListCell<>() {
            private final Button edit = new Button("Bearbeiten");
            private final Button delete = new Button("Löschen");
            private final HBox row = new HBox(8);

            {
                edit.setOnAction(event -> {
                    Schwimmer swimmer = getItem();
                    if (swimmer != null) {
                        new SwimmerController().editFromClub(swimmer);
                        refreshSwimmers();
                    }
                });
                delete.setOnAction(event -> deleteSwimmer(getItem()));
                row.setPadding(new Insets(3));
            }

            @Override
            protected void updateItem(Schwimmer swimmer, boolean empty) {
                super.updateItem(swimmer, empty);
                if (empty || swimmer == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label name = new Label(swimmer.getVorname() + " " + swimmer.getName()
                            + " (" + (swimmer.getGeburtsdatum() == null ? "" : swimmer.getGeburtsdatum().getYear()) + ")");
                    row.getChildren().setAll(name, edit, delete);
                    setGraphic(row);
                    setText(null);
                }
            }
        });
        vereinSchwimmerListView.setPlaceholder(new Label("Keine Schwimmer/innen vorhanden"));
        btnVereinBearbeiten.setDisable(true);
        btnVereinBearbeiten.setOnAction(event -> editClub(AppState.getSelectedClub()));
        btnSchwimmerAusDatei.setDisable(true);
        loadClubs();
        updateButtons();
    }

    /**
     * Loads all clubs in alphabetical order from persistent storage.
     */
    private void loadClubs() {
        try {
            List<Verein> clubs = new VereinDao(connection).findAll();
            vereinAuswahlListView.setItems(FXCollections.observableArrayList(clubs));
            lblVereineHinweis.setText(clubs.isEmpty() ? "Noch keine Vereine vorhanden." : "");
            Verein selected = AppState.getSelectedClub();
            if (selected != null) {
                clubs.stream().filter(club -> club.getId() == selected.getId()).findFirst()
                        .ifPresentOrElse(vereinAuswahlListView.getSelectionModel()::select,
                                () -> AppState.setSelectedClub(null));
            } else if (!clubs.isEmpty()) {
                vereinAuswahlListView.getSelectionModel().selectFirst();
            }
        } catch (SQLException e) {
            showError("Die Vereine konnten nicht geladen werden.", e);
        }
    }

    /**
     * Reloads the complete swimmer overview for the globally selected club.
     */
    private void refreshSwimmers() {
        Verein club = AppState.getSelectedClub();
        vereinSchwimmerListView.getItems().clear();
        if (club == null) {
            lblVereinSchwimmerHinweis.setText("Bitte zuerst einen Verein auswählen.");
            return;
        }
        try {
            vereinSchwimmerListView.setItems(FXCollections.observableArrayList(
                    new SchwimmerDao(connection).findByVerein(club.getId())));
            lblVereinSchwimmerHinweis.setText(
                    vereinSchwimmerListView.getItems().isEmpty() ? "Keine Schwimmer/innen vorhanden." : "");
        } catch (SQLException e) {
            showError("Die Schwimmerliste konnte nicht geladen werden.", e);
        }
    }

    /**
     * Opens a blank form for creating a club.
     */
    @FXML
    private void onVereinNeuClick() {
        editClub(null);
    }

    @FXML
    private void onSchwimmerAusDateiImportierenClick() {
        Verein club = AppState.getSelectedClub();
        if (club == null) {
            showValidation("Bitte zuerst einen Verein auswählen.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Schwimmer/innen-CSV auswählen");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV-Dateien", "*.csv", "*.CSV"),
                new FileChooser.ExtensionFilter("Alle Dateien", "*.*"));
        String userHome = System.getProperty("user.home");
        if (userHome != null && !userHome.isBlank()) {
            Path home = Path.of(userHome);
            if (Files.isDirectory(home)) {
                chooser.setInitialDirectory(home.toFile());
            }
        }
        java.io.File selectedFile = chooser.showOpenDialog(btnSchwimmerAusDatei.getScene().getWindow());
        if (selectedFile == null) {
            return;
        }

        SchwimmerCsvImportService importService = new SchwimmerCsvImportService(connection);
        try {
            List<SchwimmerCsvImportService.ImportSchwimmer> imports =
                    importService.parse(selectedFile.toPath());
            List<Schwimmer> existingSwimmers = new SchwimmerDao(connection).findByVerein(club.getId());
            Map<String, Schwimmer> existingByName = new HashMap<>();
            for (Schwimmer swimmer : existingSwimmers) {
                existingByName.putIfAbsent(
                        SchwimmerCsvImportService.duplicateKey(swimmer.getVorname(), swimmer.getName()), swimmer);
            }

            Set<Integer> acceptedExistingIds = new HashSet<>();
            Set<Integer> declinedExistingIds = new HashSet<>();
            for (SchwimmerCsvImportService.ImportSchwimmer imported : imports) {
                Schwimmer existing = existingByName.get(imported.duplicateKey());
                if (existing == null || acceptedExistingIds.contains(existing.getId())
                        || declinedExistingIds.contains(existing.getId())) {
                    continue;
                }
                Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                        "„" + existing.getVorname() + " " + existing.getName() + "\" ist diesem Verein bereits "
                                + "zugeordnet. Sollen die in der Datei enthaltenen Zeiten zu diesem Schwimmer "
                                + "hinzugefügt werden?",
                        ButtonType.YES, ButtonType.NO);
                confirmation.setHeaderText("Schwimmer/in bereits vorhanden");
                if (confirmation.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                    acceptedExistingIds.add(existing.getId());
                } else {
                    declinedExistingIds.add(existing.getId());
                }
            }

            SchwimmerCsvImportService.ImportErgebnis result =
                    importService.importSwimmers(club, imports, acceptedExistingIds);
            refreshSwimmers();
            showInfo("CSV-Import abgeschlossen.\n"
                    + "Neue Schwimmer/innen: " + result.neueSchwimmer() + "\n"
                    + "Ergänzte vorhandene Schwimmer/innen: " + result.ergaenzteSchwimmer() + "\n"
                    + "Übersprungene Dubletten: " + result.uebersprungeneDubletten() + "\n"
                    + "Gespeicherte Zeiten: " + result.gespeicherteLeistungen());
        } catch (IOException e) {
            showValidation("Die CSV-Datei ist ungültig oder konnte nicht gelesen werden:\n" + e.getMessage());
        } catch (SQLException e) {
            showError("Die Schwimmer/innen und Zeiten konnten nicht gespeichert werden.", e);
        }
    }

    /**
     * Opens a club form and saves name/color changes immediately.
     *
     * @param existing club to update, or {@code null} for a new club
     */
    private void editClub(Verein existing) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Verein erstellen" : "Verein bearbeiten");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        Button save = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        save.setText("Speichern");
        TextField name = new TextField(existing == null ? "" : existing.getName());
        ColorPicker colorPicker = new ColorPicker(parseColor(existing == null ? "#1E90FF" : existing.getFarbe()));
        GridPane fields = new GridPane();
        fields.setHgap(10);
        fields.setVgap(10);
        fields.addRow(0, new Label("Vereinsname"), name);
        fields.addRow(1, new Label("Vereinsfarbe"), colorPicker);
        fields.setPadding(new Insets(12));
        dialog.getDialogPane().setContent(fields);

        save.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String trimmedName = name.getText().trim();
            if (trimmedName.isEmpty()) {
                event.consume();
                showValidation("Der Vereinsname darf nicht leer sein.");
                return;
            }
            try {
                Verein club = existing == null ? new Verein() : existing;
                club.setName(trimmedName);
                club.setFarbe(toHex(colorPicker.getValue()));
                if (existing == null) {
                    new VereinDao(connection).insert(club);
                    AppState.setSelectedClub(club);
                } else {
                    new VereinDao(connection).update(club);
                }
                loadClubs();
                if (existing != null && AppState.getSelectedClub() != null
                        && AppState.getSelectedClub().getId() == club.getId()) {
                    AppState.setSelectedClub(club);
                }
            } catch (SQLException e) {
                event.consume();
                showError("Der Verein konnte nicht gespeichert werden. Prüfe, ob der Name bereits vergeben ist.", e);
            }
        });
        dialog.showAndWait();
    }

    /**
     * Deletes a club and its dependent swimmers, teams, starts, and best times after confirmation.
     *
     * @param club club selected for deletion
     */
    private void deleteClub(Verein club) {
        if (club == null) {
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Verein \"" + club.getName() + "\" wirklich löschen?\n"
                        + "Alle zugehörigen Schwimmer/innen, Bestzeiten, Mannschaften und Starts werden entfernt.",
                ButtonType.YES, ButtonType.NO);
        confirmation.setHeaderText("Verein löschen");
        if (confirmation.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }
        try {
            new VereinDao(connection).delete(club.getId());
            if (AppState.getSelectedClub() != null && AppState.getSelectedClub().getId() == club.getId()) {
                AppState.setSelectedClub(null);
            }
            loadClubs();
            refreshSwimmers();
        } catch (SQLException e) {
            showError("Der Verein konnte nicht gelöscht werden.", e);
        }
    }

    /**
     * Confirms and deletes a swimmer from the club overview, including team assignments.
     *
     * @param swimmer swimmer to remove
     */
    private void deleteSwimmer(Schwimmer swimmer) {
        if (swimmer == null) {
            return;
        }
        try {
            int assignments = new SchwimmerDao(connection).countTeamAssignments(swimmer.getId());
            String message = "Schwimmer/in " + swimmer.getVorname() + " " + swimmer.getName() + " wirklich löschen?";
            if (assignments > 0) {
                message += "\nDer Eintrag ist noch " + assignments + " Mannschaftsstart(s) zugeordnet "
                        + "und wird beim Löschen aus diesen Mannschaften entfernt.";
            }
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
            confirmation.setHeaderText("Schwimmer/in löschen");
            if (confirmation.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                new SchwimmerDao(connection).delete(swimmer.getId());
                refreshSwimmers();
            }
        } catch (SQLException e) {
            showError("Der/die Schwimmer/in konnte nicht gelöscht werden.", e);
        }
    }

    /**
     * Updates enabled controls after a selection change.
     */
    private void updateButtons() {
        boolean noClubSelected = AppState.getSelectedClub() == null;
        btnVereinBearbeiten.setDisable(noClubSelected);
        btnSchwimmerAusDatei.setDisable(noClubSelected);
    }

    /**
     * Converts a stored hex color into a JavaFX color value.
     *
     * @param value stored color string
     * @return parsed color or the default blue if the value is invalid
     */
    private Color parseColor(String value) {
        try {
            return Color.web(value);
        } catch (IllegalArgumentException e) {
            return Color.web("#1E90FF");
        }
    }

    /**
     * Formats a JavaFX color as a database-friendly six-digit hex value.
     *
     * @param color selected JavaFX color
     * @return hex color string
     */
    private String toHex(Color color) {
        return String.format("#%02X%02X%02X", (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255), (int) Math.round(color.getBlue() * 255));
    }

    /**
     * Displays validation feedback for the open club form.
     *
     * @param message validation explanation
     */
    private void showValidation(String message) {
        new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).showAndWait();
    }

    /**
     * Displays a readable database failure and preserves the existing list state.
     *
     * @param message user-friendly operation description
     * @param error database exception
     */
    private void showError(String message, Exception error) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message + "\n" + error.getMessage(), ButtonType.OK);
        alert.setHeaderText("Datenbankfehler");
        alert.showAndWait();
    }

    private void showInfo(String message) {
        new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK).showAndWait();
    }
}
