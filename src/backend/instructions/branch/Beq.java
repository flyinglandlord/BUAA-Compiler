package backend.instructions.branch;

import backend.instructions.Instructions;

public class Beq implements Instructions {
    private final String rs;
    private final String rt;
    private final String label;

    public Beq(String rs, String rt, String label) {
        this.rs = rs;
        this.rt = rt;
        this.label = label;
    }

    public String getRs() {
        return rs;
    }

    public String getRt() {
        return rt;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return "beq " + rs + ", " + rt + ", " + label;
    }
}
