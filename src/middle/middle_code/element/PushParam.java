package middle.middle_code.element;

import middle.middle_code.operand.Operand;

public class PushParam extends MidCode {
    public enum Type {
        PUSH, PUSH_ADDR
    }
    private final Type type;
    private final Operand param;

    public PushParam(Type type, Operand param) {
        this.type = type;
        this.param = param;
    }

    public Operand getParam() {
        return param;
    }

    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        if (type == Type.PUSH) {
            return "PUSH " + param;
        } else {
            return "PUSH_ADDR " + param;
        }
    }
}
