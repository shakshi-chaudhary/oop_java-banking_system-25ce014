package exception;

/** Thrown when an account or customer cannot be found by the given ID. */
public class AccountNotFoundException extends BankException {
    public AccountNotFoundException(int accountId) {
        super("ACCOUNT_NOT_FOUND", "No account found with id: " + accountId);
    }
    public AccountNotFoundException(String accountNumber) {
        super("ACCOUNT_NOT_FOUND", "No account found: " + accountNumber);
    }
}
