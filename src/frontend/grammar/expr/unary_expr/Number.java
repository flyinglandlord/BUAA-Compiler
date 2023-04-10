package frontend.grammar.expr.unary_expr;

import frontend.lexical.Token;

import java.io.PrintStream;

public class Number implements PrimaryExpBase {
    private final Token intConst;

    public Number(Token intConst) {
        this.intConst = intConst;
    }

    public Token getIntConst() {
        return intConst;
    }

    @Override
    public void print(PrintStream out) {
        out.println(intConst.toString());
        out.println("<Number>");
    }
}
