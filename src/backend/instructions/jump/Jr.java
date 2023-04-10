package backend.instructions.jump;

import backend.instructions.Instructions;

public class Jr implements Instructions {
    private final String rs;

    public Jr(String rs) {
        this.rs = rs;
    }

    public String getRs() {
        return rs;
    }

    @Override
    public String toString() {
        return "jr " + rs;
    }
}
