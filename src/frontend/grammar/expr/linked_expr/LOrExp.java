package frontend.grammar.expr.linked_expr;

import frontend.lexical.Token;

import java.util.List;

public class LOrExp extends LinkedExpr<LAndExp> {
    public LOrExp(LAndExp first, List<LAndExp> rest, List<Token> linkOperator) {
        super(first, linkOperator, rest, "<LOrExp>");
    }
}
