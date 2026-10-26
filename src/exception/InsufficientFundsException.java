package exception;

/** Thrown when a withdrawal cannot be completed due to low balance. */
public class InsufficientFundsException extends BankException {
    public InsufficientFundsException(String message) {
        super("INSUFFICIENT_FUNDS", message);
    }
}
