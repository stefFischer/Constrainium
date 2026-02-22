package at.sfischer.constraints.parser.registry;

public class FunctionCreateException extends Exception {
    public FunctionCreateException() {
    }

    public FunctionCreateException(String message) {
        super(message);
    }

    public FunctionCreateException(String message, Throwable cause) {
        super(message, cause);
    }

    public FunctionCreateException(Throwable cause) {
        super(cause);
    }

    public FunctionCreateException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
