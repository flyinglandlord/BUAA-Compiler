package frontend.grammar.decl.var_decl;

import frontend.grammar.decl.const_decl.ConstInitVal;
import frontend.lexical.Token;

import java.io.PrintStream;
import java.util.List;

public class ArrayInitVar implements InitVal {
    private final Token leftBrace;
    private final Token rightBrace;

    private final InitVal first;
    private final List<InitVal> rest;
    private final List<Token> separators;

    public ArrayInitVar(Token leftBrace, Token rightBrace, InitVal first,
                             List<InitVal> rest, List<Token> separators) {
        this.leftBrace = leftBrace;
        this.rightBrace = rightBrace;
        this.first = first;
        this.rest = rest;
        this.separators = separators;
    }

    public Token getLeftBrace() {
        return leftBrace;
    }

    public Token getRightBrace() {
        return rightBrace;
    }

    public InitVal getFirst() {
        return first;
    }

    public List<InitVal> getRest() {
        return rest;
    }

    public List<Token> getSeparators() {
        return separators;
    }

    @Override
    public void print(PrintStream out) {
        out.println(leftBrace.toString());
        if (first != null) {
            first.print(out);
        }
        for (int i = 0; i < rest.size(); i++) {
            out.println(separators.get(i).toString());
            rest.get(i).print(out);
        }
        out.println(rightBrace.toString());
        out.println("<InitVal>");
    }
}
