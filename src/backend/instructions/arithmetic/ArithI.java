package backend.instructions.arithmetic;

import backend.instructions.Instructions;

public class ArithI implements Instructions {
    public enum Type {
        addiu, andi, ori, xori, sltiu,
        sge, sgt, sle, seq, sne,
        sll, srl, sra,
    }
    private final Type type;
    private final String rd;
    private final String rs;
    private final int imm;

    public ArithI(Type type, String rd, String rs, int imm) {
        this.type = type;
        this.rd = rd;
        this.rs = rs;
        this.imm = imm;
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

    public int getImm() {
        return imm;
    }

    @Override
    public String toString() {
        return type.name() + " " + rd + ", " + rs + ", " + imm;
    }
}
