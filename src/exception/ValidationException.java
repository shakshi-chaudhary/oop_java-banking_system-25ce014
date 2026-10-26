package exception;

/** Thrown when input data fails validation rules. */
public class ValidationException extends BankException {
    public ValidationException(String field, String reason) {
        super("VALIDATION_ERROR", "Validation failed for [" + field + "]: " + reason);
    }
}
