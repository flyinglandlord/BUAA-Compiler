package backend.instructions.arithmetic;

import backend.instructions.Instructions;

public class ArithR implements Instructions {
    public enum Type {
        add, addu, sub, subu, and, or, xor, nor, sll, srl, sra, slt, sltu, seq, sge, sgt, sle, sne,
    }
    private final Type type;
    private final String rd;
    private final String rs;
    private final String rt;

    public ArithR(Type type, String rd, String rs, String rt) {
        this.type = type;
        this.rd = rd;
        this.rs = rs;
        this.rt = rt;
    }

    public Type getType() {
        return type;
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
        return type.name() + " " + rd + ", " + rs + ", " + rt;
    }
}
