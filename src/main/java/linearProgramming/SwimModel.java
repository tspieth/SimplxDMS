package linearProgramming;

public class SwimModel {
    final int numberAthletes; // athletes
    final int positions = 34; // positions
    final int positionsPerSection = 17; // positions per section
    final int maxTotal = 5;
    final int maxPerSection = 3;
    final int pauseGap; // g
    final int special1500_j0 = 7; // j=8
    final int special800_j0 = 24; // j=25
    final double[][] L; // N x 34
    private final int[][] variableIndexes;
    private final int variableCount;

    public SwimModel(int athletes, int pauseGap, double[][] L) {
        this.numberAthletes = athletes;
        this.pauseGap = pauseGap;
        this.L = L;
        variableIndexes = new int[athletes][positions];
        int nextVariable = 0;
        for (int athlete = 0; athlete < athletes; athlete++) {
            for (int position = 0; position < positions; position++) {
                if (L[athlete][position] > 0) {
                    variableIndexes[athlete][position] = nextVariable++;
                } else {
                    variableIndexes[athlete][position] = -1;
                }
            }
        }
        variableCount = nextVariable;
    }

    int nVars() {
        return variableCount;
    }

    int var(int i, int j0) {
        return variableIndexes[i][j0];
    }

    public static double[] buildObjective(SwimModel m) {
        double[] c = new double[m.nVars()];
        for (int i = 0; i < m.numberAthletes; i++) {
            for (int j0 = 0; j0 < m.positions; j0++) {
                int variable = m.var(i, j0);
                if (variable >= 0) {
                    c[variable] = m.L[i][j0];
                }
            }
        }
        return c;
    }

}
