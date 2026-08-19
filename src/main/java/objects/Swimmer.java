package objects;

import java.util.Arrays;

/**
 * Die Klasse Swimmer repräsentiert einen Schwimmer aus einem Verein.
 * <p>
 * Jeder Schwimmer besteht aus:
 * einer eindeutigen ID (automatisch generiert),
 * einem Namen,
 * einem Geschlecht (männlich oder weiblich),
 * einer Liste von Events, an denen er teilnimmt,
 * einer Liste von Punkten für jedes Event.
 * </p>
 *
 * @author Timon Spieth
 * @version 1.0
 * @since 2026-02-10
 */

public class Swimmer {
    public static int nextId = 0; // static variable to keep track of the next available ID

    public int id;
    private String name;
    private int endurance; // endurance level of the swimmer, determines how long breaks have to be between
                           // events
    private int maxSectionEvents = 3;
    private int neededBreakK = 2;
    private int totalPoints; // may be used for Optimisation
    private boolean isMale;
    private int countChoosenEvents = 0; // keeps track of how many events the swimmer has chosen
    private boolean[] eventIndices = new boolean[Competition.order.length]; // used to calculate brektimes between
                                                                            // events
    private double[] times = new double[Competition.eventCount];
    private boolean[] choosenEvents = new boolean[Competition.eventCount]; // contains true for events the swimmer
                                                                           // takes, false otherwise
    private int[] points = new int[Competition.eventCount]; // contains points for each event,
    // when there is no points for an event, the value is -1

    // =============================================================
    // Konstruktoren
    // =============================================================

    public Swimmer(String name, boolean isMale, int endurance) {
        this.id = nextId++; // assign the current nextId to id and then increment nextId

        this.name = name;
        this.endurance = endurance;
        this.isMale = isMale;
        this.totalPoints = 0;

        Arrays.fill(eventIndices, false);
        Arrays.fill(choosenEvents, false); // initialize all events as not chosen
        Arrays.fill(points, -1); // initialize all points to -1 (indicating not defined)
        Arrays.fill(times, -1); // initialize all times to -1

    }

    // Copy constructor creates DEEP COPY
    public Swimmer(Swimmer other) {
        // if (other != null) {
        this.id = other.id;

        this.name = other.name;
        this.endurance = other.endurance;
        this.isMale = other.isMale;
        this.countChoosenEvents = other.countChoosenEvents;

        this.eventIndices = Arrays.copyOf(other.eventIndices, other.eventIndices.length);
        this.choosenEvents = Arrays.copyOf(other.choosenEvents, other.choosenEvents.length);
        this.points = Arrays.copyOf(other.points, other.points.length);
        this.times = Arrays.copyOf(other.times, other.times.length);
        // }

    }

    // =============================================================
    // Getter
    // =============================================================

    public int getID() {
        return this.id;
    }

    /**
     * Gibt den Namen des Schwimmers zurück.
     * 
     * @return Name des Schwimmers
     */
    public String getName() {
        return name;
    }

    /**
     * Gibt zurück, ob der Schwimmer männlich ist.
     * 
     * @return true, wenn der Schwimmer männlich ist, sonst false
     */
    public boolean isMale() {
        return isMale;
    }

    public int getEndurance() {
        return endurance;
    }

    public int getEventCount() {
        return this.countChoosenEvents;
    }

    public int[] getPointsArr() {
        return this.points;
    }

    /**
     * Gibt die Punkte für ein bestimmtes Event zurück.
     * 
     * @param eventIndex
     * @return
     */
    public int getPointsForEvent(SwimmingEvent event) { // method to get points for a specific event
        int eventIndex = event.getIndex();
        if (eventIndex >= 0 && eventIndex < points.length) {
            return points[eventIndex];
        } else {
            throw new IllegalArgumentException("Invalid event index");
        }
    }

    public int getPointsForEventIndex(int eventIndex) {
        return this.getPointsForEvent(SwimmingEvent.values()[eventIndex]);
    }

    public int getPointsForOrderIndex(int orderIndex) {
        int eventIndex = Competition.order[orderIndex][0];
        return this.getPointsForEvent(SwimmingEvent.values()[eventIndex]);
    }

    public int getTotalPoints() { // method to calculate total points across all choosen events
        int total = 0;
        for (int i = 0; i < choosenEvents.length; i++) {
            if (choosenEvents[i]) { // only consider events that the swimmer has chosen
                total += points[i]; // add points for the event to the total
            }
        }
        return total;
    }

    public double getTimeforEvent(SwimmingEvent event) {
        return this.times[event.getIndex()];
    }
    // =============================================================
    // Setter
    // =============================================================

    public void setName(String name) {
        this.name = name;
    }

    public void setEndurance(int endurance) {
        this.endurance = endurance;
    }

    public void setIsMale(boolean isMale) {
        this.isMale = isMale;
    }

    public void setPointsForEvent(SwimmingEvent event, double time) {
        int eventIndex = event.getIndex();
        if (eventIndex >= 0 && eventIndex < this.points.length) {
            this.points[eventIndex] = Competition.calculatePoints(event, time, this.isMale); // calculate and set
                                                                                             // points for the
                                                                                             // event
        } else {
            throw new IllegalArgumentException("Invalid event index");
        }
    }

    public void setPointsForEvent(String event, String time) {
        SwimmingEvent swimmingEvent = SwimmingEvent.getByDisplayName(event);
        this.setPointsForEvent(swimmingEvent, Competition.getTimeFromString(time));
    }

    public void setTimeForEvent(SwimmingEvent event, double time) {
        int eventIndex = event.getIndex();
        if (eventIndex >= 0 && eventIndex < this.times.length) {
            this.times[eventIndex] = time; // set time for the event
        } else {
            throw new IllegalArgumentException("Invalid event index");
        }
    }

    public void setTimeForEvent(String event, String time) {
        SwimmingEvent swimmingEvent = SwimmingEvent.getByDisplayName(event);
        this.setTimeForEvent(swimmingEvent, Competition.getTimeFromString(time));
    }

    // =============================================================
    // choosing of an Event Methodes
    // =============================================================

    /**
     * Choses an event for the swimmer if
     * -swimmer has not already chosen the maximum number of events
     * -event is not already chosen
     * -event index is valid
     * 
     * @param event
     * @return true if the event was successfully chosen
     * @author Timon Spieth
     * @since 2026-02-11
     */
    private boolean chooseEvent(SwimmingEvent event) {
        int eventIndex = event.getIndex();
        if (this.countChoosenEvents >= Competition.maxEventsPerSwimmer) {
            return false; // swimmer has already chosen the maximum number of events, return false to
                          // indicate that the event was not chosen
        }
        if (eventIndex >= 0 && eventIndex < this.choosenEvents.length) {
            if (this.choosenEvents[eventIndex]) {
                return false; // event is already chosen, return false to indicate it was not added again
            } else {
                this.choosenEvents[eventIndex] = true; // mark the event as chosen
                this.countChoosenEvents++; // increment the count of chosen events
                return true; // event was successfully chosen
            }
        } else {
            throw new IllegalArgumentException("Invalid event index");
        }
    }

    public boolean chooseEvent(int competitionIndex) {
        if (competitionIndex >= 0 && competitionIndex < Competition.order.length) {
            int eventIndex = Competition.order[competitionIndex][0];
            if (eventIndex == -1) {
                return false; // this is a break, not an event, return false to indicate that no event was
                              // chosen
            }
            SwimmingEvent event = SwimmingEvent.values()[eventIndex];

            if (chooseEvent(event)) {
                eventIndices[competitionIndex] = true; // mark the event index as chosen for break time calculation
                return true; // event was successfully chosen
            } else {
                return false; // event could not be chosen (either max events reached or event already
                              // chosen), return false to indicate that the event was not chosen
            }
        } else {
            throw new IllegalArgumentException("Invalid competition index");
        }
    }

    // =============================================================
    // Logik for canChoose() Methodes
    // =============================================================

    public boolean canChooseOrderIndex(int orderIndex) {

        SwimmingEvent event;

        // Quick check if the orderIndex is available
        // If it is event is Initalised with correct event
        if (this.isMale) {
            int eventIndex = Competition.orderMale[orderIndex][0];
            if (eventIndex < 0) {
                return false; // break or female Event
            }
            event = SwimmingEvent.values()[eventIndex];
        } else {
            int eventIndex = Competition.orderFemale[orderIndex][0];
            if (eventIndex < 0) {
                return false; // break or male Event
            }
            event = SwimmingEvent.values()[eventIndex];
        }

        // if (canChooseEvent(event)) {
        // if (hasEnoughBreak(orderIndex) && hasEnoughBreakAfter(orderIndex)
        // && !hasToMuchEvents(orderIndex, this.maxSectionEvents)) {
        // return true;
        // }
        // return false;

        // } else {
        // return false;
        // }
        if (canChooseEvent(event)) {
            if (hasBreakOf(orderIndex, neededBreakK) && hasBreakOfAfter(orderIndex,
                    neededBreakK)
                    && !hasToMuchEvents(orderIndex, this.maxSectionEvents)) {
                return true;
            }
            return false;

        } else {
            return false;
        }
    }

    /**
     * Checks if the swimmer can choose a specific event
     * - swimmer has not already chosen the maximum number of events
     * - event is not already chosen
     * - event index is valid
     * 
     * @param event
     * @return true if the swimmer can choose the event, false otherwise
     * @author Timon Spieth
     */
    public boolean canChooseEvent(SwimmingEvent event) {
        int eventIndex = event.getIndex();
        if (this.countChoosenEvents >= Competition.maxEventsPerSwimmer) {
            return false; // swimmer has already chosen the maximum number of events, return false to
                          // indicate that the event cannot be chosen
        }
        if (eventIndex >= 0 && eventIndex < this.choosenEvents.length) {
            return !this.choosenEvents[eventIndex]; // return true if the event is not already chosen, false otherwise
        } else {
            throw new IllegalArgumentException("Invalid event index");
        }
    }

    // For easy calling
    public boolean canChooseEvent(int eventIndex) {
        return this.canChooseEvent(SwimmingEvent.values()[eventIndex]);
    }

    // For simplicity this methode supposes that the athlete starts in the last heat
    // of the last event he compeated in and in the first of the order index
    public boolean hasEnoughBreak(int orderIndex) {
        int currentIndex = orderIndex - 1;
        int currentBreakTime = 0;
        if (currentIndex < 0) {
            return true; // before first event no break is needed
        }
        int eventIndex = Competition.order[currentIndex][0];

        for (; currentIndex >= 0; currentIndex--) {
            eventIndex = Competition.order[currentIndex][0];
            if (!eventIndices[currentIndex]) {

                if (eventIndex == -1) {
                    currentBreakTime += Competition.order[currentIndex][1];
                } else {
                    currentBreakTime += SwimmingEvent.values()[eventIndex].getTypicalDuration();
                }
            } else {
                break; // last Compeating Event found
            }
        }
        if (currentIndex < 0) {
            return true; // swimmer has no choosen event;
        }
        int needed = SwimmingEvent.values()[eventIndex].getMinimumBreakTime();
        if (currentBreakTime <= needed) {
            // System.out.println(this.id + ". Pause ist zu Kurz " + orderIndex + " " +
            // currentBreakTime);
            return false;
        }
        return true;
    }

    // Maybe needs an Overlook works for now
    public boolean hasEnoughBreakAfter(int orderIndex) {

        int event = Competition.order[orderIndex][0];
        int breakNeeded = SwimmingEvent.values()[event].getMinimumBreakTime();
        int currentIndex = orderIndex + 1;
        int currentBreakTime = 0;
        boolean hasEventAfter = false;

        for (; currentIndex < Competition.order.length; currentIndex++) {
            if (currentBreakTime >= breakNeeded) {
                return true;
            }
            int eventIndex = Competition.order[currentIndex][0];
            if (!eventIndices[currentIndex]) {
                if (eventIndex == -1) {
                    currentBreakTime += Competition.order[currentIndex][1];
                } else {
                    currentBreakTime += SwimmingEvent.values()[eventIndex].getTypicalDuration();
                }
            } else {
                hasEventAfter = true;
                break;
            }
        }
        if (hasEventAfter && (currentBreakTime <= breakNeeded)) {
            return false;
        }
        return true;
    }

    public boolean hasToMuchEvents(int orderIndex, int maxEvents) {
        int[][] order = Competition.order; // to reduce IO
        int countEvents = 0;

        for (int i = 0; i < order.length; i++) {

            // Break reached
            if (order[i][0] == Competition.BREAK_MARKER && order[i][1] == Competition.BREAK_CODE) {

                if (orderIndex < i) { // to Put event was in the last Abschnitt
                    return (countEvents >= maxEvents);
                } else { // to Put event was not in the last Abschnitt
                    countEvents = 0;
                    continue;
                }
            }
            if (eventIndices[i]) {
                countEvents++;
            }
        }
        return (countEvents >= maxEvents); // Should work??
    }

    public boolean hasBreakOf(int orderIndex, int k) {
        int currentIndex = orderIndex - 1;
        int currentBreaks = 0;
        if (currentIndex < 0) {
            return true; // before first event no break is needed
        }
        int[][] orderGender = this.isMale ? Competition.orderMale : Competition.orderFemale;
        int eventIndex = orderGender[currentIndex][0];
        for (; currentIndex >= 0; currentIndex--) {
            eventIndex = orderGender[currentIndex][0];
            if (!eventIndices[currentIndex]) {
                if (eventIndex == -1) {
                    return true;
                } else {
                    if (eventIndex == -2) {
                        continue;
                    }
                    currentBreaks++;
                }
            } else {
                break; // last Compeating Event found
            }
        }
        if (currentIndex < 0) {
            return true; // swimmer has no choosen event;
        }
        if (currentBreaks < k) {
            // System.out.println(this.id + ". Pause ist zu Kurz " + orderIndex + " " +
            // currentBreakTime);
            return false;
        }
        return true;
    }

    public boolean hasBreakOfAfter(int orderIndex, int k) {
        int currentIndex = orderIndex + 1;
        int currentBreaks = 0;

        boolean hasEventAfter = false;

        int[][] orderGender = this.isMale ? Competition.orderMale : Competition.orderFemale;
        for (; currentIndex < orderGender.length; currentIndex++) {

            int eventIndex = orderGender[currentIndex][0];
            if (!eventIndices[currentIndex]) {
                if (eventIndex == -1) {
                    return true;
                } else {
                    if (eventIndex == -2) {
                        continue;
                    }
                    currentBreaks++;
                }
            } else {
                hasEventAfter = true;
                break; // last Compeating Event found
            }
        }
        if (hasEventAfter && currentBreaks < k) {
            // System.out.println(this.id + ". Pause ist zu Kurz " + orderIndex + " " +
            // currentBreakTime);
            return false;
        }
        return true;
    }

    // =============================================================
    // removeEvent Methodes()
    // =============================================================

    // should only be called from class
    // HELPER METHOD for removeEvent(int orderIndex)
    private boolean removeEvent(SwimmingEvent event) {
        int eventIndex = event.getIndex();
        if (eventIndex >= 0 && eventIndex < this.choosenEvents.length) {
            if (this.choosenEvents[eventIndex]) { // only remove the event if it is currently chosen
                this.choosenEvents[eventIndex] = false; // mark the event as not chosen
                this.countChoosenEvents--; // decrement the count of chosen events
                return true;
            } else {
                return false;
            }
        } else {
            throw new IllegalArgumentException("Invalid event index");
        }
    }

    public boolean removeEvent(int competitionIndex) {
        if (competitionIndex >= 0 && competitionIndex < Competition.order.length) {
            int eventIndex = Competition.order[competitionIndex][0];
            if (eventIndex == -1) {
                return false; // this is a break, not an event, return false to indicate that no event was
                              // chosen
            }
            SwimmingEvent event = SwimmingEvent.values()[eventIndex];

            if (removeEvent(event)) {
                eventIndices[competitionIndex] = false;
                return true;
            } else {
                return false;
            }
        } else {
            throw new IllegalArgumentException("Invalid competition index");
        }
    }

    // =============================================================
    // Utility Methodes Updates/Resets/HashSet...
    // =============================================================

    public void updatePoints() {
        for (int i = 0; i < times.length; i++) {
            if (times[i] != -1) { // only update points for events that have a defined time
                setPointsForEvent(SwimmingEvent.values()[i], times[i]);
            }
        }
    }

    public void resetEvents() {
        countChoosenEvents = 0;
        Arrays.fill(this.eventIndices, false);
        Arrays.fill(this.choosenEvents, false);
    }

    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Swimmer swimmer = (Swimmer) obj;
        return id == swimmer.id; // swimmers are considered equal if they have the same ID
    }

    public int hashCode() {
        return Integer.hashCode(id); // hash code is based on the unique ID of the swimmer
    }

    // =============================================================
    // toString() Methodes
    // =============================================================

    public String toString() {

        StringBuilder sb = new StringBuilder();

        // Tableheader
        String headerFormat = "%-8s | %-10s | %-10s%n";
        ;
        sb.append(String.format(headerFormat, "Event", "Time", "Points " + (isMale ? " (m)" : " (f)")));
        sb.append("-----------------------------------\n");

        // rows
        String rowFormat = "%-8s | %-10.3f | %-10d%n";
        for (SwimmingEvent event : SwimmingEvent.values()) {
            sb.append(String.format(rowFormat, event.getDisplayName(), times[event.getIndex()],
                    points[event.getIndex()]));
        }

        return "Name: " + name + ", Geschlecht: " + (isMale ? "maennlich" : "weiblich") + "\n" + sb.toString();

    }

    public String toStringBreakBefores(int orderIndex) {
        int currentIndex = orderIndex - 1;
        if (currentIndex == -1) {
            return "Infinity";
        }
        int currentBreakTime = 0;
        int eventIndex = Competition.order[currentIndex][0];
        for (; currentIndex >= 0; currentIndex--) {
            eventIndex = Competition.order[currentIndex][0];
            if (!eventIndices[currentIndex]) {

                if (eventIndex == -1) {
                    currentBreakTime += Competition.order[currentIndex][1];
                } else {
                    currentBreakTime += SwimmingEvent.values()[eventIndex].getTypicalDuration();
                }
            } else {
                break; // last Compeating Event found
            }
        }
        if (currentIndex < 0) {
            return "Infinity"; // swimmer has no choosen event;
        }
        int needed = SwimmingEvent.values()[eventIndex].getMinimumBreakTime();
        if (currentBreakTime < needed) {
            return "Pause ist zu Kurz " + currentBreakTime + " " + needed;

        }
        return Integer.toString(currentBreakTime) + "min";
    }

    public String toStringBreakBeforeNumber(int orderIndex) {
        int k = neededBreakK;
        int currentIndex = orderIndex - 1;
        int currentBreaks = 0;
        if (currentIndex < 0) {
            return "Infinity"; // before first event no break is needed
        }
        int[][] orderGender = this.isMale ? Competition.orderMale : Competition.orderFemale;
        int eventIndex = orderGender[currentIndex][0];
        for (; currentIndex >= 0; currentIndex--) {
            eventIndex = orderGender[currentIndex][0];
            if (!eventIndices[currentIndex]) {
                if (eventIndex == -1) {
                    return "NoEventsBefore Breake";
                } else {
                    if (eventIndex == -2) {
                        continue;
                    }
                    currentBreaks++;
                }
            } else {
                break; // last Compeating Event found
            }
        }
        if (currentIndex < 0) {
            return "NoEvents"; // swimmer has no choosen event;
        }
        if (currentBreaks < k) {
            // System.out.println(this.id + ". Pause ist zu Kurz " + orderIndex + " " +
            // currentBreakTime);
            return "To less Breaks " + currentBreaks;
        }
        return "Enough Breaks " + currentBreaks;
    }

    public String toStringBreakAfterNumber(int orderIndex) {
        int k = neededBreakK;
        int currentIndex = orderIndex + 1;
        int currentBreaks = 0;
        boolean hasEventAfter = false;

        int[][] orderGender = this.isMale ? Competition.orderMale : Competition.orderFemale;
        for (; currentIndex < orderGender.length; currentIndex++) {
            int eventIndex = orderGender[currentIndex][0];
            if (!eventIndices[currentIndex]) {
                if (eventIndex == -1) {
                    return "No Events Left in this section";
                } else {
                    if (eventIndex == -2) {
                        continue;
                    }
                    currentBreaks++;
                }
            } else {
                hasEventAfter = true;
                break; // last Compeating Event found
            }
        }
        if (hasEventAfter && currentBreaks < k) {
            // System.out.println(this.id + ". Pause ist zu Kurz " + orderIndex + " " +
            // currentBreakTime);
            return "To less Breaks " + currentBreaks;
        }
        if (!hasEventAfter) {
            return "LAST EVENT";
        }
        return "Enough breaks " + currentBreaks;
    }

    public String toStringBreakBefore(int orderIndex) {
        return toStringBreakBeforeNumber(orderIndex) + " vs. " + toStringBreakAfterNumber(orderIndex);
    }
}
