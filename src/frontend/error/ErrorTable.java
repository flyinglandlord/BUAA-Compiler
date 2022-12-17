package frontend.error;

import java.util.TreeSet;

public class ErrorTable {
    private final TreeSet<Error> errorTable = new TreeSet<>();

    public TreeSet<Error> getErrorTable() {
        return errorTable;
    }

    public void add(Error.ErrorType errorType, int linenumber) {
        errorTable.add(new Error(errorType, linenumber));
    }

    public void add(Error error) {
        errorTable.add(error);
    }
}
