package middle.middle_code.element;

public class Jump extends MidCode {
    private final Label target;

    public Jump(Label target) {
        this.target = target;
    }

    public Label getTarget() {
        return target;
    }

    @Override
    public String toString() {
        return "JUMP " + target.getLabel();
    }
}
