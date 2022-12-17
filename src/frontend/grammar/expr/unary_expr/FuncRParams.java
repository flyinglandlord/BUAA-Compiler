package frontend.grammar.expr.unary_expr;

import frontend.grammar.Node;
import frontend.grammar.expr.linked_expr.Exp;
import frontend.lexical.Token;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class FuncRParams implements Node {
    private final Exp firstParam;
    private final List<Token> commas;
    private final List<Exp> restParams;

    public FuncRParams(Exp first, List<Token> commas, List<Exp> params) {
        this.firstParam = first;
        this.commas = commas;
        this.restParams = params;
    }

    public Exp getFirstParam() {
        return firstParam;
    }

    public List<Token> getCommas() {
        return commas;
    }

    public List<Exp> getRestParams() {
        return restParams;
    }

    public List<Exp> getAllParams() {
        List<Exp> retParams = new ArrayList<>();
        if (firstParam != null) {
            retParams.add(firstParam);
        }
        retParams.addAll(restParams);
        return retParams;
    }

    @Override
    public void print(PrintStream out) {
        firstParam.print(out);
        for (int i = 0; i < commas.size(); i++) {
            out.println(commas.get(i).toString());
            restParams.get(i).print(out);
        }
        out.println("<FuncRParams>");
    }
}
