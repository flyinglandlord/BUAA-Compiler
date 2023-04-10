package middle.middle_code.element;

import middle.middle_code.operand.Operand;
import middle.symbol.Symbol;

public class LoadSave extends MidCode {
    public enum Op {
        LOAD,
        STORE
    }

    private final Op op;
    private final Symbol base;
    private final Operand offset;
    private final Symbol dst;
    private final Operand src;

    public LoadSave(Op op, Symbol base, Operand offset, Operand dstOrSrc) {
        this.op = op;
        this.base = base;
        this.offset = offset;
        if (op == Op.STORE) {
            this.src = dstOrSrc;
            this.dst = null;
        } else if (op == Op.LOAD) {
            this.dst = (Symbol) dstOrSrc;
            this.src = null;
        } else {
            throw new RuntimeException("Unknown op: " + op);
        }
    }

    public Op getOp() {
        return op;
    }

    public Symbol getBase() {
        return base;
    }

    public Operand getOffset() {
        return offset;
    }

    public Symbol getDst() {
        return dst;
    }

    public Operand getSrc() {
        return src;
    }

    public String toString() {
        if (op == Op.LOAD) {
            return dst + " = " + base + "[" + offset + "]";
        } else {
            return base + "[" + offset + "]" + " = " + src;
        }
    }
}
