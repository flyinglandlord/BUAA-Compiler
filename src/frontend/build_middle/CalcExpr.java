package frontend.build_middle;

import frontend.error.Error;
import frontend.error.ErrorTable;
import frontend.grammar.decl.var_decl.ArrayInitVar;
import frontend.grammar.decl.var_decl.ExpInitVar;
import frontend.grammar.decl.var_decl.InitVal;
import frontend.grammar.expr.linked_expr.AddExp;
import frontend.grammar.expr.linked_expr.Exp;
import frontend.grammar.expr.linked_expr.MulExp;
import frontend.grammar.expr.unary_expr.*;
import frontend.lexical.Token;
import middle.middle_code.operand.Immediate;
import middle.middle_code.operand.Operand;
import middle.symbol.FunctionFormParam;
import middle.symbol.Symbol;
import middle.symbol.SymbolTable;
import middle.symbol.Variable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CalcExpr {
    private final SymbolTable symbolTable;
    private final ErrorTable errorTable;

    public CalcExpr(SymbolTable symbolTable, ErrorTable errorTable) {
        this.symbolTable = symbolTable;
        this.errorTable = errorTable;
    }

    public int calcExp(Exp exp) throws ConstExpException {
        return calcAddExp(exp.getAddExp());
    }

    public int calcAddExp(AddExp exp) throws ConstExpException {
        int result = calcMulExp(exp.getFirst());
        Iterator<Token> iterOperator = exp.getLinkOperator().iterator();
        Iterator<MulExp> iterOperand = exp.getRest().iterator();
        while (iterOperator.hasNext() && iterOperand.hasNext()) {
            Token op = iterOperator.next();
            MulExp mul = iterOperand.next();
            switch (op.getTokenType()) {
                case PLUS: result = result + calcMulExp(mul); break;
                case MINU: result = result - calcMulExp(mul); break;
                default: assert false;
            }
        }
        return result;
    }

    public int calcMulExp(MulExp exp) throws ConstExpException {
        int result = calcUnaryExp(exp.getFirst());
        Iterator<Token> iterOperator = exp.getLinkOperator().iterator();
        Iterator<UnaryExp> iterOperand = exp.getRest().iterator();
        while (iterOperator.hasNext() && iterOperand.hasNext()) {
            Token op = iterOperator.next();
            UnaryExp unary = iterOperand.next();
            switch (op.getTokenType()) {
                case MULT: result = result * calcUnaryExp(unary); break;
                case DIV: result = result / calcUnaryExp(unary); break;
                case MOD: result = result % calcUnaryExp(unary); break;
                default: assert false;
            }
        }
        return result;
    }

    public int calcUnaryExp(UnaryExp exp) throws ConstExpException {
        UnaryExpBase base = exp.getUnaryExpBase();
        int result = 0;
        if (base instanceof FuncCall) {
            FuncCall call = (FuncCall) base;
            throw new ConstExpException(call.getFuncName().getLinenumber(), call.getFuncName().getContent());
        } else if (base instanceof PrimaryExp) {
            PrimaryExpBase primary = ((PrimaryExp) base).getPrimaryExpBase();
            if (primary instanceof SubExpr) {
                result = calcSubExp((SubExpr) primary);
            } else if (primary instanceof LVal) {
                result = calcLVal((LVal) primary);
            } else if (primary instanceof frontend.grammar.expr.unary_expr.Number) {
                result = extractNumber((frontend.grammar.expr.unary_expr.Number) primary);
            } else assert false;
        } else assert false;
        for (Token op : exp.getOperators()) {
            switch (op.getTokenType()) {
                case PLUS: break;
                case MINU: result = -result; break;
                case NOT: result = (result == 0) ? 1 : 0; break;
                default: assert false;
            }
        }
        return result;
    }

    public int extractNumber(frontend.grammar.expr.unary_expr.Number number) {
        return number.getIntConst().getValue();
    }

    public int calcSubExp(SubExpr exp) throws ConstExpException {
        return calcExp(exp.getExp());
    }

    public int calcLVal(LVal lVal) throws ConstExpException {
        String name = lVal.getIdent().getContent();
        if (!symbolTable.contains(name, true)) {
            errorTable.add(new Error(Error.ErrorType.UNDEFINED_IDENT, lVal.getIdent().getLinenumber()));
            return 0;
        }
        Symbol symbol = symbolTable.get(name, true);

        if (!symbol.isArray()) {
            if (symbol instanceof FunctionFormParam) throw new ConstExpException(lVal.getIdent().getLinenumber(), name);
            else if (symbol instanceof Variable) {
                if (symbol.isConst() || (symbol.getDepth() == 0 && !symbol.isTemp())) {
                    if (((Variable) symbol).getCalcConstInitVal() == null ||
                            ((Variable) symbol).getCalcConstInitVal().isEmpty()) return 0;
                    else return ((Variable) symbol).getCalcConstInitVal().get(0);
                } else {
                    if (((Variable) symbol).getInitVal() instanceof Immediate)
                        return ((Immediate) ((Variable) symbol).getInitVal()).getValue();
                    else return 0;
                }
            }
        } else {
            if (symbol instanceof FunctionFormParam) throw new ConstExpException(lVal.getIdent().getLinenumber(), name);
            ArrayList<Integer> indexes = new ArrayList<>();
            for (Subscript subscript : lVal.getSubscriptList()) {
                indexes.add(calcExp(subscript.getIndex()));
            }
            int base = 1;
            int offset = 0;
            for (int i = indexes.size() - 1; i >= 0; i--) {
                offset += indexes.get(i) * base;
                if (i > 0) {
                    base = base * symbol.getShape().get(i);
                }
            }
            if (symbol.isConst() || (symbol.getDepth() == 0 && !symbol.isTemp())) {
                if (((Variable) symbol).getCalcConstInitVal() == null ||
                        ((Variable) symbol).getCalcConstInitVal().isEmpty()) return 0;
                else return ((Variable) symbol).getCalcConstInitVal().get(offset);
            } else {
                return 0;
            }
        }
        return 0;
    }
}
