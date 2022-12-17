package frontend.grammar.func_def;

import frontend.grammar.Node;
import frontend.grammar.stmt.Block;
import frontend.lexical.Token;

import java.io.PrintStream;

// MainFuncDef → 'int' 'main' '(' ')' Block
public class MainFuncDef implements Node {
    private final Token intToken;
    private final Token mainToken;
    private final Token leftParen;
    private final Token rightParen;
    private final Block block;

    public MainFuncDef(Token intToken, Token mainToken, Token leftParen, Token rightParen, Block block) {
        this.intToken = intToken;
        this.mainToken = mainToken;
        this.leftParen = leftParen;
        this.rightParen = rightParen;
        this.block = block;
    }

    public Token getIntToken() {
        return intToken;
    }

    public Token getMainToken() {
        return mainToken;
    }

    public Token getLeftParen() {
        return leftParen;
    }

    public Token getRightParen() {
        return rightParen;
    }

    public Block getBlock() {
        return block;
    }

    @Override
    public void print(PrintStream out) {
        out.println(intToken.toString());
        out.println(mainToken.toString());
        out.println(leftParen.toString());
        out.println(rightParen.toString());
        block.print(out);
        out.println("<MainFuncDef>");
    }
}
