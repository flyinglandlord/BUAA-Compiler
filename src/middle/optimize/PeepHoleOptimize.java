package middle.optimize;

import middle.middle_code.MidCodeProgram;
import middle.middle_code.element.*;
import middle.middle_code.operand.Immediate;
import middle.symbol.Function;

public class PeepHoleOptimize {
    private final MidCodeProgram program;

    public PeepHoleOptimize(MidCodeProgram program) {
        this.program = program;
    }

    void optimizeBinaryOp(BasicBlock block, int index, BinaryOp code) {
        switch (code.getOp()) {
            case ADD:
                if (code.getOperand1() instanceof Immediate && code.getOperand2() instanceof Immediate) {
                    int ans = ((Immediate) code.getOperand1()).getValue() + ((Immediate) code.getOperand2()).getValue();
                    block.getBlock().replace(index, new UnaryOp(UnaryOp.Op.MOV, new Immediate(ans), code.getDst()));
                } else if (code.getOperand1() instanceof Immediate && ((Immediate) code.getOperand1()).getValue() == 0) {
                    block.getBlock().replace(index, new UnaryOp(UnaryOp.Op.MOV, code.getOperand2(), code.getDst()));
                } else if (code.getOperand2() instanceof Immediate && ((Immediate) code.getOperand2()).getValue() == 0) {
                    block.getBlock().replace(index, new UnaryOp(UnaryOp.Op.MOV, code.getOperand1(), code.getDst()));
                }
                break;
            case MUL:
                if (code.getOperand1() instanceof Immediate && code.getOperand2() instanceof Immediate) {
                    int ans = ((Immediate) code.getOperand1()).getValue() * ((Immediate) code.getOperand2()).getValue();
                    block.getBlock().replace(index, new UnaryOp(UnaryOp.Op.MOV, new Immediate(ans), code.getDst()));
                } else if (code.getOperand1() instanceof Immediate && ((Immediate) code.getOperand1()).getValue() == 0) {
                    block.getBlock().replace(index, new UnaryOp(UnaryOp.Op.MOV, new Immediate(0), code.getDst()));
                } else if (code.getOperand2() instanceof Immediate && ((Immediate) code.getOperand2()).getValue() == 0) {
                    block.getBlock().replace(index, new UnaryOp(UnaryOp.Op.MOV, new Immediate(0), code.getDst()));
                } else if (code.getOperand1() instanceof Immediate && ((Immediate) code.getOperand1()).getValue() == 1) {
                    block.getBlock().replace(index, new UnaryOp(UnaryOp.Op.MOV, code.getOperand2(), code.getDst()));
                } else if (code.getOperand2() instanceof Immediate && ((Immediate) code.getOperand2()).getValue() == 1) {
                    block.getBlock().replace(index, new UnaryOp(UnaryOp.Op.MOV, code.getOperand1(), code.getDst()));
                }
                break;
            case SUB:
                if (code.getOperand1() instanceof Immediate && code.getOperand2() instanceof Immediate) {
                    int ans = ((Immediate) code.getOperand1()).getValue() - ((Immediate) code.getOperand2()).getValue();
                    block.getBlock().replace(index, new UnaryOp(UnaryOp.Op.MOV, new Immediate(ans), code.getDst()));
                } else if (code.getOperand2() instanceof Immediate && ((Immediate) code.getOperand2()).getValue() == 0) {
                    block.getBlock().replace(index, new UnaryOp(UnaryOp.Op.MOV, code.getOperand1(), code.getDst()));
                }
                break;
            case DIV:
                if (code.getOperand1() instanceof Immediate && code.getOperand2() instanceof Immediate) {
                    int ans = ((Immediate) code.getOperand1()).getValue() / ((Immediate) code.getOperand2()).getValue();
                    block.getBlock().replace(index, new UnaryOp(UnaryOp.Op.MOV, new Immediate(ans), code.getDst()));
                } else if (code.getOperand2() instanceof Immediate && ((Immediate) code.getOperand2()).getValue() == 1) {
                    block.getBlock().replace(index, new UnaryOp(UnaryOp.Op.MOV, code.getOperand1(), code.getDst()));
                }
                break;
            case MOD:
                if (code.getOperand1() instanceof Immediate && code.getOperand2() instanceof Immediate) {
                    int ans = ((Immediate) code.getOperand1()).getValue() % ((Immediate) code.getOperand2()).getValue();
                    block.getBlock().replace(index, new UnaryOp(UnaryOp.Op.MOV, new Immediate(ans), code.getDst()));
                } else if (code.getOperand2() instanceof Immediate && ((Immediate) code.getOperand2()).getValue() == 1) {
                    block.getBlock().replace(index, new UnaryOp(UnaryOp.Op.MOV, new Immediate(0), code.getDst()));
                }
                break;
            case SLL:
                if (code.getOperand1() instanceof Immediate && code.getOperand2() instanceof Immediate) {
                    int ans = ((Immediate) code.getOperand1()).getValue() << ((Immediate) code.getOperand2()).getValue();
                    block.getBlock().replace(index, new UnaryOp(UnaryOp.Op.MOV, new Immediate(ans), code.getDst()));
                } else if (code.getOperand2() instanceof Immediate && ((Immediate) code.getOperand2()).getValue() == 0) {
                    block.getBlock().replace(index, new UnaryOp(UnaryOp.Op.MOV, code.getOperand1(), code.getDst()));
                }
                break;
            case SRL:
                if (code.getOperand1() instanceof Immediate && code.getOperand2() instanceof Immediate) {
                    int ans = ((Immediate) code.getOperand1()).getValue() >> ((Immediate) code.getOperand2()).getValue();
                    block.getBlock().replace(index, new UnaryOp(UnaryOp.Op.MOV, new Immediate(ans), code.getDst()));
                } else if (code.getOperand2() instanceof Immediate && ((Immediate) code.getOperand2()).getValue() == 0) {
                    block.getBlock().replace(index, new UnaryOp(UnaryOp.Op.MOV, code.getOperand1(), code.getDst()));
                }
                break;
            case SRA:
                if (code.getOperand1() instanceof Immediate && code.getOperand2() instanceof Immediate) {
                    int ans = ((Immediate) code.getOperand1()).getValue() >>> ((Immediate) code.getOperand2()).getValue();
                    block.getBlock().replace(index, new UnaryOp(UnaryOp.Op.MOV, new Immediate(ans), code.getDst()));
                } else if (code.getOperand2() instanceof Immediate && ((Immediate) code.getOperand2()).getValue() == 0) {
                    block.getBlock().replace(index, new UnaryOp(UnaryOp.Op.MOV, code.getOperand1(), code.getDst()));
                }
                break;
            case GE:
            case GT:
            case LE:
            case LT:
            case EQ:
            case NE:
                if (code.getOperand1() instanceof Immediate && code.getOperand2() instanceof Immediate) {
                    boolean ans = false;
                    switch (code.getOp()) {
                        case GE:
                            ans = ((Immediate) code.getOperand1()).getValue() >= ((Immediate) code.getOperand2()).getValue();
                            break;
                        case GT:
                            ans = ((Immediate) code.getOperand1()).getValue() > ((Immediate) code.getOperand2()).getValue();
                            break;
                        case LE:
                            ans = ((Immediate) code.getOperand1()).getValue() <= ((Immediate) code.getOperand2()).getValue();
                            break;
                        case LT:
                            ans = ((Immediate) code.getOperand1()).getValue() < ((Immediate) code.getOperand2()).getValue();
                            break;
                        case EQ:
                            ans = ((Immediate) code.getOperand1()).getValue() == ((Immediate) code.getOperand2()).getValue();
                            break;
                        case NE:
                            ans = ((Immediate) code.getOperand1()).getValue() != ((Immediate) code.getOperand2()).getValue();
                            break;
                    }
                    block.getBlock().replace(index, new UnaryOp(UnaryOp.Op.MOV, new Immediate(ans ? 1 : 0), code.getDst()));
                }
                break;
        }
    }

    private void optimizeUnaryOp(BasicBlock block, int index, UnaryOp code) {
        switch (code.getOp()) {
            case NEG:
                if (code.getOperand1() instanceof Immediate) {
                    int ans = -((Immediate) code.getOperand1()).getValue();
                    block.getBlock().replace(index, new UnaryOp(UnaryOp.Op.MOV, new Immediate(ans), code.getDst()));
                }
                break;
            case NOT:
                if (code.getOperand1() instanceof Immediate) {
                    int ans = ((Immediate) code.getOperand1()).getValue() == 0 ? 1 : 0;
                    block.getBlock().replace(index, new UnaryOp(UnaryOp.Op.MOV, new Immediate(ans), code.getDst()));
                }
                break;
        }
    }

    private void optimizeBranch(BasicBlock block, int index, Branch code) {
        if (code.getCond() instanceof Immediate) {
            boolean ans = false;
            switch (code.getType()) {
                case EQ:
                    ans = ((Immediate) code.getCond()).getValue() == 0;
                    break;
                case NE:
                    ans = ((Immediate) code.getCond()).getValue() != 0;
                    break;
                case GE:
                    ans = ((Immediate) code.getCond()).getValue() >= 0;
                    break;
                case GT:
                    ans = ((Immediate) code.getCond()).getValue() > 0;
                    break;
                case LE:
                    ans = ((Immediate) code.getCond()).getValue() <= 0;
                    break;
                case LT:
                    ans = ((Immediate) code.getCond()).getValue() < 0;
                    break;
            }
            if (ans) {
                block.getBlock().replace(index, new Jump(code.getTarget()));
            } else {
                block.getBlock().replace(index, new Empty());
            }
        }
    }

    private void optimize(Function function) {
        for (BasicBlock block : function.getBasicBlocks()) {
            if (block.getPrev().size() == 0 && function.getBasicBlocks().get(0) != block) {
                for (int i = 0; i < block.getBlock().size(); i++) {
                    if (!(block.getBlock().get(i) instanceof Jump)
                            && !(block.getBlock().get(i) instanceof Branch)) {
                        block.getBlock().replace(i, new Empty());
                    }
                }
            } else {
                for (int i = 0; i < block.getBlock().size(); i++) {
                    MidCode midCode = block.getBlock().get(i);
                    if (midCode instanceof BinaryOp) {
                        optimizeBinaryOp(block, i, (BinaryOp) midCode);
                    } else if (midCode instanceof UnaryOp) {
                        optimizeUnaryOp(block, i, (UnaryOp) midCode);
                    } else if (midCode instanceof Branch) {
                        optimizeBranch(block, i, (Branch) midCode);
                    }
                }
                for (int i = 0; i < block.getBlock().size()-1; i++) {
                    MidCode first = block.getBlock().get(i);
                    MidCode second = block.getBlock().get(i+1);
                    if (second instanceof Assign) {
                        if (first instanceof BinaryOp && ((BinaryOp) first).getOp() != BinaryOp.Op.MOD) {
                            BinaryOp binaryOp = (BinaryOp) first;
                            Assign assign = (Assign) second;
                            if (binaryOp.getDst().equals(assign.getSrc()) && binaryOp.getDst().isTemp()) {
                                block.getBlock().replace(i,
                                        new BinaryOp(binaryOp.getOp(),
                                                binaryOp.getOperand1(), binaryOp.getOperand2(), assign.getDst()));
                                block.getBlock().replace(i+1, new Empty());
                            }
                        } else if (first instanceof UnaryOp) {
                            UnaryOp unaryOp = (UnaryOp) first;
                            Assign assign = (Assign) second;
                            if (unaryOp.getDst().equals(assign.getSrc()) && unaryOp.getDst().isTemp()) {
                                block.getBlock().replace(i,
                                        new UnaryOp(unaryOp.getOp(),
                                                unaryOp.getOperand1(), assign.getDst()));
                                block.getBlock().replace(i+1, new Empty());
                            }
                        } else if (first instanceof LoadSave && ((LoadSave) first).getOp() == LoadSave.Op.LOAD) {
                            LoadSave loadSave = (LoadSave) first;
                            Assign assign = (Assign) second;
                            if (loadSave.getDst().equals(assign.getSrc()) && loadSave.getDst().isTemp()) {
                                block.getBlock().replace(i,
                                        new LoadSave(loadSave.getOp(),
                                                loadSave.getBase(), loadSave.getOffset(), assign.getDst()));
                                block.getBlock().replace(i+1, new Empty());
                            }
                        }
                    }
                }
            }
        }
    }

    public void optimize() {
        program.getFunctionTable().values().forEach(this::optimize);
    }
}
