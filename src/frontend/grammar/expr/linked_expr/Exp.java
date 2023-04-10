package frontend.grammar.expr.linked_expr;

import frontend.error.Error;
import frontend.grammar.Node;
import frontend.grammar.expr.unary_expr.*;
import frontend.grammar.expr.unary_expr.Number;

import java.io.PrintStream;

public class Exp implements Node {
    private final AddExp addExp;

    public Exp(AddExp addExp) {
        this.addExp = addExp;
    }

    public AddExp getAddExp() {
        return addExp;
    }

    public LVal getSingleLVal() {
        if (addExp.getRest().size() != 0) return null;
        MulExp mulExp = addExp.getFirst();
        if (mulExp.getRest().size() != 0) return null;
        UnaryExpBase base = mulExp.getFirst().getUnaryExpBase();
        if (!(base instanceof PrimaryExp)) return null;
        PrimaryExpBase primaryExpBase = ((PrimaryExp) base).getPrimaryExpBase();
        if (primaryExpBase instanceof LVal) return (LVal) primaryExpBase;
        else return null;
    }

    public FuncCall getSingleFuncCall() {
        if (addExp.getRest().size() != 0) return null;
        MulExp mulExp = addExp.getFirst();
        if (mulExp.getRest().size() != 0) return null;
        UnaryExpBase base = mulExp.getFirst().getUnaryExpBase();
        if (base instanceof FuncCall) return (FuncCall) base;
        else return null;
    }

    public int getSingleNumber() {
        if (addExp.getRest().size() != 0) return -1;
        MulExp mulExp = addExp.getFirst();
        if (mulExp.getRest().size() != 0) return -1;
        UnaryExpBase base = mulExp.getFirst().getUnaryExpBase();
        if (!(base instanceof PrimaryExp)) return -1;
        PrimaryExpBase primaryExpBase = ((PrimaryExp) base).getPrimaryExpBase();
        if (primaryExpBase instanceof Number) return ((Number) primaryExpBase).getIntConst().getValue();
        else return -1;
    }

    public int getLinenumber() {
        // TODO: 返回表达式首个Token的行号
        UnaryExpBase first1 = addExp.getFirst().getFirst().getUnaryExpBase();
        if (first1 instanceof FuncCall) {
            return ((FuncCall) first1).getFuncName().getLinenumber();
        } else if (first1 instanceof PrimaryExp) {
            PrimaryExpBase first2 = ((PrimaryExp) first1).getPrimaryExpBase();
            if (first2 instanceof SubExpr) {
                return ((SubExpr) first2).getLeftParen().getLinenumber();
            } else if (first2 instanceof LVal) {
                return ((LVal) first2).getLinenumber();
            } else if (first2 instanceof frontend.grammar.expr.unary_expr.Number) {
                return ((frontend.grammar.expr.unary_expr.Number) first2).getIntConst().getLinenumber();
            } else {
                throw new RuntimeException("Something wrong with the Expression.");
            }
        } else {
            throw new RuntimeException("Something wrong with the Expression.");
        }
    }

    @Override
    public void print(PrintStream out) {
        addExp.print(out);
        out.println("<Exp>");
    }
}
