package frontend.grammar.expr.unary_expr;

import frontend.grammar.expr.linked_expr.Exp;
import frontend.lexical.Token;

import java.io.PrintStream;

public class SubExpr implements PrimaryExpBase {
    private final Token leftParen;
    private final Exp exp;
    private final Token rightParen;

    public SubExpr(Token leftParen, Exp exp, Token rightParen) {
        this.leftParen = leftParen;
        this.exp = exp;
        this.rightParen = rightParen;
    }

    public Token getLeftParen() {
        return leftParen;
    }

    public Exp getExp() {
        return exp;
    }

    public Token getRightParen() {
        return rightParen;
    }

    @Override
    public void print(PrintStream out) {
        out.println(leftParen.toString());
        exp.print(out);
        if (rightParen != null) {
            out.println(rightParen.toString());
        }
    }
}
