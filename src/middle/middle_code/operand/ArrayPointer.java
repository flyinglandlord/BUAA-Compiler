package middle.middle_code.operand;

import middle.symbol.Symbol;

import java.util.List;

// 用于传递函数参数时作为实参的数组指针
public class ArrayPointer extends Symbol {
    private final Symbol base;
    // 作为函数实参传递时的维数
    private final int dimension;
    // 传递时，传入的维度的下标，即这个List的size为dimension
    private final Operand offset;

    public ArrayPointer(Symbol base, int dimension, Operand offset) {
        this.offset = offset;
        if (!base.isArray())
            throw new RuntimeException("base is not array");
        this.base = base;
        this.dimension = dimension;
    }

    public Operand getOffset() {
        return offset;
    }

    public Symbol getBase() {
        return base;
    }

    public int getDimension() {
        return dimension;
    }

    @Override
    public String getName() {
        return base.getName();
    }

    @Override
    public boolean isArray() {
        return true;
    }

    @Override
    public boolean isConst() {
        return false;
    }

    @Override
    public int getSize() {
        return base.getSize();
    }

    @Override
    public List<Integer> getShape() {
        return base.getShape();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Pointer#");
        sb.append(base);
        sb.append("[").append(offset).append("]");
        return sb.toString();
    }
}
