package middle.middle_code.element;

import middle.middle_code.operand.Operand;
import middle.symbol.Symbol;

public class Assign extends MidCode {
    // dst可能为ArrayItem, Variable, FunctionFormParam
    private final Symbol dst;
    private final Operand src;

    public Assign(Symbol dst, Operand src) {
        this.dst = dst;
        this.src = src;
    }

    public Symbol getDst() {
        return dst;
    }

    public Operand getSrc() {
        return src;
    }

    public String toString() {
        return dst.toString() + " = " + src.toString();
    }
}
