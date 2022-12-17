package backend.instructions.jump;

import backend.instructions.Instructions;

public class J implements Instructions {
    private final String label;

    public J(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return "j " + label;
    }
}
