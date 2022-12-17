package frontend.grammar.func_def;

import frontend.grammar.Node;
import frontend.lexical.Token;

import java.io.PrintStream;
import java.util.List;

// 函数形参定义
// FuncFParam → BType Ident ['[' ']' { '[' ConstExp ']' }]
public class FuncFParam implements Node {
    private final Token bType;
    private final Token ident;
    private final List<FuncFParamArrayDef> subscripts;

    public FuncFParam(Token bType, Token ident, List<FuncFParamArrayDef> subscripts) {
        this.bType = bType;
        this.ident = ident;
        this.subscripts = subscripts;
    }

    public FuncFParam(Token bType, Token ident) {
        this.bType = bType;
        this.ident = ident;
        this.subscripts = null;
    }

    public Token getbType() {
        return bType;
    }

    public Token getIdent() {
        return ident;
    }

    public List<FuncFParamArrayDef> getSubscripts() {
        return subscripts;
    }

    @Override
    public void print(PrintStream out) {
        out.println(bType.toString());
        out.println(ident.toString());
        if (subscripts != null) {
            for (FuncFParamArrayDef subscript : subscripts) {
                subscript.print(out);
            }
        }
        out.println("<FuncFParam>");
    }
}
