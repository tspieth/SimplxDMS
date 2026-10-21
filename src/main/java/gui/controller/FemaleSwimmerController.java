package gui.controller;

/**
 * Shows and edits only female swimmers in the Schwimmerinnen tab.
 */
public class FemaleSwimmerController extends SwimmerController {
    /**
     * Identifies the female swimmer tab filter.
     *
     * @return database gender value
     */
    @Override
    protected String getGender() {
        return "w";
    }
}
