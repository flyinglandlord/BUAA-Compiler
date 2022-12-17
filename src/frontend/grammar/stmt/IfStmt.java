package frontend.grammar.stmt;

import frontend.grammar.expr.linked_expr.Cond;
import frontend.lexical.Token;

import java.io.PrintStream;

// 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
public class IfStmt extends Stmt {
    private final Token ifToken;
    private final Token leftParen;
    private final Cond cond;
    private final Token rightParen;
    private final Stmt stmt;
    private final Token elseToken;
    private final Stmt elseStmt;

    public IfStmt(Token ifToken, Token leftParen,
                  Cond cond, Token rightParen, Stmt stmt,
                  Token elseToken, Stmt elseStmt) {
        super(null, ifToken.getLinenumber());
        this.ifToken = ifToken;
        this.leftParen = leftParen;
        this.cond = cond;
        this.rightParen = rightParen;
        this.stmt = stmt;
        this.elseToken = elseToken;
        this.elseStmt = elseStmt;
    }

    public IfStmt(Token ifToken, Token leftParen, Cond cond, Token rightParen, Stmt stmt) {
        super(null, ifToken.getLinenumber());
        this.ifToken = ifToken;
        this.leftParen = leftParen;
        this.cond = cond;
        this.rightParen = rightParen;
        this.stmt = stmt;
        this.elseToken = null;
        this.elseStmt = null;
    }

    public Token getIfToken() {
        return ifToken;
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

    public Token getElseToken() {
        return elseToken;
    }

    public Stmt getElseStmt() {
        return elseStmt;
    }

    @Override
    public void print(PrintStream out) {
        out.println(ifToken.toString());
        out.println(leftParen.toString());
        cond.print(out);
        if (rightParen != null) {
            out.println(rightParen.toString());
        }
        stmt.print(out);
        if (elseToken != null) {
            out.println(elseToken.toString());
            elseStmt.print(out);
        }
        super.print(out);
    }
}
