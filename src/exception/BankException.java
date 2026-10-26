// Lab 8 – Custom exception hierarchy
package exception;

/**
 * Root of the MiniBank exception hierarchy.
 * Lab 8: all bank-specific checked exceptions extend this class.
 */
public class BankException extends Exception {
    private final String errorCode;

    public BankException(String message) {
        super(message);
        this.errorCode = "BANK_ERR";
    }

    public BankException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BankException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }

    @Override
    public String toString() {
        return "[" + errorCode + "] " + getMessage();
    }
}
