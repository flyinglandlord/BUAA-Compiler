package backend.instructions.branch;

import backend.instructions.Instructions;

public class Bgtz implements Instructions {
    private final String rs;
    private final String label;

    public Bgtz(String rs, String label) {
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
        return "bgtz " + rs + ", " + label;
    }
}
