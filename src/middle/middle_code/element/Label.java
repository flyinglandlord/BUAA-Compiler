package middle.middle_code.element;

public class Label extends MidCode {
    private static int labelCount = 0;
    private final String label;
    private final boolean hasId;

    public Label(String label, boolean hasId) {
        this.hasId = hasId;
        if (hasId) {
            this.label = label + labelCount++;
        } else {
            this.label = label;
        }
    }

    public static int getLabelCount() {
        return labelCount;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return "LABEL " + label + ":";
    }
}
