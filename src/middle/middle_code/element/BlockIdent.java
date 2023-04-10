package middle.middle_code.element;

public class BlockIdent extends MidCode {
    private final int depth;
    private final int location;

    public BlockIdent(Type type, int depth, int location) {
        this.depth = depth;
        this.location = location;
        this.type = type;
    }

    public enum Type {
        BEGIN, END,
    }
    private final Type type;

    public int getDepth() {
        return depth;
    }

    public int getLocation() {
        return location;
    }

    public Type getType() {
        return type;
    }

    public String toString() {
        if (type == Type.BEGIN) {
            return "BEGIN BLOCK [" + depth + ", " + location + "]";
        } else {
            return "END BLOCK [" + depth + ", " + location + "]";
        }
    }
}
