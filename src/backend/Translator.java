package backend;

import backend.instructions.Comment;
import backend.instructions.Instructions;
import backend.instructions.Newline;
import backend.instructions.arithmetic.*;
import backend.instructions.branch.*;
import backend.instructions.jump.J;
import backend.instructions.jump.Jal;
import backend.instructions.jump.Jr;
import backend.instructions.memory.Lw;
import backend.instructions.memory.Sw;
import backend.instructions.other.*;
import middle.middle_code.MidCodeProgram;
import middle.middle_code.element.*;
import middle.middle_code.operand.ArrayPointer;
import middle.middle_code.operand.Immediate;
import middle.middle_code.operand.Operand;
import middle.optimize.BasicBlock;
import middle.symbol.Function;
import middle.symbol.FunctionFormParam;
import middle.symbol.Symbol;
import middle.symbol.Variable;

import java.util.*;
import java.util.stream.Collectors;

import static backend.RegisterMap.id2regName;
import static middle.optimize.RegAllocator.getAllDef;
import static middle.optimize.RegAllocator.getAllUse;

public class Translator {
    private final MidCodeProgram midCodeProgram;
    private final List<Instructions> text = new ArrayList<>();
    private final StringBuilder data = new StringBuilder();
    private Function currentFunction = null;
    private final RegisterMap registerMap = new RegisterMap();
    private int globalVariableSize = 0;
    private final Map<Symbol, Integer> tempVarUsageCount = new HashMap<>();
    private final boolean[] dirty = new boolean[32];
    private int hit = 0;
    private int miss = 0;
    private int shift = 0;

    public Translator(MidCodeProgram midCodeProgram) {
        this.midCodeProgram = midCodeProgram;
    }

    public MidCodeProgram getMidCodeProgram() {
        return midCodeProgram;
    }

    public void removeRedundantJump() {
        for (int i = 0; i < text.size()-1; i++) {
            if (text.get(i) instanceof J) {
                J jump = (J) text.get(i);
                int j = i + 1;
                while (text.get(j) instanceof Comment || text.get(j) instanceof Newline) j++;
                if (text.get(j) instanceof backend.instructions.Label) {
                    backend.instructions.Label label = (backend.instructions.Label) text.get(j);
                    if (Objects.equals(label.getLabel(), jump.getLabel())) {
                        text.remove(i);
                        i--;
                    }
                }
            }
        }
    }

    public StringBuilder getMipsCode() {
        System.out.println("register allocation:");
        System.out.println("hit: " + hit + "\nmiss: " + miss + "\nshift: " + shift + "\ntotal: " + (hit + miss));
        StringBuilder mipsCode = new StringBuilder();

        mipsCode.append(".data").append('\n');
        mipsCode.append(data).append('\n').append('\n');

        removeRedundantJump();
        mipsCode.append(".text").append('\n');
        for (Instructions instructions : text) {
            mipsCode.append('\t').append(instructions).append('\n');
        }
        return mipsCode;
    }

    // load表示是否需要从内存加载曾经的值, not表示不能使用哪些寄存器（因为有可能有两个运算数，你其中一个把另外一个换走了，这就错了）
    private int getNextAvailableRegister(int... not) {
        RegisterMap.regClockCount++;
        if (not.length == 0) return RegisterMap.regClockCount % registerMap.getAllocatableRegisters().size();
        else {
            while(true) {
                boolean ok = true;
                for (int notReg : not) {
                    if (notReg == registerMap
                            .getAllocatableRegisters()
                            .get(RegisterMap.regClockCount % registerMap.getAllocatableRegisters().size())) {
                        ok = false;
                        break;
                    }
                }
                if (ok) break;
                else RegisterMap.regClockCount++;
            }
            return RegisterMap.regClockCount % registerMap.getAllocatableRegisters().size();
        }
    }
    private int allocTempRegister(Symbol symbol, boolean load, int... not) {
        if (registerMap.isAllocated(symbol)) {
            hit++;
            return registerMap.getRegisterOfSymbol(symbol);
        }
        miss++;
        // System.out.println(registerMap.getAllocatedRegisters());
        if (!registerMap.hasFreeRegister()) {
            shift++;
            // 寄存器池已满，需要置换掉一个寄存器
            int register = registerMap.getAllocatableRegisters().get(getNextAvailableRegister(not));
            // System.out.println(register);
            Symbol var = registerMap.getSymbolOfRegister(register);
            // 将该变量存储进内存
            if (var.getDepth() == 0 && !var.isTemp()) {
                //text.add(new Sw(id2regName.get(register), String.valueOf(var.getAddress()), "$gp"));
                //mipsCode.append(String.format("\tsw %s, %d($gp)", id2regName.get(register), var.getAddress())).append("\n");
            } else {
                if (dirty[register]) text.add(new Sw(id2regName.get(register), String.valueOf(var.getAddress()), "$sp"));
                //mipsCode.append(String.format("\tsw %s, %d($sp)", id2regName.get(register), var.getAddress())).append("\n");
            }
            registerMap.cancelAssignRegister(register);
        }
        int register = registerMap.assignRegister(symbol);
        dirty[register] = false;
        if (load && symbol.hasAddress()) {
            if (symbol.isGlobal()) {
                text.add(new Lw(id2regName.get(register), String.valueOf(symbol.getAddress()), "$gp"));
                //mipsCode.append(String.format("\tlw %s, %d($gp)", id2regName.get(register), symbol.getAddress())).append("\n");
            } else {
                text.add(new Lw(id2regName.get(register), String.valueOf(symbol.getAddress()), "$sp"));
                //mipsCode.append(String.format("\tlw %s, %d($sp)", id2regName.get(register), symbol.getAddress())).append("\n");
            }
        }
        text.add(new Comment("Alloc register: " + id2regName.get(register) + " for " + symbol.getName()));
        System.out.println("Alloc register: " + id2regName.get(register) + " for " + symbol.getName());
        return register;
    }

    public void saveTempRegisters() {
        for (int register : registerMap.getAllocatableRegisters()) {
            if (registerMap.isAllocated(register)) {
                Symbol symbol = registerMap.getSymbolOfRegister(register);
                if (symbol.isGlobal()) {
                    //text.add(new Sw(id2regName.get(register), String.valueOf(symbol.getAddress()), "$gp"));
                    //mipsCode.append(String.format("\tsw %s, %d($gp)", id2regName.get(register), symbol.getAddress())).append("\n");
                } else {
                    // 临时变量不会再被使用，不需要保存
                    if (symbol.isTemp() && (!tempVarUsageCount.containsKey(symbol) || tempVarUsageCount.get(symbol) == 0))
                        continue;
                    // 数组指针不需要保存
                    if (symbol.isArray()) continue;
                    if (!dirty[register]) continue;
                    text.add(new Sw(id2regName.get(register), String.valueOf(symbol.getAddress()), "$sp"));
                    //mipsCode.append(String.format("\tsw %s, %d($sp)", id2regName.get(register), symbol.getAddress())).append("\n");
                }
            }
        }
    }

    private int allocGlobalRegister(Symbol symbol) {
        allocatedGlobalRegisters.put(currentFunction.getRegMap().get(symbol), symbol);
        return currentFunction.getRegMap().get(symbol);
    }

    private Set<Symbol> activeOutOfCode(MidCode target) {
        for (BasicBlock block : currentFunction.getBasicBlocks()) {
            if (!block.getBlock().getMidCodeList().contains(target)) continue;
            final Set<Symbol> out = new HashSet<>(block.getOut_live());
            for (int i = block.getBlock().size()-1; i >= 0; --i) {
                MidCode code = block.getBlock().get(i);
                final Symbol def = getAllDef(code);
                if (def != null && !def.isGlobal()) {
                    out.remove(def);
                }
                if (code.equals(target)) break;
                out.addAll(getAllUse(code).stream()
                        .filter(symbol -> !symbol.isGlobal())
                        .collect(Collectors.toSet()));
            }
            return out;
        }
        return Collections.emptySet();
    }

    private final Map<Integer, Symbol> allocatedGlobalRegisters = new HashMap<>();

    // 只会在调用函数前保存全局寄存器
    public void saveGlobalRegisters(MidCode code) {
        final Set<Symbol> out = activeOutOfCode(code);
        for (Map.Entry<Integer, Symbol> entry : allocatedGlobalRegisters.entrySet()) {
            Symbol symbol = entry.getValue();
            int register = entry.getKey();
            if (out.contains(symbol))
                text.add(new Sw(id2regName.get(register), String.valueOf(symbol.getAddress()), "$sp"));
        }
    }

    public void restoreGlobalRegisters(MidCode code) {
        final Set<Symbol> out = activeOutOfCode(code);
        for (Map.Entry<Integer, Symbol> entry : allocatedGlobalRegisters.entrySet()) {
            Symbol symbol = entry.getValue();
            int register = entry.getKey();
            if (out.contains(symbol))
                text.add(new Lw(id2regName.get(register), String.valueOf(symbol.getAddress()), "$sp"));
        }
    }

    private int allocRegister(Symbol symbol, boolean load, int... not) {
        if (!currentFunction.getRegMap().containsKey(symbol))
            return allocTempRegister(symbol, load, not);
        else
            return allocGlobalRegister(symbol);
    }

    public void clearTempRegisters() {
        for (int register : registerMap.getAllocatableRegisters()) {
            if (registerMap.isAllocated(register)) {
                dirty[register] = false;
                registerMap.cancelAssignRegister(register);
            }
        }
    }

    private void consumeTempVar(Symbol symbol) {
        if (!symbol.isTemp()) {
            return;
        }
        assert tempVarUsageCount.containsKey(symbol);
        int count = tempVarUsageCount.get(symbol);
        if (count == 1) {
            tempVarUsageCount.remove(symbol);
            // 如果不再使用的临时变量在寄存器中，应释放寄存器
            if (registerMap.isAllocated(symbol)) {
                text.add(new Comment("Free register: " + id2regName.get(registerMap.getRegisterOfSymbol(symbol)) + " for " + symbol.getName()));
                registerMap.cancelAssignRegister(registerMap.getRegisterOfSymbol(symbol));
            }
        } else {
            tempVarUsageCount.put(symbol, count - 1);
        }
    }

    private void recordTempVar(Operand operand) {
        if (operand instanceof Symbol && ((Symbol) operand).isTemp()) {
            Symbol tempSymbol = (Symbol) operand;
            tempVarUsageCount.merge(tempSymbol, 1, Integer::sum);
        }
    }

    private void statisticTempVarUsage() {
        for (Function function : midCodeProgram.getFunctionTable().values()) {
            for (MidCode code : function.getBody().getMidCodeList()) {
                if (code instanceof BinaryOp) {
                    recordTempVar(((BinaryOp) code).getOperand1());
                    recordTempVar(((BinaryOp) code).getOperand2());
                } else if (code instanceof UnaryOp) {
                    recordTempVar(((UnaryOp) code).getOperand1());
                } else if (code instanceof Print) {
                    recordTempVar(((Print) code).getSrc());
                } else if (code instanceof Return) {
                    if (((Return) code).hasValue()) recordTempVar(((Return) code).getValue());
                } else if (code instanceof Assign) {
                    recordTempVar(((Assign) code).getSrc());
                } else if (code instanceof Branch) {
                    recordTempVar(((Branch) code).getCond());
                } else if (code instanceof LoadSave) {
                    if (((LoadSave) code).getOp() == LoadSave.Op.STORE) {
                        recordTempVar(((LoadSave) code).getOffset());
                        recordTempVar(((LoadSave) code).getSrc());
                    } else if (((LoadSave) code).getOp() == LoadSave.Op.LOAD) {
                        recordTempVar(((LoadSave) code).getOffset());
                    }
                } else if (code instanceof DeclareVar) {
                    recordTempVar(((DeclareVar) code).getInitValue());
                } else if (code instanceof DeclareArray) {
                    List<Operand> initVals = ((DeclareArray) code).getInitValue();
                    if (initVals != null) initVals.forEach(this::recordTempVar);
                } else if (code instanceof PushParam) {
                    if (((PushParam) code).getType() == PushParam.Type.PUSH) {
                        recordTempVar(((PushParam) code).getParam());
                    } else {
                        ArrayPointer param = (ArrayPointer) ((PushParam) code).getParam();
                        recordTempVar(param.getOffset());
                    }
                } else if (code instanceof Branch2Var) {
                    recordTempVar(((Branch2Var) code).getOperand2());
                    recordTempVar(((Branch2Var) code).getOperand1());
                }
            }
        }
    }

    private void calcStackSize() {
        for (MidCode i : midCodeProgram.getGlobalVarDeclCode().getMidCodeList()) {
            if (i instanceof DeclareVar) globalVariableSize += 4;
            else if (i instanceof DeclareArray) globalVariableSize += 4 * ((DeclareArray) i).getSize();
        }
        int globalOffset = 0;
        for (MidCode i : midCodeProgram.getGlobalVarDeclCode().getMidCodeList()) {
            if (i instanceof DeclareVar) {
                ((DeclareVar) i).getVar().setAddress(globalOffset - globalVariableSize);
                globalOffset += 4;
            } else if (i instanceof DeclareArray) {
                DeclareArray declareArray = (DeclareArray) i;
                declareArray.getVar().setAddress(globalOffset - globalVariableSize);
                globalOffset += 4 * declareArray.getSize();
            }
        }
        for (Function function : midCodeProgram.getFunctionTable().values()) {
            function.allocateAddress(0);
            System.out.println("function: " + function.getName());
            System.out.println("Stack size: " + function.getStackSize());
            for (Symbol i : function.getParamTable().getSymbolList()) {
                System.out.println(i + ":" + i.getAddress());
            }
            for (Symbol i : function.getParamTable().getTempVarTable().values()) {
                System.out.println(i + ":" + i.getAddress());
            }
        }
    }

    public void translateData() {
        //mipsCode.append(".data").append("\n");
        // 全局字符串部分
        for (Map.Entry<Symbol, String> entry : midCodeProgram.getGlobalStringTable().entrySet()) {
            data.append("STRING_").append(entry.getKey()).append(":").append("\n");
            data.append("\t").append(".asciiz ").append("\"").append(entry.getValue()).append("\"").append("\n");
        }

        // 全局数组部分
        for (MidCode i : midCodeProgram.getGlobalVarDeclCode().getMidCodeList()) {
            if (i instanceof DeclareArray) {
                if (((DeclareArray) i).getInitValue() == null) {
                    data.append("GLOBAL_").append(((DeclareArray) i).getVar().getName()).append(":").append("\n");
                    data.append("\t").append(".space ").append(((DeclareArray) i).getSize() * 4).append("\n");
                } else {
                    data.append("GLOBAL_").append(((DeclareArray) i).getVar().getName()).append(":").append("\n");
                    data.append("\t").append(".word ");
                    for (int j = 0; j < ((DeclareArray) i).getSize(); j++) {
                        int initValue = ((Immediate)((DeclareArray) i).getInitValue().get(j)).getValue();
                        data.append(initValue);
                        if (j < ((DeclareArray) i).getSize()-1) data.append(",");
                    }
                    data.append("\n");
                }
            }
        }

        // 全局变量部分
        data.append("\n");
        //mipsCode.append(".text\n");
        //mipsCode.append("\t").append("addiu $gp, $gp, ").append(globalVariableSize).append("\n");
        text.add(new ArithI(ArithI.Type.addiu, "$gp", "$gp", globalVariableSize));
        for (MidCode i : midCodeProgram.getGlobalVarDeclCode().getMidCodeList()) {
            if (i instanceof DeclareVar) {
                if (((DeclareVar) i).getType() == DeclareVar.Type.CONST_DEF) continue;
                translateDeclareVar((DeclareVar) i);
            }
        }
    }

    public void translateFunction(Function function) {
        clearTempRegisters();
        registerMap.setAllocatableRegisters(function.getLocalRegisters());
        allocatedGlobalRegisters.clear();
        RegisterMap.regClockCount = 0;
        currentFunction = function;
        // mipsCode.append(function.getName()).append(":").append("\n");
        for (int i = 0; i < function.getBody().getMidCodeList().size(); i++) {
            MidCode code = function.getBody().getMidCodeList().get(i);
            if (code instanceof FuncCallIdent) {
                text.add(new Comment(code.toString()));
                text.add(new Newline());
                if (((FuncCallIdent) code).getType() == FuncCallIdent.Type.BEGIN) {
                    i = translateCall(i, ((FuncCallIdent) code).getFunction());
                }
                code = function.getBody().getMidCodeList().get(i);
                text.add(new Comment(code.toString()));
                text.add(new Newline());
            } else {
                translate(code);
            }
        }
        text.add(new Jr("$ra"));
        //mipsCode.append("\t").append("jr $ra").append("\n");
    }

    public void translateUnaryOp(UnaryOp code) {
        int dst;
        if (code.getOperand1() instanceof Immediate) {
            int value = ((Immediate) code.getOperand1()).getValue();
            dst = allocRegister(code.getDst(), false);
            switch (code.getOp()) {
                case MOV:
                    text.add(new Li(id2regName.get(dst), value));
                    break;
                case NEG:
                    text.add(new Li(id2regName.get(dst), -value));
                    break;
                case NOT:
                    text.add(new Li(id2regName.get(dst), value == 0 ? 1 : 0));
                    break;
                default:
                    throw new RuntimeException("Unknown unary operation");
            }
        } else {
            int operand1 = allocRegister((Symbol) code.getOperand1(), true);
            dst = allocRegister(code.getDst(), false);
            switch (code.getOp()) {
                case MOV:
                    text.add(new Move(id2regName.get(dst), id2regName.get(operand1)));
                    break;
                case NEG:
                    text.add(new Neg(id2regName.get(dst), id2regName.get(operand1)));
                    break;
                case NOT:
                    text.add(new ArithR(ArithR.Type.seq, id2regName.get(dst), id2regName.get(operand1), "$zero"));
                    break;
                default:
                    throw new RuntimeException("Unknown unary operation");
            }
        }
        dirty[dst] = true;
        if (code.getDst().isGlobal()) {
            text.add(new Sw(id2regName.get(dst), String.valueOf(code.getDst().getAddress()), "$gp"));
        }
    }

    private List<Instructions> mulToShiftOptimize(String regDst, String regSrc1, int imm) {
        if (imm == 0) return Collections.singletonList(new Li(regDst, 0));
        else if (imm == 1) return Collections.singletonList(new Move(regDst, regSrc1));
        else if ((Math.abs(imm) & (Math.abs(imm) - 1)) == 0) {
            int shift = 0;
            int value = Math.abs(imm);
            while (value > 1) {
                value >>= 1;
                shift++;
            }
            if (imm > 0) {
                return Collections.singletonList(new ArithI(ArithI.Type.sll, regDst, regSrc1, shift));
            } else {
                return Arrays.asList(
                        new ArithI(ArithI.Type.sll, regDst, regSrc1, shift),
                        new ArithR(ArithR.Type.subu, regDst, "$zero", regDst));
            }
        } else {
            return Arrays.asList(new Li("$v0", imm), new Mul(regDst, regSrc1, "$v0"));
        }
    }

    private long[] chooseMultiplier(int imm, int prec) {
        int N = 32;
        long k = (long) Math.ceil((Math.log(imm) / Math.log(2)));
        long sh_post = k;
        long m_low = (long) Math.floor(Math.pow(2, N+k)/imm);
        long m_high = (long) Math.floor((Math.pow(2, N+k) + Math.pow(2, N+k-prec))/imm);
        while ((Math.floor(m_low >> 1) < Math.floor(m_high >> 1)) && sh_post > 0) {
            m_low = (long) Math.floor(m_low >> 1);
            m_high = (long) Math.floor(m_high >> 1);
            sh_post = sh_post - 1;
        }
        return new long[]{m_high, sh_post, k};
    }

    private List<Instructions> divOptimize(String regDst, String regSrc1, int imm) {
        List<Instructions> instructions = new ArrayList<>();
        if (imm == 1) return Collections.singletonList(new Move(regDst, regSrc1));
        else if (imm == -1) return Collections.singletonList(new Neg(regDst, regSrc1));

        if ((Math.abs(imm) & (Math.abs(imm) - 1)) == 0) {       // 如果|imm|是2的整数次幂
            int shift = 0;
            int value = Math.abs(imm);
            while (value > 1) {
                value >>= 1;
                shift++;
            }
            instructions.add(new ArithI(ArithI.Type.sra, regDst, regSrc1, shift));
            if (imm < 0) instructions.add(new ArithR(ArithR.Type.subu, regDst, "$zero", regDst));
            return instructions;
        } else {                            // 否则把除法转换为定点乘法
            long[] multiplier = chooseMultiplier(imm, 31);
            long m = multiplier[0];
            long sh_post = multiplier[1];
            long l = multiplier[2];

            if (m < (1L << 31)) {
                instructions.add(new Li("$v0", (int) m));
                instructions.add(new Mult(regSrc1, "$v0"));
                instructions.add(new Mfhi("$v0"));
                instructions.add(new ArithI(ArithI.Type.sra, /*regDst*/"$a1", "$v0", (int) sh_post));
                instructions.add(new ArithI(ArithI.Type.sra, "$v0", regSrc1, 31));
                instructions.add(new ArithR(ArithR.Type.subu, regDst, /*regDst*/"$a1", "$v0"));
                /*generate("li", reg3, Integer.toString((int) m));
                generate("mult", b, reg3);
                generate("mfhi", reg3);
                generate("sra", reg4, reg3, Integer.toString((int) sh_post));
                generate("sra", reg3, b, String.valueOf(31));
                generate("subu", reg4, reg4, reg3, true);*/
            } else {
                instructions.add(new Li("$v0", (int) (m - Math.pow(2, 32))));
                instructions.add(new Mult(regSrc1, "$v0"));
                instructions.add(new Mfhi("$v0"));
                instructions.add(new ArithR(ArithR.Type.addu, "$v0", "$v0", regSrc1));
                instructions.add(new ArithI(ArithI.Type.sra, /*regDst*/"$a1", "$v0", (int) sh_post));
                instructions.add(new ArithI(ArithI.Type.sra, "$v0", regSrc1, 31));
                instructions.add(new ArithR(ArithR.Type.subu, regDst, /*regDst*/"$a1", "$v0"));
                /*generate("li", reg3, Integer.toString((int) (m - Math.pow(2, utils.N))));
                generate("mult", b, reg3);
                generate("mfhi", reg3);
                generate("addu", reg3, b, reg3);
                generate("sra", reg4, reg3, Integer.toString((int) sh_post));
                generate("sra", reg3, b, String.valueOf(31));
                generate("subu", reg4, reg4, reg3, true);*/
            }
            if (imm < 0) instructions.add(new Neg(regDst, regDst));
            return instructions;
        }
    }

    private List<Instructions> binaryOp2Mips(BinaryOp.Op op, String regDst, String regSrc1, int imm) {
        List<Instructions> tmp = new ArrayList<>();
        switch (op) {
            case ADD:
                return Collections.singletonList(new ArithI(ArithI.Type.addiu, regDst, regSrc1, imm));
            case SUB:
                return Collections.singletonList(new ArithI(ArithI.Type.addiu, regDst, regSrc1, -imm));
            case MUL:
                return mulToShiftOptimize(regDst, regSrc1, imm);
            case DIV:
                // Warning: 这里的imm好像会导致出现一些错误
                return divOptimize(regDst, regSrc1, imm);
            case MOD:
                tmp.addAll(divOptimize(regDst, regSrc1, imm));
                tmp.add(new Mul(regDst, regDst, String.valueOf(imm)));
                tmp.add(new ArithR(ArithR.Type.subu, regDst, regSrc1, regDst));
                return tmp;
            case GE:
                return Collections.singletonList(new ArithI(ArithI.Type.sge, regDst, regSrc1, imm));
            case GT:
                return Collections.singletonList(new ArithI(ArithI.Type.sgt, regDst, regSrc1, imm));
            case LE:
                return Collections.singletonList(new ArithI(ArithI.Type.sle, regDst, regSrc1, imm));
            case LT:
                tmp.add(new Li("$v0", imm));
                tmp.add(new ArithR(ArithR.Type.slt, regDst, regSrc1, "$v0"));
                return tmp;
            case EQ:
                return Collections.singletonList(new ArithI(ArithI.Type.seq, regDst, regSrc1, imm));
            case NE:
                return Collections.singletonList(new ArithI(ArithI.Type.sne, regDst, regSrc1, imm));
            default:
                throw new AssertionError("Bad BinaryOp");
        }
    }

    private List<Instructions> binaryOp2Mips(BinaryOp.Op op, String regDst, String regSrc1, String regSrc2) {
        List<Instructions> tmp = new ArrayList<>();
        switch (op) {
            case ADD:
                return Collections.singletonList(new ArithR(ArithR.Type.addu, regDst, regSrc1, regSrc2));
            case SUB:
                return Collections.singletonList(new ArithR(ArithR.Type.subu, regDst, regSrc1, regSrc2));
            case MUL:
                return Collections.singletonList(new Mul(regDst, regSrc1, regSrc2));
            case DIV:
                tmp.add(new Div(regSrc1, regSrc2));
                tmp.add(new Mflo(regDst));
                return tmp;
            case MOD:
                tmp.add(new Div(regSrc1, regSrc2));
                tmp.add(new Mfhi(regDst));
                return tmp;
            case GE:
                return Collections.singletonList(new ArithR(ArithR.Type.sge, regDst, regSrc1, regSrc2));
            case GT:
                return Collections.singletonList(new ArithR(ArithR.Type.sgt, regDst, regSrc1, regSrc2));
            case LE:
                return Collections.singletonList(new ArithR(ArithR.Type.sle, regDst, regSrc1, regSrc2));
            case LT:
                return Collections.singletonList(new ArithR(ArithR.Type.slt, regDst, regSrc1, regSrc2));
            case EQ:
                return Collections.singletonList(new ArithR(ArithR.Type.seq, regDst, regSrc1, regSrc2));
            case NE:
                return Collections.singletonList(new ArithR(ArithR.Type.sne, regDst, regSrc1, regSrc2));
            default:
                throw new AssertionError("Bad BinaryOp");
        }
    }

    public void translateBinaryOp(BinaryOp code) {
        int regDst, regSrc1, regSrc2;
        if (code.getOperand1() instanceof Immediate) {
            if (code.getOperand2() instanceof Immediate) {
                // 双立即数, 直接算出结果
                int src1 = ((Immediate) code.getOperand1()).getValue();
                int src2 = ((Immediate) code.getOperand2()).getValue();
                regDst = allocRegister(code.getDst(), false);
                int result;
                switch (code.getOp()) {
                    case ADD: result = src1 + src2; break;
                    case SUB: result = src1 - src2; break;
                    case MUL: result = src1 * src2; break;
                    case DIV: result = src1 / src2; break;
                    case MOD: result = src1 % src2; break;
                    case GE: result = (src1 >= src2) ? 1 : 0; break;
                    case GT: result = (src1 > src2) ? 1 : 0; break;
                    case LE: result = (src1 <= src2) ? 1 : 0; break;
                    case LT: result = (src1 < src2) ? 1 : 0; break;
                    case EQ: result = (src1 == src2) ? 1 : 0; break;
                    case NE: result = (src1 != src2) ? 1 : 0; break;
                    default: throw new AssertionError("Bad BinaryOp");
                }
                text.add(new Li(id2regName.get(regDst), result));
            } else {
                regSrc2 = allocRegister((Symbol) code.getOperand2(), true);
                regDst = allocRegister(code.getDst(), false);
                consumeTempVar((Symbol) code.getOperand2());
                switch (code.getOp()) {
                    case ADD: case EQ: case MUL:
                        // 交换操作数
                        text.addAll(binaryOp2Mips(code.getOp(), id2regName.get(regDst), id2regName.get(regSrc2), ((Immediate) code.getOperand1()).getValue()));
                        break;
                    case SUB: case DIV: case MOD: case GE: case GT: case LE: case LT: case NE:
                        text.add(new Li("$v0", ((Immediate) code.getOperand1()).getValue()));
                        text.addAll(binaryOp2Mips(code.getOp(), id2regName.get(regDst), "$v0", id2regName.get(regSrc2)));
                        break;
                    default: throw new AssertionError("Bad BinaryOp");
                }
            }
        } else {
            if (code.getOperand2() instanceof Immediate) {
                // 寄存器, 立即数 (I 型指令)
                regSrc1 = allocRegister((Symbol) code.getOperand1(), true);
                regDst = allocRegister(code.getDst(), false);
                consumeTempVar((Symbol) code.getOperand1());
                int immediate = ((Immediate) code.getOperand2()).getValue();
                text.addAll(binaryOp2Mips(code.getOp(), id2regName.get(regDst), id2regName.get(regSrc1), immediate));
            } else {
                // 寄存器, 寄存器 (R 型指令)
                regSrc1 = allocRegister((Symbol) code.getOperand1(), true);
                regSrc2 = allocRegister((Symbol) code.getOperand2(), true, regSrc1);
                consumeTempVar((Symbol) code.getOperand1());
                consumeTempVar((Symbol) code.getOperand2());
                regDst = allocRegister(code.getDst(), false);
                text.addAll(binaryOp2Mips(code.getOp(), id2regName.get(regDst), id2regName.get(regSrc1), id2regName.get(regSrc2)));
            }
        }
        dirty[regDst] = true;
        if (code.getDst().isGlobal()) {
            text.add(new Sw(id2regName.get(regDst), String.valueOf(code.getDst().getAddress()), "$gp"));
        }
    }

    public void translateAssign(Assign code) {
        int regSrc, regDst = -1;
        if (code.getDst().getDepth() == 0) {    // 全局变量
            if (code.getSrc() instanceof Immediate) {
                // 立即数, 全局变量 (I 型指令)
                int immediate = ((Immediate) code.getSrc()).getValue();
                text.add(new Li("$a0", immediate));
                text.add(new Sw("$a0", String.valueOf(code.getDst().getAddress()), "$gp"));
//                mipsCode.append("\t")
//                        .append("li ").append(id2regName.get(4))
//                        .append(", ").append(immediate).append("\n");
//                mipsCode.append("\t")
//                        .append("sw ").append(id2regName.get(4))
//                        .append(", ").append(code.getDst().getAddress()).append("($gp)").append("\n");
                if (registerMap.isAllocated(code.getDst())) {
                    regDst = allocRegister(code.getDst(), false);
                    text.add(new Li(id2regName.get(regDst), immediate));
/*                    mipsCode.append("\t")
                            .append("li ").append(id2regName.get(regDst))
                            .append(",").append(immediate).append("\n")*/;
                }
            } else {
                // 寄存器, 全局变量 (R 型指令)
                regSrc = allocRegister((Symbol) code.getSrc(), true);
                consumeTempVar((Symbol) code.getSrc());
                text.add(new Sw(id2regName.get(regSrc), String.valueOf(code.getDst().getAddress()), "$gp"));
                /*mipsCode.append("\t")
                        .append("sw ").append(id2regName.get(regSrc))
                        .append(", ").append(code.getDst().getAddress()).append("($gp)").append("\n");*/
                if (registerMap.isAllocated(code.getDst())) {
                    regDst = allocRegister(code.getDst(), false, regSrc);
                    text.add(new Lw(id2regName.get(regDst), String.valueOf(code.getDst().getAddress()), "$gp"));
                    /*mipsCode.append("\t")
                            .append("lw ").append(id2regName.get(regDst))
                            .append(", ").append(code.getDst().getAddress()).append("($gp)").append("\n");*/
                }
            }
        } else {
            if (code.getSrc() instanceof Immediate) {
                // 立即数, 寄存器 (I 型指令)
                regDst = allocRegister(code.getDst(), false);
                int immediate = ((Immediate) code.getSrc()).getValue();
                text.add(new Li(id2regName.get(regDst), immediate));
/*                mipsCode.append("\t")
                        .append("li ").append(id2regName.get(regDst))
                        .append(", ").append(immediate).append("\n");*/
            } else {
                // 寄存器, 寄存器 (R 型指令)
                regSrc = allocRegister((Symbol) code.getSrc(), true);
                regDst = allocRegister(code.getDst(), false);
                consumeTempVar((Symbol) code.getSrc());
                text.add(new Move(id2regName.get(regDst), id2regName.get(regSrc)));
/*                mipsCode.append("\t")
                        .append("move ").append(id2regName.get(regDst))
                        .append(", ").append(id2regName.get(regSrc)).append("\n");*/
            }
        }
        if (regDst != -1) dirty[regDst] = true;
    }

    public void translateLoadSave(LoadSave code) {
        int regOffset, regBase;
        if (code.getOp() == LoadSave.Op.LOAD) {
            int regDst;
            Symbol dst = code.getDst();
            Symbol base = code.getBase();
            Operand offset = code.getOffset();
            if (base.getDepth() == 0) {
                if (offset instanceof Immediate) {
                    // 立即数, 寄存器 (I 型指令)
                    int immediate = ((Immediate) offset).getValue() * 4;
                    regDst = allocRegister(dst, false);
                    text.add(new Lw(id2regName.get(regDst), "GLOBAL_" + base.getName() + " + " + immediate, "$zero"));
                    /*mipsCode.append("\t")
                            .append("lw ").append(id2regName.get(regDst)).append(",")
                            .append("GLOBAL_").append(base.getName()).append(" + ")
                            .append(immediate).append("($zero)\n");*/
                } else {
                    // 寄存器, 寄存器 (R 型指令)
                    regOffset = allocRegister((Symbol) offset, true);
                    regDst = allocRegister(dst, false);
                    consumeTempVar((Symbol) offset);
                    text.add(new ArithI(ArithI.Type.sll, "$a1", id2regName.get(regOffset), 2));
                    text.add(new Lw(id2regName.get(regDst), "GLOBAL_" + base.getName(), "$a1"));
/*                    mipsCode.append("\t")
                            .append("sll ").append(id2regName.get(5)).append(", ")
                            .append(id2regName.get(regOffset)).append(", 2").append("\n");
                    mipsCode.append("\t")
                            .append("lw ").append(id2regName.get(regDst)).append(",")
                            .append("GLOBAL_").append(base.getName()).append("(")
                            .append(id2regName.get(5)).append(")\n");*/
                }
            } else {
                if (base instanceof FunctionFormParam) regBase = allocRegister(base, true);
                else regBase = 29;
                if (offset instanceof Immediate) {
                    // 立即数, 寄存器 (I 型指令)
                    regDst = allocRegister(dst, false);
                    int arrayBaseOffset = base.getAddress();
                    int immediate = ((Immediate) offset).getValue();

                    if (base instanceof FunctionFormParam) arrayBaseOffset = 0;
                    int itemOffset = arrayBaseOffset + immediate * 4;
                    text.add(new Lw(id2regName.get(regDst), String.valueOf(itemOffset), id2regName.get(regBase)));
                    /*mipsCode.append("\t")
                            .append("lw ").append(id2regName.get(regDst))
                            .append(", ").append(itemOffset).append("(")
                            .append(id2regName.get(regBase)).append(")\n");*/
                } else {
                    // 寄存器, 寄存器 (R 型指令)
                    regOffset = allocRegister((Symbol) offset, true, regBase);
                    regDst = allocRegister(dst, false);
                    consumeTempVar((Symbol) offset);
                    text.add(new ArithI(ArithI.Type.sll, "$a1", id2regName.get(regOffset), 2));
                    text.add(new ArithR(ArithR.Type.addu, "$a1", id2regName.get(regBase), "$a1"));
                    /*mipsCode.append("\t")
                            .append("sll ").append(id2regName.get(5)).append(", ")
                            .append(id2regName.get(regOffset)).append(", 2\n");
                    mipsCode.append("\t")
                            .append("addu ").append(id2regName.get(5)).append(", ")
                            .append(id2regName.get(5)).append(", ")
                            .append(id2regName.get(regBase)).append("\n");*/
                    if (base instanceof Variable) {
                        //text.add(new ArithI(ArithI.Type.addiu, "$a1", "$a1", base.getAddress()));
                        text.add(new Lw(id2regName.get(regDst), String.valueOf(base.getAddress()), "$a1"));
                        /*mipsCode.append("\t")
                                .append("addiu ").append(id2regName.get(5))
                                .append(", ").append(id2regName.get(5)).append(", ")
                                .append(base.getAddress()).append("\n");*/
                    } else {
                        text.add(new Lw(id2regName.get(regDst), "0", "$a1"));
                    }
                    /*mipsCode.append("\t")
                            .append("lw ").append(id2regName.get(regDst)).append(", ")
                            .append("0(").append(id2regName.get(5)).append(")\n");*/
                }
            }
            if (code.getDst().isGlobal()) {
                text.add(new Sw(id2regName.get(regDst), String.valueOf(code.getDst().getAddress()), "$gp"));
            }
            dirty[regDst] = true;
        } else if (code.getOp() == LoadSave.Op.STORE) {
            Operand src = code.getSrc();
            Symbol base = code.getBase();
            Operand offset = code.getOffset();
            int regSrc;
            if (src instanceof Immediate) {
                regSrc = 2;
                int immediate = ((Immediate) src).getValue();
                text.add(new Li(id2regName.get(regSrc), immediate));
                /*mipsCode.append("\t")
                        .append("li ").append(id2regName.get(regSrc))
                        .append(", ").append(immediate).append("\n");*/
            } else {
                regSrc = allocRegister((Symbol) src, true);
            }
            if (base.getDepth() == 0) {
                if (offset instanceof Immediate) {
                    // 立即数, 寄存器 (I 型指令)
                    int immediate = ((Immediate) offset).getValue();
                    text.add(new Sw(id2regName.get(regSrc), "GLOBAL_" + base.getName() + " + " + immediate * 4, "$zero"));
                    /*mipsCode.append("\t")
                            .append("sw ").append(id2regName.get(regSrc)).append(",")
                            .append("GLOBAL_").append(base.getName()).append(" + ")
                            .append(immediate * 4).append("\n");*/
                } else {
                    // 寄存器, 寄存器 (R 型指令)
                    regOffset = allocRegister((Symbol) offset, true, regSrc);
                    consumeTempVar((Symbol) offset);
                    text.add(new ArithI(ArithI.Type.sll, "$a1", id2regName.get(regOffset), 2));
                    text.add(new Sw(id2regName.get(regSrc), "GLOBAL_" + base.getName(), "$a1"));
                    /*mipsCode.append("\t")
                            .append("sll ").append(id2regName.get(5)).append(", ")
                            .append(id2regName.get(regOffset)).append(", 2").append("\n");
                    mipsCode.append("\t")
                            .append("sw ").append(id2regName.get(regSrc)).append(",")
                            .append("GLOBAL_").append(base.getName()).append("(")
                            .append(id2regName.get(5)).append(")\n");*/
                }
            } else {
                if (base instanceof FunctionFormParam) regBase = allocRegister(base, true, regSrc);
                else regBase = 29;
                if (offset instanceof Immediate) {
                    // 立即数, 寄存器 (I 型指令)
                    int arrayBaseOffset = base.getAddress();
                    int immediate = ((Immediate) offset).getValue();

                    if (base instanceof FunctionFormParam) arrayBaseOffset = 0;
                    int itemOffset = arrayBaseOffset + immediate * 4;
                    text.add(new Sw(id2regName.get(regSrc), String.valueOf(itemOffset), id2regName.get(regBase)));
                    /*mipsCode.append("\t")
                            .append("sw ").append(id2regName.get(regSrc))
                            .append(", ").append(itemOffset).append("(")
                            .append(id2regName.get(regBase)).append(")\n");*/
                } else {
                    // 寄存器, 寄存器 (R 型指令)
                    regOffset = allocRegister((Symbol) offset, true, regBase, regSrc);
                    consumeTempVar((Symbol) offset);
                    text.add(new ArithI(ArithI.Type.sll, "$a1", id2regName.get(regOffset), 2));
                    text.add(new ArithR(ArithR.Type.addu, "$a1", id2regName.get(regBase), "$a1"));
                    /*mipsCode.append("\t")
                            .append("sll ").append(id2regName.get(5)).append(", ")
                            .append(id2regName.get(regOffset)).append(", 2\n");
                    mipsCode.append("\t")
                            .append("addu ").append(id2regName.get(5)).append(", ")
                            .append(id2regName.get(5)).append(", ")
                            .append(id2regName.get(regBase)).append("\n");*/
                    if (base instanceof Variable) {
                        //text.add(new ArithI(ArithI.Type.addiu, "$a1", "$a1", base.getAddress()));
                        text.add(new Sw(id2regName.get(regSrc), String.valueOf(base.getAddress()), "$a1"));
                        /*mipsCode.append("\t")
                                .append("addiu ").append(id2regName.get(5))
                                .append(", ").append(id2regName.get(5)).append(", ")
                                .append(base.getAddress()).append("\n");*/
                    } else {
                        text.add(new Sw(id2regName.get(regSrc), "0", "$a1"));
                    }
                    /*mipsCode.append("\t")
                            .append("sw ").append(id2regName.get(regSrc)).append(", ")
                            .append("0(").append(id2regName.get(5)).append(")\n");*/
                }
            }
            if (src instanceof Symbol) consumeTempVar((Symbol) src);
        }
    }

    public void translateReturn(Return code) {
        if (currentFunction.getName().equals("main")) {
            text.add(new Li("$v0", 10));
            text.add(new Syscall());
        } else {
            Operand ret = code.getValue();
            if (ret instanceof Immediate) {
                int immediate = ((Immediate) ret).getValue();
                text.add(new Li("$v0", immediate));
            } else if (ret != null) {
                int regRet = allocRegister((Symbol) ret, true);
                consumeTempVar((Symbol) ret);
                text.add(new Move("$v0", id2regName.get(regRet)));
            }
            text.add(new Jr("$ra"));
        }
    }

    public void translateDeclareVar(DeclareVar code) {
        if (code.getVar().getDepth() == 0) {    // 全局变量
            if (code.getInitValue() == null) {
                text.add(new Li("$v0", 0));
                text.add(new Sw("$v0", String.valueOf(code.getVar().getAddress()), "$gp"));
                /*mipsCode.append("\t")
                        .append("li ").append(id2regName.get(4))
                        .append(", ").append(0).append("\n");
                mipsCode.append("\t")
                        .append("sw ").append(id2regName.get(4))
                        .append(", ").append(code.getVar().getAddress()).append("($gp)").append("\n");*/
            } else {
                if (code.getInitValue() instanceof Immediate) {
                    int immediate = ((Immediate) code.getInitValue()).getValue();
                    text.add(new Li("$v0", immediate));
                    text.add(new Sw("$v0", String.valueOf(code.getVar().getAddress()), "$gp"));
                    /*mipsCode.append("\t")
                            .append("li ").append(id2regName.get(4))
                            .append(", ").append(immediate).append("\n");
                    mipsCode.append("\t")
                            .append("sw ").append(id2regName.get(4))
                            .append(", ").append(code.getVar().getAddress()).append("($gp)").append("\n");*/
                } else {
                    int regInit = allocRegister((Symbol) code.getInitValue(), true);
                    consumeTempVar((Symbol) code.getInitValue());
                    text.add(new Sw(id2regName.get(regInit), String.valueOf(code.getVar().getAddress()), "$gp"));
                }
            }
        } else {
            if (code.getInitValue() == null) {
                allocRegister(code.getVar(), false);
            } else {
                if (code.getInitValue() instanceof Immediate) {
                    int immediate = ((Immediate) code.getInitValue()).getValue();
                    int regVar = allocRegister(code.getVar(), false);
                    text.add(new Li(id2regName.get(regVar), immediate));
                    /*mipsCode.append("\t")
                            .append("li ").append(id2regName.get(regVar))
                            .append(", ").append(immediate).append("\n");*/
                } else {
                    int regInit = allocRegister((Symbol) code.getInitValue(), true);
                    int regVar = allocRegister(code.getVar(), false, regInit);
                    consumeTempVar((Symbol) code.getInitValue());
                    text.add(new Move(id2regName.get(regVar), id2regName.get(regInit)));
                    /*mipsCode.append("\t")
                            .append("move ").append(id2regName.get(regVar))
                            .append(", ").append(id2regName.get(regInit)).append("\n");*/
                }
            }
        }
    }

    public void translateDeclareArray(DeclareArray code) {
        if (code.getVar().getDepth() == 0) throw new RuntimeException("Global array is not supposed to be declared here.");
        int offset = code.getVar().getAddress();
        if (code.getInitValue() != null) {
            for (Operand initValue : code.getInitValue()) {
                if (initValue instanceof Immediate) {
                    text.add(new Li("$a0", ((Immediate) initValue).getValue()));
                    text.add(new Sw("$a0", String.valueOf(offset), "$sp"));
                } else {
                    int regInit = allocRegister((Symbol) initValue, true);
                    consumeTempVar((Symbol) initValue);
                    text.add(new Sw(id2regName.get(regInit), String.valueOf(offset), "$sp"));
                }
                offset += 4;
            }
        }
    }

    public void translateGetInt(GetInt code) {
        text.add(new Li("$v0", 5));
        text.add(new Syscall());
        if (code.getDst().getDepth() == 0 && !code.getDst().isTemp()) {
            text.add(new Sw("$v0", String.valueOf(code.getDst().getAddress()), "$gp"));
            if (registerMap.isAllocated(code.getDst())) {
                int regDst = allocRegister(code.getDst(), false);
                text.add(new Move(id2regName.get(regDst), "$v0"));
                dirty[regDst] = true;
            }
        } else {
            int regDst = allocRegister(code.getDst(), false);
            text.add(new Move(id2regName.get(regDst), "$v0"));
            dirty[regDst] = true;
        }
    }

    public void translatePrint(Print code) {
        if (code.getSrc() instanceof Immediate) {
            text.add(new Li("$a0", ((Immediate) code.getSrc()).getValue()));
            text.add(new Li("$v0", 1));
            text.add(new Syscall());
        } else {
            Symbol src = (Symbol) code.getSrc();
            if (src.isString()) {
                text.add(new La("$a0", "STRING_" + src.getName()));
                text.add(new Li("$v0", 4));
                text.add(new Syscall());
            } else {
                int regSrc = allocRegister(src, true);
                consumeTempVar(src);
                text.add(new Move("$a0", id2regName.get(regSrc)));
                text.add(new Li("$v0", 1));
                text.add(new Syscall());
            }
        }
    }

    public int translateCall(int id, Function function) {
        // 将$ra压栈
        if (!currentFunction.getName().equals("main")) text.add(new Sw("$ra", "-4", "$sp"));
        id++;
        int i = 0;
        // 将参数压栈
        while (currentFunction.getBody().get(id) instanceof PushParam) {
            PushParam code = (PushParam) currentFunction.getBody().get(id);
            text.add(new Comment(code.toString()));
            Operand arg = code.getParam();
            if (code.getType() == PushParam.Type.PUSH) {
                if (arg instanceof Immediate) {
                    text.add(new Li("$v0", ((Immediate) arg).getValue()));
                    text.add(new Sw("$v0", String.valueOf(-function.getStackSize() + i * 4 - 4), "$sp"));
                } else {
                    int regArg = allocRegister((Symbol) arg, true);
                    consumeTempVar((Symbol) arg);
                    text.add(new Sw(id2regName.get(regArg), String.valueOf(-function.getStackSize() + i * 4 - 4), "$sp"));
                }
            } else if (code.getType() == PushParam.Type.PUSH_ADDR) {
                if (arg instanceof ArrayPointer) {
                    ArrayPointer arrayPointer = (ArrayPointer) arg;
                    if (arrayPointer.getBase().getDepth() == 0) {     // 是全局数组
                        int regOffset;
                        if (arrayPointer.getOffset() instanceof Immediate) {
                            int baseOffset = ((Immediate) arrayPointer.getOffset()).getValue() * 4;
                            text.add(new La("$a1", "GLOBAL_"+arrayPointer.getBase().getName()));
                            if (baseOffset != 0) text.add(new ArithI(ArithI.Type.addiu, "$a1", "$a1", baseOffset));
                            text.add(new Sw("$a1", String.valueOf(-function.getStackSize() + i * 4 - 4), "$sp"));
                        } else {
                            regOffset = allocRegister((Symbol) arrayPointer.getOffset(), true);
                            consumeTempVar((Symbol) arrayPointer.getOffset());
                            text.add(new ArithI(ArithI.Type.sll, "$v0", id2regName.get(regOffset), 2));
                            text.add(new La("$a1", "GLOBAL_"+arrayPointer.getBase().getName()));
                            text.add(new ArithR(ArithR.Type.addu, "$a1", "$a1", "$v0"));
                            text.add(new Sw("$a1", String.valueOf(-function.getStackSize() + i * 4 - 4), "$sp"));
                        }
                    } else {    // 是局部数组
                        if (arrayPointer.getBase() instanceof Variable) {
                            if (arrayPointer.getOffset() instanceof Immediate) {
                                int baseOffset = arrayPointer.getBase().getAddress()
                                        + ((Immediate) arrayPointer.getOffset()).getValue() * 4;
                                text.add(new ArithI(ArithI.Type.addiu, "$a1", "$sp", baseOffset));
                                text.add(new Sw("$a1", String.valueOf(-function.getStackSize() + i * 4 - 4), "$sp"));
                            } else {
                                int regOffset = allocRegister((Symbol) arrayPointer.getOffset(), true);
                                consumeTempVar((Symbol) arrayPointer.getOffset());
                                text.add(new ArithI(ArithI.Type.sll, "$v0", id2regName.get(regOffset), 2));
                                text.add(new ArithI(ArithI.Type.addiu, "$a1", "$sp", arrayPointer.getBase().getAddress()));
                                text.add(new ArithR(ArithR.Type.addu, "$a1", "$a1", "$v0"));
                                text.add(new Sw("$a1", String.valueOf(-function.getStackSize() + i * 4 - 4), "$sp"));
                            }
                        } else if (arrayPointer.getBase() instanceof FunctionFormParam) {
                            int regBase = allocRegister(arrayPointer.getBase(), true);
                            if (arrayPointer.getOffset() instanceof Immediate) {
                                int baseOffset = ((Immediate) arrayPointer.getOffset()).getValue() * 4;
                                if (baseOffset == 0) {
                                    text.add(new Sw(id2regName.get(regBase), String.valueOf(-function.getStackSize() + i * 4 - 4), "$sp"));
                                } else {
                                    text.add(new ArithI(ArithI.Type.addiu, "$a1", id2regName.get(regBase), baseOffset));
                                    text.add(new Sw("$a1", String.valueOf(-function.getStackSize() + i * 4 - 4), "$sp"));
                                }
                            } else {
                                int regOffset = allocRegister((Symbol) arrayPointer.getOffset(), true, regBase);
                                consumeTempVar((Symbol) arrayPointer.getOffset());
                                text.add(new ArithI(ArithI.Type.sll, "$v0", id2regName.get(regOffset), 2));
                                text.add(new ArithR(ArithR.Type.addu, "$a1", id2regName.get(regBase), "$v0"));
                                text.add(new Sw("$a1", String.valueOf(-function.getStackSize() + i * 4 - 4), "$sp"));
                            }
                        }
                    }
                } else {
                    throw new RuntimeException("Push address of non-array pointer");
                }
            }
            text.add(new Newline());
            i++; id++;
        }
        // 调用函数
        if (currentFunction.getBody().get(id) instanceof Call) {
            Call code = (Call) currentFunction.getBody().get(id);
            text.add(new Comment(code.toString()));
            // 将所有使用的寄存器保存
            saveTempRegisters();
            // 取消所有的寄存器分配
            clearTempRegisters();
            saveGlobalRegisters(code);
            // 移动$sp指针
            text.add(new ArithI(ArithI.Type.addiu, "$sp", "$sp", -function.getStackSize() - 4));
            /*mipsCode.append("\t").append("addiu $sp, $sp, ")
                .append(-function.getStackSize() - 4).append("\n");*/
            // 调用函数
            text.add(new Jal(function.getName()));
            clearTempRegisters();
            // 恢复$sp寄存器
            text.add(new ArithI(ArithI.Type.addiu, "$sp", "$sp", function.getStackSize() + 4));
            restoreGlobalRegisters(code);
            // 恢复$ra寄存器
            if (!currentFunction.getName().equals("main")) text.add(new Lw("$ra", "-4", "$sp"));
            // 存返回值
            if (code.getRet() != null) {
                int regRet = allocRegister((Symbol) code.getRet(), false);
                text.add(new Move(id2regName.get(regRet), "$v0"));
                dirty[regRet] = true;
            }
            text.add(new Newline());
            id++;
        }
        return id;
    }

    public void translateLabel(Label code) {
        text.add(new backend.instructions.Label(code.getLabel()));
    }

    public void translateBranch(Branch code) {
        if (code.getCond() instanceof Immediate) {
            saveTempRegisters();
            int val = ((Immediate) code.getCond()).getValue();
            switch (code.getType()) {
                case EQ:
                    if (val == 0) text.add(new J(code.getTarget().getLabel()));
                    break;
                case NE:
                    if (val != 0) text.add(new J(code.getTarget().getLabel()));
                    break;
                case GT:
                    if (val > 0) text.add(new J(code.getTarget().getLabel()));
                    break;
                case GE:
                    if (val >= 0) text.add(new J(code.getTarget().getLabel()));
                    break;
                case LT:
                    if (val < 0) text.add(new J(code.getTarget().getLabel()));
                    break;
                case LE:
                    if (val <= 0) text.add(new J(code.getTarget().getLabel()));
                    break;
            }
        } else {
            int regCond = allocRegister((Symbol) code.getCond(), true);
            consumeTempVar((Symbol) code.getCond());
            saveTempRegisters();
            /*Symbol symbol = (Symbol) code.getCond();
            if (symbol.isGlobal()) {
                text.add(new Lw(id2regName.get(regCond), String.valueOf(symbol.getAddress()), "$gp"));
                //mipsCode.append(String.format("\tlw %s, %d($gp)", id2regName.get(regCond), symbol.getAddress())).append("\n");
            } else {
                text.add(new Lw(id2regName.get(regCond), String.valueOf(symbol.getAddress()), "$sp"));
                //mipsCode.append(String.format("\tlw %s, %d($sp)", id2regName.get(regCond), symbol.getAddress())).append("\n");
            }*/
            switch(code.getType()) {
                case EQ:
                    text.add(new Beq(id2regName.get(regCond), "$zero", code.getTarget().getLabel()));
                    break;
                case NE:
                    text.add(new Bne(id2regName.get(regCond), "$zero", code.getTarget().getLabel()));
                    break;
                case GT:
                    text.add(new Bgtz(id2regName.get(regCond), code.getTarget().getLabel()));
                    break;
                case GE:
                    text.add(new Bgez(id2regName.get(regCond), code.getTarget().getLabel()));
                    break;
                case LT:
                    text.add(new Bltz(id2regName.get(regCond), code.getTarget().getLabel()));
                    break;
                case LE:
                    text.add(new Blez(id2regName.get(regCond), code.getTarget().getLabel()));
                    break;
            }
        }
        clearTempRegisters();
    }

    public void translateJump(Jump code) {
        saveTempRegisters();
        clearTempRegisters();
        text.add(new J(code.getTarget().getLabel()));
    }

    public void translateBranch2Var(Branch2Var code) {
        int regOp1 = -1, regOp2 = -1;
        if (code.getOperand1() instanceof Symbol) {
            regOp1 = allocRegister((Symbol) code.getOperand1(), true);
            consumeTempVar((Symbol) code.getOperand1());
        }
        if (code.getOperand2() instanceof Symbol) {
            regOp2 = allocRegister((Symbol) code.getOperand2(), true);
            consumeTempVar((Symbol) code.getOperand2());
        }
        saveTempRegisters();
        switch(code.getType()) {
            case EQ:
                text.add(new Beq(regOp1 != -1 ? id2regName.get(regOp1) : String.valueOf(((Immediate) code.getOperand1()).getValue()),
                        regOp2 != -1 ? id2regName.get(regOp2) : String.valueOf(((Immediate) code.getOperand2()).getValue()),
                        code.getTarget().getLabel()));
                break;
            case NE:
                text.add(new Bne(regOp1 != -1 ? id2regName.get(regOp1) : String.valueOf(((Immediate) code.getOperand1()).getValue()),
                        regOp2 != -1 ? id2regName.get(regOp2) : String.valueOf(((Immediate) code.getOperand2()).getValue()),
                        code.getTarget().getLabel()));
                break;
            default:
                throw new RuntimeException("unimplemented");
        }
        clearTempRegisters();
    }

    public void translateParamDef(ParamDef code) {
        Symbol symbol = code.getParam();
        int register = allocRegister(code.getParam(), true);
        if (currentFunction.getRegMap().containsKey(code.getParam())) {
            if (symbol.isGlobal()) {
                text.add(new Lw(id2regName.get(register), String.valueOf(symbol.getAddress()), "$gp"));
            } else {
                text.add(new Lw(id2regName.get(register), String.valueOf(symbol.getAddress()), "$sp"));
            }
        }
    }

    public void translate(MidCode code) {
        //mipsCode.append("# ").append(code).append("\n");
        text.add(new Comment(code.toString()));
        if (code instanceof Assign) {
            translateAssign((Assign) code);
        } else if (code instanceof UnaryOp) {
            translateUnaryOp((UnaryOp) code);
        } else if (code instanceof BinaryOp) {
            translateBinaryOp((BinaryOp) code);
        } else if (code instanceof Return) {
            translateReturn((Return) code);
        } else if (code instanceof LoadSave) {
            translateLoadSave((LoadSave) code);
        } else if (code instanceof DeclareVar) {
            translateDeclareVar((DeclareVar) code);
        } else if (code instanceof Print) {
            translatePrint((Print) code);
        } else if (code instanceof GetInt) {
            translateGetInt((GetInt) code);
        } else if (code instanceof DeclareArray) {
            translateDeclareArray((DeclareArray) code);
        } else if (code instanceof Jump) {
            translateJump((Jump) code);
        } else if (code instanceof Branch) {
            translateBranch((Branch) code);
        } else if (code instanceof Label) {
            translateLabel((Label) code);
        } else if (code instanceof Branch2Var) {
            translateBranch2Var((Branch2Var) code);
        } else if (code instanceof ParamDef) {
            translateParamDef((ParamDef) code);
        } else {
            // throw new RuntimeException("Unknown MidCode type");
        }
        text.add(new Newline());
    }

    public void toMips() {
        calcStackSize();
        translateData();
        statisticTempVarUsage();
        int stackSize = midCodeProgram.getFunctionTable().get("main").getStackSize();
        text.add(new ArithI(ArithI.Type.addiu, "$sp", "$sp", -stackSize));
        //mipsCode.append("\taddiu $sp, $sp, ").append(-midCodeProgram.getFunctionTable().get("main").getStackSize()).append("\n");
        text.add(new Newline());
        text.add(new Comment("function main"));
        translateFunction(midCodeProgram.getFunctionTable().get("main"));
        for (Function function : midCodeProgram.getFunctionTable().values()) {
            if (function.getName().equals("main")) continue;
            text.add(new Newline());
            text.add(new Comment("function " + function.getName()));
            translateFunction(function);
        }
    }
}
