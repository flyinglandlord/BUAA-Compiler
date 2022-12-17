package frontend.grammar.expr.linked_expr;

import frontend.lexical.Token;

import java.util.List;

public class LAndExp extends LinkedExpr<EqExp> {
    public LAndExp(EqExp first, List<EqExp> rest, List<Token> linkOperator) {
        super(first, linkOperator, rest, "<LAndExp>");
    }
}
