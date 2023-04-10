package middle.middle_code.element;

import middle.middle_code.operand.Immediate;
import middle.middle_code.operand.Operand;
import middle.symbol.Symbol;

public class DeclareVar extends MidCode {
    public enum Type {
        VAR_DEF, CONST_DEF
    }
    private final Type type;
    private final Symbol var;
    private final Operand initValue;

    public DeclareVar(Type type, Symbol var, Operand value) {
        this.type = type;
        this.var = var;
        this.initValue = value;
    }

    public DeclareVar(Type type, Symbol var) {
        this.type = type;
        this.var = var;
        this.initValue = null;
    }

    public Type getType() {
        return type;
    }

    public Symbol getVar() {
        return var;
    }

    public Operand getInitValue() {
        return initValue;
    }

    public String toString() {
        if (type == Type.VAR_DEF) {
            if (initValue != null) {
                return "DECLARE_VAR " + var + " = " + initValue;
            } else {
                return "DECLARE_VAR " + var;
            }
        } else {
            return "CONST DECLARE_VAR " + var + " = " + initValue;
        }
    }
}
