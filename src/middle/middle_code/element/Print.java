package middle.middle_code.element;

import middle.middle_code.operand.Operand;

public class Print extends MidCode {
    private final Operand src;

    public Print(Operand src) {
        this.src = src;
    }

    public Operand getSrc() {
        return src;
    }

    public String toString() {
        return "PRINT " + src.toString();
    }
}
