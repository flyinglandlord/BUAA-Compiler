package backend.instructions.arithmetic;

import backend.instructions.Instructions;

public class Neg implements Instructions {
    private final String rd;
    private final String rs;

    public Neg(String rd, String rs) {
        this.rd = rd;
        this.rs = rs;
    }

    public String getRd() {
        return rd;
    }

    public String getRs() {
        return rs;
    }

    @Override
    public String toString() {
        return "neg " + rd + ", " + rs;
    }
}
