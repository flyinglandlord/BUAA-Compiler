package frontend.grammar.expr.unary_expr;

import frontend.grammar.Node;
import frontend.lexical.Token;

import java.io.PrintStream;
import java.util.List;

public class UnaryExp implements Node {
    private final UnaryExpBase unaryExpBase;
    private final List<Token> operators;

    public UnaryExp(UnaryExpBase unaryExpBase, List<Token> operators) {
        this.unaryExpBase = unaryExpBase;
        this.operators = operators;
    }

    public UnaryExpBase getUnaryExpBase() {
        return unaryExpBase;
    }

    public List<Token> getOperators() {
        return operators;
    }

    @Override
    public void print(PrintStream out) {
        for (Token operator : operators) {
            out.println(operator.toString());
            out.println("<UnaryOp>");
        }
        unaryExpBase.print(out);
        out.println("<UnaryExp>");
        int i = operators.size();
        while (i > 0) {
            out.println("<UnaryExp>");
            i--;
        }
    }
}
