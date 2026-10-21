package gui.controller;

import database.DatabaseManager;
import database.model.Geschlecht;
import database.model.Mannschaft;
import database.model.Verein;
import database.model.Wettkampf;
import gui.AppState;
import linearProgramming.LinearProgramming;
import javafx.collections.FXCollections;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import service.WettkampfAnsichtService;
import service.WettkampfImportService;
import service.WettkampfService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Shows a read-only competition schedule and team assignments.
 */
public class CompetitionController {
    @FXML private ComboBox<Wettkampf> wettkampfComboBox;
    @FXML private Label lblWettkampfHinweis;
    @FXML private Button btnWettkampfLoeschen;
    @FXML private ListView<Mannschaft> mannschaftListView;
    @FXML private Button btnMannschaftLoeschen;
    @FXML private Spinner<Integer> spinnerPauseGap;
    @FXML private ToggleButton toggleMaennlich;
    @FXML private ToggleButton toggleWeiblich;
    @FXML private ToggleGroup genderToggleGroup;
    @FXML private GridPane gridLaeufe;
    @FXML private Label lblMannschaftspunkte;

    private final Connection connection = DatabaseManager.getInstance().getConnection();
    private final WettkampfService wettkampfService = new WettkampfService(connection);
    private final WettkampfAnsichtService ansichtService = new WettkampfAnsichtService(connection);
    private Geschlecht selectedGender = Geschlecht.MAENNLICH;
    private final ChangeListener<Verein> selectedClubListener =
            (observable, oldClub, newClub) -> {
                refreshTeams(true);
                refreshGrid();
            };

    @FXML
    public void initialize() {
        wettkampfComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            refreshTeams(true);
            refreshGrid();
        });
        mannschaftListView.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> refreshGrid());
        mannschaftListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Mannschaft team, boolean empty) {
                super.updateItem(team, empty);
                Verein club = AppState.selectedVereinProperty().get();
                setText(empty || team == null ? null
                        : (club == null ? "" : club.getName() + " – ") + team.getBezeichnung());
            }
        });
        mannschaftListView.setPlaceholder(new Label("Kein Verein ausgewählt"));
        toggleMaennlich.setSelected(true);
        toggleMaennlich.setOnAction(event -> chooseGender(toggleMaennlich, Geschlecht.MAENNLICH));
        toggleWeiblich.setOnAction(event -> chooseGender(toggleWeiblich, Geschlecht.WEIBLICH));
        btnMannschaftLoeschen.setDisable(true);
        btnWettkampfLoeschen.setDisable(true);
        spinnerPauseGap.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 60, 2));
        spinnerPauseGap.setEditable(true);
        mannschaftListView.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> btnMannschaftLoeschen.setDisable(newValue == null));
        AppState.selectedVerein.addListener(selectedClubListener);
        gridLaeufe.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene == null) {
                AppState.selectedVerein.removeListener(selectedClubListener);
            }
        });
        loadCompetitions(true);
    }

    @FXML
    private void onMannschaftBerechnenClick() {
        Verein club = AppState.selectedVereinProperty().get();
        Wettkampf competition = wettkampfComboBox.getValue();
        if (club == null || competition == null) {
            showError("Bitte zuerst einen Verein und einen Wettkampf auswählen.");
            return;
        }
        try {
            spinnerPauseGap.commitValue();
            int pauseGap = spinnerPauseGap.getValue();
            if (pauseGap < 0) {
                throw new IllegalArgumentException("Die Pausenlänge darf nicht negativ sein.");
            }
            String result = LinearProgramming.toStringsimplexXBnB(connection, club, competition,
                    selectedGender == Geschlecht.MAENNLICH, pauseGap);
            refreshTeams(false);
            String teamNamePrefix = "Mannschaft \"";
            int nameStart = result.indexOf(teamNamePrefix);
            int nameEnd = result.indexOf("\" gespeichert.", nameStart + teamNamePrefix.length());
            if (nameStart >= 0 && nameEnd > nameStart) {
                String teamName = result.substring(nameStart + teamNamePrefix.length(), nameEnd);
                mannschaftListView.getItems().stream()
                        .filter(team -> team.getBezeichnung().equals(teamName))
                        .findFirst()
                        .ifPresent(team -> mannschaftListView.getSelectionModel().select(team));
            }
            showInfo(result);
        } catch (SQLException | IllegalArgumentException | IllegalStateException e) {
            showError("Die Mannschaft konnte nicht berechnet und gespeichert werden.", e);
        }
    }

    private void chooseGender(ToggleButton selectedButton, Geschlecht gender) {
        if (!selectedButton.isSelected()) {
            selectedButton.setSelected(true);
            return;
        }
        selectedGender = gender;
        refreshGrid();
    }

    private void loadCompetitions(boolean preferBundledCompetition) {
        try {
            List<Wettkampf> competitions = wettkampfService.ladeWettkaempfe();
            wettkampfComboBox.setItems(FXCollections.observableArrayList(competitions));
            btnWettkampfLoeschen.setDisable(competitions.isEmpty());
            lblWettkampfHinweis.setVisible(competitions.isEmpty());
            lblWettkampfHinweis.setManaged(competitions.isEmpty());
            if (competitions.isEmpty()) {
                refreshTeams(false);
                refreshGrid();
                return;
            }
            Wettkampf initial = preferBundledCompetition
                    ? competitions.stream().filter(wettkampf -> wettkampf.getName().equals("DMS2025"))
                    .findFirst().orElse(competitions.get(0))
                    : competitions.get(0);
            wettkampfComboBox.getSelectionModel().select(initial);
        } catch (SQLException e) {
            showError("Die Wettkämpfe konnten nicht geladen werden.", e);
        }
    }

    private void refreshTeams(boolean selectFirst) {
        mannschaftListView.getItems().clear();
        Wettkampf competition = wettkampfComboBox.getValue();
        Verein club = AppState.selectedVereinProperty().get();
        if (club == null) {
            mannschaftListView.setPlaceholder(new Label("Kein Verein ausgewählt"));
            return;
        }
        if (competition == null) {
            mannschaftListView.setPlaceholder(new Label("Kein Wettkampf vorhanden"));
            return;
        }
        try {
            List<Mannschaft> teams = wettkampfService.ladeMannschaften(competition.getId(), club.getId());
            mannschaftListView.setItems(FXCollections.observableArrayList(teams));
            mannschaftListView.setPlaceholder(new Label("Keine Mannschaften"));
            if (selectFirst && !teams.isEmpty()) {
                mannschaftListView.getSelectionModel().selectFirst();
            }
        } catch (SQLException e) {
            showError("Die Mannschaften konnten nicht geladen werden.", e);
            mannschaftListView.setPlaceholder(new Label("Mannschaften konnten nicht geladen werden"));
        }
    }

    private void refreshGrid() {
        gridLaeufe.getChildren().clear();
        gridLaeufe.getColumnConstraints().clear();
        Wettkampf competition = wettkampfComboBox.getValue();
        Mannschaft team = mannschaftListView.getSelectionModel().getSelectedItem();
        if (competition == null) {
            lblMannschaftspunkte.setText("Mannschaftspunkte: –");
            return;
        }
        try {
            WettkampfAnsichtService.Ansicht view = ansichtService.lade(competition.getId(), team, selectedGender);
            int columnCount = view.abschnitte().size() * 2;
            for (int column = 0; column < columnCount; column++) {
                ColumnConstraints constraints = new ColumnConstraints();
                constraints.setHgrow(Priority.ALWAYS);
                constraints.setMinWidth(130);
                gridLaeufe.getColumnConstraints().add(constraints);
            }
            for (int sectionIndex = 0; sectionIndex < view.abschnitte().size(); sectionIndex++) {
                int section = view.abschnitte().get(sectionIndex);
                int firstColumn = sectionIndex * 2;
                Label heading = new Label("Abschnitt " + section);
                heading.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                gridLaeufe.add(heading, firstColumn, 0, 2, 1);
                List<WettkampfAnsichtService.LaufZeile> events = view.laeufe().stream()
                        .filter(run -> run.abschnitt() == section)
                        .sorted(Comparator.comparingInt(WettkampfAnsichtService.LaufZeile::wettkampfNummer))
                        .toList();
                for (int row = 0; row < events.size(); row++) {
                    WettkampfAnsichtService.LaufZeile event = events.get(row);
                    Label eventLabel = new Label("WK" + event.wettkampfNummer() + " · "
                            + event.distanz() + " m " + event.lage());
                    Label assignment = new Label(event.zuordnung());
                    if (event.tooltip() != null) {
                        assignment.setTooltip(new Tooltip(event.tooltip()));
                    }
                    gridLaeufe.add(eventLabel, firstColumn, row + 1);
                    gridLaeufe.add(assignment, firstColumn + 1, row + 1);
                    GridPane.setMargin(eventLabel, new Insets(2, 4, 2, 0));
                }
            }
            lblMannschaftspunkte.setText(view.mannschaftspunkte() == null
                    ? "Mannschaftspunkte: –" : "Mannschaftspunkte: " + view.mannschaftspunkte());
        } catch (SQLException e) {
            lblMannschaftspunkte.setText("Mannschaftspunkte: –");
            showError("Der Wettkampfablauf konnte nicht geladen werden.", e);
        }
    }

    @FXML
    private void onWettkampfdateiEinlesenClick(javafx.event.ActionEvent actionEvent) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Wettkampfdatei auswählen");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("DSV7- und Textdateien", "*.dsv7", "*.DSV7", "*.txt", "*.TXT"),
                new FileChooser.ExtensionFilter("Alle Dateien", "*.*"));
        String userHome = System.getProperty("user.home");
        if (userHome != null && !userHome.isBlank()) {
            Path home = Path.of(userHome);
            if (Files.isDirectory(home)) {
                chooser.setInitialDirectory(home.toFile());
            }
        }
        Window owner = ((Node) actionEvent.getSource()).getScene().getWindow();
        java.io.File selected = chooser.showOpenDialog(owner);
        if (selected == null) {
            return;
        }
        Path path = selected.toPath();
        String filename = path.getFileName().toString();
        int extension = filename.lastIndexOf('.');
        String name = extension > 0 ? filename.substring(0, extension) : filename;
        try {
            WettkampfImportService importService = new WettkampfImportService(connection);
            if (importService.wettkampfNameExistiert(name)) {
                showError("Ein Wettkampf mit diesem Namen existiert bereits.");
                return;
            }
            byte[] bytes = Files.readAllBytes(path);
            WettkampfImportService.ImportErgebnis result = importService.importiere(name, bytes);
            loadCompetitions(false);
            wettkampfComboBox.getItems().stream()
                    .filter(wettkampf -> Objects.equals(wettkampf.getName(), result.wettkampfName()))
                    .findFirst().ifPresent(wettkampfComboBox.getSelectionModel()::select);
            showInfo("Wettkampf \"" + result.wettkampfName() + "\" wurde importiert.\n"
                    + "Läufe: " + result.anzahlLaeufe() + "\nPausen: " + result.anzahlPausen()
                    + "\nÜbersprungene Nicht-Einzelwettkämpfe: " + result.uebersprungeneNichtEinzel());
        } catch (importer.DsvParseException e) {
            showError("Die Wettkampfdatei ist fehlerhaft.\n" + e.getMessage(), e);
        } catch (SQLException e) {
            String message = e.getMessage() != null && e.getMessage().contains("existiert bereits")
                    ? "Ein Wettkampf mit diesem Namen existiert bereits."
                    : "Der Wettkampf konnte nicht gespeichert werden.";
            showError(message, e);
        } catch (IOException e) {
            showError("Die Wettkampfdatei konnte nicht gelesen werden.", e);
        }
    }

    @FXML
    private void onMannschaftLoeschenClick() {
        Mannschaft team = mannschaftListView.getSelectionModel().getSelectedItem();
        if (team == null) {
            return;
        }
        try {
            long starts = wettkampfService.anzahlStarts(team);
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                    "Mannschaft \"" + team.getBezeichnung() + "\" und " + starts
                            + " Zuordnung(en) wirklich löschen?", ButtonType.YES, ButtonType.NO);
            confirmation.setHeaderText("Mannschaft löschen");
            if (confirmation.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
                return;
            }
            wettkampfService.loescheMannschaft(team);
            refreshTeams(false);
            mannschaftListView.getSelectionModel().clearSelection();
            refreshGrid();
        } catch (SQLException e) {
            showError("Die Mannschaft konnte nicht gelöscht werden.", e);
        }
    }

    @FXML
    private void onWettkampfLoeschenClick() {
        Wettkampf competition = wettkampfComboBox.getValue();
        if (competition == null) {
            return;
        }
        try {
            WettkampfService.LoeschInfo info = wettkampfService.loeschInfo(competition);
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                    "Wettkampf \"" + competition.getName() + "\" wirklich löschen?\n"
                            + "Gelöscht werden " + info.laeufe() + " Läufe und "
                            + info.mannschaften() + " Mannschaften samt Zuordnungen.",
                    ButtonType.YES, ButtonType.NO);
            confirmation.setHeaderText("Wettkampf löschen");
            if (confirmation.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
                return;
            }
            wettkampfService.loescheWettkampf(competition);
            loadCompetitions(false);
        } catch (SQLException e) {
            showError("Der Wettkampf konnte nicht gelöscht werden.", e);
        }
    }

    @FXML
    private void onReferenzzeitenBearbeitenClick(javafx.event.ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/fina-reference-times-view.fxml"));
            Parent view = loader.load();
            Window owner = ((Node) actionEvent.getSource()).getScene().getWindow();
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("FINA-Referenzzeiten bearbeiten");
            stage.initOwner(owner);
            stage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            stage.setScene(new javafx.scene.Scene(view));
            stage.setMinWidth(700);
            stage.setMinHeight(600);
            loader.<FinaReferenceTimeController>getController().setStage(stage);
            stage.showAndWait();
            refreshGrid();
        } catch (IOException e) {
            showError("Die FINA-Referenzzeiten konnten nicht geöffnet werden.", e);
        }
    }

    private void showInfo(String message) {
        new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK).showAndWait();
    }

    private void showError(String message, Exception error) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message + "\n" + error.getMessage(), ButtonType.OK);
        alert.setHeaderText("Fehler");
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText("Fehler");
        alert.showAndWait();
    }
}
