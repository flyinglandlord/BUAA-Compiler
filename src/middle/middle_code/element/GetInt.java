package middle.middle_code.element;

import middle.symbol.Symbol;

public class GetInt extends MidCode {
    private final Symbol dst;

    public GetInt(Symbol dst) {
        this.dst = dst;
    }

    public Symbol getDst() {
        return dst;
    }

    @Override
    public String toString() {
        return dst + " = GETINT";
    }
}
