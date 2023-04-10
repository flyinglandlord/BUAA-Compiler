package middle.middle_code.element;

import middle.symbol.Function;

public class FuncCallIdent extends MidCode {
    public enum Type {
        BEGIN, END,
    }
    private final Type type;
    private final Function function;

    public FuncCallIdent(Type type, Function function) {
        this.type = type;
        this.function = function;
    }

    public Type getType() {
        return type;
    }

    public Function getFunction() {
        return function;
    }

    public String toString() {
        if (type == Type.BEGIN) {
            return "BEGIN CALL " + function.getName();
        } else {
            return "END CALL " + function.getName();
        }
    }
}
