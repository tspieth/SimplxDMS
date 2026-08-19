package gui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class SwimmerController {
    @FXML
    private TableView<Object> schwimmerTableView; // Object -> später gegen dein Schwimmer-Model tauschen
    @FXML private TableColumn<Object, String> colSchwimmerName;
    @FXML private TableColumn<Object, String> colSchwimmerVerein;
    @FXML private TableColumn<Object, String> colSchwimmerJahrgang;

    @FXML private Button btnSchwimmerBearbeiten;
    @FXML private Button btnSchwimmerNeu;
    @FXML private Button btnSchwimmerEinlesen;

    @FXML
    public void initialize() {
        System.out.println("SchwimmerController geladen");
    }

    @FXML
    private void onSchwimmerBearbeitenClick() {
        Object ausgewaehlt = schwimmerTableView.getSelectionModel().getSelectedItem();
        if (ausgewaehlt != null) {
            System.out.println("Bearbeiten: " + ausgewaehlt);
        }
    }

    @FXML
    private void onSchwimmerNeuClick() {
        System.out.println("Neuen Schwimmer/Schwimmerin erstellen geklickt");
    }

    @FXML
    private void onSchwimmerEinlesenClick() {
        System.out.println("Datei mit Schwimmern einlesen geklickt");
    }
}
