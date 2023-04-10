package middle.optimize;

import middle.middle_code.MidCodeProgram;

public class DeleteCommonSubExpr {
    private final MidCodeProgram midCodeProgram;

    public DeleteCommonSubExpr(MidCodeProgram midCodeProgram) {
        this.midCodeProgram = midCodeProgram;
    }

    private BasicBlock optimize(BasicBlock basicBlock) {
        BasicBlock result = new BasicBlock(basicBlock.getId());
        basicBlock.getPrev().forEach(result::addPrev);
        basicBlock.getNext().forEach(result::addNext);

        return result;
    }

    public void optimize() {
        midCodeProgram.getFunctionTable().values().forEach(function -> {
            for (int i = 0; i < function.getBasicBlocks().size(); i++) {
                function.getBasicBlocks().set(i, optimize(function.getBasicBlocks().get(i)));
            }
        });
    }
}
