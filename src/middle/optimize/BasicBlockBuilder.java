package middle.optimize;

import middle.middle_code.MidCodeProgram;
import middle.middle_code.element.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BasicBlockBuilder {
    private final MidCodeProgram midCodeProgram;

    public BasicBlockBuilder(MidCodeProgram midCodeProgram) {
        this.midCodeProgram = midCodeProgram;
    }

    public void build() {
        midCodeProgram.getFunctionTable().values().forEach(function -> {
            boolean[] tag = new boolean[function.getBody().getMidCodeList().size()];
            int[] blockId = new int[function.getBody().getMidCodeList().size()];
            List<MidCode> body = function.getBody().getMidCodeList();
            tag[0] = true;
            for (int i = 0; i < tag.length; i++) {
                MidCode code = body.get(i);
                if (code instanceof Jump) {
                    Jump jump = (Jump) code;
                    tag[body.indexOf(jump.getTarget())] = true;
                    if (i + 1 < tag.length) tag[i+1] = true;
                } else if (code instanceof Branch) {
                    Branch branch = (Branch) code;
                    tag[body.indexOf(branch.getTarget())] = true;
                    if (i + 1 < tag.length) tag[i + 1] = true;
                } else if (code instanceof  Branch2Var) {
                    Branch2Var branch = (Branch2Var) code;
                    tag[body.indexOf(branch.getTarget())] = true;
                    if (i + 1 < tag.length) tag[i + 1] = true;
                } else if (code instanceof Return) {
                    if (i + 1 < tag.length) tag[i + 1] = true;
                }
            }

            int id = 1;
            for (int i = 0; i < tag.length; i++) {
                if (tag[i]) {
                    blockId[i] = id++;
                }
            }

            List<BasicBlock> basicBlocks = new ArrayList<>();
            for (int i = 1; i < id; i++) basicBlocks.add(new BasicBlock(i));
            id = 0;
            for (int i = 0; i < tag.length; i++) {
                MidCode code = body.get(i);
                if (tag[i]) id++;
                if (code instanceof Jump) {
                    Jump jump = (Jump) code;
                    basicBlocks.get(id-1).addMidCode(code);
                    basicBlocks.get(id-1).addOriginalIndex(i+1);
                    basicBlocks.get(id-1).addNext(basicBlocks.get(blockId[body.indexOf(jump.getTarget())]-1));
                    basicBlocks.get(blockId[body.indexOf(jump.getTarget())]-1).addPrev(basicBlocks.get(id-1));
                } else if (code instanceof Branch) {
                    Branch branch = (Branch) code;
                    basicBlocks.get(id-1).addMidCode(code);
                    basicBlocks.get(id-1).addOriginalIndex(i+1);
                    basicBlocks.get(id-1).addNext(basicBlocks.get(blockId[body.indexOf(branch.getTarget())]-1));
                    basicBlocks.get(id-1).addNext(basicBlocks.get(blockId[i+1]-1));
                    basicBlocks.get(blockId[body.indexOf(branch.getTarget())]-1).addPrev(basicBlocks.get(id-1));
                    basicBlocks.get(blockId[i+1]-1).addPrev(basicBlocks.get(id-1));
                } else if (code instanceof Return) {
                    basicBlocks.get(id-1).addMidCode(code);
                    basicBlocks.get(id-1).addOriginalIndex(i+1);
                } else if (code instanceof Branch2Var) {
                    Branch2Var branch = (Branch2Var) code;
                    basicBlocks.get(id-1).addMidCode(code);
                    basicBlocks.get(id-1).addOriginalIndex(i+1);
                    basicBlocks.get(id-1).addNext(basicBlocks.get(blockId[body.indexOf(branch.getTarget())]-1));
                    basicBlocks.get(id-1).addNext(basicBlocks.get(blockId[i+1]-1));
                    basicBlocks.get(blockId[body.indexOf(branch.getTarget())]-1).addPrev(basicBlocks.get(id-1));
                    basicBlocks.get(blockId[i+1]-1).addPrev(basicBlocks.get(id-1));
                } else {
                    //System.out.println(code);
                    basicBlocks.get(id-1).addMidCode(code);
                    basicBlocks.get(id-1).addOriginalIndex(i+1);
                    if (i+1 < tag.length && tag[i+1]) {
                        if (id != 0) {
                            basicBlocks.get(id-1).addNext(basicBlocks.get(blockId[i+1]-1));
                            basicBlocks.get(blockId[i+1]-1).addPrev(basicBlocks.get(id-1));
                        }
                    }
                }
            }
            // basicBlocks.removeIf(b -> b.getBlock().getMidCodeList().isEmpty());
            function.setBasicBlocks(basicBlocks);
        });
    }
}
