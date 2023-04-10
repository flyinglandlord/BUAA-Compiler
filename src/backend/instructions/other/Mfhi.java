package backend.instructions.other;

import backend.instructions.Instructions;

public class Mfhi implements Instructions {
    private final String dest;

    public Mfhi(String dest) {
        this.dest = dest;
    }

    @Override
    public String toString() {
        return "mfhi " + dest;
    }
}
