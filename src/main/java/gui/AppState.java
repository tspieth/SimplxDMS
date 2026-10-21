package gui;

import database.model.Verein;

import java.util.function.Consumer;

/**
 * Holds application-wide state that must be shared between the independently loaded tabs.
 */
public final class AppState {
    private static Verein selectedClub;
    private static Consumer<Verein> selectionListener = club -> { };

    private AppState() {
    }

    /**
     * Returns the club currently selected by the user.
     *
     * @return the selected club, or {@code null} if no club is selected
     */
    public static Verein getSelectedClub() {
        return selectedClub;
    }

    /**
     * Changes the globally selected club and notifies the main window.
     *
     * @param club selected club, or {@code null} to clear the selection
     */
    public static void setSelectedClub(Verein club) {
        selectedClub = club;
        selectionListener.accept(club);
    }

    /**
     * Registers the main window callback responsible for refreshing status and theme.
     *
     * @param listener callback invoked whenever the selected club changes
     */
    public static void setSelectionListener(Consumer<Verein> listener) {
        selectionListener = listener == null ? club -> { } : listener;
        selectionListener.accept(selectedClub);
    }
}
