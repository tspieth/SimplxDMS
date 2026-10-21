package gui;

import database.model.Verein;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.function.Consumer;

/**
 * Holds application-wide state that must be shared between the independently loaded tabs.
 */
public final class AppState {
    public static final ObjectProperty<Verein> selectedVerein = new SimpleObjectProperty<>();
    private static Consumer<Verein> selectionListener = club -> { };

    static {
        selectedVerein.addListener((observable, oldClub, newClub) -> selectionListener.accept(newClub));
    }

    private AppState() {
    }

    /**
     * Returns the club currently selected by the user.
     *
     * @return the selected club, or {@code null} if no club is selected
     */
    public static Verein getSelectedClub() {
        return selectedVerein.get();
    }

    public static ObjectProperty<Verein> selectedVereinProperty() {
        return selectedVerein;
    }

    /**
     * Changes the globally selected club and notifies the main window.
     *
     * @param club selected club, or {@code null} to clear the selection
     */
    public static void setSelectedClub(Verein club) {
        selectedVerein.set(club);
    }

    /**
     * Registers the main window callback responsible for refreshing status and theme.
     *
     * @param listener callback invoked whenever the selected club changes
     */
    public static void setSelectionListener(Consumer<Verein> listener) {
        selectionListener = listener == null ? club -> { } : listener;
        selectionListener.accept(selectedVerein.get());
    }
}
