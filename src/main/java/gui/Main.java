package gui;

import database.DatabaseManager;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import service.StartupService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        StringBuilder startupImportErrors = new StringBuilder();
        StartupService startupService = new StartupService(DatabaseManager.getInstance().getConnection());
        try {
            startupService.importBundledReferenceTimesIfMissing();
        } catch (Exception e) {
            startupImportErrors.append("Die Referenzzeiten konnten nicht geladen werden.\n")
                    .append(e.getMessage());
        }
        try {
            startupService.importBundledIfEmpty();
        } catch (Exception e) {
            if (!startupImportErrors.isEmpty()) {
                startupImportErrors.append("\n\n");
            }
            startupImportErrors.append("Der mitgelieferte Wettkampf konnte nicht importiert werden.\n")
                    .append(e.getMessage());
        }
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/main-view.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1200, 720);
        primaryStage.setTitle("SimplxDMS");
        primaryStage.setScene(scene);
        primaryStage.show();
        if (!startupImportErrors.isEmpty()) {
            new Alert(Alert.AlertType.ERROR, startupImportErrors.toString(), ButtonType.OK).showAndWait();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}