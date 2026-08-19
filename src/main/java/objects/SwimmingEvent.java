package objects;

/**
 * Die Klasse Event repräsentiert die verschiedenen Schwimmevents, die in der
 * DMS stattfinden.
 * <p>
 * Jedes Event hat einen eindeutigen Index und einen Anzeigenamen.
 * </p>
 * 
 * @author Timon Spieth
 * @version 1.0
 * @since 2026-02-10
 * 
 */
public enum SwimmingEvent {
    FREESTYLE_50(0, "50K", 2),
    FREESTYLE_100(1, "100K", 3),
    FREESTYLE_200(2, "200K", 5),
    FREESTYLE_400(3, "400K", 10),
    FREESTYLE_800(4, "800K", 20),
    FREESTYLE_1500(5, "1500K", 37),

    BACKSTROKE_50(6, "50R", 2),
    BACKSTROKE_100(7, "100R", 3),
    BACKSTROKE_200(8, "200R", 6),

    BREASTSTROKE_50(9, "50B", 2),
    BREASTSTROKE_100(10, "100B", 3),
    BREASTSTROKE_200(11, "200B", 7),

    BUTTERFLY_50(12, "50S", 2),
    BUTTERFLY_100(13, "100S", 3),
    BUTTERFLY_200(14, "200S", 6),

    MEDLEY_100(15, "100Lg", 3),
    MEDLEY_200(16, "200Lg", 6),
    MEDLEY_400(17, "400Lg", 10);

    private final int index;
    private final String displayName;
    private final int typicalDuration;;

    // typicalDuration is an estimate of how long the event takes
    // in minutes based on DMS 2023 (2. Bundesliga) protocol where every event had 2
    // heats
    SwimmingEvent(int index, String displayName, int typicalDuration) {
        this.index = index;
        this.displayName = displayName;
        this.typicalDuration = typicalDuration;
    }

    public int getIndex() {
        return index;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getTypicalDuration() {
        return typicalDuration;
    }

    public static SwimmingEvent getByDisplayName(String displayName) {
        for (SwimmingEvent event : values()) {
            if (event.displayName.equals(displayName)) {
                return event;
            }
        }
        throw new IllegalArgumentException("No event with display name " + displayName);
    }

    public int getMinimumBreakTime() {
        int distance = extractDistance();

        if (distance <= 50)
            return 15;
        if (distance <= 100)
            return 20;
        if (distance <= 200)
            return 30;
        if (distance <= 400)
            return 45;
        return 60; // 800 & 1500
    }

    private int extractDistance() {
        // liest führende Zahl aus "50K", "200Lg", etc.
        String digits = displayName.replaceAll("\\D.*", "");
        return Integer.parseInt(digits);
    }
}