package middle.middle_code.element;

import middle.middle_code.operand.Operand;

public class Return extends MidCode {
    private final Operand value;    // Nullable

    public Return() {
        this.value = null;
    }

    public Return(Operand value) {
        this.value = value;
    }

    public Operand getValue() {
        return value;
    }

    public boolean hasValue() {
        return null != value;
    }

    @Override
    public String toString() {
        return "RETURN" + (hasValue() ? " " + value : "");
    }
}
