package middle.middle_code.element;

import middle.middle_code.operand.Operand;
import middle.symbol.Symbol;

public class UnaryOp extends MidCode {
    public enum Op {
        NOT, NEG, MOV
    }
    private final Op op;
    private final Operand operand1;
    private final Symbol dst;

    public UnaryOp(Op op, Operand operand1, Symbol dst) {
        this.op = op;
        this.operand1 = operand1;
        this.dst = dst;
    }

    public Op getOp() {
        return op;
    }

    public Operand getOperand1() {
        return operand1;
    }

    public Symbol getDst() {
        return dst;
    }

    public String toString() {
        if (op == middle.middle_code.element.UnaryOp.Op.MOV) {
            return dst + " = " + operand1;
        } else {
            return dst + " = " + op + " " + operand1;
        }
    }
}
