package backend;

import frontend.lexical.Token;
import middle.symbol.Symbol;

import java.util.*;

// 目前为了方便，暂时采用dhy的寄存器分配策略
// 可自由分配的寄存器：从 a2($6) 到 t9($25); v0($2) 作为函数返回值, v0($2) 也可用来存储一个立即数[upd:11/19], a0($4) 用于系统调用, a1($5)用于访存时计算偏移量
public class RegisterMap {
    public static int regClockCount = 0;
    private List<Integer> allocatableRegisters = new ArrayList<>();

    public List<Integer> getAllocatableRegisters() {
        return allocatableRegisters;
    }

    public void setAllocatableRegisters(List<Integer> allocatableRegisters) {
        this.allocatableRegisters = allocatableRegisters;
        this.freeRegisters = new HashSet<>(allocatableRegisters);
    }

    public static final Map<Integer, String> id2regName = new HashMap<>();
    static {
        id2regName.put(0, "$zero");
        id2regName.put(1, "$at");
        id2regName.put(2, "$v0");
        id2regName.put(3, "$v1");
        id2regName.put(4, "$a0");
        id2regName.put(5, "$a1");
        id2regName.put(6, "$a2");
        id2regName.put(7, "$a3");
        id2regName.put(8, "$t0");
        id2regName.put(9, "$t1");
        id2regName.put(10, "$t2");
        id2regName.put(11, "$t3");
        id2regName.put(12, "$t4");
        id2regName.put(13, "$t5");
        id2regName.put(14, "$t6");
        id2regName.put(15, "$t7");
        id2regName.put(16, "$s0");
        id2regName.put(17, "$s1");
        id2regName.put(18, "$s2");
        id2regName.put(19, "$s3");
        id2regName.put(20, "$s4");
        id2regName.put(21, "$s5");
        id2regName.put(22, "$s6");
        id2regName.put(23, "$s7");
        id2regName.put(24, "$t8");
        id2regName.put(25, "$t9");
        id2regName.put(26, "$k0");
        id2regName.put(27, "$k1");
        id2regName.put(28, "$gp");
        id2regName.put(29, "$sp");
        id2regName.put(30, "$fp");
        id2regName.put(31, "$ra");
    }

    // 当前未使用(可自由分配)的寄存器
    private Set<Integer> freeRegisters = new HashSet<>(allocatableRegisters);
    // 已经分配出去的寄存器对应的符号
    private final Map<Integer, Symbol> allocatedRegisters = new HashMap<>();
    // 符号对应到寄存器
    private final Map<Symbol, Integer> symbolToRegister = new HashMap<>();

    public boolean isAllocated(Symbol symbol) {
        return symbolToRegister.containsKey(symbol);
    }

    public boolean isAllocated(int register) {
        return allocatedRegisters.containsKey(register);
    }

    public int getRegisterOfSymbol(Symbol symbol) {
        if (!symbolToRegister.containsKey(symbol)) {
            throw new AssertionError(String.format("%s not assigned register!", symbol.toString()));
        }
        return symbolToRegister.get(symbol);
    }

    public int assignRegister(Symbol symbol) {
        if (symbolToRegister.containsKey(symbol)) {
            throw new AssertionError(String.format("%s already assigned register!", symbol.toString()));
        }
        if (freeRegisters.isEmpty()) {
            throw new AssertionError("No free register!");
        }
        int register = freeRegisters.iterator().next();
        freeRegisters.remove(register);
        allocatedRegisters.put(register, symbol);
        symbolToRegister.put(symbol, register);
        return register;
    }

    public void cancelAssignRegister(Symbol symbol) {
        if (!symbolToRegister.containsKey(symbol)) {
            throw new AssertionError(String.format("%s not assigned register!", symbol.toString()));
        }
        int register = symbolToRegister.get(symbol);
        freeRegisters.add(register);
        allocatedRegisters.remove(register);
        symbolToRegister.remove(symbol);
    }

    public void cancelAssignRegister(int register) {
        if (!allocatedRegisters.containsKey(register)) {
            throw new AssertionError(String.format("%d not assigned register!", register));
        }
        Symbol symbol = allocatedRegisters.get(register);
        freeRegisters.add(register);
        allocatedRegisters.remove(register);
        symbolToRegister.remove(symbol);
    }

    public void clear() {
        allocatedRegisters.clear();
        symbolToRegister.clear();
        freeRegisters.clear();
        freeRegisters.addAll(allocatableRegisters);
    }

    public boolean hasFreeRegister() {
        return !freeRegisters.isEmpty();
    }

    public Set<Integer> getFreeRegisters() {
        return freeRegisters;
    }

    public Map<Integer, Symbol> getAllocatedRegisters() {
        return allocatedRegisters;
    }

    public Symbol getSymbolOfRegister(int register) {
        return allocatedRegisters.get(register);
    }

    public Map<Symbol, Integer> getSymbolToRegister() {
        return symbolToRegister;
    }
}
