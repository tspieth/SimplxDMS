package linearProgramming;

public class SimplexNode {

    public final int[] vars; // fixed var indices
    public final int[] vals; // 0 or 1

    SimplexNode(int[] vars, int[] vals) {
        this.vars = vars;
        this.vals = vals;
    }
}
