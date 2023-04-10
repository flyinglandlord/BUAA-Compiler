package middle.optimize;

import middle.middle_code.MidCodeList;
import middle.middle_code.element.BlockIdent;
import middle.middle_code.element.Label;
import middle.middle_code.element.MidCode;
import middle.symbol.Symbol;
import util.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BasicBlock {
    private final int id;
    private final MidCodeList block = new MidCodeList();
    private final List<Integer> originalIndex = new ArrayList<>();
    private final Set<BasicBlock> prev = new HashSet<>();
    private final Set<BasicBlock> next = new HashSet<>();
    private Set<Pair<Integer, Integer>> kill = null;
    private Set<Pair<Integer, Integer>> gen = null;
    private Set<Pair<Integer, Integer>> in_reaching = null;
    private Set<Pair<Integer, Integer>> out_reaching = null;

    private Set<Symbol> def;
    private Set<Symbol> use;
    private Set<Symbol> in_live = new HashSet<>();
    private Set<Symbol> out_live = new HashSet<>();

    public BasicBlock(int id) {
        this.id = id;
    }

    public void setKill(Set<Pair<Integer, Integer>> kill) {
        this.kill = kill;
    }

    public void setGen(Set<Pair<Integer, Integer>> gen) {
        this.gen = gen;
    }

    public void setDef(Set<Symbol> def) {
        this.def = def;
    }

    public void setUse(Set<Symbol> use) {
        this.use = use;
    }

    public void setIn_reaching(Set<Pair<Integer, Integer>> in_reaching) {
        this.in_reaching = in_reaching;
    }

    public void setOut_reaching(Set<Pair<Integer, Integer>> out_reaching) {
        this.out_reaching = out_reaching;
    }

    public void setIn_live(Set<Symbol> in_live) {
        this.in_live = in_live;
    }

    public void setOut_live(Set<Symbol> out_live) {
        this.out_live = out_live;
    }

    public Set<Pair<Integer, Integer>> getKill() {
        return kill;
    }

    public Set<Pair<Integer, Integer>> getGen() {
        return gen;
    }

    public Set<Pair<Integer, Integer>> getIn_reaching() {
        return in_reaching;
    }

    public Set<Pair<Integer, Integer>> getOut_reaching() {
        return out_reaching;
    }

    public Set<Symbol> getDef() {
        return def;
    }

    public Set<Symbol> getUse() {
        return use;
    }

    public Set<Symbol> getIn_live() {
        return in_live;
    }

    public Set<Symbol> getOut_live() {
        return out_live;
    }

    public void addMidCode(MidCode midCode) {
        block.addMidCode(midCode);
    }

    public MidCodeList getBlock() {
        return block;
    }

    public void addPrev(BasicBlock prev) {
        this.prev.add(prev);
    }

    public void addNext(BasicBlock next) {
        this.next.add(next);
    }

    public Set<BasicBlock> getPrev() {
        return prev;
    }

    public Set<BasicBlock> getNext() {
        return next;
    }

    public int getId() {
        return id;
    }

    public void addOriginalIndex(int index) {
        originalIndex.add(index);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BasicBlock ").append(id).append("\n");
        builder.append("==============================\n");
        builder.append("prev: [");
        prev.forEach(basicBlock -> builder.append(basicBlock.getId()).append(" "));
        builder.append("]\nnext: [");
        next.forEach(basicBlock -> builder.append(basicBlock.getId()).append(" "));
        builder.append("]\ngen: [");
        gen.forEach(pair -> builder.append(pair).append(" "));
        builder.append("\nkill: [");
        kill.forEach(pair -> builder.append(pair).append(" "));
        builder.append("]\nin_reaching: ");
        builder.append(in_reaching);
        builder.append("\nout_reaching: ");
        builder.append(out_reaching);
        builder.append("\ndef: ");
        builder.append(def);
        builder.append("\nuse: ");
        builder.append(use);
        builder.append("\nin_live: ");
        builder.append(in_live);
        builder.append("\nout_live: ");
        builder.append(out_live);
        builder.append("\n==============================\n");
        for (int i = 0; i < block.size(); i++) {
            builder.append(originalIndex.get(i)).append(": ").append(block.get(i)).append("\n");
        }
        builder.append("==============================\n");
        return builder.toString();
    }
}
