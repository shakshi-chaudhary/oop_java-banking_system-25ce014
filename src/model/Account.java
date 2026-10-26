// Labs 2, 5, 8 – Abstract Account base class
// Lab 2: basic fields, deposit/withdraw returning boolean
// Lab 5: abstract class with template method
// Lab 8: throws exceptions instead of returning false
package model;

import exception.InsufficientFundsException;
import exception.InvalidAmountException;
import model.annotation.Id;
import model.annotation.Positive;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Abstract base class for all account types.
 * Lab 5: abstract class; subclasses must implement {@link #getAccountType()}.
 * Lab 6: implements {@link Transactable}.
 * Lab 8: deposit/withdraw throw checked exceptions.
 */
public abstract class Account implements Transactable, Serializable, Cloneable {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static int idCounter = 2000;

    @Id
    private final int accountId;

    @Positive
    private double balance;

    private final int    customerId;
    private final String accountNumber;

    /** Ordered list of "timestamp | type | amount | balance" entries. */
    private final List<String> transactionHistory = new ArrayList<>();

    protected Account(int customerId, double initialBalance) {
        this.accountId     = ++idCounter;
        this.customerId    = customerId;
        this.accountNumber = "ACC" + String.format("%06d", accountId);
        this.balance       = initialBalance;
        log("OPEN", initialBalance);
    }

    // ── Abstract contract ────────────────────────────────────────────────────
    /** E.g. "Savings", "Current", "FixedDeposit". */
    public abstract String getAccountType();

    // ── Transactable implementation ──────────────────────────────────────────
    @Override
    public synchronized void deposit(double amount)
            throws InvalidAmountException {
        if (amount <= 0)
            throw new InvalidAmountException("Deposit amount must be positive: " + amount);
        balance += amount;
        log("DEPOSIT", amount);
    }

    @Override
    public synchronized void withdraw(double amount)
            throws InsufficientFundsException, InvalidAmountException {
        validateWithdraw(amount);
        balance -= amount;
        log("WITHDRAW", amount);
    }

    /**
     * Hook for subclasses to add extra withdraw rules (e.g. overdraft check).
     * Default: amount must be positive and ≤ balance.
     */
    protected void validateWithdraw(double amount)
            throws InsufficientFundsException, InvalidAmountException {
        if (amount <= 0)
            throw new InvalidAmountException("Withdraw amount must be positive: " + amount);
        if (amount > balance)
            throw new InsufficientFundsException(
                    String.format("Insufficient funds: balance=%.2f, requested=%.2f",
                            balance, amount));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void log(String type, double amount) {
        transactionHistory.add(String.format("%s | %-8s | %10.2f | %10.2f",
                LocalDateTime.now().format(FMT), type, amount, balance));
    }

    public List<String> getTransactionHistory() {
        return Collections.unmodifiableList(transactionHistory);
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public int    getAccountId()     { return accountId; }
    public int    getCustomerId()    { return customerId; }
    public String getAccountNumber() { return accountNumber; }
    public double getBalance()       { return balance; }

    // ── Object overrides ──────────────────────────────────────────────────────
    @Override
    public String toString() {
        return String.format("%s[id=%d, acNo=%s, custId=%d, balance=%.2f]",
                getAccountType(), accountId, accountNumber, customerId, balance);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account a)) return false;
        return accountId == a.accountId;
    }

    @Override
    public int hashCode() { return Objects.hash(accountId); }

    @Override
    public Account clone() {
        try {
            return (Account) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
