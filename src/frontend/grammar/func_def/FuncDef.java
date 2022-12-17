package frontend.grammar.func_def;

import frontend.grammar.Node;
import frontend.grammar.stmt.Block;
import frontend.lexical.Token;

import java.io.PrintStream;

// FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
public class FuncDef implements Node {
    private final FuncType funcType;
    private final Token ident;
    private final Token leftParen;
    private final FuncFParams funcFParams;
    private final Token rightParen;
    private final Block block;

    public FuncDef(FuncType funcType, Token ident, Token leftParen, FuncFParams funcFParams, Token rightParen, Block block) {
        this.funcType = funcType;
        this.ident = ident;
        this.leftParen = leftParen;
        this.funcFParams = funcFParams;
        this.rightParen = rightParen;
        this.block = block;
    }

    public FuncDef(FuncType funcType, Token ident, Token leftParen, Token rightParen, Block block) {
        this.funcType = funcType;
        this.ident = ident;
        this.leftParen = leftParen;
        this.funcFParams = null;
        this.rightParen = rightParen;
        this.block = block;
    }

    public FuncType getFuncType() {
        return funcType;
    }

    public Token getIdent() {
        return ident;
    }

    public Token getLeftParen() {
        return leftParen;
    }

    public FuncFParams getFuncFParams() {
        return funcFParams;
    }

    public Token getRightParen() {
        return rightParen;
    }

    public Block getBlock() {
        return block;
    }

    @Override
    public void print(PrintStream out) {
        funcType.print(out);
        out.println(ident.toString());
        out.println(leftParen.toString());
        if (funcFParams != null) {
            funcFParams.print(out);
        }
        if (rightParen != null) {
            out.println(rightParen.toString());
        }
        block.print(out);
        out.println("<FuncDef>");
    }
}
