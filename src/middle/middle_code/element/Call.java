package middle.middle_code.element;

import middle.middle_code.operand.ArrayPointer;
import middle.middle_code.operand.Operand;
import middle.symbol.Function;

import java.util.List;

public class Call extends MidCode {
    private final Function function;
    private final List<Operand> args;
    private final Operand ret;

    public Call(Function function, List<Operand> args) {
        this.function = function;
        this.args = args;
        this.ret = null;
    }

    public Call(Function function, List<Operand> args, Operand ret) {
        this.function = function;
        this.args = args;
        this.ret = ret;
    }

    public Function getFunction() {
        return function;
    }

    // arg可能为ArrayItem, Variable, FunctionFormParam, ArrayPointer
    public List<Operand> getArgs() {
        return args;
    }

    public Operand getRet() {
        return ret;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        /*for (Operand arg : args) {
            // TODO: push stack
            if (arg instanceof ArrayPointer) {
                sb.append("# PUSH ARRAY ").append(arg).append("\n");
            } else {
                sb.append("# PUSH ").append(arg).append("\n");
            }

        }*/
        sb.append("CALL ").append(function.getName());
        sb.append(" RET ").append(ret);
        return sb.toString();
    }
}
