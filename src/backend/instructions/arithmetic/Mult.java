package backend.instructions.arithmetic;

import backend.instructions.Instructions;

public class Mult implements Instructions {
    private final String rs;
    private final String rt;

    public Mult(String rs, String rt) {
        this.rs = rs;
        this.rt = rt;
    }

    public String getRs() {
        return rs;
    }

    public String getRt() {
        return rt;
    }

    @Override
    public String toString() {
        return "mult " + rs + ", " + rt;
    }
}
