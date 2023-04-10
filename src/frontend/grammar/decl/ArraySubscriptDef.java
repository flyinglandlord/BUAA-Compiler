package frontend.grammar.decl;

import frontend.grammar.Node;
import frontend.grammar.expr.linked_expr.ConstExp;
import frontend.lexical.Token;

import java.io.PrintStream;

public class ArraySubscriptDef implements Node {
    private final Token leftBracket;
    private final Token rightBracket;
    private final ConstExp arraySize;

    public ArraySubscriptDef(Token leftBracket, ConstExp arraySize, Token rightBracket) {
        this.leftBracket = leftBracket;
        this.arraySize = arraySize;
        this.rightBracket = rightBracket;
    }

    public Token getLeftBracket() {
        return leftBracket;
    }

    public Token getRightBracket() {
        return rightBracket;
    }

    public ConstExp getArraySize() {
        return arraySize;
    }

    @Override
    public void print(PrintStream out) {
        out.println(leftBracket.toString());
        arraySize.print(out);
        if (rightBracket != null) {
            out.println(rightBracket.toString());
        }
    }
}
