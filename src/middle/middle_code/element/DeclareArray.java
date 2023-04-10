package middle.middle_code.element;

import middle.middle_code.operand.Operand;
import middle.symbol.Symbol;

import java.util.List;

public class DeclareArray extends MidCode {
    public enum Type {
        VAR_DEF, CONST_DEF
    }
    private final Type type;
    private final Symbol var;
    private final int size;
    private final List<Operand> initValue;

    public DeclareArray(Type type, Symbol var, int size, List<Operand> value) {
        this.type = type;
        this.var = var;
        this.size = size;
        this.initValue = value;
    }

    public DeclareArray(Type type, Symbol var, int size) {
        this.type = type;
        this.var = var;
        this.size = size;
        this.initValue = null;
    }

    public Symbol getVar() {
        return var;
    }

    public int getSize() {
        return size;
    }

    public Type getType() {
        return type;
    }

    public List<Operand> getInitValue() {
        return initValue;
    }

    @Override
    public String toString() {
        if (initValue == null) {
            if (type == Type.VAR_DEF) {
                return "DECLARE_ARRAY " + var + "[" + size + "]";
            } else {
                return "CONST DECLARE_ARRAY " + var + "[" + size + "]";
            }

        } else {
            if (type == Type.VAR_DEF) {
                return "DECLARE_ARRAY " + var + "[" + size + "] = " + initValue;
            } else {
                return "CONST DECLARE_ARRAY " + var + "[" + size + "] = " + initValue;
            }
        }
    }
}
