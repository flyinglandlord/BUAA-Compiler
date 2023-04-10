package middle.middle_code.element;

import middle.middle_code.operand.Immediate;
import middle.middle_code.operand.Operand;
import middle.symbol.Symbol;

public class BinaryOp extends MidCode {
    public enum Op {
        ADD, SUB, MUL, DIV, MOD,
        GE, GT, LE, LT, EQ, NE,
        SLL, SRL, SRA,
        AND
    }
    private final Op op;
    private final Operand operand1;
    private final Operand operand2;
    // dst可能为ArrayItem, Variable, FunctionFormParam
    private final Symbol dst;

    public BinaryOp(Op op, Operand operand1, Operand operand2, Symbol dst) {
        // 保证如果有Immediate类型，operand1一定是Immediate
        this.op = op;
        this.operand1 = operand1;
        this.operand2 = operand2;
        this.dst = dst;
    }

    public Op getOp() {
        return op;
    }

    public Operand getOperand1() {
        return operand1;
    }

    public Operand getOperand2() {
        return operand2;
    }

    public Symbol getDst() {
        return dst;
    }

    public String toString() {
        if (op != null) return dst + " = " + operand1 + " " + op.name() + " " + operand2;
        else return dst + " = " + operand1 + " " + operand2;
    }
}
