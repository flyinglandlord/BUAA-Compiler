package frontend.grammar.stmt;

import frontend.grammar.expr.linked_expr.Exp;
import frontend.grammar.expr.unary_expr.LVal;
import frontend.lexical.Token;

import java.io.PrintStream;

// LVal '=' Exp ';'
public class AssignStmt extends Stmt {
    private final LVal lVal;
    private final Token assignToken;
    private final Exp exp;

    public AssignStmt(LVal lVal, Token assignToken, Exp exp, Token semicolon) {
        super(semicolon, lVal.getLinenumber());
        this.lVal = lVal;
        this.assignToken = assignToken;
        this.exp = exp;
    }

    public LVal getlVal() {
        return lVal;
    }

    public Token getAssignToken() {
        return assignToken;
    }

    public Exp getExp() {
        return exp;
    }

    @Override
    public void print(PrintStream out) {
        lVal.print(out);
        out.println(assignToken.toString());
        exp.print(out);
        super.print(out);
    }
}
