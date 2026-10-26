// Lab 5 – FixedDepositAccount
// Lab 6 – implements InterestBearing; withdraw locked until maturity
package model;

import exception.InsufficientFundsException;
import exception.InvalidAmountException;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Fixed-deposit account: withdraw is locked until the maturity date.
 * Lab 5: concrete subclass of Account.
 * Lab 6: implements InterestBearing.
 */
public class FixedDepositAccount extends Account implements InterestBearing, Serializable {
    private static final long serialVersionUID = 1L;

    public static final double DEFAULT_INTEREST_RATE = 0.07; // 7% p.a.

    private final double    interestRate;
    private final LocalDate maturityDate;

    public FixedDepositAccount(int customerId, double initialBalance, int tenureMonths) {
        super(customerId, initialBalance);
        this.interestRate = DEFAULT_INTEREST_RATE;
        this.maturityDate = LocalDate.now().plusMonths(tenureMonths);
    }

    public FixedDepositAccount(int customerId, double initialBalance,
                               int tenureMonths, double interestRate) {
        super(customerId, initialBalance);
        this.interestRate = interestRate;
        this.maturityDate = LocalDate.now().plusMonths(tenureMonths);
    }

    @Override
    public String getAccountType() { return "FixedDeposit"; }

    /** Withdraw is not permitted before maturity. */
    @Override
    protected void validateWithdraw(double amount)
            throws InsufficientFundsException, InvalidAmountException {
        if (LocalDate.now().isBefore(maturityDate))
            throw new InsufficientFundsException(
                    "Fixed deposit cannot be withdrawn before maturity: " + maturityDate);
        super.validateWithdraw(amount);
    }

    // ── InterestBearing ──────────────────────────────────────────────────────
    @Override
    public double getInterestRate() { return interestRate; }

    @Override
    public void applyInterest() {
        double interest = getBalance() * interestRate;
        try {
            deposit(interest);
        } catch (InvalidAmountException e) {
            throw new IllegalStateException(e);
        }
        System.out.printf("FD Interest applied: %.2f. New balance: %.2f%n",
                interest, getBalance());
    }

    public LocalDate getMaturityDate() { return maturityDate; }

    @Override
    public String toString() {
        return super.toString().replace("]",
                ", maturity=" + maturityDate + "]");
    }
}
