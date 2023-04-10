package frontend.grammar.expr.linked_expr;

import frontend.grammar.expr.unary_expr.UnaryExp;
import frontend.lexical.Token;

import java.util.List;

public class MulExp extends LinkedExpr<UnaryExp> {
    public MulExp(UnaryExp first, List<UnaryExp> rest, List<Token> linkOperator) {
        super(first, linkOperator, rest, "<MulExp>");
    }
}
