package frontend.grammar.stmt;

import java.io.PrintStream;

// 语句块式语句不需要分号, 因此semicolon设为null
public class BlockStmt extends Stmt {
    private final Block block;

    public BlockStmt(Block block) {
        super(null, block.getLeftBrace().getLinenumber());
        this.block = block;
    }

    public Block getBlock() {
        return block;
    }

    @Override
    public void print(PrintStream out) {
        block.print(out);
        super.print(out);
    }
}
