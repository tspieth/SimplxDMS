package gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import java.io.IOException;

public class MainController {

    @FXML private StackPane contentArea;
    @FXML private BorderPane mainRoot;
    @FXML private HBox tabBar;
    @FXML private Label selectedClubLabel;

    @FXML
    public void initialize() {
        AppState.setSelectionListener(this::updateSelectedClub);
        loadView("/gui/wettkampf-view.fxml");
    }

    @FXML private void onTabWettkampfClick() { loadView("/gui/wettkampf-view.fxml"); }
    @FXML private void onTabSchwimmerinnenClick() { loadView("/gui/schwimmerinnen-view.fxml"); }
    @FXML private void onTabSchwimmerClick() { loadView("/gui/schwimmer-view.fxml"); }
    @FXML private void onTabVereinClick() { loadView("/gui/verein-view.fxml"); }
    @FXML private void onTabEinstellungenClick() { loadView("/gui/einstellungen-view.fxml"); }

    /**
     * Reflects the globally selected club in the status bar and applies its UI color.
     *
     * @param club selected club, or {@code null} when no club is selected
     */
    private void updateSelectedClub(database.model.Verein club) {
        if (selectedClubLabel == null) {
            return;
        }
        if (club == null) {
            selectedClubLabel.setText("Kein Verein ausgewählt");
            selectedClubLabel.setStyle("-fx-background-color: #555555; -fx-text-fill: white;");
            mainRoot.setStyle("");
            tabBar.setStyle("-fx-background-color: #2b2b2b;");
        } else {
            selectedClubLabel.setText("Verein: " + club.getName());
            selectedClubLabel.setStyle("-fx-background-color: " + club.getFarbe()
                    + "; -fx-text-fill: white; -fx-padding: 8 12 8 12;");
            mainRoot.setStyle("-fx-accent: " + club.getFarbe() + ";");
            tabBar.setStyle("-fx-background-color: " + club.getFarbe() + ";");
        }
    }

    /**
     * Loads a tab view and reports FXML/resource errors to the user.
     *
     * @param fxmlPath classpath path to the tab resource
     */
    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR,
                    "Die Ansicht konnte nicht geladen werden: " + e.getMessage()).showAndWait();
        }
    }
}