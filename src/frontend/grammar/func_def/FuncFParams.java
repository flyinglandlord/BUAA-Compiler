package frontend.grammar.func_def;

import frontend.grammar.Node;
import frontend.lexical.Token;

import java.io.PrintStream;
import java.util.List;

// 函数形参列表
// FuncFParams → FuncFParam { ',' FuncFParam }
public class FuncFParams implements Node {
    private final FuncFParam first;
    private final List<Token> commas;
    private final List<FuncFParam> rest;

    public FuncFParams(FuncFParam first, List<Token> commas, List<FuncFParam> rest) {
        this.first = first;
        this.commas = commas;
        this.rest = rest;
    }

    public FuncFParam getFirst() {
        return first;
    }

    public List<Token> getCommas() {
        return commas;
    }

    public List<FuncFParam> getRest() {
        return rest;
    }


    @Override
    public void print(PrintStream out) {
        first.print(out);
        for (int i = 0; i < commas.size(); i++) {
            out.println(commas.get(i).toString());
            rest.get(i).print(out);
        }
        out.println("<FuncFParams>");
    }
}
