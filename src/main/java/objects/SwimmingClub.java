package objects;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Die Klasse SwimmingClub repräsentiert einen Schwimmverein, der an der DMS
 * teilnimmt.
 * <p>
 * Jeder Schwimmverein besteht aus einer Liste von Schwimmern und einer Liste
 * von Schwimmer
 * die aktuell in der DMS teilnehmen (teamSwimmer). Die Klasse enthält eine
 * Methode zur Berechnung der Gesamtpunkte des Vereins.
 * </p>
 * 
 * @author Timon Spieth
 * @version 1.0
 * @since 2026-02-11
 */
public class SwimmingClub {

    private List<Swimmer> swimmers;
    private String clubName;

    private Map<Integer, Swimmer> rowNumberToSwimmer = new HashMap<Integer, Swimmer>();

    private Map<SwimmingEvent, List<Swimmer>> leaderboardsMale = new HashMap<SwimmingEvent, List<Swimmer>>();
    private Map<SwimmingEvent, List<Swimmer>> leaderboardsFemale = new HashMap<SwimmingEvent, List<Swimmer>>();

    // =============================================================
    // Konstruktoren
    // =============================================================

    public SwimmingClub(List<Swimmer> swimmers) {
        this.swimmers = swimmers;
        generateLeaderboards();
    }

    public SwimmingClub(String clubName, List<Swimmer> swimmers) {
        this.swimmers = swimmers;
        this.clubName = clubName;
        generateLeaderboards();
    }

    // =============================================================
    // Getter
    // =============================================================

    public Map<SwimmingEvent, List<Swimmer>> getLeaderboards(boolean isMale) {
        return isMale ? leaderboardsMale : leaderboardsFemale;
    }

    public List<Swimmer> getAllSwimmer() {
        return swimmers;
    }

    public String getClubName() {
        return this.clubName;
    }

    public Map<Integer, Swimmer> getRowNumberToSwimmer() {
        return this.rowNumberToSwimmer;
    }

    // =============================================================
    // Creater/Generator
    // =============================================================

    public void generateLeaderboards(boolean isMale) {
        for (SwimmingEvent event : SwimmingEvent.values()) {
            List<Swimmer> swimmerByPoints = swimmers.stream()
                    .filter(swimmer -> swimmer.getPointsForEvent(event) != -1 && swimmer.isMale() == isMale)
                    .sorted((s1, s2) -> Integer.compare(s2.getPointsForEvent(event), s1.getPointsForEvent(event)))
                    .toList();
            if (isMale) {
                leaderboardsMale.put(event, swimmerByPoints);
            } else {
                leaderboardsFemale.put(event, swimmerByPoints);
            }
        }
    }

    public void generateLeaderboards() {
        generateLeaderboards(true);
        generateLeaderboards(false);
    }

    public double[][] createMatrixForSimplex(boolean isMale) {

        List<Swimmer> allfromGender = this.swimmers.stream().filter(s -> s.isMale() == isMale).toList();

        double[][] pointMatrix = new double[allfromGender.size()][SwimmingEvent.values().length * 2 - 2];

        int pos = 0;
        for (Swimmer s : allfromGender) {
            this.rowNumberToSwimmer.put(pos, s);
            int[] points = s.getPointsArr();
            double[] rowForMatrix = new double[SwimmingEvent.values().length * 2 - 2];
            int sectionLength = points.length - 1;
            for (int i = 0; i < points.length; i++) {
                switch (SwimmingEvent.values()[i].getDisplayName()) {
                    case "100Lg":
                        rowForMatrix[0] = points[i];
                        rowForMatrix[0 + sectionLength] = points[i];

                        break;
                    case "200K":
                        rowForMatrix[1] = points[i];
                        rowForMatrix[1 + sectionLength] = points[i];

                        break;
                    case "100B":
                        rowForMatrix[2] = points[i];
                        rowForMatrix[2 + sectionLength] = points[i];

                        break;
                    case "200R":
                        rowForMatrix[3] = points[i];
                        rowForMatrix[3 + sectionLength] = points[i];

                        break;
                    case "100S":
                        rowForMatrix[4] = points[i];
                        rowForMatrix[4 + sectionLength] = points[i];

                        break;
                    case "50B":
                        rowForMatrix[5] = points[i];
                        rowForMatrix[5 + sectionLength] = points[i];

                        break;
                    case "200Lg":
                        rowForMatrix[6] = points[i];
                        rowForMatrix[6 + sectionLength] = points[i];

                        break;
                    case "1500K":
                        if (isMale) {
                            rowForMatrix[7] = points[i];
                        } else {
                            rowForMatrix[7 + sectionLength] = points[i];
                        }

                        break;
                    case "800K":
                        if (isMale) {
                            rowForMatrix[7 + sectionLength] = points[i];
                        } else {
                            rowForMatrix[7] = points[i];
                        }
                        break;
                    case "50S":
                        rowForMatrix[8] = points[i];
                        rowForMatrix[8 + sectionLength] = points[i];

                        break;
                    case "200B":
                        rowForMatrix[9] = points[i];
                        rowForMatrix[9 + sectionLength] = points[i];

                        break;
                    case "100R":
                        rowForMatrix[10] = points[i];
                        rowForMatrix[10 + sectionLength] = points[i];

                        break;
                    case "200S":
                        rowForMatrix[11] = points[i];
                        rowForMatrix[11 + sectionLength] = points[i];

                        break;
                    case "50K":
                        rowForMatrix[12] = points[i];
                        rowForMatrix[12 + sectionLength] = points[i];

                        break;
                    case "400Lg":
                        rowForMatrix[13] = points[i];
                        rowForMatrix[13 + sectionLength] = points[i];

                        break;
                    case "50R":
                        rowForMatrix[14] = points[i];
                        rowForMatrix[14 + sectionLength] = points[i];

                        break;
                    case "400K":
                        rowForMatrix[15] = points[i];
                        rowForMatrix[15 + sectionLength] = points[i];

                        break;
                    case "100K":
                        rowForMatrix[16] = points[i];
                        rowForMatrix[16 + sectionLength] = points[i];

                        break;
                    default:
                        System.out.println("Should not be reached");
                        break;
                }
            }
            pointMatrix[pos] = rowForMatrix;
            pos++;

        }
        return pointMatrix;

    }

    // =============================================================
    // toString() Methodes
    // =============================================================

    public String toStringLeaderboard(int topN, SwimmingEvent event, boolean isMale) {
        StringBuilder sb = new StringBuilder();
        sb.append("Leaderboard for ").append(event.getDisplayName()).append(isMale ? " (m)" : " (f)").append(":\n");
        List<Swimmer> swimmersForEvent = isMale ? leaderboardsMale.get(event) : leaderboardsFemale.get(event);
        for (int i = 0; i < Math.min(topN, swimmersForEvent.size()); i++) {
            Swimmer swimmer = swimmersForEvent.get(i);
            sb.append((i + 1)).append(". ").append(swimmer.getName()).append(" - ")
                    .append(swimmer.getPointsForEvent(event)).append(" points\n");
        }
        return sb.toString();
    }

    public String toStringLeaderboards(int topN, boolean isMale) {
        StringBuilder sb = new StringBuilder();
        for (SwimmingEvent event : SwimmingEvent.values()) {
            sb.append(toStringLeaderboard(topN, event, isMale)).append("\n");
            sb.append("****************************\n");
        }
        return sb.toString();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Swimming Club:\n");
        for (Swimmer swimmer : swimmers) {
            sb.append(swimmer.toString()).append("\n");
            sb.append("*******************************\n");
        }
        return sb.toString();
    }

    // DEPRECATED
    // public void addSwimmerToTeam(Swimmer swimmer) {
    // if (swimmers.contains(swimmer) && !teamSwimmer.contains(swimmer)) {
    // teamSwimmer.add(swimmer); // adds a swimmer to the team if they are in the
    // swimmers list and not already
    // // in the team
    // }
    // }

    // public void addMultipleSwimmersToTeam(List<Swimmer> swimmersToAdd) {
    // for (Swimmer swimmer : swimmersToAdd) {
    // addSwimmerToTeam(swimmer); // adds multiple swimmers to the team using the
    // addSwimmerToTeam method

    // }
    // }
    //
    // public int getTotalPoints() {
    // int totalPoints = 0;
    // for (Swimmer swimmer : teamSwimmer) {
    // totalPoints += swimmer.getTotalPoints(); // adds the total points of each
    // swimmer to the total
    // }
    // return totalPoints;
    // }

}