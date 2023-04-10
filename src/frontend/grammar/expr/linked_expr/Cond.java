package frontend.grammar.expr.linked_expr;

import frontend.grammar.Node;

import java.io.PrintStream;

public class Cond implements Node {
    private final LOrExp lOrExp;

    public Cond(LOrExp lOrExp) {
        this.lOrExp = lOrExp;
    }

    public LOrExp getLOrExp() {
        return lOrExp;
    }

    @Override
    public void print(PrintStream out) {
        lOrExp.print(out);
        out.println("<Cond>");
    }
}
