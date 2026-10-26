// Lab 5 – FixedDepositAccount
// Lab 6 – implements InterestBearing; withdraw locked until maturity
package model;

import exception.AccountLockedException;
import exception.InsufficientFundsException;
import exception.InvalidAmountException;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

    /** Database restoration constructor. */
    public FixedDepositAccount(int accountId, int customerId, String accountNumber,
                               double balance, boolean isLocked, double dailyLimit,
                               double interestRate, LocalDate maturityDate) {
        super(accountId, customerId, accountNumber, balance, isLocked, dailyLimit);
        this.interestRate = interestRate > 0 ? interestRate : DEFAULT_INTEREST_RATE;
        this.maturityDate = maturityDate != null ? maturityDate : LocalDate.now().plusMonths(12);
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
        BigDecimal bal = BigDecimal.valueOf(getBalance());
        BigDecimal rate = BigDecimal.valueOf(interestRate);
        double interest = bal.multiply(rate).setScale(2, RoundingMode.HALF_UP).doubleValue();
        try {
            deposit(interest);
        } catch (InvalidAmountException | AccountLockedException e) {
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
