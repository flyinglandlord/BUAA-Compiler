package middle.optimize;

import middle.middle_code.MidCodeProgram;
import middle.middle_code.element.*;
import middle.middle_code.operand.Immediate;
import middle.middle_code.operand.Operand;
import middle.symbol.Function;
import middle.symbol.Symbol;

public class BranchToBranch2Var {
    private final MidCodeProgram program;

    public BranchToBranch2Var(MidCodeProgram program) {
        this.program = program;
    }

    private void optimize(Function function) {
        for (BasicBlock block : function.getBasicBlocks()) {
            for (int i = 0; i < block.getBlock().size() - 1; i++) {
                MidCode first = block.getBlock().get(i);
                MidCode second = block.getBlock().get(i+1);
                if (first instanceof BinaryOp &&
                        (((BinaryOp) first).getOp() == BinaryOp.Op.EQ || ((BinaryOp) first).getOp() == BinaryOp.Op.NE) &&
                        ((BinaryOp) first).getOperand1() instanceof Symbol &&
                        ((BinaryOp) first).getOperand2() instanceof Symbol &&
                        second instanceof Branch) {
                    BinaryOp binaryOp = (BinaryOp) first;
                    Branch branch = (Branch) second;
                    if (binaryOp.getDst().equals(branch.getCond()) && binaryOp.getDst().isTemp() &&
                            binaryOp.getOperand1() instanceof Symbol && binaryOp.getOperand2() instanceof Symbol) {
                        if (binaryOp.getOp() == BinaryOp.Op.NE && branch.getType() == Branch.Type.EQ ||
                                binaryOp.getOp() == BinaryOp.Op.EQ && branch.getType() == Branch.Type.NE) {
                            block.getBlock().replace(i,
                                    new Branch2Var(
                                            Branch.Type.EQ,
                                            branch.getTarget(), binaryOp.getOperand1(), binaryOp.getOperand2()));
                        } else {
                            block.getBlock().replace(i,
                                    new Branch2Var(
                                            Branch.Type.NE,
                                            branch.getTarget(), binaryOp.getOperand1(), binaryOp.getOperand2()));
                        }
                        block.getBlock().replace(i + 1, new Empty());
                    }
                } else if (first instanceof BinaryOp &&
                        ((BinaryOp) first).getOperand2() instanceof Immediate &&
                        ((Immediate) ((BinaryOp) first).getOperand2()).getValue() == 0 &&
                        second instanceof Branch &&
                        ((BinaryOp) first).getDst().equals(((Branch) second).getCond()) &&
                        ((BinaryOp) first).getDst().isTemp()) {
                    BinaryOp binaryOp = (BinaryOp) first;
                    Branch branch = (Branch) second;
                    if (binaryOp.getDst().equals(branch.getCond()) && binaryOp.getDst().isTemp()) {
                        if (branch.getType() == Branch.Type.EQ) {
                            doReplace_reverse(i, binaryOp, branch, block, binaryOp.getOperand1());
                        } else if (branch.getType() == Branch.Type.NE) {
                            doReplace(i, binaryOp, branch, block, binaryOp.getOperand1());
                        }
                    }
                }
            }
        }
    }

    private void doReplace(int i, BinaryOp binaryOp, Branch branch, BasicBlock block, Operand var) {
        System.out.println("BranchToBranch2Var: " + binaryOp + " -> " + branch);
        switch (binaryOp.getOp()) {
            case EQ:
                block.getBlock().replace(i, new Branch(
                        Branch.Type.EQ, branch.getTarget(),
                        var));
                block.getBlock().replace(i + 1, new Empty());
                break;
            case NE:
                block.getBlock().replace(i, new Branch(
                        Branch.Type.NE, branch.getTarget(),
                        var));
                block.getBlock().replace(i + 1, new Empty());
                break;
            case LT:
                block.getBlock().replace(i, new Branch(
                        Branch.Type.LT, branch.getTarget(),
                        var));
                block.getBlock().replace(i + 1, new Empty());
                break;
            case LE:
                block.getBlock().replace(i, new Branch(
                        Branch.Type.LE, branch.getTarget(),
                        var));
                block.getBlock().replace(i + 1, new Empty());
                break;
            case GT:
                block.getBlock().replace(i, new Branch(
                        Branch.Type.GT, branch.getTarget(),
                        var));
                block.getBlock().replace(i + 1, new Empty());
                break;
            case GE:
                block.getBlock().replace(i, new Branch(
                        Branch.Type.GE, branch.getTarget(),
                        var));
                block.getBlock().replace(i + 1, new Empty());
                break;
        }
    }

    private void doReplace_reverse(int i, BinaryOp binaryOp, Branch branch, BasicBlock block, Operand var) {
        System.out.println("BranchToBranch2Var Reverse: " + binaryOp + " -> " + branch);
        switch (binaryOp.getOp()) {
            case EQ:
                block.getBlock().replace(i, new Branch(
                        Branch.Type.NE, branch.getTarget(),
                        var));
                block.getBlock().replace(i + 1, new Empty());
                break;
            case NE:
                block.getBlock().replace(i, new Branch(
                        Branch.Type.EQ, branch.getTarget(),
                        var));
                block.getBlock().replace(i + 1, new Empty());
                break;
            case LT:
                block.getBlock().replace(i, new Branch(
                        Branch.Type.GE, branch.getTarget(),
                        var));
                block.getBlock().replace(i + 1, new Empty());
                break;
            case LE:
                block.getBlock().replace(i, new Branch(
                        Branch.Type.GT, branch.getTarget(),
                        var));
                block.getBlock().replace(i + 1, new Empty());
                break;
            case GT:
                block.getBlock().replace(i, new Branch(
                        Branch.Type.LE, branch.getTarget(),
                        var));
                block.getBlock().replace(i + 1, new Empty());
                break;
            case GE:
                block.getBlock().replace(i, new Branch(
                        Branch.Type.LT, branch.getTarget(),
                        var));
                block.getBlock().replace(i + 1, new Empty());
                break;
        }
    }

    public void optimize() {
        program.getFunctionTable().values().forEach(this::optimize);
    }
}
