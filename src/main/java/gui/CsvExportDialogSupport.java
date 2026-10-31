package gui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.prefs.Preferences;

/** Shared save-dialog behavior for CSV exports. */
public final class CsvExportDialogSupport {
    private static final String LAST_EXPORT_DIRECTORY = "lastExportDirectory";
    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(CsvExportDialogSupport.class);

    private CsvExportDialogSupport() {
    }

    public static Path chooseTarget(Window owner, String title, String proposedName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV-Dateien", "*.csv", "*.CSV"),
                new FileChooser.ExtensionFilter("Alle Dateien", "*.*"));
        chooser.setInitialFileName(ensureCsvExtension(sanitizeFileName(proposedName)));
        initialDirectory().ifPresent(directory -> chooser.setInitialDirectory(directory.toFile()));

        File selected = chooser.showSaveDialog(owner);
        if (selected == null) {
            return null;
        }
        Path selectedPath = selected.toPath().toAbsolutePath().normalize();
        String cleanedName = ensureCsvExtension(sanitizeFileName(selectedPath.getFileName().toString()));
        Path target = selectedPath.resolveSibling(cleanedName);
        if (Files.exists(target) && !confirmOverwrite(owner, target)) {
            return null;
        }
        return target;
    }

    public static void rememberSuccessfulExport(Path target) {
        Path parent = target.toAbsolutePath().normalize().getParent();
        if (parent != null) {
            PREFERENCES.put(LAST_EXPORT_DIRECTORY, parent.toString());
        }
    }

    public static String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").strip().replace(' ', '_');
    }

    public static String ensureCsvExtension(String fileName) {
        return fileName.toLowerCase(Locale.ROOT).endsWith(".csv") ? fileName : fileName + ".csv";
    }

    private static java.util.Optional<Path> initialDirectory() {
        String lastDirectory = PREFERENCES.get(LAST_EXPORT_DIRECTORY, "");
        if (!lastDirectory.isBlank()) {
            Path directory = Path.of(lastDirectory);
            if (Files.isDirectory(directory)) {
                return java.util.Optional.of(directory);
            }
        }
        String userHome = System.getProperty("user.home");
        if (userHome != null && !userHome.isBlank()) {
            Path home = Path.of(userHome);
            if (Files.isDirectory(home)) {
                return java.util.Optional.of(home);
            }
        }
        return java.util.Optional.empty();
    }

    private static boolean confirmOverwrite(Window owner, Path target) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Die Datei „" + target.getFileName() + "“ ist bereits vorhanden. Soll sie ersetzt werden?",
                ButtonType.YES, ButtonType.NO);
        confirmation.setHeaderText("Datei überschreiben?");
        if (owner != null) {
            confirmation.initOwner(owner);
            confirmation.initModality(Modality.WINDOW_MODAL);
        }
        return confirmation.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }
}
