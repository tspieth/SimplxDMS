package gui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;

public class ClubController {
    @FXML
    private ListView<String> vereinSchwimmerListView;
    @FXML private ListView<String> vereinAuswahlListView;
    @FXML private Button btnVereinNeu;
    @FXML private Button btnVereinBearbeiten;

    @FXML
    public void initialize() {
        System.out.println("VereinController geladen");
    }

    @FXML
    private void onVereinNeuClick() {
        System.out.println("Neuen Verein erstellen geklickt");
    }

    @FXML
    private void onVereinBearbeitenClick() {
        String ausgewaehlt = vereinAuswahlListView.getSelectionModel().getSelectedItem();
        if (ausgewaehlt != null) {
            System.out.println("Verein bearbeiten: " + ausgewaehlt);
        }
    }
}
