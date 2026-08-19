package objects;

public class Competition {

    /*
     * League mapping:
     * 0 - 1. Bundesliga
     * 1 - 2. Bundesliga
     * 2 - Oberliga
     * 3 - Regionaliga
     * 4 - Bezirksliga
     * 
     */
    public static final int BREAK_MARKER = -1;
    public static final int BREAK_CODE = 60;

    public static int eventCount = SwimmingEvent.values().length; // total number of events in the competition

    public static double[] baseTimesMale; // contains the base times for each event, used to calculate points
    public static double[] baseTimesFemale; // contains the base times for each event, used to calculate points
    public static int league; // Maybe used to determine typicalDuration of Events and Breaks

    public static int maxEventsPerSwimmer = 5; // maximum number of events a swimmer can choose

    public static int[][] order; // orders events by time, the first eventIndex in the order is the one that
                                 // takes place first, and so on
                                 // order[i][0] gives eventIndex
                                 // order[i][1] gives the approximate time the event takes
                                 // if there is a break order[i][0] is -1 and
                                 // order[i][1] is the duration of the break

    public static int[][] orderMale; // same but orderMale[i][0] gives -2 if there is a female event
                                     // in this case orderMale[i][1] gives the duration of the event
    public static int[][] orderFemale; // same but orderMale[i][0] gives -2 if there is a female event
                                       // in this case orderMale[i][1] gives the duration of the event
    public static int[] simpleOrder;
    public static int[] simpleOrderFemale;
    public static int[] simpleOrderMale;

    /**
     * Diese Methode Berechnet die Punkte basierend auf den BaseTimes
     * 
     * Damit die Berechnung korrekt ist muss baseTimesMale[]/baseTimesFemale
     * mit den korrekten Zeiten initialisiert sein
     * 
     * @param event  das Event
     * @param time   die Zeit vom Sportler in Sekunden
     * @param isMale true, wenn der Sportler maennlich ist
     * @return die berechneten Punkte
     */
    public static int calculatePoints(SwimmingEvent event, double time, boolean isMale) {
        // calculates points for a given event and time using the formula:
        // points = (baseTime / time) * 1000
        // where baseTime is the base time for the event in the current league
        int eventIndex = event.getIndex();
        double baseTime = isMale ? baseTimesMale[eventIndex] : baseTimesFemale[eventIndex];
        return (int) (Math.pow(baseTime / time, 3) * 1000); // points are truncated to an integer
    }

    // ================================================
    // Setter
    // ================================================

    public static boolean setBaseTimesMale(double[] baseTimes) {
        if (baseTimes.length != eventCount) {
            return false; // base times array must have 18 elements, one for each event
        }
        baseTimesMale = baseTimes;
        return true;
    }

    public static boolean setBaseTimesFemale(double[] baseTimes) {
        if (baseTimes.length != eventCount) {
            return false; // base times array must have 18 elements, one for each event
        }
        baseTimesFemale = baseTimes;
        return true;
    }

    public static void setOrder(int[][] newOrder) {
        order = newOrder;
    }

    public static void setOrder(int[][] order, boolean isMale) {
        if (isMale) {
            orderMale = order;
        } else {
            orderFemale = order;
        }
    }

    // Set Order Array without breakes and events only once
    public static void setSimpleOrder() {
        int breaks = 0;
        for (int i = 0; i < orderFemale.length; i++) {
            if (orderFemale[i][0] < 0) {
                breaks += 1;
            }
        }

        simpleOrder = new int[orderFemale.length - breaks];

        for (int i = 0, j = 0; i < simpleOrder.length;) {
            if (orderFemale[j][0] == -1) {
                j++;
                continue;
            }
            if (orderFemale[j][0] == -2) {
                System.out.println("SHOULDNT HAPPEN");
            }

            simpleOrder[i] = orderFemale[j][0];
            i++;
            j += 2;

        }
    }

    public static void setSimpleOrderFemale() {
        simpleOrderFemale = simpleOrder;
    }

    // Set Order Array without breakes and events only once
    public static void setSimpleOrderMale() {
        int breaks = 0;
        for (int i = 0; i < orderMale.length; i++) {
            if (orderMale[i][0] < 0) {
                breaks += 1;
            }
        }

        simpleOrderMale = new int[orderMale.length - breaks];

        for (int i = 0, j = 1; i < simpleOrderMale.length;) {

            if (orderMale[j][0] == -2) {
                j++;
            }

            simpleOrderMale[i] = orderMale[j][0];
            i++;
            j += 2;

        }
    }

    // ================================================
    // Getter
    // ================================================

    public static double getTimeFromString(String timeString) {
        // converts a time string in the format "mm:ss.SS" or "ss.SS" to a double
        // representing the time in seconds
        String[] parts = timeString.split(":");
        double time = 0;
        if (parts.length == 2) {
            time += Integer.parseInt(parts[0]) * 60; // minutes to seconds
            time += Double.parseDouble(parts[1]); // seconds
        } else if (parts.length == 1) {
            time += Double.parseDouble(parts[0]); // seconds only
        } else if (parts.length == 0) {
            time = -1; // -1 indicates no time was provided
        } else {
            throw new IllegalArgumentException("Invalid time format: " + timeString);
        }
        return time;
    }

    public static int getEventIndexByOrderIndex(int orderIndex) {
        return order[orderIndex][0];
    }

    // ================================================
    // toString() Methodes
    // ================================================

    /**
     * Prints the Event Order
     * Attention: eventOrderMale/eventOrderFemale have a different Typ
     * order[i][0] could be -2 so an Error will occure if order[][] is faultfully
     * assigned
     * 
     * @return eventOrder as String
     * @author Timon Spieth
     * @version 1
     */
    public static String toStringOrder() {
        StringBuilder sb = new StringBuilder();
        sb.append("Event Order:\n");
        int j = 1;
        for (int i = 0; i < order.length; i++) {
            int eventIndex = order[i][0];
            int eventTime = order[i][1];
            if (eventIndex == -1) {
                sb.append(String.format("------ BREAK (approx. %d min) ------ %n", eventTime));
                continue;
            }
            sb.append(String.format("%02d %5s (approx. %d min)%n", j++,
                    SwimmingEvent.values()[eventIndex].getDisplayName(), eventTime));
        }
        return sb.toString();
    }

    /**
     * Returns the BaseTimes for a given Gender formatted as a String
     * 
     * Attention: BaseTimes for the Gender should be initialized
     * 
     * @param isMale true if baseTimesMale[] is wanted
     * @return eventOrder as String
     * @author Timon Spieth
     */
    public static String toStringBaseTimes(boolean isMale) {
        double[] baseTimes = isMale ? baseTimesMale : baseTimesFemale;
        StringBuilder sb = new StringBuilder();

        // Tableheader
        String headerFormat = "%-8s | %-10s%n";
        sb.append(String.format(headerFormat, "Event", "BaseTime" + (isMale ? " (m)" : " (f)")));
        sb.append("----------------------\n");

        // rows
        String rowFormat = "%-8s | %-10.3f%n";
        for (SwimmingEvent event : SwimmingEvent.values()) {
            sb.append(String.format(rowFormat, event.getDisplayName(), baseTimes[event.getIndex()]));
        }

        return sb.toString();
    }

}