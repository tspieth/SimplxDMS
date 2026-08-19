package linearProgramming;

import objects.Competition;
import objects.Swimmer;
import objects.SwimmingClub;
import objects.SwimmingEvent;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;

public class LinearProgramming {

    // ----------------------------
    // Config
    // ----------------------------
    public static final double EPS = 1e-9;
    public static final double INT_TOL = 1e-6;
    public static final double PRUNE_TOL = 1e-7;

    public static boolean isIntegral01(double[] x) {
        for (double v : x) {
            double r = Math.rint(v);
            if (Math.abs(v - r) > INT_TOL)
                return false;
        }
        return true;
    }

    public static int chooseBranchVar(double[] x) {
        int best = -1;
        double bestScore = -1.0;
        for (int k = 0; k < x.length; k++) {
            double v = x[k];
            if (v > INT_TOL && v < 1.0 - INT_TOL) {
                double score = 0.5 - Math.abs(v - 0.5);
                if (score > bestScore) {
                    bestScore = score;
                    best = k;
                }
            }
        }
        return best;
    }

    public static Solution greedyInitial(SwimModel m) {
        int N = m.numberAthletes;
        int P = m.positions;
        int S = m.positionsPerSection;

        int[] total = new int[N];
        int[][] perSec = new int[N][2];
        boolean[][] usedIndex = new boolean[N][S]; // index 0..16 used across both sections (incl. special idx=7)
        int[] assigned = new int[P];
        Arrays.fill(assigned, -1);

        // Track pause: for each athlete and section and local index
        boolean[][][] usedLocal = new boolean[N][2][S];

        for (int j0 = 0; j0 < P; j0++) {
            int sec = (j0 < S) ? 0 : 1;
            int idx = j0 % S;

            int bestI = -1;
            double bestL = Double.NEGATIVE_INFINITY;

            for (int i = 0; i < N; i++) {
                if (total[i] >= m.maxTotal)
                    continue;
                if (perSec[i][sec] >= m.maxPerSection)
                    continue;

                // index uniqueness across both sections
                if (idx != 7) {
                    if (usedIndex[i][idx])
                        continue;
                } else {
                    // special idx=7: still mutual exclusion across sections (1500 vs 800)
                    if (usedIndex[i][7])
                        continue;
                }

                // pause backward within section
                boolean ok = true;
                for (int d = 1; d <= m.pauseGap; d++) {
                    int prev = idx - d;
                    if (prev < 0)
                        break;
                    if (usedLocal[i][sec][prev]) {
                        ok = false;
                        break;
                    }
                }
                if (!ok)
                    continue;

                double li = m.L[i][j0];
                if (li > bestL) {
                    bestL = li;
                    bestI = i;
                }
            }

            if (bestI < 0) {
                return new Solution(Double.NEGATIVE_INFINITY, null);
            }

            assigned[j0] = bestI;
            total[bestI]++;
            perSec[bestI][sec]++;
            usedLocal[bestI][sec][idx] = true;

            if (idx != 7)
                usedIndex[bestI][idx] = true;
            else
                usedIndex[bestI][7] = true;
        }

        double[] x = new double[m.nVars()];
        double val = 0.0;
        for (int j0 = 0; j0 < P; j0++) {
            int i = assigned[j0];
            x[m.var(i, j0)] = 1.0;
            val += m.L[i][j0];
        }
        return new Solution(val, x);
    }

    public static Solution branchAndBound(SwimModel m, Constraints base) {
        Solution incumbent = greedyInitial(m);
        double bestValue = incumbent.bestValue;
        double[] bestX = incumbent.bestX;

        Deque<SimplexNode> stack = new ArrayDeque<>();
        stack.push(new SimplexNode(new int[0], new int[0]));

        while (!stack.isEmpty()) {
            SimplexNode node = stack.pop();

            // quick contradiction check in node fixings
            if (node.vars.length > 1) {
                HashMap<Integer, Integer> seen = new HashMap<>();
                boolean bad = false;
                for (int t = 0; t < node.vars.length; t++) {
                    int v = node.vars[t], val = node.vals[t];
                    Integer prev = seen.put(v, val);
                    if (prev != null && prev != val) {
                        bad = true;
                        break;
                    }
                }
                if (bad)
                    continue;
            }

            LPResult lp = LPResult.solveLP(m, base, node);
            if (!lp.feasible)
                continue;

            double ub = lp.value;
            if (ub <= bestValue + PRUNE_TOL)
                continue;

            if (isIntegral01(lp.x)) {
                if (ub > bestValue) {
                    bestValue = ub;
                    bestX = lp.x.clone();
                }
                continue;
            }

            int k = chooseBranchVar(lp.x);
            if (k < 0)
                continue;

            // Branch x_k = 1 and x_k = 0
            // Heuristic: branch toward lp.x[k] first
            int[] vars0 = Arrays.copyOf(node.vars, node.vars.length + 1);
            int[] vals0 = Arrays.copyOf(node.vals, node.vals.length + 1);
            vars0[vars0.length - 1] = k;
            vals0[vals0.length - 1] = 0;

            int[] vars1 = Arrays.copyOf(node.vars, node.vars.length + 1);
            int[] vals1 = Arrays.copyOf(node.vals, node.vals.length + 1);
            vars1[vars1.length - 1] = k;
            vals1[vals1.length - 1] = 1;

            if (lp.x[k] >= 0.5) {
                stack.push(new SimplexNode(vars0, vals0));
                stack.push(new SimplexNode(vars1, vals1));
            } else {
                stack.push(new SimplexNode(vars1, vals1));
                stack.push(new SimplexNode(vars0, vals0));
            }
        }

        return new Solution(bestValue, bestX);
    }

    public static int[] decodeAssignment(SwimModel m, double[] x) {
        int[] assign = new int[m.positions];
        Arrays.fill(assign, -1);
        if (x == null)
            return assign;

        for (int j0 = 0; j0 < m.positions; j0++) {
            int bestI = -1;
            double bestV = -1;
            for (int i = 0; i < m.numberAthletes; i++) {
                double v = x[m.var(i, j0)];
                if (v > bestV) {
                    bestV = v;
                    bestI = i;
                }
            }
            assign[j0] = bestI;
        }
        return assign;
    }

    public static String toStringsimplexXBnB(SwimmingClub club, boolean isMale, int pauseGap) {

        double[][] L = club.createMatrixForSimplex(isMale);
        SwimModel model = new SwimModel(L.length, pauseGap, L);
        Constraints base = Constraints.buildBaseConstraints(model);

        Solution sol = branchAndBound(model, base);

        StringBuilder sb = new StringBuilder();

        int[] assign = decodeAssignment(model, sol.bestX);
        int totalPoints = 0;
        int[] simpleOrder = isMale ? Competition.simpleOrderMale : Competition.simpleOrderFemale;
        for (int j0 = 0; j0 < 34; j0++) {
            int j = j0 + 1;
            int section = (j0 < 17) ? 1 : 2;
            int idx = (j0 % 17) + 1; // 1..17
            String eventName;

            eventName = SwimmingEvent.values()[simpleOrder[j0]].getDisplayName();
            Swimmer s = club.getRowNumberToSwimmer().get(assign[j0]);
            String swimmerName = s.getName();
            int points = s.getPointsForEventIndex(simpleOrder[j0]);
            totalPoints += points;

            sb.append(String.format("Abschnitt%d | %-6s | Punkte = %4d | %20s%n", section,
                    eventName, points, swimmerName));
        }
        sb.append("Best objective value = " + totalPoints + "\n");

        return sb.toString();
    }
}
