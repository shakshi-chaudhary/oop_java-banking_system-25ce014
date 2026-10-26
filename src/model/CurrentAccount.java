// Lab 5 – CurrentAccount
// Lab 6 – implements Premium (overdraft is a premium feature)
package model;

import exception.InsufficientFundsException;
import exception.InvalidAmountException;

import java.io.Serializable;

/**
 * Current account: supports overdraft up to a configurable limit.
 * Lab 5: concrete subclass of Account.
 * Lab 6: implements Premium.
 */
public class CurrentAccount extends Account implements Premium, Serializable {
    private static final long serialVersionUID = 1L;

    public static final double DEFAULT_OVERDRAFT_LIMIT = 10_000.0;

    private double overdraftLimit;

    public CurrentAccount(int customerId, double initialBalance) {
        super(customerId, initialBalance);
        this.overdraftLimit = DEFAULT_OVERDRAFT_LIMIT;
    }

    public CurrentAccount(int customerId, double initialBalance, double overdraftLimit) {
        super(customerId, initialBalance);
        this.overdraftLimit = overdraftLimit;
    }

    @Override
    public String getAccountType() { return "Current"; }

    // ── Withdraw rule: balance may go negative up to overdraftLimit ─────────
    @Override
    protected void validateWithdraw(double amount)
            throws InsufficientFundsException, InvalidAmountException {
        if (amount <= 0)
            throw new InvalidAmountException("Withdraw amount must be positive: " + amount);
        if (getBalance() - amount < -overdraftLimit)
            throw new InsufficientFundsException(
                    String.format("Overdraft limit of %.2f exceeded", overdraftLimit));
    }

    // ── Premium ──────────────────────────────────────────────────────────────
    @Override
    public String[] getPremiumBenefits() {
        return new String[]{
            "Overdraft facility up to ₹" + overdraftLimit,
            "Free RTGS/NEFT transactions",
            "Dedicated relationship manager"
        };
    }

    public double getOverdraftLimit()            { return overdraftLimit; }
    public void   setOverdraftLimit(double limit){ this.overdraftLimit = limit; }
}
