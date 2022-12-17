package backend.instructions.jump;

import backend.instructions.Instructions;

public class Jal implements Instructions {
    private final String label;

    public Jal(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return "jal " + label;
    }
}
