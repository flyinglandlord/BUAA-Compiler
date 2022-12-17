package frontend.grammar.stmt;

import frontend.grammar.expr.linked_expr.Exp;
import frontend.lexical.Token;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

// 'printf''('FormatString{','Exp}')'';'
public class PrintStmt extends Stmt{
    private final Token printfToken;
    private final Token leftParen;
    private final Token formatString;
    private final List<Token> commas;
    private final List<Exp> exp;
    private final Token rightParen;

    public PrintStmt(Token printfToken, Token leftParen,
                     Token formatString, List<Token> commas, List<Exp> exp,
                     Token rightParen, Token semicolon) {
        super(semicolon, printfToken.getLinenumber());
        this.printfToken = printfToken;
        this.leftParen = leftParen;
        this.formatString = formatString;
        this.commas = commas;
        this.exp = exp;
        this.rightParen = rightParen;
    }

    public PrintStmt(Token printfToken, Token leftParen, Token formatString, Token rightParen, Token semicolon) {
        super(semicolon, printfToken.getLinenumber());
        this.printfToken = printfToken;
        this.leftParen = leftParen;
        this.formatString = formatString;
        this.rightParen = rightParen;
        this.commas = null;
        this.exp = new ArrayList<>();
    }

    public Token getPrintfToken() {
        return printfToken;
    }

    public Token getLeftParen() {
        return leftParen;
    }

    public Token getFormatString() {
        return formatString;
    }

    public List<Token> getCommas() {
        return commas;
    }

    public List<Exp> getExp() {
        return exp;
    }

    public Token getRightParen() {
        return rightParen;
    }

    @Override
    public void print(PrintStream out) {
        out.println(printfToken.toString());
        out.println(leftParen.toString());
        out.println(formatString.toString());
        if (commas != null) {
            for (int i = 0; i < commas.size(); i++) {
                out.println(commas.get(i).toString());
                exp.get(i).print(out);
            }
        }
        if (rightParen != null) {
            out.println(rightParen.toString());
        }
        super.print(out);
    }
}
