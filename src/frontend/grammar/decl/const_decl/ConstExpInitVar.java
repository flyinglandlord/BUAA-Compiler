package frontend.grammar.decl.const_decl;

import frontend.grammar.Node;
import frontend.grammar.expr.linked_expr.ConstExp;

import java.io.PrintStream;

public class ConstExpInitVar implements ConstInitVal, Node {
    private final ConstExp constExp;

    public ConstExpInitVar(ConstExp constExp) {
        this.constExp = constExp;
    }

    public ConstExp getConstExp() {
        return constExp;
    }

    @Override
    public void print(PrintStream out) {
        constExp.print(out);
        out.println("<ConstInitVal>");
    }
}
