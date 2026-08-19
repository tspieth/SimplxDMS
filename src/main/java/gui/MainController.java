package gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import java.io.IOException;

public class MainController {

    @FXML private StackPane contentArea;

    @FXML
    public void initialize() {
        loadView("/gui/wettkampf-view.fxml");
    }

    @FXML private void onTabWettkampfClick() { loadView("/gui/wettkampf-view.fxml"); }
    @FXML private void onTabSchwimmerinnenClick() { loadView("/gui/schwimmerinnen-view.fxml"); }
    @FXML private void onTabSchwimmerClick() { loadView("/gui/schwimmer-view.fxml"); }
    @FXML private void onTabVereinClick() { loadView("/gui/verein-view.fxml"); }
    @FXML private void onTabEinstellungenClick() { loadView("/gui/einstellungen-view.fxml"); }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}