package backend.instructions.arithmetic;

import backend.instructions.Instructions;

public class Mul implements Instructions {
    private final String rd;
    private final String rs;
    private final String rt;

    public Mul(String rd, String rs, String rt) {
        this.rd = rd;
        this.rs = rs;
        this.rt = rt;
    }

    public String getRd() {
        return rd;
    }

    public String getRs() {
        return rs;
    }

    public String getRt() {
        return rt;
    }

    @Override
    public String toString() {
        return "mul " + rd + ", " + rs + ", " + rt;
    }
}
