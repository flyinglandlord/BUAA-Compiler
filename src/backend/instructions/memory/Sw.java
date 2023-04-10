package backend.instructions.memory;

import backend.instructions.Instructions;

public class Sw implements Instructions {
    private final String rd;
    private final String offset;
    private final String base;

    public Sw(String rd, String offset, String base) {
        this.rd = rd;
        this.offset = offset;
        this.base = base;
    }

    public String getRd() {
        return rd;
    }

    public String getOffset() {
        return offset;
    }

    public String getBase() {
        return base;
    }

    @Override
    public String toString() {
        return "sw " + rd + ", " + offset + "(" + base + ")";
    }
}
