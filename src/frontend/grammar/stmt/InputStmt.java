package frontend.grammar.stmt;

import frontend.grammar.expr.unary_expr.LVal;
import frontend.lexical.Token;

import java.io.PrintStream;

// 输入语句
// LVal '=' 'getint''('')'';'
public class InputStmt extends Stmt {
    private final LVal lVal;
    private final Token assignToken;
    private final Token getintToken;
    private final Token leftParen;
    private final Token rightParen;

    public InputStmt(LVal lVal, Token assignToken, Token getintToken,
                     Token leftParen, Token rightParen, Token semicolon) {
        super(semicolon, lVal.getLinenumber());
        this.lVal = lVal;
        this.assignToken = assignToken;
        this.getintToken = getintToken;
        this.leftParen = leftParen;
        this.rightParen = rightParen;
    }

    public LVal getlVal() {
        return lVal;
    }

    public Token getAssignToken() {
        return assignToken;
    }

    public Token getGetintToken() {
        return getintToken;
    }

    public Token getLeftParen() {
        return leftParen;
    }

    public Token getRightParen() {
        return rightParen;
    }

    @Override
    public void print(PrintStream out) {
        lVal.print(out);
        out.println(assignToken.toString());
        out.println(getintToken.toString());
        out.println(leftParen.toString());
        if (rightParen != null) {
            out.println(rightParen.toString());
        }
        super.print(out);
    }
}
