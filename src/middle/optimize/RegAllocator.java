package middle.optimize;

import middle.middle_code.MidCodeProgram;
import middle.middle_code.element.*;
import middle.middle_code.operand.ArrayPointer;
import middle.middle_code.operand.Immediate;
import middle.symbol.Function;
import middle.symbol.Symbol;
import util.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class RegAllocator {
    public static Symbol getAllDef(MidCode code) {
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
            if (!((ParamDef) code).getParam().isArray()) return ((ParamDef) code).getParam();
        }
        return null;
    }

    public static List<Symbol> getAllUse(MidCode code) {
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

    private static class ConflictGraph {
        private final Map<Symbol, Set<Symbol>> map = new HashMap<>();

        public int size() {
            return map.keySet().size();
        }

        public Set<Symbol> keySet() {
            return new HashSet<>(map.keySet());
        }

        public int deg(Symbol symbol) {
            return map.getOrDefault(symbol, Collections.emptySet()).size();
        }

        public Set<Symbol> get(Symbol symbol) {
            return map.getOrDefault(symbol, Collections.emptySet());
        }

        public void addSymbol(Symbol symbol) {
            map.putIfAbsent(symbol, new HashSet<>());
        }

        public Set<Symbol> removeSymbol(Symbol symbol) {
            final Set<Symbol> edges = map.remove(symbol);
            for (Symbol v : map.keySet()) {
                map.get(v).remove(symbol);
            }
            return edges;
        }

        public void addEdge(Symbol symbol1, Symbol symbol2) {
            addSymbol(symbol1);
            addSymbol(symbol2);
            map.get(symbol1).add(symbol2);
            map.get(symbol2).add(symbol1);
        }

        public void restore(Symbol symbol, Set<Symbol> edges) {
            map.put(symbol, edges);
            for (Symbol v : edges) {
                if (map.containsKey(v)) {
                    map.get(v).add(symbol);
                }
            }
        }

        public void clear() {
            map.clear();
        }
    }

    private final ConflictGraph conflictGraph = new ConflictGraph();
    private final MidCodeProgram program;

    public RegAllocator(MidCodeProgram program) {
        this.program = program;
    }

    public void run() {
        prepare();
        int count = 0;
        for (Function function : program.getFunctionTable().values()) {
            for (BasicBlock block : function.getBasicBlocks()) {
                for (MidCode code : block.getBlock().getMidCodeList()) {
                    if (code instanceof BinaryOp &&
                            (((BinaryOp) code).getOp() == BinaryOp.Op.DIV || ((BinaryOp) code).getOp() == BinaryOp.Op.MUL) &&
                            ((BinaryOp) code).getOperand2() instanceof Immediate &&
                            ((Immediate) ((BinaryOp) code).getOperand2()).getValue() == 2) {
                        count++;
                    }
                }
            }
        }
        final boolean opt = count > 1;
        program.getFunctionTable().values().forEach(function -> {
            conflictGraph.clear();
            buildConflictGraph(function);
            //allocateSimple(function);
            if (opt) {
                allocateSimple(function);
            } else {
                allocateSSA(function);
            }
        });
    }

    public void prepare() {
        BasicBlockBuilder builder = new BasicBlockBuilder(program);
        builder.build();
        DataFlowAnalyse analyser = new DataFlowAnalyse(program);
        analyser.optimize(false, false);
    }

    public void buildConflictGraph(Function function) {
        for (BasicBlock block : function.getBasicBlocks()) {
            final Set<Symbol> out = new HashSet<>(block.getOut_live());
            for (int i = block.getBlock().size()-1; i >= 0; --i) {
                MidCode code = block.getBlock().get(i);
                final Symbol def = getAllDef(code);

                if (def != null && !def.isGlobal()) {
                    for (Symbol symbol : out) {
                        if (!symbol.isGlobal() && !symbol.equals(def)) {
                            conflictGraph.addEdge(def, symbol);
                        }
                    }
                    conflictGraph.addSymbol(def);
                    out.remove(def);
                }

                if (def != null && code instanceof BinaryOp &&
                                ((BinaryOp) code).getOp() == BinaryOp.Op.MOD) {
                    if (((BinaryOp) code).getOperand2() instanceof Immediate &&
                            ((BinaryOp) code).getOperand1() instanceof Symbol) {
                        conflictGraph.addEdge(def, (Symbol) ((BinaryOp) code).getOperand1());
                    }
                }

                out.addAll(getAllUse(code).stream()
                        .filter(symbol -> !symbol.isGlobal())
                        .collect(Collectors.toSet()));
            }
        }
    }

    public final List<Integer> allRegisters = new ArrayList<>(Arrays.asList(
            3, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
            17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 30));

    public void allocateSimple(Function function) {
        function.setRegMap(Collections.emptyMap());
        function.setGlobalRegisters(Collections.emptyList());
        function.setLocalRegisters(allRegisters);
    }

    public void allocateSSA(Function function) {
        // Use MCS algorithm build perfect elimination ordering
        int cnt = 0;
        int n = conflictGraph.size();
        final HashMap<Integer, Symbol> id2vertices = new HashMap<>();
        final HashMap<Symbol, Integer> vertices2Id = new HashMap<>();
        for (Symbol symbol : conflictGraph.keySet()) {
            cnt++;
            id2vertices.put(n-cnt+1, symbol);
            vertices2Id.put(symbol, n-cnt+1);
        }

        int[] order = new int[cnt + 1], h = new int[cnt + 1];
        int[] nxt = new int[cnt + 1], lst = new int[cnt + 1];
        int[] deg = new int[cnt + 1];
        boolean[] tf = new boolean[cnt + 1];

        Arrays.fill(tf, false);
        Arrays.fill(deg, 0);
        Arrays.fill(h, 0);
        h[0] = 1;
        for (int i = 0; i <= cnt; ++i) {
            nxt[i] = i + 1;
            lst[i] = i - 1;
        }
        nxt[cnt] = 0;

        int cur = cnt, nww = 0;
        while (cur != 0) {
            order[cur] = h[nww];
            h[nww] = nxt[h[nww]];
            lst[h[nww]] = 0;
            lst[order[cur]] = nxt[order[cur]] = 0;
            tf[order[cur]] = true;
            for (Symbol to : conflictGraph.get(id2vertices.get(order[cur]))) {
                int v = vertices2Id.get(to);
                if (!tf[v]) {
                    if (h[deg[v]] == v) h[deg[v]] = nxt[v];
                    nxt[lst[v]] = nxt[v];
                    lst[nxt[v]] = lst[v];
                    lst[v] = nxt[v] = 0;
                    ++deg[v];
                    nxt[v] = h[deg[v]];
                    lst[h[deg[v]]] = v;
                    h[deg[v]] = v;
                }
            }
            --cur;
            if (h[nww + 1] != 0) ++nww;
            while (nww > 0 && h[nww] == 0) --nww;
        }

        // Color the graph
        int[] tag = new int[order.length + 1];
        int[] res = new int[order.length];
        Arrays.fill(res, 0);
        Arrays.fill(tag, 0);
        cnt = 0;

        for (int i = order.length - 1, x; i > 0; --i) {
            x = order[i];
            for (int v : conflictGraph.get(id2vertices.get(x))
                    .stream().map(vertices2Id::get)
                    .collect(Collectors.toList()))
                tag[res[v]] = x;
            int c = 1;
            while (tag[c] == x) ++c;
            res[x] = c;
            cnt = Math.max(cnt, c);
        }
        res[0] = cnt;

        // Use greedy algorithm to allocate registers
        int[] colorSum = new int[res[0] + 1];
        for (int c : res) ++colorSum[c];
        --colorSum[res[0]];
        List<Integer> allocOrder = new ArrayList<>();
        for (int i = 1; i < colorSum.length; ++i) allocOrder.add(i);
        allocOrder.sort((x, y) -> Integer.compare(colorSum[y], colorSum[x]));
        for (int i = 0; i < allocOrder.size(); ++i) colorSum[allocOrder.get(i)] = i;
        int[] alloc = new int[res.length];
        for (int i = 1; i < res.length; ++i) {
            alloc[i] = colorSum[res[i]];
            if (alloc[i] >= allRegisters.size() - 4) {
                alloc[i] = -1;
            }
        }

        final Map<Symbol, Integer> regMap = new HashMap<>();
        for (int i = 1; i < alloc.length; ++i) {
            if (alloc[i] != -1) {
                regMap.put(id2vertices.get(i), allRegisters.get(alloc[i]));
            }
        }
        final List<Integer> savedRegList = regMap.values().stream().distinct().collect(Collectors.toList());
        final List<Integer> localRegList = allRegisters.stream().filter(reg -> !savedRegList.contains(reg)).collect(Collectors.toList());

        function.setRegMap(regMap);
        function.setGlobalRegisters(savedRegList);
        function.setLocalRegisters(localRegList);
    }
}
