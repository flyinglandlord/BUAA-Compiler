package middle.middle_code.element;

import middle.symbol.Symbol;

public class ParamDef extends MidCode {
    private final Symbol param;

    public ParamDef(Symbol param) {
        this.param = param;
    }

    public Symbol getParam() {
        return param;
    }

    @Override
    public String toString() {
        if (param.isArray()) return "DEF PARAM ARRAY " + param;
        else return "DEF PARAM " + param;
    }
}
