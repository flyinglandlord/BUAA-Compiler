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
    private final Token getIntToken;

    public VarDef(Token ident, Token assignToken, Token getIntToken) {
        this.ident = ident;
        this.arraySubscriptDefs = null;
        this.assignToken = assignToken;
        this.initVal = null;
        this.getIntToken = getIntToken;
    }


    public VarDef(Token ident, List<ArraySubscriptDef> arraySubscriptDefs,
                    Token assignToken, InitVal initVal) {
        this.ident = ident;
        this.arraySubscriptDefs = arraySubscriptDefs;
        this.assignToken = assignToken;
        this.initVal = initVal;
        this.getIntToken = null;
    }

    public VarDef(Token ident, List<ArraySubscriptDef> arraySubscriptDefs) {
        this.ident = ident;
        this.arraySubscriptDefs = arraySubscriptDefs;
        this.assignToken = null;
        this.initVal = null;
        this.getIntToken = null;
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

    public Token getGetIntToken() {
        return getIntToken;
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
