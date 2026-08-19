package experiments;

public class ExperimentBranchNBound {

    /*
     * public static void main(String[] args) {
     * // Lade den SwimmingClub aus den Ressourcen
     * SwimmingClub club = SwimmingClub.loadFromCSV(
     * "OptimalDMSAlgo/resources/betterClub.csv",
     * "OptimalDMSAlgo/resources/order.csv",
     * "OptimalDMSAlgo/resources/orderMale.csv");
     * 
     * if (club == null) {
     * System.out.println("Fehler beim Laden des Clubs");
     * return;
     * }
     * 
     * // Löse für männliche Schwimmer
     * System.out.println("============ Branch and Bound - MÄNNER ============");
     * TeamNode rootMale = new TeamNode(club, true);
     * System.out.println("Initial Upper Bound (männlich): " +
     * rootMale.getUpperBound());
     * 
     * long startTime = System.currentTimeMillis();
     * TeamNode solutionMale = BranchNBound.solve(rootMale, 0);
     * long endTime = System.currentTimeMillis();
     * 
     * if (solutionMale != null) {
     * System.out.println("Beste Lösung (männlich): " +
     * solutionMale.getTotalPoints() + " Punkte");
     * System.out.println("Zeit: " + (endTime - startTime) + " ms");
     * } else {
     * System.out.println("Keine Lösung gefunden (männlich)");
     * }
     * 
     * // Löse für weibliche Schwimmer
     * System.out.println("\n============ Branch and Bound - FRAUEN ============");
     * TeamNode rootFemale = new TeamNode(club, false);
     * System.out.println("Initial Upper Bound (weiblich): " +
     * rootFemale.getUpperBound());
     * 
     * startTime = System.currentTimeMillis();
     * TeamNode solutionFemale = BranchNBound.solve(rootFemale, 0);
     * endTime = System.currentTimeMillis();
     * 
     * if (solutionFemale != null) {
     * System.out.println("Beste Lösung (weiblich): " +
     * solutionFemale.getTotalPoints() + " Punkte");
     * System.out.println("Zeit: " + (endTime - startTime) + " ms");
     * } else {
     * System.out.println("Keine Lösung gefunden (weiblich)");
     * }
     * 
     * // Gesamtergebnis
     * if (solutionMale != null && solutionFemale != null) {
     * System.out.println("\n============ GESAMTERGEBNIS ============");
     * System.out.println("Männer: " + solutionMale.getTotalPoints());
     * System.out.println("Frauen: " + solutionFemale.getTotalPoints());
     * System.out.println("Gesamt: " + (solutionMale.getTotalPoints() +
     * solutionFemale.getTotalPoints()));
     * }
     * }
     */

}
