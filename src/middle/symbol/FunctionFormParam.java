package middle.symbol;

import java.util.ArrayList;
import java.util.List;

// 参数肯定都是int类型的了，也不会是const类型
public class FunctionFormParam extends Symbol {
    public String name;
    private final int dimension;
    private final List<Integer> shape;

    // 数组参数构造函数
    public FunctionFormParam(String name, int dimension, List<Integer> shape) {
        this.name = name;
        this.dimension = dimension;
        this.shape = shape;
    }

    // 非数组参数构造函数
    public FunctionFormParam(String name) {
        this.name = name;
        this.dimension = 0;
        this.shape = new ArrayList<>();
    }

    @Override
    public String getName() {
        return name;
    }

    public int getDimension() {
        return dimension;
    }

    public List<Integer> getShape() {
        return shape;
    }

    public boolean isArray() {
        return dimension > 0;
    }

    // 函数形参肯定不是const类型
    public boolean isConst() {
        return false;
    }

    public String toString() {
        if (!isTemp() && !isString()) {
            return name + "@<" + getDepth() + "," + getLocation() + ">";
        } else {
            return name;
        }
    }

    // 函数形参如果是变量则size=4, 如果是数组，传地址size=4
    public int getSize() {
        return 4;
    }
}
