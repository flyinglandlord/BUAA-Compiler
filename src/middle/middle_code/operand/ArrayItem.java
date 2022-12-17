package middle.middle_code.operand;

import middle.symbol.Symbol;
import middle.symbol.Variable;

import java.util.List;

// 用于数组作为左值时的具体数组值
public class ArrayItem implements Operand {
    private final Symbol base;
    private final Operand offset;
    // 一定是满维数

    public ArrayItem(Symbol base, Operand offset) {
        this.base = base;
        this.offset = offset;
    }

    public List<Integer> getShape() {
        return base.getShape();
    }

    public Symbol getBase() {
        return base;
    }

    public Operand getOffset() {
        return offset;
    }

    public String toString() {
        return base.getName() + "[" + offset + "]";
    }
}
