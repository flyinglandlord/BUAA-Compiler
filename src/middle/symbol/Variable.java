package middle.symbol;

import frontend.grammar.decl.const_decl.ConstInitVal;
import frontend.grammar.decl.var_decl.InitVal;

import java.util.ArrayList;
import java.util.List;

// 变量也总是int类型的
public class Variable extends Symbol {
    private final String name;
    private final int dimension;
    private final List<Integer> shape;
    private final boolean isConst;
    private final InitVal initVal;
    private final ConstInitVal constInitVal;
    // 如果是数组，就依次存放，不足的填0
    // 如果是变量，则初始值就是 calcConstInitVal[0]
    // 这一项仅仅对const类型或者全局的变量有用
    // 因为全局变量的初始值也一定可以算出一个常数的
    // 但是以后就不能再用了
    private List<Integer> calcConstInitVal = new ArrayList<>();

    // 数组变量构造函数
    public Variable(String name, boolean isConst, InitVal initVal, int dimension, List<Integer> shape) {
        this.name = name;
        this.isConst = isConst;
        this.initVal = initVal;
        this.constInitVal = null;
        this.dimension = dimension;
        this.shape = shape;
    }

    // 数组变量构造函数，未初始化
    public Variable(String name, boolean isConst, int dimension, List<Integer> shape) {
        this.name = name;
        this.isConst = isConst;
        this.initVal = null;
        this.constInitVal = null;
        this.dimension = dimension;
        this.shape = shape;
    }

    // 数组常量构造函数
    public Variable(String name, boolean isConst, ConstInitVal constInitVal,
                    int dimension, List<Integer> shape, List<Integer> calcConstInitVal) {
        this.name = name;
        this.isConst = isConst;
        this.constInitVal = constInitVal;
        this.initVal = null;
        this.dimension = dimension;
        this.shape = shape;
        this.calcConstInitVal = calcConstInitVal;
    }

    // 全局数组被初始化一定能算出值
    public Variable(String name, boolean isConst, InitVal initVal,
                    int dimension, List<Integer> shape, List<Integer> calcInitVal) {
        this.name = name;
        this.isConst = isConst;
        this.constInitVal = null;
        this.initVal = initVal;
        this.dimension = dimension;
        this.shape = shape;
        this.calcConstInitVal = calcInitVal;
    }

    // 全局变量被初始化一定能算出值
    public Variable(String name, boolean isConst, InitVal initVal, int calcInitVal) {
        this.name = name;
        this.isConst = isConst;
        this.initVal = initVal;
        this.constInitVal = null;
        this.dimension = 0;
        this.shape = new ArrayList<>();
        this.calcConstInitVal = new ArrayList<>();
        this.calcConstInitVal.add(calcInitVal);
    }

    // 非数组变量构造函数
    public Variable(String name, boolean isConst, InitVal initVal) {
        this.name = name;
        this.isConst = isConst;
        this.initVal = initVal;
        this.constInitVal = null;
        this.dimension = 0;
        this.shape = new ArrayList<>();
    }

    // 非数组变量构造函数，未被初始化
    public Variable(String name, boolean isConst) {
        this.name = name;
        this.isConst = isConst;
        this.initVal = null;
        this.constInitVal = null;
        this.dimension = 0;
        this.shape = new ArrayList<>();
    }

    // 非数组常量构造函数
    public Variable(String name, boolean isConst, ConstInitVal constInitVal, int calcConstInitVal) {
        this.name = name;
        this.isConst = isConst;
        this.initVal = null;
        this.constInitVal = constInitVal;
        this.dimension = 0;
        this.shape = new ArrayList<>();
        this.calcConstInitVal = new ArrayList<>();
        this.calcConstInitVal.add(calcConstInitVal);
    }

    public List<Integer> getCalcConstInitVal() {
        return calcConstInitVal;
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

    public boolean isConst() {
        return isConst;
    }

    public InitVal getInitVal() {
        return initVal;
    }

    public ConstInitVal getConstInitVal() {
        return constInitVal;
    }

    public boolean isArray() {
        return dimension > 0;
    }

    public String toString() {
        if (!isTemp() && !isString()) {
            return name + "@<" + getDepth() + "," + getLocation() + ">";
        } else return name;
    }

    public int getSize() {
        int size = 1;
        for (int dimensionSize: shape) {
            size *= dimensionSize;
        }
        return size * 4;
    }
}
