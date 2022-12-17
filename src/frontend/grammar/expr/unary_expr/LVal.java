package frontend.grammar.expr.unary_expr;

import frontend.lexical.Token;

import java.io.PrintStream;
import java.util.List;

// 左值事实上有且仅有变量、数组两种情况
// 注意subscriptList有可能为空
public class LVal implements PrimaryExpBase {
    private final Token ident;
    private final List<Subscript> subscriptList;

    public LVal(Token ident, List<Subscript> subscriptList) {
        this.ident = ident;
        this.subscriptList = subscriptList;
    }

    public Token getIdent() {
        return ident;
    }

    public List<Subscript> getSubscriptList() {
        return subscriptList;
    }

    public int getLinenumber() {
        return ident.getLinenumber();
    }

    public void print(PrintStream out) {
        out.println(ident.toString());
        if (subscriptList != null) {
            for (Subscript subscript : subscriptList) {
                subscript.print(out);
            }
        }
        out.println("<LVal>");
    }
}
