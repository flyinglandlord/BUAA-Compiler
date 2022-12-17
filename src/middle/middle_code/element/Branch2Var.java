package middle.middle_code.element;

import middle.middle_code.element.Branch.Type;
import middle.middle_code.operand.Operand;

public class Branch2Var extends MidCode {
    private final Type type;
    private final Label target;
    private final Operand operand1;
    private final Operand operand2;

    public Branch2Var(Type type, Label target, Operand operand1, Operand operand2) {
        this.type = type;
        this.target = target;
        this.operand1 = operand1;
        this.operand2 = operand2;
    }

    public Type getType() {
        return type;
    }

    public Label getTarget() {
        return target;
    }

    public Operand getOperand1() {
        return operand1;
    }

    public Operand getOperand2() {
        return operand2;
    }

    @Override
    public String toString() {
        return "BRANCH " + operand1 + " " +  type + " " + operand2 + " " + target.getLabel();
    }
}
