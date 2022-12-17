package backend.instructions.branch;

import backend.instructions.Instructions;

public class Blez implements Instructions {
    private final String rs;
    private final String label;

    public Blez(String rs, String label) {
        this.rs = rs;
        this.label = label;
    }

    public String getRs() {
        return rs;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return "blez " + rs + ", " + label;
    }
}
