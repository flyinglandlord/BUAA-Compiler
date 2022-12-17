package frontend.error;

public class Error implements Comparable<Error> {
    private final ErrorType errorType;
    private final int linenumber;

    public Error(ErrorType errorType, int linenumber) {
        this.errorType = errorType;
        this.linenumber = linenumber;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public int getLinenumber() {
        return linenumber;
    }

    @Override
    public String toString() {
        return linenumber + " " + errorType.toString();
    }

    @Override
    public int compareTo(Error o) {
        return this.linenumber - o.linenumber;
    }

    public enum ErrorType {
        ILLEGAL_FORMAT_STRING("a"),
        REDEFINED_IDENT("b"),
        UNDEFINED_IDENT("c"),
        MISMATCH_PARAM_NUM("d"),
        MISMATCH_PARAM_TYPE("e"),
        VOID_FUNC_RETURN_VALUE("f"),
        MISSING_RETURN("g"),
        MODIFY_CONST("h"),
        MISSING_SEMICOLON("i"),
        MISSING_RIGHT_PARENT("j"),
        MISSING_RIGHT_BRACKET("k"),
        MISMATCH_PARAM_PRINTF("l"),
        CONTROL_OUTSIDE_LOOP("m");

        private final String errorCode;

        ErrorType(String code) {
            this.errorCode = code;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }
}
