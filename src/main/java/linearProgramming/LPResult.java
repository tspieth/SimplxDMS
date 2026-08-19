package linearProgramming;

public class LPResult {
    final boolean feasible;
    final double[] x;
    final double value;

    LPResult(boolean feasible, double[] x, double value) {
        this.feasible = feasible;
        this.x = x;
        this.value = value;
    }

    static LPResult solveLP(SwimModel m, Constraints base, SimplexNode node) {
        int n = m.nVars();

        // Build A,b with fixings appended
        int mBase = base.b.length;
        int mExtra = (node == null) ? 0 : node.vars.length;
        double[][] A = new double[mBase + mExtra][n];
        double[] b = new double[mBase + mExtra];

        // copy base
        for (int i = 0; i < mBase; i++) {
            A[i] = base.A[i]; // safe: we never mutate rows in solver construction
            b[i] = base.b[i];
        }

        // append fix constraints as <=
        // fix to 0: x_k <= 0
        // fix to 1: x_k >= 1 => -x_k <= -1
        if (node != null) {
            for (int t = 0; t < node.vars.length; t++) {
                int varIdx = node.vars[t];
                int val = node.vals[t];
                double[] row = new double[n];
                if (val == 0) {
                    row[varIdx] = 1.0;
                    A[mBase + t] = row;
                    b[mBase + t] = 0.0;
                } else {
                    row[varIdx] = -1.0;
                    A[mBase + t] = row;
                    b[mBase + t] = -1.0;
                }
            }
        }

        double[] c = SwimModel.buildObjective(m);
        double[] x = new double[n];
        DoubleSimplexSolver solver = new DoubleSimplexSolver(A, b, c);
        double val = solver.solve(x);

        if (Double.isNaN(val))
            return new LPResult(false, null, Double.NEGATIVE_INFINITY);
        if (Double.isInfinite(val))
            return new LPResult(false, null, Double.POSITIVE_INFINITY);

        return new LPResult(true, x, val);
    }
}