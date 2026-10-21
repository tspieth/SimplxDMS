package gui.controller;

import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;

public class CompetitionController {
    @FXML private ListView<String> laeufeListView;
    @FXML private ListView<String> mannschaftListView;
    @FXML private Button btnMannschaftLoeschen;
    @FXML private Button btnWettkampfdateiEinlesen;

    @FXML
    public void initialize() {
        System.out.println("WettkampfController geladen");
    }

    @FXML
    private void onMannschaftLoeschenClick() {
        String ausgewaehlt = mannschaftListView.getSelectionModel().getSelectedItem();
        if (ausgewaehlt != null) {
            mannschaftListView.getItems().remove(ausgewaehlt);
        }
    }

    @FXML
    private void onWettkampfdateiEinlesenClick() {
        System.out.println("Wettkampfdatei einlesen geklickt");
    }

    @FXML
    private void onReferenzzeitenBearbeitenClick(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/fina-reference-times-view.fxml"));
            Parent view = loader.load();

            Window owner = ((Node) actionEvent.getSource()).getScene().getWindow();

            Stage stage = new Stage();
            stage.setTitle("FINA-Referenzzeiten bearbeiten");
            stage.initOwner(owner);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setScene(new Scene(view));
            stage.setMinWidth(700);
            stage.setMinHeight(600);

            // Erst nach dem Anzeigen kennt die Stage ihre echte Größe
            stage.setOnShown(e -> centerOnOwner(stage, owner));

            loader.<FinaReferenceTimeController>getController().setStage(stage);
            stage.showAndWait();
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR,
                    "Die Ansicht der FINA-Referenzzeiten konnte nicht geladen werden.\n" + e.getMessage(),
                    ButtonType.OK).showAndWait();
        }
    }

    private static void centerOnOwner(Stage stage, Window owner) {
        Runnable center = () -> {
            stage.setX(owner.getX() + (owner.getWidth() - stage.getWidth()) / 2);
            stage.setY(owner.getY() + (owner.getHeight() - stage.getHeight()) / 2);
        };

        // bei jeder Größenänderung neu zentrieren
        ChangeListener<Number> listener = (obs, o, n) -> center.run();
        stage.widthProperty().addListener(listener);
        stage.heightProperty().addListener(listener);

        center.run();

        // nach dem ersten Layout-Durchlauf die Listener wieder entfernen,
        // damit das Fenster nicht festklebt, wenn der Nutzer es später aufzieht
        Platform.runLater(() -> Platform.runLater(() -> {
            stage.widthProperty().removeListener(listener);
            stage.heightProperty().removeListener(listener);
            center.run();
        }));
    }
    /**
     * Sizes the reference-time window to its content and keeps it inside the usable screen area.
     *
     * @param stage reference-time window that has just been shown
     */
    private void sizeStageToFit(Stage stage) {
        stage.getScene().getRoot().applyCss();
        stage.getScene().getRoot().layout();
        stage.sizeToScene();

        Rectangle2D screenBounds = Screen.getScreensForRectangle(
                        stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight())
                .stream()
                .findFirst()
                .orElse(Screen.getPrimary())
                .getVisualBounds();
        double availableWidth = Math.max(1, screenBounds.getWidth() - 32);
        double availableHeight = Math.max(1, screenBounds.getHeight() - 48);
        stage.setMaxWidth(availableWidth);
        stage.setMaxHeight(availableHeight);
        stage.setWidth(Math.min(stage.getWidth(), availableWidth));
        stage.setHeight(Math.min(stage.getHeight(), availableHeight));
        stage.setX(screenBounds.getMinX() + (screenBounds.getWidth() - stage.getWidth()) / 2);
        stage.setY(screenBounds.getMinY() + (screenBounds.getHeight() - stage.getHeight()) / 2);
    }
}
