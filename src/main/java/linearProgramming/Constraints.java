package linearProgramming;

import java.util.ArrayList;

// Constraints as Ax <= b
public class Constraints {
    final double[][] A;
    final double[] b;

    Constraints(double[][] A, double[] b) {
        this.A = A;
        this.b = b;
    }

    public static Constraints buildBaseConstraints(SwimModel m) {
        int n = m.nVars();
        ArrayList<double[]> rows = new ArrayList<>();
        ArrayList<Double> rhs = new ArrayList<>();

        // helper: add Ax <= b
        final class H {
            void addLe(double[] row, double b) {
                rows.add(row);
                rhs.add(b);
            }

            void addEq(double[] row, double b) {
                addLe(row, b);
                double[] neg = row.clone();
                for (int k = 0; k < neg.length; k++)
                    neg[k] = -neg[k];
                addLe(neg, -b);
            }
        }
        H h = new H();

        // (1) Each position j0 has at most one athlete: sum_i x[i,j0] <= 1
        for (int j0 = 0; j0 < m.positions; j0++) {
            double[] row = new double[n];
            for (int i = 0; i < m.numberAthletes; i++) {
                int variable = m.var(i, j0);
                if (variable >= 0) {
                    row[variable] = 1.0;
                }
            }
            h.addLe(row, 1.0);
        }

        // (2) Max total per athlete: sum_j x[i,j] <= 5
        for (int i = 0; i < m.numberAthletes; i++) {
            double[] row = new double[n];
            for (int j0 = 0; j0 < m.positions; j0++) {
                int variable = m.var(i, j0);
                if (variable >= 0) {
                    row[variable] = 1.0;
                }
            }
            if (hasNonZero(row)) {
                h.addLe(row, m.maxTotal);
            }
        }

        // (3) Max per section per athlete: <= 3
        for (int i = 0; i < m.numberAthletes; i++) {
            // section 1 (0..16)
            double[] r1 = new double[n];
            for (int j0 = 0; j0 < m.positionsPerSection; j0++) {
                int variable = m.var(i, j0);
                if (variable >= 0) {
                    r1[variable] = 1.0;
                }
            }
            if (hasNonZero(r1)) {
                h.addLe(r1, m.maxPerSection);
            }

            // section 2 (17..33)
            double[] r2 = new double[n];
            for (int j0 = m.positionsPerSection; j0 < m.positions; j0++) {
                int variable = m.var(i, j0);
                if (variable >= 0) {
                    r2[variable] = 1.0;
                }
            }
            if (hasNonZero(r2)) {
                h.addLe(r2, m.maxPerSection);
            }
        }

        // (4) Same index across sections at most once, excluding special index 8
        // (0-based idx=7):
        // For idx != 7: x(i, idx) + x(i, idx+17) <= 1
        for (int i = 0; i < m.numberAthletes; i++) {
            for (int idx = 0; idx < m.positionsPerSection; idx++) {
                if (idx == 7)
                    continue;
                double[] row = new double[n];
                int firstVariable = m.var(i, idx);
                int secondVariable = m.var(i, idx + m.positionsPerSection);
                if (firstVariable >= 0 && secondVariable >= 0) {
                    row[firstVariable] = 1.0;
                    row[secondVariable] = 1.0;
                    h.addLe(row, 1.0);
                }
            }
        }

        // (5) 1500/800 exclusion: x(i,8) + x(i,25) <= 1 -> j0=7 and j0=24
        for (int i = 0; i < m.numberAthletes; i++) {
            double[] row = new double[n];
            int firstVariable = m.var(i, m.special1500_j0);
            int secondVariable = m.var(i, m.special800_j0);
            if (firstVariable >= 0 && secondVariable >= 0) {
                row[firstVariable] = 1.0;
                row[secondVariable] = 1.0;
                h.addLe(row, 1.0);
            }
        }

        // (6) Pause gap within each section: for d=1..g, no starts within d
        int g = m.pauseGap;
        if (g > 0) {
            for (int i = 0; i < m.numberAthletes; i++) {
                for (int base = 0; base <= m.positionsPerSection; base += m.positionsPerSection) { // base=0 (sec1) and
                                                                                                   // base=17 (sec2)
                    for (int k = 0; k < m.positionsPerSection; k++) {
                        for (int d = 1; d <= g; d++) {
                            int k2 = k + d;
                            if (k2 >= m.positionsPerSection)
                                break;
                            double[] row = new double[n];
                            int firstVariable = m.var(i, base + k);
                            int secondVariable = m.var(i, base + k2);
                            if (firstVariable >= 0 && secondVariable >= 0) {
                                row[firstVariable] = 1.0;
                                row[secondVariable] = 1.0;
                                h.addLe(row, 1.0);
                            }
                        }
                    }
                }
            }
        }

        // Materialize
        double[][] A = new double[rows.size()][n];
        double[] b = new double[rhs.size()];
        for (int i = 0; i < rows.size(); i++) {
            A[i] = rows.get(i);
            b[i] = rhs.get(i);
        }
        return new Constraints(A, b);
    }

    private static boolean hasNonZero(double[] row) {
        for (double value : row) {
            if (value != 0) {
                return true;
            }
        }
        return false;
    }
}
