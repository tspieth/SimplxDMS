package linearProgramming;

import java.util.Arrays;

// ----------------------------
// Two-phase simplex (KACTL-style) for:
// max c^T x s.t. A x <= b, x >= 0
// Returns:
// Double.NaN for infeasible
// Double.POSITIVE_INFINITY for unbounded
// otherwise optimal value, and writes x (size n) if provided
// ----------------------------
public class DoubleSimplexSolver {
    final int m; // constraints
    final int n; // variables
    final int[] B; // basic var indices
    final int[] N; // non-basic var indices
    final double[][] D; // tableau: (m+2) x (n+2)

    public DoubleSimplexSolver(double[][] A, double[] b, double[] c) {
        this.m = b.length;
        this.n = c.length;
        this.B = new int[m];
        this.N = new int[n + 1]; // +1 artificial col

        this.D = new double[m + 2][n + 2];

        // Fill constraint rows
        for (int i = 0; i < m; i++) {
            System.arraycopy(A[i], 0, D[i], 0, n);
            B[i] = n + i;
            D[i][n] = -1.0; // artificial variable column
            D[i][n + 1] = b[i]; // RHS
        }

        // Objective row
        for (int j = 0; j < n; j++) {
            N[j] = j;
            D[m][j] = -c[j];
        }
        N[n] = -1; // artificial variable id
        D[m + 1][n] = 1.0; // phase-1 objective
    }

    void pivot(int r, int s) {
        double inv = 1.0 / D[r][s];

        // Update all except r,s
        for (int i = 0; i < m + 2; i++) {
            if (i == r)
                continue;
            for (int j = 0; j < n + 2; j++) {
                if (j == s)
                    continue;
                D[i][j] -= D[r][j] * D[i][s] * inv;
            }
        }

        // Update row r
        for (int j = 0; j < n + 2; j++) {
            if (j == s)
                continue;
            D[r][j] *= inv;
        }

        // Update column s
        for (int i = 0; i < m + 2; i++) {
            if (i == r)
                continue;
            D[i][s] *= -inv;
        }

        D[r][s] = inv;

        int tmp = B[r];
        B[r] = N[s];
        N[s] = tmp;
    }

    boolean simplex(int phase) {
        int xRow = (phase == 1) ? (m + 1) : m;

        while (true) {
            int s = -1;
            for (int j = 0; j <= n; j++) {
                if (phase == 2 && N[j] == -1)
                    continue; // don't enter artificial in phase 2
                if (s == -1 ||
                        D[xRow][j] < D[xRow][s] - LinearProgramming.EPS ||
                        (Math.abs(D[xRow][j] - D[xRow][s]) <= LinearProgramming.EPS && N[j] < N[s])) {
                    s = j;
                }
            }
            if (D[xRow][s] >= -LinearProgramming.EPS)
                return true; // optimal

            int r = -1;
            for (int i = 0; i < m; i++) {
                if (D[i][s] <= LinearProgramming.EPS)
                    continue;
                if (r == -1)
                    r = i;
                else {
                    double lhs = D[i][n + 1] / D[i][s];
                    double rhs = D[r][n + 1] / D[r][s];
                    if (lhs < rhs - LinearProgramming.EPS
                            || (Math.abs(lhs - rhs) <= LinearProgramming.EPS && B[i] < B[r])) {
                        r = i;
                    }
                }
            }
            if (r == -1)
                return false; // unbounded
            pivot(r, s);
        }
    }

    double solve(double[] xOut) {
        // Find most negative RHS
        int r = 0;
        for (int i = 1; i < m; i++) {
            if (D[i][n + 1] < D[r][n + 1])
                r = i;
        }

        // Phase 1 if needed
        if (D[r][n + 1] < -LinearProgramming.EPS) {
            pivot(r, n); // bring artificial into basis
            if (!simplex(1) || D[m + 1][n + 1] < -LinearProgramming.EPS)
                return Double.NaN; // infeasible
            if (D[m + 1][n + 1] > LinearProgramming.EPS)
                return Double.NaN; // infeasible (numerical)

            // Remove artificial variable if it's basic
            for (int i = 0; i < m; i++) {
                if (B[i] == -1) {
                    int s = -1;
                    for (int j = 0; j <= n; j++) {
                        if (s == -1 ||
                                D[i][j] < D[i][s] - LinearProgramming.EPS ||
                                (Math.abs(D[i][j] - D[i][s]) <= LinearProgramming.EPS && N[j] < N[s])) {
                            s = j;
                        }
                    }
                    pivot(i, s);
                }
            }
        }

        // Phase 2
        if (!simplex(2))
            return Double.POSITIVE_INFINITY;

        if (xOut != null) {
            Arrays.fill(xOut, 0.0);
            for (int i = 0; i < m; i++) {
                if (B[i] >= 0 && B[i] < n)
                    xOut[B[i]] = D[i][n + 1];
            }
            // clamp tiny negatives
            for (int k = 0; k < xOut.length; k++) {
                if (xOut[k] < 0 && xOut[k] > -1e-10)
                    xOut[k] = 0.0;
            }
        }

        return D[m][n + 1];
    }
}