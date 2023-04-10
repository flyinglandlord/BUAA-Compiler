package frontend.grammar.func_def;

import frontend.grammar.Node;
import frontend.lexical.Token;

import java.io.PrintStream;

// FuncType → 'void' | 'int'
public class FuncType implements Node {
    private final Token bType;

    public FuncType(Token bType) {
        this.bType = bType;
    }

    public Token getbType() {
        return bType;
    }

    @Override
    public void print(PrintStream out) {
        out.println(bType.toString());
        out.println("<FuncType>");
    }
}
