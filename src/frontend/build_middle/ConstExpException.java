package frontend.build_middle;

public class ConstExpException extends Exception {
    private final int linenumber;
    private final String name;

    public ConstExpException(int linenumber, String name) {
        this.linenumber = linenumber;
        this.name = name;
    }
}
