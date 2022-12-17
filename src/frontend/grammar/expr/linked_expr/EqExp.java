package frontend.grammar.expr.linked_expr;

import frontend.lexical.Token;

import java.util.List;

public class EqExp extends LinkedExpr<RelExp> {
    public EqExp(RelExp first, List<RelExp> rest, List<Token> linkOperator) {
        super(first, linkOperator, rest, "<EqExp>");
    }
}
