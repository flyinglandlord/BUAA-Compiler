package frontend.grammar.expr.unary_expr;

import java.io.PrintStream;

public class PrimaryExp implements UnaryExpBase {
    private final PrimaryExpBase primaryExpBase;

    public PrimaryExp(PrimaryExpBase primaryExpBase) {
        this.primaryExpBase = primaryExpBase;
    }

    public PrimaryExpBase getPrimaryExpBase() {
        return primaryExpBase;
    }

    @Override
    public void print(PrintStream out) {
        primaryExpBase.print(out);
        out.println("<PrimaryExp>");
    }
}
