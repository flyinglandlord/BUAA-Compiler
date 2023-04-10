package frontend.grammar.func_def;

import frontend.grammar.Node;
import frontend.grammar.expr.linked_expr.ConstExp;
import frontend.lexical.Token;

import java.io.PrintStream;

// 函数形参数组下标定义
// TODO: 未来可以把类名改成ArraySubscriptDef之类的
// FuncFParamArrayDef -> '[' ConstExp ']'
public class FuncFParamArrayDef implements Node {
    private final Token leftBracket;
    private final ConstExp constExp;
    private final Token rightBracket;

    public FuncFParamArrayDef(Token leftBracket, ConstExp constExp, Token rightBracket) {
        this.leftBracket = leftBracket;
        this.constExp = constExp;
        this.rightBracket = rightBracket;
    }

    public FuncFParamArrayDef(Token leftBracket, Token rightBracket) {
        this.leftBracket = leftBracket;
        this.constExp = null;
        this.rightBracket = rightBracket;
    }

    public Token getLeftBracket() {
        return leftBracket;
    }

    public ConstExp getConstExp() {
        return constExp;
    }

    public Token getRightBracket() {
        return rightBracket;
    }


    @Override
    public void print(PrintStream out) {
        out.println(leftBracket.toString());
        if (constExp != null) {
            constExp.print(out);
        }
        if (rightBracket != null) {
            out.println(rightBracket.toString());
        }
    }
}
