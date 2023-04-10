package frontend.grammar.stmt;

import frontend.grammar.expr.unary_expr.LVal;
import frontend.lexical.Token;

public class SelfAddStmt extends Stmt {
    private LVal lVal;
    private Token operator;
    public SelfAddStmt(LVal lVal, Token operator, Token semicolon) {
        super(semicolon, lVal.getLinenumber());
        this.lVal = lVal;
        this.operator = operator;
    }

    public LVal getlVal() {
        return lVal;
    }

    public Token getOperator() {
        return operator;
    }

    @Override
    public void print(java.io.PrintStream out) {
        lVal.print(out);
        out.println(operator.toString());
        super.print(out);
    }
}
