package frontend.grammar.expr.linked_expr;

import frontend.grammar.expr.unary_expr.*;
import frontend.grammar.expr.unary_expr.Number;

import java.io.PrintStream;

public class ConstExp extends Exp {
    public ConstExp(AddExp addExp) {
        super(addExp);
    }

    public int getSingleNumber() {
        if (getAddExp().getRest().size() != 0) return -1;
        MulExp mulExp = getAddExp().getFirst();
        if (mulExp.getRest().size() != 0) return -1;
        UnaryExpBase base = mulExp.getFirst().getUnaryExpBase();
        if (!(base instanceof PrimaryExp)) return -1;
        PrimaryExpBase primaryExpBase = ((PrimaryExp) base).getPrimaryExpBase();
        if (primaryExpBase instanceof Number) return ((Number) primaryExpBase).getIntConst().getValue();
        else return -1;
    }

    @Override
    public void print(PrintStream out) {
        getAddExp().print(out);
        out.println("<ConstExp>");
    }
}
