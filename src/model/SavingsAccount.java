// Lab 5 – SavingsAccount
// Lab 6 – implements InterestBearing
package model;

import exception.InsufficientFundsException;
import exception.InvalidAmountException;

import java.io.Serializable;

/**
 * Savings account: minimum balance enforced on withdraw; earns interest.
 * Lab 5: concrete subclass of Account.
 * Lab 6: implements InterestBearing.
 */
public class SavingsAccount extends Account implements InterestBearing, Serializable {
    private static final long serialVersionUID = 1L;

    public static final double DEFAULT_INTEREST_RATE = 0.04; // 4% p.a.
    public static final double MIN_BALANCE           = 500.0;

    private double interestRate;

    public SavingsAccount(int customerId, double initialBalance) {
        super(customerId, initialBalance);
        this.interestRate = DEFAULT_INTEREST_RATE;
    }

    public SavingsAccount(int customerId, double initialBalance, double interestRate) {
        super(customerId, initialBalance);
        this.interestRate = interestRate;
    }

    @Override
    public String getAccountType() { return "Savings"; }

    // ── Withdraw rule: balance must not drop below MIN_BALANCE ──────────────
    @Override
    protected void validateWithdraw(double amount)
            throws InsufficientFundsException, InvalidAmountException {
        super.validateWithdraw(amount);
        if (getBalance() - amount < MIN_BALANCE)
            throw new InsufficientFundsException(
                    String.format("Savings account must maintain minimum balance of %.2f", MIN_BALANCE));
    }

    // ── InterestBearing ──────────────────────────────────────────────────────
    @Override
    public double getInterestRate() { return interestRate; }

    public void setInterestRate(double rate) { this.interestRate = rate; }

    @Override
    public void applyInterest() {
        double interest = getBalance() * interestRate;
        try {
            deposit(interest);
        } catch (InvalidAmountException e) {
            // interest is always positive – should never happen
            throw new IllegalStateException(e);
        }
        System.out.printf("Interest applied: %.2f. New balance: %.2f%n",
                interest, getBalance());
    }
}
