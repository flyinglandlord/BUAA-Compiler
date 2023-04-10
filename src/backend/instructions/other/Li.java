package backend.instructions.other;

import backend.instructions.Instructions;

public class Li implements Instructions {
    private String reg;
    private int value;

    public Li(String reg, int value) {
        this.reg = reg;
        this.value = value;
    }

    public String toString() {
        return "li " + reg + ", " + value;
    }
}
