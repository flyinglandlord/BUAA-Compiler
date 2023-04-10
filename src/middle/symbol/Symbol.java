package middle.symbol;

import middle.middle_code.operand.Operand;

import java.util.List;

public abstract class Symbol implements Operand {
    private int depth;
    private int location;
    public void setPosition(int depth, int location) {
        this.depth = depth;
        this.location = location;
    }

    private boolean isTemp = false;
    private int tempId;
    private boolean isString = false;
    private int stringId;
    private static int tempVarCount = 0;
    private static int stringVarCount = 0;

    public int getDepth() {
        return depth;
    }

    public int getLocation() {
        return location;
    }

    public static Symbol tempSymbol() {
        Symbol tempSymbol = new Variable("#T" + tempVarCount, false);
        tempSymbol.isTemp = true;
        tempSymbol.tempId = tempVarCount;
        tempVarCount++;
        return tempSymbol;
    }

    public static Symbol stringSymbol() {
        Symbol stringSymbol = new Variable("_STR" + stringVarCount, false);
        stringSymbol.isString = true;
        stringSymbol.stringId = stringVarCount;
        stringVarCount++;
        return stringSymbol;
    }

    public abstract String getName();
    public abstract boolean isArray();
    public abstract boolean isConst();
    public abstract int getSize();
    public abstract List<Integer> getShape();
    public abstract String toString();

    public boolean isTemp() {
        return isTemp;
    }

    public boolean isString() {
        return isString;
    }

    // 储存变量相对于当前$sp指针的位置
    private int address = -1;

    public boolean hasAddress() {
        return address != -1;
    }

    public int getAddress() {
        return address;
    }

    public void setAddress(int address) {
        this.address = address;
    }

    public static Symbol getReturnSymbol() {
        return returnSymbol;
    }

    public boolean isGlobal() {
        return depth == 0 && !isTemp && !isString;
    }

    private static final Symbol returnSymbol = new Symbol() {
        @Override
        public String getName() {
            return "#RET";
        }

        @Override
        public boolean isArray() {
            return false;
        }

        @Override
        public boolean isConst() {
            return false;
        }

        @Override
        public int getSize() {
            return 4;
        }

        @Override
        public List<Integer> getShape() {
            return null;
        }

        @Override
        public String toString() {
            return "#RET";
        }
    };
}
