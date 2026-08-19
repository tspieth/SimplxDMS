package gui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;


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
}
