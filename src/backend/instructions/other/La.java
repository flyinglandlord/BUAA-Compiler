package backend.instructions.other;

import backend.instructions.Instructions;

public class La implements Instructions {
    private final String rd;
    private final String label;

    public La(String rd, String label) {
        this.rd = rd;
        this.label = label;
    }

    public String getRd() {
        return rd;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return "la " + rd + ", " + label;
    }
}
