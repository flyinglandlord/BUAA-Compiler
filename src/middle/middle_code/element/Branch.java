package middle.middle_code.element;

import middle.middle_code.operand.Operand;

public class Branch extends MidCode {
    public enum Type {
        EQ, NE, LT, LE, GT, GE,
    }
    private final Type type;
    private final Label target;
    private final Operand cond;

    public Branch(Type type, Label target, Operand cond) {
        this.type = type;
        this.target = target;
        this.cond = cond;
    }

    public Type getType() {
        return type;
    }

    public Label getTarget() {
        return target;
    }

    public Operand getCond() {
        return cond;
    }

    @Override
    public String toString() {
        return "BRANCH " + type + " " + target.getLabel() + " " + cond;
    }
}
