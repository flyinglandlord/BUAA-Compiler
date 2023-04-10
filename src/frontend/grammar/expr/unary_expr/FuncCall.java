package frontend.grammar.expr.unary_expr;

import frontend.lexical.Token;

import java.util.List;

public class FuncCall implements UnaryExpBase {
    private final Token funcName;
    private final Token leftParen;
    private final FuncRParams funcRParamsList;
    private final Token rightParen;

    public FuncCall(Token funcName, Token leftParen, FuncRParams funcRParamsList, Token rightParen) {
        this.funcName = funcName;
        this.leftParen = leftParen;
        this.funcRParamsList = funcRParamsList;
        this.rightParen = rightParen;
    }

    public FuncCall(Token funcName, Token leftParen, Token rightParen) {
        this.funcName = funcName;
        this.leftParen = leftParen;
        this.funcRParamsList = null;
        this.rightParen = rightParen;
    }

    public Token getFuncName() {
        return funcName;
    }

    public Token getLeftParen() {
        return leftParen;
    }

    public FuncRParams getFuncRParamsList() {
        return funcRParamsList;
    }

    public Token getRightParen() {
        return rightParen;
    }

    public boolean hasParams() {
        return funcRParamsList != null;
    }

    @Override
    public void print(java.io.PrintStream out) {
        out.println(funcName.toString());
        out.println(leftParen);
        if (funcRParamsList != null) {
            funcRParamsList.print(out);
        }
        if (rightParen != null) {
            out.println(rightParen);
        }
    }
}
