package exception;

/** Thrown when a requested amount is invalid (zero, negative, non-numeric). */
public class InvalidAmountException extends BankException {
    public InvalidAmountException(String message) {
        super("INVALID_AMOUNT", message);
    }
}
