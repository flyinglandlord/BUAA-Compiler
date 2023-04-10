package middle.symbol;

import frontend.grammar.stmt.Block;
import middle.middle_code.MidCodeList;
import middle.middle_code.element.Empty;
import middle.middle_code.element.MidCode;
import middle.optimize.BasicBlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Function {
    private final String name;
    private final SymbolTable paramTable;
    private final Block functionBody;
    private List<FunctionFormParam> paramList;
    private MidCodeList body;
    private List<BasicBlock> basicBlocks = new ArrayList<>();

    public enum ReturnType {
        INT,
        VOID,
    }
    private final ReturnType returnType;

    public Function(String name, SymbolTable symbolTable, Block functionBody, ReturnType returnType) {
        this.name = name;
        this.paramTable = symbolTable;
        this.functionBody = functionBody;
        this.returnType = returnType;
        this.paramList = new ArrayList<>();
    }

    public MidCodeList getBody() {
        return body;
    }

    public void setBody(MidCodeList body) {
        this.body = body;
    }

    public String getName() {
        return name;
    }

    public SymbolTable getParamTable() {
        return paramTable;
    }

    public Block getFunctionBody() {
        return functionBody;
    }

    public List<FunctionFormParam> getParamList() {
        return paramList;
    }

    public void addParam(FunctionFormParam param) {
        paramList.add(param);
        paramTable.add(param);
    }
    public ReturnType getReturnType() {
        return returnType;
    }

    public int getStackSize() {
        return paramTable.getStackSize();
    }

    public void allocateAddress(int offset) {
        for (FunctionFormParam param : paramList) {
            param.setAddress(offset);
            offset += param.getSize();
        }
        paramTable.setAddress(offset);
    }

    public void addBasicBlock(BasicBlock basicBlock) {
        basicBlocks.add(basicBlock);
    }

    public void setBasicBlocks(List<BasicBlock> basicBlocks) {
        this.basicBlocks = basicBlocks;
    }

    public BasicBlock getBasicBlock(int id) {
        return basicBlocks.get(id-1);
    }

    public MidCode getCode(int blockId, int codeId) {
        return basicBlocks.get(blockId-1).getBlock().get(codeId);
    }

    public Function rebuild() {
        System.out.println(name);
        Function newFunction = new Function(name, paramTable, functionBody, returnType);
        newFunction.paramList = paramList;
        MidCodeList newBody = new MidCodeList();
        for (BasicBlock basicBlock : basicBlocks) {
            for (MidCode midCode : basicBlock.getBlock().getMidCodeList()) {
                if (midCode instanceof Empty) continue;
                newBody.addMidCode(midCode);
            }
        }
        System.out.println(newBody.size());
        newFunction.setBody(newBody);
        return newFunction;
    }

    public List<BasicBlock> getBasicBlocks() {
        return basicBlocks;
    }

    private Map<Symbol, Integer> regMap = new HashMap<>();
    private List<Integer> globalRegisters = new ArrayList<>();
    private List<Integer> localRegisters = new ArrayList<>();

    public Map<Symbol, Integer> getRegMap() {
        return regMap;
    }

    public void setRegMap(Map<Symbol, Integer> regMap) {
        this.regMap = regMap;
    }

    public List<Integer> getGlobalRegisters() {
        return globalRegisters;
    }

    public void setGlobalRegisters(List<Integer> globalRegisters) {
        this.globalRegisters = globalRegisters;
    }

    public List<Integer> getLocalRegisters() {
        return localRegisters;
    }

    public void setLocalRegisters(List<Integer> localRegisters) {
        this.localRegisters = localRegisters;
    }
}
