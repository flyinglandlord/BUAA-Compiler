package frontend.grammar.expr.linked_expr;

import frontend.lexical.Token;

import java.util.List;

public class AddExp extends LinkedExpr<MulExp>{
    public AddExp(MulExp first, List<MulExp> rest, List<Token> linkOperator) {
        super(first, linkOperator, rest, "<AddExp>");
    }
}
