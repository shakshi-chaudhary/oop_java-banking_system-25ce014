// Labs 2, 5, 8 – Abstract Account base class
// Lab 2: basic fields, deposit/withdraw returning boolean
// Lab 5: abstract class with template method
// Lab 8: throws exceptions instead of returning false
package model;

import exception.AccountLockedException;
import exception.InsufficientFundsException;
import exception.InvalidAmountException;
import model.annotation.Id;
import model.annotation.Positive;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
 * Professional enhancement: BigDecimal calculations, account locking, and daily limits.
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

    private boolean isLocked = false;
    private double dailyLimit = 50_000.0;

    /** Ordered list of "timestamp | type | amount | balance" entries. */
    private final List<String> transactionHistory = new ArrayList<>();

    protected Account(int customerId, double initialBalance) {
        this.accountId     = ++idCounter;
        this.customerId    = customerId;
        this.accountNumber = "ACC" + String.format("%06d", accountId);
        this.balance       = round(initialBalance);
        log("OPEN", this.balance);
    }

    /** Constructor for reconstructing account from database or serialized store. */
    protected Account(int accountId, int customerId, String accountNumber,
                      double balance, boolean isLocked, double dailyLimit) {
        this.accountId     = accountId;
        this.customerId    = customerId;
        this.accountNumber = (accountNumber != null && !accountNumber.isBlank())
                ? accountNumber : "ACC" + String.format("%06d", accountId);
        this.balance       = round(balance);
        this.isLocked      = isLocked;
        this.dailyLimit    = dailyLimit > 0 ? dailyLimit : 50_000.0;
        updateIdCounter(accountId);
    }

    public static synchronized void updateIdCounter(int id) {
        if (id > idCounter) {
            idCounter = id;
        }
    }

    public static synchronized int peekNextId() {
        return idCounter + 1;
    }

    // ── Abstract contract ────────────────────────────────────────────────────
    /** E.g. "Savings", "Current", "FixedDeposit". */
    public abstract String getAccountType();

    // ── Transactable implementation ──────────────────────────────────────────
    @Override
    public synchronized void deposit(double amount)
            throws InvalidAmountException, AccountLockedException {
        if (amount <= 0)
            throw new InvalidAmountException("Deposit amount must be positive: " + amount);
        if (isLocked)
            throw new AccountLockedException("Account " + accountNumber + " is locked. Deposits are not permitted.");

        BigDecimal cur = BigDecimal.valueOf(balance);
        BigDecimal add = BigDecimal.valueOf(amount);
        this.balance = cur.add(add).setScale(2, RoundingMode.HALF_UP).doubleValue();
        log("DEPOSIT", amount);
    }

    @Override
    public synchronized void withdraw(double amount)
            throws InsufficientFundsException, InvalidAmountException, AccountLockedException {
        if (isLocked)
            throw new AccountLockedException("Account " + accountNumber + " is locked. Withdrawals are not permitted.");
        validateWithdraw(amount);

        BigDecimal cur = BigDecimal.valueOf(balance);
        BigDecimal sub = BigDecimal.valueOf(amount);
        this.balance = cur.subtract(sub).setScale(2, RoundingMode.HALF_UP).doubleValue();
        log("WITHDRAW", amount);
    }

    /**
     * Hook for subclasses to add extra withdraw rules (e.g. overdraft check).
     * Default: amount must be positive and <= balance.
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

    // ── Precision helper ─────────────────────────────────────────────────────
    protected static double round(double val) {
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    // ── Locking & Limits ─────────────────────────────────────────────────────
    public boolean isLocked() { return isLocked; }
    public void setLocked(boolean locked) { this.isLocked = locked; }
    public void lock() { this.isLocked = true; }
    public void unlock() { this.isLocked = false; }

    public double getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(double limit) { this.dailyLimit = limit; }

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
        return String.format("%s[id=%d, acNo=%s, custId=%d, balance=%.2f, locked=%b]",
                getAccountType(), accountId, accountNumber, customerId, balance, isLocked);
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
