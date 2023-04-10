package frontend.grammar.stmt;

import frontend.grammar.expr.linked_expr.Cond;
import frontend.lexical.Token;

import java.io.PrintStream;

// 'while' '(' Cond ')' Stmt
public class WhileStmt extends Stmt {
    private final Token whileToken;
    private final Token leftParen;
    private final Cond cond;
    private final Token rightParen;
    private final Stmt stmt;

    public WhileStmt(Token whileToken, Token leftParen, Cond cond,
                     Token rightParen, Stmt stmt) {
        super(null, whileToken.getLinenumber());
        this.whileToken = whileToken;
        this.leftParen = leftParen;
        this.cond = cond;
        this.rightParen = rightParen;
        this.stmt = stmt;
    }

    public Token getWhileToken() {
        return whileToken;
    }

    public Token getLeftParen() {
        return leftParen;
    }

    public Cond getCond() {
        return cond;
    }

    public Token getRightParen() {
        return rightParen;
    }

    public Stmt getStmt() {
        return stmt;
    }

    @Override
    public void print(PrintStream out) {
        out.println(whileToken.toString());
        out.println(leftParen.toString());
        cond.print(out);
        if (rightParen != null) {
            out.println(rightParen.toString());
        }
        stmt.print(out);
        super.print(out);
    }
}
