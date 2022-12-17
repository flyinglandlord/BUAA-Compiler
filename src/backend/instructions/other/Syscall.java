package backend.instructions.other;

import backend.instructions.Instructions;

public class Syscall implements Instructions {
    @Override
    public String toString() {
        return "syscall";
    }
}
