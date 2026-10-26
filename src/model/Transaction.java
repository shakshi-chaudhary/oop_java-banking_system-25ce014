package model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain model representing a financial transaction or audit record.
 * Tracks transaction ID, account ID, type, amount, post-transaction balance,
 * destination/source account (for transfers), timestamp, status, and description.
 */
public class Transaction implements Serializable, Comparable<Transaction> {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String        transactionId;
    private final int           accountId;
    private final String        type;
    private final double        amount;
    private final double        balanceAfter;
    private final Integer       targetAccountId;
    private final LocalDateTime timestamp;
    private final String        status;       // SUCCESS, FAILED
    private final String        description;

    public Transaction(String transactionId, int accountId, String type, double amount,
                       double balanceAfter, Integer targetAccountId, LocalDateTime timestamp,
                       String status, String description) {
        this.transactionId   = transactionId != null ? transactionId : generateId();
        this.accountId       = accountId;
        this.type            = type;
        this.amount          = amount;
        this.balanceAfter    = balanceAfter;
        this.targetAccountId = targetAccountId;
        this.timestamp       = timestamp != null ? timestamp : LocalDateTime.now();
        this.status          = status != null ? status : "SUCCESS";
        this.description     = description != null ? description : "";
    }

    public static Transaction create(int accountId, String type, double amount,
                                     double balanceAfter, Integer targetAccountId,
                                     String status, String description) {
        return new Transaction(generateId(), accountId, type, amount, balanceAfter,
                targetAccountId, LocalDateTime.now(), status, description);
    }

    public static String generateId() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public String        getTransactionId()   { return transactionId; }
    public int           getAccountId()       { return accountId; }
    public String        getType()            { return type; }
    public double        getAmount()          { return amount; }
    public double        getBalanceAfter()    { return balanceAfter; }
    public Integer       getTargetAccountId() { return targetAccountId; }
    public LocalDateTime getTimestamp()       { return timestamp; }
    public String        getStatus()          { return status; }
    public String        getDescription()     { return description; }

    public String getFormattedTimestamp() {
        return timestamp != null ? timestamp.format(FMT) : "N/A";
    }

    @Override
    public int compareTo(Transaction other) {
        if (other == null || other.timestamp == null) return 1;
        if (this.timestamp == null) return -1;
        return other.timestamp.compareTo(this.timestamp); // latest first
    }

    @Override
    public String toString() {
        return String.format("%s | %-12s | %-12s | %10.2f | Bal: %10.2f | %-7s | %s",
                getFormattedTimestamp(), transactionId, type, amount, balanceAfter, status, description);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction that)) return false;
        return Objects.equals(transactionId, that.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }
}
