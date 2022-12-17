package frontend.grammar.decl.var_decl;

import frontend.grammar.expr.linked_expr.ConstExp;
import frontend.grammar.expr.linked_expr.Exp;

import java.io.PrintStream;

public class ExpInitVar implements InitVal {
    private final Exp exp;

    public ExpInitVar(Exp exp) {
        this.exp = exp;
    }

    public Exp getExp() {
        return exp;
    }

    @Override
    public void print(PrintStream out) {
        exp.print(out);
        out.println("<InitVal>");
    }
}
