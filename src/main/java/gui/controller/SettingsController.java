package gui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

public class SettingsController {

    @FXML
    private ComboBox<String> cbSprache;
    @FXML private CheckBox cbDarkMode;
    @FXML private TextField tfDatenbankpfad;

    @FXML
    public void initialize() {
        cbSprache.getItems().addAll("Deutsch", "English");
        cbSprache.getSelectionModel().selectFirst();
    }

    @FXML
    private void onDatenbankpfadWaehlenClick() {
        System.out.println("Datenbankpfad auswählen geklickt");
    }

    @FXML
    private void onEinstellungenSpeichernClick() {
        System.out.println("Einstellungen gespeichert");
    }
}
