package frontend.grammar.expr.unary_expr;

import frontend.grammar.Node;
import frontend.grammar.expr.linked_expr.Exp;
import frontend.lexical.Token;

import java.io.PrintStream;

public class Subscript implements Node {
    private final Token leftBracket;
    private final Token rightBracket;
    private final Exp index;

    public Subscript(Token leftBracket, Token rightBracket, Exp index) {
        this.leftBracket = leftBracket;
        this.rightBracket = rightBracket;
        this.index = index;
    }

    public Token getLeftBracket() {
        return leftBracket;
    }

    public Token getRightBracket() {
        return rightBracket;
    }

    public Exp getIndex() {
        return index;
    }

    @Override
    public void print(PrintStream out) {
        out.println(leftBracket.toString());
        index.print(out);
        if (rightBracket != null) {
            out.println(rightBracket.toString());
        }
    }
}
