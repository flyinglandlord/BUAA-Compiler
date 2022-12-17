package frontend.grammar.stmt;

import frontend.lexical.Token;

import java.io.PrintStream;

// 仅含有一个分号
public class EmptyStmt extends Stmt {
    public EmptyStmt(Token semicolon) {
        super(semicolon, semicolon.getLinenumber());
    }

    @Override
    public void print(PrintStream out) {
        super.print(out);
    }
}
