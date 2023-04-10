package middle.symbol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SymbolTable {
    private final int depth;
    private final int location;
    private final Map<String, Symbol> symbolTable = new HashMap<>();
    private final Map<String, Symbol> tempVarTable = new HashMap<>();
    private final List<Symbol> symbolList = new ArrayList<>();
    private final SymbolTable parent;
    private final List<SymbolTable> children = new ArrayList<>();

    public SymbolTable(int depth, int location, SymbolTable parent) {
        this.depth = depth;
        this.location = location;
        this.parent = parent;
    }

    public SymbolTable() {
        this.depth = 0;
        this.location = 0;
        parent = null;
    }

    public Map<String, Symbol> getTempVarTable() {
        return tempVarTable;
    }

    public int getDepth() {
        return depth;
    }

    public int getLocation() {
        return location;
    }

    public void addChild(SymbolTable child) {
        children.add(child);
    }

    public void addTempVar(Symbol symbol) {
        symbol.setPosition(depth, location);
        tempVarTable.put(symbol.getName(), symbol);
    }

    public Symbol getTempVar(String name, boolean recursion) {
        Symbol symbol = tempVarTable.get(name);
        if (symbol == null && recursion && parent != null) {
            return parent.getTempVar(name, recursion);
        }
        return symbol;
    }

    public List<SymbolTable> getChildren() {
        return children;
    }

    public Map<String, Symbol> getSymbolTable() {
        return symbolTable;
    }

    public List<Symbol> getSymbolList() {
        return symbolList;
    }

    public SymbolTable getParent() {
        return parent;
    }

    public void add(Symbol symbol) {
        symbol.setPosition(depth, location);
        symbolTable.put(symbol.getName(), symbol);
        symbolList.add(symbol);
    }

    public Symbol get(String name, boolean recursive) {
        Symbol symbol = symbolTable.get(name);
        if (symbol != null) {
            return symbol;
        }
        if (recursive && parent != null) {
            return parent.get(name, true);
        }
        return null;
    }

    public boolean contains(String name, boolean recursive) {
        if (symbolTable.containsKey(name)) {
            return true;
        }
        if (recursive && parent != null) {
            return parent.contains(name, true);
        }
        return false;
    }

    public int size() {
        return symbolTable.size();
    }

    public int getStackSize() {
        int size = 0;
        for (Symbol symbol : symbolList) {
            size += symbol.getSize();
        }
        for (Symbol tempSymbol : tempVarTable.values()) {
            size += tempSymbol.getSize();
        }
        for (SymbolTable chile : children) {
            size += chile.getStackSize();
        }
        return size;
    }

    public void setAddress(int offset) {
        for (Symbol symbol : symbolTable.values()) {
            if (symbol.hasAddress()) continue;
            symbol.setAddress(offset);
            offset += symbol.getSize();
        }
        for (Symbol symbol : tempVarTable.values()) {
            if (symbol.hasAddress()) continue;
            symbol.setAddress(offset);
            offset += symbol.getSize();
        }
        for (SymbolTable child : children) {
            child.setAddress(offset);
            offset += child.getStackSize();
        }
    }
}
