package linearProgramming;

import database.model.Verein;
import database.model.Wettkampf;
import objects.Competition;
import objects.Swimmer;
import objects.SwimmingClub;
import objects.SwimmingEvent;
import service.TeamOptimizationService;

import java.sql.Connection;
import java.sql.SQLException;
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
        return optimize(club, isMale, pauseGap).log();
    }

    public static String toStringsimplexXBnB(Connection connection, Verein club, Wettkampf competition,
                                               boolean isMale, int pauseGap) throws SQLException {
        return new TeamOptimizationService(connection)
                .generateAndSaveTeamForClub(club, competition, isMale, pauseGap);
    }

    public static OptimizationResult toStringsimplexXBnB(double[][] points, int[] swimmerIds,
                                                           String[] swimmerNames, int[] eventIndexes,
                                                           int pauseGap) {
        if (points.length != swimmerIds.length || points.length != swimmerNames.length
                || eventIndexes.length != 34) {
            throw new IllegalArgumentException("Optimierungsmatrix und Schwimmerdaten passen nicht zusammen.");
        }
        if (pauseGap < 0) {
            throw new IllegalArgumentException("Die Pausenlänge darf nicht negativ sein.");
        }
        for (double[] row : points) {
            if (row.length != 34) {
                throw new IllegalArgumentException("Jede Zeile der Optimierungsmatrix muss 34 Positionen enthalten.");
            }
        }
        for (int eventIndex : eventIndexes) {
            if (eventIndex < 0 || eventIndex >= SwimmingEvent.values().length) {
                throw new IllegalArgumentException("Ungültiger Eventindex in der Optimierungsreihenfolge.");
            }
        }

        SwimModel model = new SwimModel(points.length, pauseGap, points);
        Constraints base = Constraints.buildBaseConstraints(model);
        Solution solution = branchAndBound(model, base);
        int[] assignedSwimmerIds = new int[34];
        Arrays.fill(assignedSwimmerIds, -1);
        StringBuilder log = new StringBuilder();
        int totalPoints = 0;

        if (solution.bestX != null) {
            for (int position = 0; position < 34; position++) {
                int swimmerIndex = -1;
                for (int swimmer = 0; swimmer < points.length; swimmer++) {
                    if (solution.bestX[model.var(swimmer, position)] > 0.5) {
                        swimmerIndex = swimmer;
                        break;
                    }
                }
                if (swimmerIndex >= 0) {
                    assignedSwimmerIds[position] = swimmerIds[swimmerIndex];
                    int pointsForEvent = (int) Math.round(points[swimmerIndex][position]);
                    totalPoints += pointsForEvent;
                    log.append(String.format("Abschnitt%d | %-6s | Punkte = %4d | %20s%n",
                            position < 17 ? 1 : 2, SwimmingEvent.values()[eventIndexes[position]].getDisplayName(),
                            pointsForEvent, swimmerNames[swimmerIndex]));
                } else {
                    log.append(String.format("Abschnitt%d | %-6s | UNBESETZT%n",
                            position < 17 ? 1 : 2, SwimmingEvent.values()[eventIndexes[position]].getDisplayName()));
                }
            }
        } else {
            for (int position = 0; position < 34; position++) {
                log.append(String.format("Abschnitt%d | %-6s | UNBESETZT%n",
                        position < 17 ? 1 : 2, SwimmingEvent.values()[eventIndexes[position]].getDisplayName()));
            }
        }
        log.append("Best objective value = ").append(totalPoints).append('\n');
        return new OptimizationResult(log.toString(), assignedSwimmerIds, eventIndexes.clone(), totalPoints);
    }

    public static OptimizationResult optimize(SwimmingClub club, boolean isMale, int pauseGap) {
        int[] simpleOrder = getSimpleOrder(isMale);
        if (pauseGap < 0) {
            throw new IllegalArgumentException("Die Pausenlänge darf nicht negativ sein.");
        }
        double[][] L = club.createMatrixForSimplex(isMale);
        if (L.length == 0) {
            throw new IllegalStateException("Keine freien Schwimmer in der Datenbank gefunden.");
        }
        SwimModel model = new SwimModel(L.length, pauseGap, L);
        Constraints base = Constraints.buildBaseConstraints(model);

        Solution sol = branchAndBound(model, base);
        if (sol.bestX == null) {
            throw new IllegalStateException("Für die freien Schwimmer wurde keine zulässige Mannschaft gefunden.");
        }
        int[] assign = decodeAssignment(model, sol.bestX);
        StringBuilder sb = new StringBuilder();
        int[] assignedSwimmerIds = new int[34];
        int[] assignedEventIndexes = new int[34];
        int totalPoints = 0;
        for (int j0 = 0; j0 < 34; j0++) {
            int eventIndex = simpleOrder[j0];
            Swimmer s = club.getRowNumberToSwimmer().get(assign[j0]);
            if (s == null) {
                throw new IllegalStateException("Der Optimierer hat eine unvollständige Mannschaft geliefert.");
            }
            String swimmerName = s.getName();
            int points = Math.max(0, s.getPointsForEventIndex(eventIndex));
            totalPoints += points;
            assignedSwimmerIds[j0] = s.getID();
            assignedEventIndexes[j0] = eventIndex;
            sb.append(String.format("Abschnitt%d | %-6s | Punkte = %4d | %20s%n",
                    j0 < 17 ? 1 : 2, SwimmingEvent.values()[eventIndex].getDisplayName(), points, swimmerName));
        }
        sb.append("Best objective value = " + totalPoints + "\n");
        return new OptimizationResult(sb.toString(), assignedSwimmerIds, assignedEventIndexes, totalPoints);
    }

    private static int[] getSimpleOrder(boolean isMale) {
        String[] eventsPerSection = {
                "100Lg", "200K", "100B", "200R", "100S", "50B", "200Lg",
                "", "50S", "200B", "100R", "200S", "50K", "400Lg", "50R", "400K", "100K"
        };
        eventsPerSection[7] = isMale ? "1500K" : "800K";
        String[] eventNames = Arrays.copyOf(eventsPerSection, 34);
        System.arraycopy(eventsPerSection, 0, eventNames, 17, 17);
        eventNames[24] = isMale ? "800K" : "1500K";
        int[] order = new int[34];
        for (int i = 0; i < order.length; i++) {
            int eventIndex = SwimmingEvent.getByDisplayName(eventNames[i]).getIndex();
            order[i] = eventIndex;
        }
        if (isMale) {
            Competition.simpleOrderMale = order;
        } else {
            Competition.simpleOrderFemale = order;
        }
        return order;
    }

    public record OptimizationResult(String log, int[] swimmerIds, int[] eventIndexes, int totalPoints) {
    }
}
