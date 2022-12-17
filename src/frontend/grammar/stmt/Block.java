package frontend.grammar.stmt;

import frontend.lexical.Token;

import java.io.PrintStream;
import java.util.List;

// '{' { BlockItem } '}'
public class Block extends Stmt {
    private final Token leftBrace;
    private final List<BlockItem> blockItems;
    private final Token rightBrace;

    public Block(Token leftBrace, List<BlockItem> blockItems, Token rightBrace) {
        super(null, leftBrace.getLinenumber());
        this.leftBrace = leftBrace;
        this.blockItems = blockItems;
        this.rightBrace = rightBrace;
    }

    public Token getLeftBrace() {
        return leftBrace;
    }

    public List<BlockItem> getBlockItems() {
        return blockItems;
    }

    public Token getRightBrace() {
        return rightBrace;
    }

    @Override
    public void print(PrintStream out) {
        out.println(leftBrace.toString());
        for (BlockItem blockItem : blockItems) {
            blockItem.print(out);
        }
        out.println(rightBrace.toString());
        out.println("<Block>");
    }
}
