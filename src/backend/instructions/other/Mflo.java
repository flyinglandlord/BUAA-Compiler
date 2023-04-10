package backend.instructions.other;

import backend.instructions.Instructions;

public class Mflo implements Instructions {
    private String reg;

    public Mflo(String reg) {
        this.reg = reg;
    }

    public String toString() {
        return "mflo " + reg;
    }
}
