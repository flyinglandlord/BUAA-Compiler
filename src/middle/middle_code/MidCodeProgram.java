package middle.middle_code;

import middle.middle_code.element.MidCode;
import middle.symbol.Function;
import middle.symbol.FunctionFormParam;
import middle.symbol.Symbol;
import middle.symbol.SymbolTable;

import java.util.HashMap;
import java.util.Map;

public class MidCodeProgram {
    private SymbolTable globalSymbolTable = new SymbolTable();
    private MidCodeList globalVarDeclCode = new MidCodeList();
    private final Map<String, Function> functionTable = new HashMap<>();
    private Map<Symbol, String> globalStringTable = new HashMap<>();

    public MidCodeList getGlobalVarDeclCode() {
        return globalVarDeclCode;
    }

    public Map<String, Function> getFunctionTable() {
        return functionTable;
    }

    public Map<Symbol, String> getGlobalStringTable() {
        return globalStringTable;
    }

    public SymbolTable getGlobalSymbolTable() {
        return globalSymbolTable;
    }

    public MidCodeProgram rebuild() {
        MidCodeProgram newProgram = new MidCodeProgram();
        newProgram.globalSymbolTable = globalSymbolTable;
        newProgram.globalStringTable = globalStringTable;
        newProgram.globalVarDeclCode = globalVarDeclCode;
        for (Function function : functionTable.values()) {
            newProgram.functionTable.put(function.getName(), function.rebuild());
        }
        return newProgram;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("GLOBAL_VAR_DECL:\n");
        for (MidCode code : globalVarDeclCode.getMidCodeList()) {
            builder.append(code).append("\n");
        }
        builder.append("\n");
        builder.append("FUNCTIONS:\n");
        for (Function function : functionTable.values()) {
            builder.append(function.getName()).append("\n");
            // builder.append(function.getBody().getMidCodeList().get(0)).append("\n");
            /*for (FunctionFormParam args : function.getParamList()) {
                if (args.isArray()) {
                    builder.append("PARAM ARRAY ");
                    builder.append(args);
                    for (int i = 0; i < args.getDimension(); i++) {
                        if (args.getShape().get(i) == 0) builder.append("[").append("]");
                        else builder.append("[").append(args.getShape().get(i)).append("]");
                    }
                    builder.append("\n");
                } else {
                    builder.append("PARAM ").append(args).append("\n");
                }
            }*/
            for (int i = 0; i < function.getBody().getMidCodeList().size(); i++) {
                builder.append(function.getBody().getMidCodeList().get(i)).append("\n");
            }
            builder.append("\n");
        }
        builder.append("\n");
        builder.append("GLOBAL_STRINGS:\n");
        for (Map.Entry<Symbol, String> entry : globalStringTable.entrySet()) {
            builder.append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
        }
        return builder.toString();
    }
}
