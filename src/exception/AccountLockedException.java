package exception;

/** Thrown when attempting to withdraw from a locked account (e.g. FD before maturity). */
public class AccountLockedException extends BankException {
    public AccountLockedException(String message) {
        super("ACCOUNT_LOCKED", message);
    }
}
