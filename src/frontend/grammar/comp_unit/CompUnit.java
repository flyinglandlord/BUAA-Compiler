package frontend.grammar.comp_unit;

import frontend.grammar.Node;
import frontend.grammar.decl.Decl;
import frontend.grammar.func_def.FuncDef;
import frontend.grammar.func_def.MainFuncDef;

import java.io.PrintStream;
import java.util.List;

// CompUnit → {Decl} {FuncDef} MainFuncDef
public class CompUnit implements Node {
    private final List<Decl> declList;
    private final List<FuncDef> funcDefList;
    private final MainFuncDef mainFuncDef;

    public CompUnit(List<Decl> declList, List<FuncDef> funcDefList, MainFuncDef mainFuncDef) {
        this.declList = declList;
        this.funcDefList = funcDefList;
        this.mainFuncDef = mainFuncDef;
    }

    public List<Decl> getDeclList() {
        return declList;
    }

    public List<FuncDef> getFuncDefList() {
        return funcDefList;
    }

    public MainFuncDef getMainFuncDef() {
        return mainFuncDef;
    }

    @Override
    public void print(PrintStream out) {
        for (Decl decl : declList) {
            decl.print(out);
        }
        for (FuncDef funcDef : funcDefList) {
            funcDef.print(out);
        }
        mainFuncDef.print(out);
        out.println("<CompUnit>");
    }
}
