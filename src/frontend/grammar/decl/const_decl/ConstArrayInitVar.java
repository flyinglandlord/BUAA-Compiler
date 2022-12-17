package frontend.grammar.decl.const_decl;

import frontend.grammar.Node;
import frontend.lexical.Token;

import java.io.PrintStream;
import java.util.List;

public class ConstArrayInitVar implements ConstInitVal, Node {
    private final Token leftBrace;
    private final Token rightBrace;

    private final ConstInitVal first;
    private final List<ConstInitVal> rest;
    private final List<Token> separators;

    public ConstArrayInitVar(Token leftBrace, Token rightBrace, ConstInitVal first,
                             List<ConstInitVal> rest, List<Token> separators) {
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

    public ConstInitVal getFirst() {
        return first;
    }

    public List<ConstInitVal> getRest() {
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
        out.println("<ConstInitVal>");
    }
}
