package frontend.grammar.decl.var_decl;

import frontend.grammar.Node;
import frontend.grammar.decl.ArraySubscriptDef;;
import frontend.lexical.Token;

import java.io.PrintStream;
import java.util.List;

public class VarDef implements Node {
    private final Token ident;
    private final List<ArraySubscriptDef> arraySubscriptDefs;
    private final Token assignToken;
    private final InitVal initVal;


    public VarDef(Token ident, List<ArraySubscriptDef> arraySubscriptDefs,
                    Token assignToken, InitVal initVal) {
        this.ident = ident;
        this.arraySubscriptDefs = arraySubscriptDefs;
        this.assignToken = assignToken;
        this.initVal = initVal;
    }

    public VarDef(Token ident, List<ArraySubscriptDef> arraySubscriptDefs) {
        this.ident = ident;
        this.arraySubscriptDefs = arraySubscriptDefs;
        this.assignToken = null;
        this.initVal = null;
    }

    public Token getIdent() {
        return ident;
    }

    public List<ArraySubscriptDef> getArrayDefs() {
        return arraySubscriptDefs;
    }

    public Token getAssignToken() {
        return assignToken;
    }

    public InitVal getInitVal() {
        return initVal;
    }

    @Override
    public void print(PrintStream out) {
        out.println(ident.toString());
        for (ArraySubscriptDef arraySubscriptDef : arraySubscriptDefs) arraySubscriptDef.print(out);
        if (assignToken != null) out.println(assignToken);
        if (initVal != null) initVal.print(out);
        out.println("<VarDef>");
    }
}
