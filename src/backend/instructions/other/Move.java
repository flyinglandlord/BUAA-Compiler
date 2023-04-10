package backend.instructions.other;

import backend.instructions.Instructions;

public class Move implements Instructions {
    private final String dest;
    private final String src;

    public Move(String dest, String src) {
        this.dest = dest;
        this.src = src;
    }

    public String getDest() {
        return dest;
    }

    public String getSrc() {
        return src;
    }

    @Override
    public String toString() {
        return "move " + dest + ", " + src;
    }
}
