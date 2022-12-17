package middle.optimize;

import middle.middle_code.MidCodeProgram;
import middle.middle_code.element.*;
import middle.middle_code.operand.ArrayPointer;
import middle.middle_code.operand.Immediate;
import middle.middle_code.operand.Operand;
import middle.symbol.Function;
import middle.symbol.Symbol;
import util.Pair;

import java.util.*;

public class DataFlowAnalyse {
    private final MidCodeProgram midCodeProgram;
    private final Map<Symbol, HashSet<Pair<Integer, Integer>>> activeDefOfICode = new HashMap<>();

    public DataFlowAnalyse(MidCodeProgram midCodeProgram) {
        this.midCodeProgram = midCodeProgram;
    }

    private Set<Pair<Integer, Integer>> getAllKill(Function function, Symbol symbol) {
        Set<Pair<Integer, Integer>> result = new HashSet<>();
        for (int i = 0; i < function.getBasicBlocks().size(); i++) {
            BasicBlock basicBlock = function.getBasicBlocks().get(i);
            for (int j = 0; j < basicBlock.getBlock().size(); j++) {
                MidCode code = basicBlock.getBlock().get(j);
                if (code instanceof Assign) {
                    if (((Assign) code).getDst().equals(symbol)) {
                        result.add(new Pair<>(i, j));
                    }
                } else if (code instanceof BinaryOp) {
                    if (((BinaryOp) code).getDst().equals(symbol)) {
                        result.add(new Pair<>(i, j));
                    }
                } else if (code instanceof UnaryOp) {
                    if (((UnaryOp) code).getDst().equals(symbol)) {
                        result.add(new Pair<>(i, j));
                    }
                } else if (code instanceof LoadSave) {
                    if (((LoadSave) code).getOp() == LoadSave.Op.LOAD) {
                        if (((LoadSave) code).getDst().equals(symbol)) {
                            result.add(new Pair<>(i, j));
                        }
                    }
                } else if (code instanceof GetInt) {
                    if (((GetInt) code).getDst().equals(symbol)) {
                        result.add(new Pair<>(i, j));
                    }
                } else if (code instanceof Call) {
                    if (Objects.equals(((Call) code).getRet(), symbol)) {
                        result.add(new Pair<>(i, j));
                    }
                } else if (code instanceof ParamDef) {
                    if (Objects.equals(((ParamDef) code).getParam(), symbol)) {
                        result.add(new Pair<>(i, j));
                    }
                }
            }
        }
        return result;
    }

    private void calcGenKill(Function function) {
        for (BasicBlock basicBlock : function.getBasicBlocks()) {
            Set<Pair<Integer, Integer>> gen = new HashSet<>();
            Set<Pair<Integer, Integer>> kill = new HashSet<>();
            //for (int i = 0; i < basicBlock.getBlock().size(); i++) { gen.add(new HashSet<>()); kill.add(new HashSet<>()); }
            for (int i = basicBlock.getBlock().size()-1; i >= 0; i--) {
                MidCode code = basicBlock.getBlock().get(i);
                if (getAllDef(code) != null) {
                    if (!kill.contains(new Pair<>(basicBlock.getId(), i))) {
                        gen.add(new Pair<>(basicBlock.getId(), i));
                    }
                    kill.addAll(activeDefOfICode.get(getAllDef(code)));
                }
            }
            basicBlock.setGen(gen);
            basicBlock.setKill(kill);
        }
    }

    private void reachDefAnalyse(Function function) {
        calcGenKill(function);
        boolean isChanged;
        do {
            isChanged = false;
            for (BasicBlock basicBlock : function.getBasicBlocks()) {
                Set<Pair<Integer, Integer>> in = new HashSet<>();
                for (BasicBlock predecessor : basicBlock.getPrev()) {
                    if (predecessor.getOut_reaching() != null) {
                        in.addAll(predecessor.getOut_reaching());
                    }
                }
                Set<Pair<Integer, Integer>> out = basicBlock.getOut_reaching();
                if (out == null) out = new HashSet<>(basicBlock.getGen());
                out.addAll(basicBlock.getGen());
                for (Pair<Integer, Integer> pair : in) {
                    if (!basicBlock.getKill().contains(pair) && !out.contains(pair)) {
                        out.add(pair);
                        isChanged = true;
                    }
                }
                //System.out.println(out);
                //System.out.println(basicBlock.getOut_reaching());
                basicBlock.setIn_reaching(in);
                basicBlock.setOut_reaching(out);
            }
        } while (isChanged);
    }

    private List<Symbol> getAllUse(MidCode code) {
        List<Symbol> ans = new ArrayList<>();
        if (code instanceof Assign && ((Assign) code).getSrc() instanceof Symbol) {
            return Collections.singletonList((Symbol) ((Assign) code).getSrc());
        } else if (code instanceof BinaryOp) {
            if (((BinaryOp) code).getOperand1() instanceof Symbol) ans.add((Symbol) ((BinaryOp) code).getOperand1());
            if (((BinaryOp) code).getOperand2() instanceof Symbol) ans.add((Symbol) ((BinaryOp) code).getOperand2());
            return ans;
        } else if (code instanceof UnaryOp && ((UnaryOp) code).getOperand1() instanceof Symbol) {
            return Collections.singletonList((Symbol) ((UnaryOp) code).getOperand1());
        } else if (code instanceof LoadSave) {
            if (((LoadSave) code).getOp() == LoadSave.Op.LOAD) {
                if (((LoadSave) code).getOffset() instanceof Symbol) return Collections.singletonList((Symbol) ((LoadSave) code).getOffset());
            } else if (((LoadSave) code).getOp() == LoadSave.Op.STORE) {
                if (((LoadSave) code).getOffset() instanceof Symbol) ans.add((Symbol) ((LoadSave) code).getOffset());
                if (((LoadSave) code).getSrc() instanceof Symbol) ans.add((Symbol) ((LoadSave) code).getSrc());
                return ans;
            }
        } else if (code instanceof PushParam) {
            if (((PushParam) code).getType() == PushParam.Type.PUSH) {
                if (((PushParam) code).getParam() instanceof Symbol)
                    return Collections.singletonList((Symbol) ((PushParam) code).getParam());
            } else if (((PushParam) code).getType() == PushParam.Type.PUSH_ADDR) {
                if (((PushParam) code).getParam() instanceof ArrayPointer) {
                    if (((ArrayPointer) ((PushParam) code).getParam()).getOffset() instanceof Symbol)
                        return Collections.singletonList((Symbol) ((ArrayPointer) ((PushParam) code).getParam()).getOffset());
                }
            }
        } else if (code instanceof Branch) {
            if (((Branch) code).getCond() instanceof Symbol) return Collections.singletonList((Symbol) ((Branch) code).getCond());
        } else if (code instanceof Print) {
            if (((Print) code).getSrc() instanceof Symbol && !((Symbol) ((Print) code).getSrc()).isString())
                return Collections.singletonList((Symbol) ((Print) code).getSrc());
        } else if (code instanceof Return) {
            if (((Return) code).getValue() instanceof Symbol) return Collections.singletonList((Symbol) ((Return) code).getValue());
        } else if (code instanceof Branch2Var) {
            if (((Branch2Var) code).getOperand1() instanceof Symbol) ans.add((Symbol) ((Branch2Var) code).getOperand1());
            if (((Branch2Var) code).getOperand2() instanceof Symbol) ans.add((Symbol) ((Branch2Var) code).getOperand2());
        }
        return ans;
    }

    private Symbol getAllDef(MidCode code) {
        if (code instanceof Assign) {
            return ((Assign) code).getDst();
        } else if (code instanceof BinaryOp) {
            return ((BinaryOp) code).getDst();
        } else if (code instanceof UnaryOp) {
            return ((UnaryOp) code).getDst();
        } else if (code instanceof LoadSave) {
            if (((LoadSave) code).getOp() == LoadSave.Op.LOAD) {
                return ((LoadSave) code).getDst();
            }
        } else if (code instanceof GetInt) {
            return ((GetInt) code).getDst();
        } else if (code instanceof Call) {
            if (((Call) code).getRet() != null) return (Symbol) ((Call) code).getRet();
        } else if (code instanceof ParamDef) {
            return ((ParamDef) code).getParam();
        }
        return null;
    }

    private void calcDefUse(Function function) {
        for (BasicBlock basicBlock : function.getBasicBlocks()) {
            Set<Symbol> def = new HashSet<>();
            Set<Symbol> use = new HashSet<>();
            for (MidCode code : basicBlock.getBlock().getMidCodeList()) {
                for (Symbol i : getAllUse(code)) {
                    if (!def.contains(i)) use.add(i);
                }
                if (getAllDef(code) != null) {
                    if (!use.contains(getAllDef(code))) def.add(getAllDef(code));
                    if (!activeDefOfICode.containsKey(getAllDef(code)))
                        activeDefOfICode.put(getAllDef(code), new HashSet<>());
                    activeDefOfICode.get(getAllDef(code)).add(new Pair<>(
                            basicBlock.getId(), basicBlock.getBlock().getMidCodeList().indexOf(code)));
                }
            }
            basicBlock.setDef(def);
            basicBlock.setUse(use);
        }
    }

    private void liveVariableAnalyse(Function function) {
        calcDefUse(function);
        boolean isChanged;
        do {
            isChanged = false;
            for (BasicBlock basicBlock : function.getBasicBlocks()) {
                Set<Symbol> out = new HashSet<>();
                for (BasicBlock successor : basicBlock.getNext()) {
                    out.addAll(successor.getIn_live());
                }
                Set<Symbol> in = new HashSet<>(out);
                in.removeAll(basicBlock.getDef());
                in.addAll(basicBlock.getUse());
                isChanged = isChanged || !in.equals(basicBlock.getIn_live());
                basicBlock.setIn_live(in);
                basicBlock.setOut_live(out);
            }
        } while (isChanged);
    }

    private BasicBlock getBlockByCode(Function function, MidCode target) {
        for (BasicBlock basicBlock : function.getBasicBlocks()) {
            if (basicBlock.getBlock().getMidCodeList().contains(target)) {
                return basicBlock;
            }
        }
        return null;
    }

    protected Set<Pair<Integer, Integer>> reachInOfCode(Function function, MidCode target) {
        BasicBlock block = getBlockByCode(function, target);
        assert block != null;
        Set<Pair<Integer, Integer>> res = block.getIn_reaching();
        for (MidCode code : block.getBlock().getMidCodeList()) {
            if (code == target) break;
            Symbol def = getAllDef(code);
            if (def != null) {
                if (activeDefOfICode.containsKey(def)) res.removeAll(activeDefOfICode.get(def));
                res.add(new Pair<>(block.getId(), block.getBlock().getMidCodeList().indexOf(code)));
            }
        }
        return res;
    }

    protected boolean pathFinder(BasicBlock curBlock, MidCode curCode, MidCode tarCode, Symbol targetValue,
                                 Set<BasicBlock> visited) {
        visited.add(curBlock);
        Iterator<MidCode> iterator = curBlock.getBlock().getMidCodeList().iterator();
        while (iterator.hasNext()) {
            MidCode code = iterator.next();
            if (code == curCode) break;
        }
        while (iterator.hasNext()) {
            MidCode code = iterator.next();
            if (code == tarCode) return false;
            if (Objects.equals(getAllDef(code), targetValue)) {
                return true;
            }
        }
        for (BasicBlock b : curBlock.getNext()) {
            if (!visited.contains(b)) {
                // System.out.println(b);
                if (pathFinder(b, b.getBlock().get(0), tarCode, targetValue, visited)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean pathFind(BasicBlock curBlock, MidCode curCode, MidCode tarCode, Symbol targetValue) {
        final Set<BasicBlock> visited = new HashSet<>();
        return pathFinder(curBlock, curCode, tarCode, targetValue, visited);
    }

    private void propagateValue(Function function) {
        for (BasicBlock block : function.getBasicBlocks()) {
            for (int i = 0; i < block.getBlock().getMidCodeList().size(); i++) {
                MidCode code = block.getBlock().getMidCodeList().get(i);
                final List<Symbol> use = getAllUse(code);
                if (!use.isEmpty()) {
                    final List<Operand> aft = new ArrayList<>(use);
                    Set<Pair<Integer, Integer>> reachIn = reachInOfCode(function, code);
                    for (int j = 0; j < use.size(); ++j) {
                        List<Pair<Integer, Integer>> reach = new LinkedList<>();
                        //System.out.println(use.get(j));
                        for (Pair<Integer, Integer> c : reachIn) {
                            MidCode curCode = function.getCode(c.getFirst(), c.getSecond());
                            if (curCode instanceof Assign) {
                                final Assign assign = (Assign) curCode;
                                if (assign.getDst().equals(use.get(j))) {
                                    reach.add(c);
                                }
                            } else if (curCode instanceof UnaryOp) {
                                final UnaryOp move = (UnaryOp) curCode;
                                if (move.getDst().equals(use.get(j))) {
                                    reach.add(c);
                                }
                            } else if (curCode instanceof BinaryOp) {
                                final BinaryOp move = (BinaryOp) curCode;
                                if (move.getDst().equals(use.get(j))) {
                                    reach.add(c);
                                }
                            } else if (curCode instanceof LoadSave) {
                                final LoadSave move = (LoadSave) curCode;
                                if (((LoadSave) curCode).getOp() == LoadSave.Op.LOAD) {
                                    if (move.getDst().equals(use.get(j))) {
                                        reach.add(c);
                                    }
                                }
                            } else if (curCode instanceof GetInt) {
                                final GetInt move = (GetInt) curCode;
                                if (move.getDst().equals(use.get(j))) {
                                    reach.add(c);
                                }
                            } else if (curCode instanceof Call) {
                                final Call move = (Call) curCode;
                                if (move.getRet() != null && move.getRet().equals(use.get(j))) {
                                    reach.add(c);
                                }
                            } else if (curCode instanceof ParamDef) {
                                final ParamDef paramDef = (ParamDef) curCode;
                                if (paramDef.getParam().equals(use.get(j))) {
                                    reach.add(c);
                                }
                            }
                        }
                        System.out.println("==========");
                        System.out.println(code);
                        System.out.println(reach);
                        if (reach.size() == 1) {
                            MidCode curCode = function.getCode(reach.get(0).getFirst(), reach.get(0).getSecond());
                            Operand value = null;
                            if (curCode instanceof Assign) value = ((Assign) curCode).getSrc();
                            else if (curCode instanceof UnaryOp
                                    && ((UnaryOp) curCode).getOp() == UnaryOp.Op.MOV)
                                value = ((UnaryOp) curCode).getOperand1();
                            System.out.println(reach.get(0));
                            System.out.println(curCode);
                            System.out.println(value);
                            if (use.get(0).isArray() || use.get(0).isGlobal()) continue;
                            if (value == null) continue;
                            if (value instanceof Immediate) {  // 立即数
                                aft.set(j, value);
                            }/* else if (value instanceof Symbol) {  // 变量
                                if (((Symbol) value).isGlobal()) continue;
                                if (((Symbol) value).isArray()) continue;
                                if (((Symbol) value).isTemp()) continue;
                                if (!pathFind(getBlockByCode(function, curCode), curCode, code, (Symbol) value)) { // copy value
                                    aft.set(j, value);
                                }
                            }*/
                        }
                    }
                    MidCode aftCode = null;
                    if (code instanceof Assign && aft.size() > 0) {
                        aftCode = new Assign(((Assign) code).getDst(), aft.get(0));
                    } else if (code instanceof UnaryOp && aft.size() > 0) {
                        aftCode = new UnaryOp(((UnaryOp) code).getOp(), aft.get(0), ((UnaryOp) code).getDst());
                    } else if (code instanceof BinaryOp) {
                        if (((BinaryOp) code).getOperand1() instanceof Symbol
                                && ((BinaryOp) code).getOperand2() instanceof Symbol) {
                            aftCode = new BinaryOp(((BinaryOp) code).getOp(), aft.get(0), aft.get(1), ((BinaryOp) code).getDst());
                        } else if (((BinaryOp) code).getOperand1() instanceof Symbol) {
                            aftCode = new BinaryOp(((BinaryOp) code).getOp(), aft.get(0), ((BinaryOp) code).getOperand2(), ((BinaryOp) code).getDst());
                        } else if (((BinaryOp) code).getOperand2() instanceof Symbol) {
                            aftCode = new BinaryOp(((BinaryOp) code).getOp(), ((BinaryOp) code).getOperand1(), aft.get(0), ((BinaryOp) code).getDst());
                        }
                    } else if (code instanceof LoadSave) {
                        if (((LoadSave) code).getOp() == LoadSave.Op.LOAD && aft.size() > 0) {
                            aftCode = new LoadSave(((LoadSave) code).getOp(), ((LoadSave) code).getBase(), aft.get(0), ((LoadSave) code).getDst());
                        } else if (((LoadSave) code).getOp() == LoadSave.Op.STORE) {
                            if (((LoadSave) code).getSrc() instanceof Symbol && ((LoadSave) code).getOffset() instanceof Symbol) {
                                aftCode = new LoadSave(((LoadSave) code).getOp(), ((LoadSave) code).getBase(), aft.get(0), aft.get(1));
                            } else if (((LoadSave) code).getSrc() instanceof Symbol) {
                                aftCode = new LoadSave(((LoadSave) code).getOp(), ((LoadSave) code).getBase(), ((LoadSave) code).getOffset(), aft.get(0));
                            } else if (((LoadSave) code).getOffset() instanceof Symbol) {
                                aftCode = new LoadSave(((LoadSave) code).getOp(), ((LoadSave) code).getBase(), aft.get(0), ((LoadSave) code).getSrc());
                            }
                        }
                    } else if (code instanceof PushParam) {
                        if (((PushParam) code).getType() == PushParam.Type.PUSH && aft.size() > 0) {
                            aftCode = new PushParam(((PushParam) code).getType(), aft.get(0));
                        } else if (aft.size() > 0) {
                            ArrayPointer param = (ArrayPointer) ((PushParam) code).getParam();
                            aftCode = new PushParam(((PushParam) code).getType(),
                                    new ArrayPointer(param.getBase(), param.getDimension(), aft.get(0)));
                        }
                    } else if (code instanceof Branch) {
                        if (((Branch) code).getCond() instanceof Symbol && aft.size() > 0) {
                            aftCode = new Branch(((Branch) code).getType(), ((Branch) code).getTarget(), aft.get(0));
                        }
                    }
                    if (aftCode != null) {
                        System.out.println("replace: " + code + " with " + aftCode);
                        block.getBlock().replace(code, aftCode);
                    }
                }
            }
        }
    }

    private void deleteUselessCode(Function function) {
        for (BasicBlock block : function.getBasicBlocks()) {
            Set<Symbol> live = new HashSet<>(block.getOut_live());
            for (int i = block.getBlock().getMidCodeList().size() - 1; i >= 0; i--) {
                MidCode code = block.getBlock().getMidCodeList().get(i);
                // System.out.println(code);
                // System.out.println(live);
                Symbol defSymbol = getAllDef(code);
                if (defSymbol != null && live.contains(defSymbol)) {
                    live.remove(defSymbol);
                    live.addAll(getAllUse(code));
                } else {
                    live.addAll(getAllUse(code));
                    if (defSymbol != null
                            && !defSymbol.isGlobal()
                            && !(code instanceof GetInt)
                            && !(code instanceof Call)
                            && !(code instanceof PushParam)
                            && !(code instanceof ParamDef)) {
                        System.out.println("remove: " + code);
                        block.getBlock().replace(i, new Empty());
                    }
                }
            }
        }
    }

    private Set<Pair<Integer, Integer>> getLoopStartEnd(Function function) {
        Set<Pair<Integer, Integer>> loopStartEnd = new HashSet<>();
        for (BasicBlock block : function.getBasicBlocks()) {
            for (int k = 0; k < block.getBlock().getMidCodeList().size(); k++) {
                MidCode code = block.getBlock().getMidCodeList().get(k);
                if (code instanceof Label && ((Label) code).getLabel().startsWith("LOOP_BODY")) {
                    int start = block.getId();
                    int end = -1;
                    for (int i = function.getBasicBlocks().size() - 1; i >= start; i--) {
                        BasicBlock endBlock = function.getBasicBlocks().get(i);
                        MidCode last = endBlock.getBlock().get(endBlock.getBlock().size() - 1);
                        if (last instanceof Branch && ((Branch) last).getTarget().getLabel().equals(((Label) code).getLabel())) {
                            end = endBlock.getId();
                            break;
                        }
                    }
                    if (end != -1) loopStartEnd.add(new Pair<>(start, end));
                    break;
                }
            }
        }
        return loopStartEnd;
    }

    public Set<BasicBlock> calcDom(Function function, int start, int end) {
        HashMap<BasicBlock, Set<BasicBlock>> in_dom = new HashMap<>();
        HashMap<BasicBlock, Set<BasicBlock>> out_dom = new HashMap<>();
        final BasicBlock ENTRY = new BasicBlock(-1);
        ENTRY.addNext(function.getBasicBlocks().get(start-1));
        out_dom.put(ENTRY, new HashSet<>());
        out_dom.get(ENTRY).add(ENTRY);
        for (int i = start; i <= end; i++) {
            BasicBlock block = function.getBasicBlocks().get(i-1);
            in_dom.put(block, new HashSet<>());
            out_dom.put(block, new HashSet<>());
            for (int j = start; j <= end; j++) {
                out_dom.get(block).add(function.getBasicBlocks().get(j-1));
            }
            out_dom.get(block).add(ENTRY);
        }
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = start; i <= end; i++) {
                BasicBlock block = function.getBasicBlocks().get(i-1);
                Set<BasicBlock> tmp = new HashSet<>();
                Set<BasicBlock> new_out_dom = new HashSet<>();
                boolean first = true;
                for (BasicBlock prev : block.getPrev()) {
                    if (first) {
                        if (prev.getId() < start || prev.getId() > end) tmp.addAll(out_dom.get(ENTRY));
                        else tmp.addAll(out_dom.get(prev));
                    } else {
                        if (prev.getId() < start || prev.getId() > end) tmp.retainAll(out_dom.get(ENTRY));
                        else tmp.retainAll(out_dom.get(prev));
                    }
                    first = false;
                }
                if (block.getPrev().isEmpty()) {
                    for (int j = start; j <= end; j++) {
                        tmp.add(function.getBasicBlocks().get(j-1));
                    }
                    tmp.add(ENTRY);
                }
                in_dom.put(block, tmp);
                new_out_dom.add(block);
                new_out_dom.addAll(in_dom.get(block));
                if (!new_out_dom.equals(out_dom.get(block))) {
                    out_dom.put(block, new_out_dom);
                    changed = true;
                }
                // print out_dom
                /*out_dom.forEach((k, v) -> {
                    System.out.print(k.getId() + ": ");
                    v.forEach(b -> System.out.print(b.getId() + " "));
                    System.out.println();
                });*/
            }
        }
        Set<BasicBlock> res = out_dom.get(function.getBasicBlocks().get(end-1));
        res.remove(ENTRY);
        return res;
    }

    private List<Pair<Integer, Integer>> getReachSet(Function function, MidCode code) {
        Set<Pair<Integer, Integer>> reachIn = reachInOfCode(function, code);
        /*for (Pair<Integer, Integer> pair : reachIn) {
            System.out.println(function.getCode(pair.getFirst(), pair.getSecond()));
        }*/
        List<Pair<Integer, Integer>> reach = new ArrayList<>();
        for (Pair<Integer, Integer> c : reachIn) {
            MidCode curCode = function.getCode(c.getFirst(), c.getSecond());
            if (curCode instanceof Assign) {
                final Assign assign = (Assign) curCode;
                if (getAllUse(code).contains(assign.getDst())) {
                    reach.add(c);
                }
            } else if (curCode instanceof UnaryOp) {
                final UnaryOp move = (UnaryOp) curCode;
                if (getAllUse(code).contains(move.getDst())) {
                    reach.add(c);
                }
            } else if (curCode instanceof BinaryOp) {
                final BinaryOp move = (BinaryOp) curCode;
                if (getAllUse(code).contains(move.getDst())) {
                    reach.add(c);
                }
            } else if (curCode instanceof LoadSave) {
                final LoadSave move = (LoadSave) curCode;
                if (((LoadSave) curCode).getOp() == LoadSave.Op.LOAD) {
                    if (getAllUse(code).contains(move.getDst())) {
                        reach.add(c);
                    }
                }
            } else if (curCode instanceof GetInt) {
                final GetInt move = (GetInt) curCode;
                if (getAllUse(code).contains(move.getDst())) {
                    reach.add(c);
                }
            } else if (curCode instanceof Call) {
                final Call move = (Call) curCode;
                if (move.getRet() != null && move.getRet() instanceof Symbol && getAllUse(code).contains((Symbol) move.getRet())) {
                    reach.add(c);
                }
            } else if (curCode instanceof ParamDef) {
                final ParamDef paramDef = (ParamDef) curCode;
                if (getAllUse(code).contains(paramDef.getParam())) {
                    reach.add(c);
                }
            }
        }
        return reach;
    }

    private boolean checkDefSymbol(Symbol symbol, int start, int end, Function function, int defBlockId, int defI,
                                   HashMap<MidCode, List<Pair<Integer, Integer>>> reach) {
        if (symbol == null) return true;
        for (int blockId = start; blockId <= end; blockId++) {
            BasicBlock block = function.getBasicBlocks().get(blockId-1);
            for (int i = 0; i < block.getBlock().getMidCodeList().size(); i++) {
                MidCode code = block.getBlock().getMidCodeList().get(i);
                if (blockId == defBlockId) continue;
                // 如果这个符号在循环中重复定义了，那么不提取
                if (Objects.equals(symbol, getAllDef(code))) {
                    //System.out.println("re def: " + code);
                    return false;
                }
                // 如果这行代码使用了该符号，那么该符号的值只能来源于defI处的定义
                if (getAllUse(code).contains(symbol)) {
                    for (Pair<Integer, Integer> p : reach.get(code)) {
                        MidCode curCode = function.getCode(p.getFirst(), p.getSecond());
                        if (getAllDef(curCode) != null && Objects.equals(getAllDef(curCode), symbol)) {
                            if (p.getFirst() != defBlockId || p.getSecond() != defI) {
                                System.out.println("illegal: " + code);
                                return false;
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    public void loopOptimize(Function function) {
        HashMap<MidCode, List<Pair<Integer, Integer>>> reach = new HashMap<>();
        for (int i = 0; i < function.getBasicBlocks().size(); i++) {
            BasicBlock block = function.getBasicBlocks().get(i);
            for (int j = 0; j < block.getBlock().getMidCodeList().size(); j++) {
                MidCode code = block.getBlock().getMidCodeList().get(j);
                reach.put(code, getReachSet(function, code));
            }
        }
        getLoopStartEnd(function).forEach(pair -> {
            int start = pair.getFirst();
            int end = pair.getSecond();
            List<MidCode> extractOut = new ArrayList<>();
            Set<BasicBlock> DOMSet = calcDom(function, start, end);

            // 逐个检查循环必经点集合中的代码能否外提
            for (BasicBlock block : DOMSet) {
                for (int i = 0; i < block.getBlock().getMidCodeList().size(); i++) {
                    MidCode code = block.getBlock().getMidCodeList().get(i);
                    // 检查右值：右值最近的定义点不能位于循环内部，但是可以是已经被提取出来的部分
                    boolean ok = true;
                    System.out.println("==={code}===: " + code);
                    reach.getOrDefault(code, Collections.emptyList()).forEach((pair1) -> {
                        System.out.println(function.getCode(pair1.getFirst(), pair1.getSecond()));
                    });
                    List<Symbol> rValue = getAllUse(code);
                    for (Symbol rVal : rValue) {
                        if (rVal.isConst()) continue;
                        for (Pair<Integer, Integer> p : reach.getOrDefault(code, Collections.emptyList())) {
                            if (function.getCode(p.getFirst(), p.getSecond()) instanceof Empty) continue;
                            if ((p.getFirst() <= end && p.getFirst() >= start) &&
                                    !extractOut.contains(function.getCode(p.getFirst(), p.getSecond())) &&
                                    Objects.equals(getAllDef(function.getCode(p.getFirst(), p.getSecond())), rVal)) {
                                ok = false;
                                break;
                            }
                        }
                    }
                    if (!ok) continue;
                    // 检查左值：左值如果在循环中被使用，则必须由这句代码定义
                    System.out.println("check def: " + code);
                    if (code instanceof Assign || code instanceof BinaryOp || code instanceof UnaryOp ||
                            (code instanceof LoadSave && ((LoadSave) code).getOp() == LoadSave.Op.STORE) ||
                            code instanceof DeclareVar) {
                        Symbol defSymbol = getAllDef(code);
                        if (checkDefSymbol(defSymbol, start, end, function, block.getId(), i, reach)) {
                            System.out.println("extract: " + code);
                            block.getBlock().replace(i, new Empty());
                            extractOut.add(code);
                        }
                    }
                }
            }

            // 逐个将extractOut中的代码插入到循环前面
            for (MidCode code : extractOut) {
                function.getBasicBlocks().get(start-2).getBlock()
                        .insertBefore(function.getBasicBlocks().get(start-2).getBlock().size()-1, code);
            }
        });

    }

    public void optimize(boolean isOptimize, boolean isLoopOptimize) {
        midCodeProgram.getFunctionTable().values().forEach(function -> {
            System.out.println(function.getName());
            liveVariableAnalyse(function);
            reachDefAnalyse(function);
            if (isOptimize) {
                propagateValue(function);
                deleteUselessCode(function);
            }
            if (isLoopOptimize) {
                loopOptimize(function);
            }
        });
    }
}
