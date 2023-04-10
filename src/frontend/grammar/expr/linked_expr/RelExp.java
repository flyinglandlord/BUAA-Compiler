package frontend.grammar.expr.linked_expr;

import frontend.lexical.Token;

import java.util.List;

public class RelExp extends LinkedExpr<AddExp> {
    public RelExp(AddExp first, List<AddExp> rest, List<Token> linkOperator) {
        super(first, linkOperator, rest, "<RelExp>");
    }
}
