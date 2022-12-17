package frontend.grammar.decl.const_decl;

import frontend.grammar.Node;
import frontend.grammar.decl.ArraySubscriptDef;
import frontend.lexical.Token;

import java.io.PrintStream;
import java.util.List;

public class ConstDef implements Node {
    private final Token ident;
    private final List<ArraySubscriptDef> arraySubscriptDefs;
    private final Token assignToken;
    private final ConstInitVal constInitVal;


    public ConstDef(Token ident, List<ArraySubscriptDef> arraySubscriptDefs,
                    Token assignToken, ConstInitVal constInitVal) {
        this.ident = ident;
        this.arraySubscriptDefs = arraySubscriptDefs;
        this.assignToken = assignToken;
        this.constInitVal = constInitVal;
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

    public ConstInitVal getConstInitVal() {
        return constInitVal;
    }

    @Override
    public void print(PrintStream out) {
        out.println(ident.toString());
        if (arraySubscriptDefs != null) {
            for (ArraySubscriptDef arraySubscriptDef : arraySubscriptDefs) {
                arraySubscriptDef.print(out);
            }
        }
        out.println(assignToken.toString());
        constInitVal.print(out);
        out.println("<ConstDef>");
    }
}
