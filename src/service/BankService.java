// Lab 14 – BankService (ties everything together)
package service;

import exception.*;
import model.*;
import service.db.DatabaseManager;
import util.*;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

/**
 * Application-level service layer.
 * Coordinates in-memory storage, SQLite JDBC persistence, authentication,
 * daily transaction limits, transaction history audit, CSV export, and financial reporting.
 */
public class BankService {

    private final AccountStore store;
    private final DatabaseManager db;
    private final AuthService authService;
    private final TransactionProcessor processor;

    public BankService() {
        this(new AccountStore(), new DatabaseManager());
    }

    public BankService(DatabaseManager db) {
        this(new AccountStore(), db);
    }

    public BankService(AccountStore store, DatabaseManager db) {
        this.store       = store;
        this.db          = db;
        this.authService = new AuthService(this.db);
        this.processor   = new TransactionProcessor(4);
    }

    // ── Persistence Lifecycle ────────────────────────────────────────────────
    /**
     * Loads saved data on startup from both SQLite and legacy accounts.dat.
     */
    public void loadData() {
        // 1. Sync store with SQLite records if available
        try {
            List<Customer> dbCustomers = db.loadAllCustomers();
            for (Customer c : dbCustomers) {
                store.addCustomer(c);
                Customer.updateIdCounter(c.getCustomerId());
            }
            List<Account> dbAccounts = db.loadAllAccounts();
            for (Account a : dbAccounts) {
                store.addAccount(a);
                Account.updateIdCounter(a.getAccountId());
            }
        } catch (SQLException e) {
            System.out.println("Warning: Could not load data from database: " + e.getMessage());
        }

        // 2. Load legacy data if present and not yet in store
        try {
            store.load();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Note on legacy file: " + e.getMessage());
        }
    }

    /**
     * Persists all data to SQLite and legacy serialization on application exit.
     */
    public void saveData() {
        // Save to SQLite
        try {
            for (Customer c : store.allCustomers()) {
                db.saveCustomer(c);
            }
            for (Account a : store.allAccounts()) {
                db.saveAccount(a);
            }
        } catch (SQLException e) {
            System.err.println("Database save warning: " + e.getMessage());
        }

        // Save legacy file
        try {
            store.save();
        } catch (IOException e) {
            System.out.println("Warning: could not save legacy data – " + e.getMessage());
        }
    }

    // ── Customer Operations ──────────────────────────────────────────────────
    public Customer createCustomer(String name, String email,
                                   String phone, Address address)
            throws ValidationException {
        if (!Validator.isNotBlank(name))
            throw new ValidationException("name", "must not be blank");
        if (!Validator.isValidEmail(email))
            throw new ValidationException("email", "invalid format: " + email);
        if (!Validator.isValidPhone(phone))
            throw new ValidationException("phone", "must be 10-digit Indian mobile");

        Customer c = new Customer(name, email, phone, address);
        AnnotationValidator.validate(c);
        store.addCustomer(c);

        try {
            db.saveCustomer(c);
        } catch (SQLException e) {
            // Non-fatal if DB is offline or mock
        }
        return c;
    }

    public Optional<Customer> findCustomer(int id) {
        return store.findCustomer(id);
    }

    public List<Customer> allCustomers() {
        return List.copyOf(store.allCustomers());
    }

    public boolean hasCustomers() {
        return !store.allCustomers().isEmpty();
    }

    public int customerCount() {
        return store.allCustomers().size();
    }

    public boolean hasAccounts() {
        return !store.allAccounts().isEmpty();
    }

    public int accountCount() {
        return store.allAccounts().size();
    }

    // ── Account Operations ───────────────────────────────────────────────────
    public SavingsAccount createSavingsAccount(int customerId, double initialBalance)
            throws AccountNotFoundException, ValidationException {
        store.getCustomer(customerId); // verify customer exists – throws if absent
        SavingsAccount acc = new SavingsAccount(customerId, initialBalance);
        store.addAccount(acc);
        persistAccountQuietly(acc);
        recordTxQuietly(acc.getAccountId(), "OPEN", initialBalance, initialBalance, null, "SUCCESS", "Account opened");
        return acc;
    }

    public CurrentAccount createCurrentAccount(int customerId, double initialBalance)
            throws AccountNotFoundException, ValidationException {
        store.getCustomer(customerId);
        CurrentAccount acc = new CurrentAccount(customerId, initialBalance);
        store.addAccount(acc);
        persistAccountQuietly(acc);
        recordTxQuietly(acc.getAccountId(), "OPEN", initialBalance, initialBalance, null, "SUCCESS", "Account opened");
        return acc;
    }

    public FixedDepositAccount createFixedDeposit(int customerId,
                                                   double amount, int months)
            throws AccountNotFoundException {
        store.getCustomer(customerId);
        FixedDepositAccount acc = new FixedDepositAccount(customerId, amount, months);
        store.addAccount(acc);
        persistAccountQuietly(acc);
        recordTxQuietly(acc.getAccountId(), "OPEN", amount, amount, null, "SUCCESS",
                "FD opened for " + months + " months tenure");
        return acc;
    }

    public List<Account> getAccountsForCustomer(int customerId) {
        return store.accountsForCustomer(customerId);
    }

    // ── Transactions ─────────────────────────────────────────────────────────
    public void deposit(int accountId, double amount)
            throws AccountNotFoundException, InvalidAmountException, AccountLockedException {
        if (amount <= 0) {
            throw new InvalidAmountException("Deposit amount must be positive: " + amount);
        }
        Account acc = store.getAccount(accountId);
        if (acc.isLocked()) {
            throw new AccountLockedException("Account " + acc.getAccountNumber() + " is locked. Deposits are rejected.");
        }

        acc.deposit(amount);
        persistAccountQuietly(acc);

        recordTxQuietly(acc.getAccountId(), "DEPOSIT", amount, acc.getBalance(), null, "SUCCESS", "Cash / Electronic Deposit");
        logTx(acc.getAccountNumber(), "DEPOSIT", amount);

        System.out.printf("Deposited %.2f → %s. Balance: %.2f%n",
                amount, acc.getAccountNumber(), acc.getBalance());
    }

    public void withdraw(int accountId, double amount)
            throws AccountNotFoundException, InsufficientFundsException, InvalidAmountException, AccountLockedException {
        if (amount <= 0) {
            throw new InvalidAmountException("Withdraw amount must be positive: " + amount);
        }
        Account acc = store.getAccount(accountId);
        if (acc.isLocked()) {
            throw new AccountLockedException("Account " + acc.getAccountNumber() + " is locked. Withdrawals are rejected.");
        }

        // Check daily transaction limit
        double spentToday = getSpentToday(accountId);
        if (spentToday + amount > acc.getDailyLimit()) {
            String msg = String.format("Daily limit of ₹%,.2f exceeded. Already spent today: ₹%,.2f, Requested: ₹%,.2f",
                    acc.getDailyLimit(), spentToday, amount);
            recordTxQuietly(acc.getAccountId(), "WITHDRAW", amount, acc.getBalance(), null, "FAILED", msg);
            throw new InsufficientFundsException(msg);
        }

        try {
            acc.withdraw(amount);
        } catch (InsufficientFundsException | InvalidAmountException e) {
            recordTxQuietly(acc.getAccountId(), "WITHDRAW", amount, acc.getBalance(), null, "FAILED", e.getMessage());
            throw e;
        }

        persistAccountQuietly(acc);
        recordTxQuietly(acc.getAccountId(), "WITHDRAW", amount, acc.getBalance(), null, "SUCCESS", "Withdrawal");
        logTx(acc.getAccountNumber(), "WITHDRAW", amount);

        System.out.printf("Withdrew %.2f ← %s. Balance: %.2f%n",
                amount, acc.getAccountNumber(), acc.getBalance());
    }

    public void transfer(int fromId, int toId, double amount)
            throws AccountNotFoundException, InsufficientFundsException, InvalidAmountException, AccountLockedException {
        if (fromId == toId) {
            throw new InvalidAmountException("Cannot transfer to the same account: ID " + fromId);
        }
        if (amount <= 0) {
            throw new InvalidAmountException("Transfer amount must be positive: " + amount);
        }

        Account from = store.getAccount(fromId);
        Account to   = store.getAccount(toId);

        if (from.isLocked()) {
            throw new AccountLockedException("Source account " + from.getAccountNumber() + " is locked.");
        }
        if (to.isLocked()) {
            throw new AccountLockedException("Destination account " + to.getAccountNumber() + " is locked.");
        }

        // Daily limit check on source account
        double spentToday = getSpentToday(fromId);
        if (spentToday + amount > from.getDailyLimit()) {
            String msg = String.format("Transfer exceeds daily limit of ₹%,.2f. Spent today: ₹%,.2f, Requested: ₹%,.2f",
                    from.getDailyLimit(), spentToday, amount);
            recordTxQuietly(fromId, "TRANSFER_OUT", amount, from.getBalance(), toId, "FAILED", msg);
            throw new InsufficientFundsException(msg);
        }

        // Lock in consistent global order to prevent deadlocks (Lab 10)
        Account first  = fromId < toId ? from : to;
        Account second = fromId < toId ? to   : from;

        synchronized (first) {
            synchronized (second) {
                // Ensure atomic execution: if withdrawal fails, balances remain untouched
                try {
                    from.withdraw(amount);
                    try {
                        to.deposit(amount);
                    } catch (Exception e) {
                        // Rollback source account if deposit failed
                        try { from.deposit(amount); } catch (Exception ignored) {}
                        throw e;
                    }
                } catch (Exception e) {
                    recordTxQuietly(fromId, "TRANSFER_OUT", amount, from.getBalance(), toId, "FAILED", e.getMessage());
                    throw e;
                }

                // Both succeeded: persist state and record transaction audit
                persistAccountQuietly(from);
                persistAccountQuietly(to);

                recordTxQuietly(fromId, "TRANSFER_OUT", amount, from.getBalance(), toId, "SUCCESS",
                        "Transfer to " + to.getAccountNumber());
                recordTxQuietly(toId, "TRANSFER_IN", amount, to.getBalance(), fromId, "SUCCESS",
                        "Transfer from " + from.getAccountNumber());

                logTx(from.getAccountNumber(), "TRANSFER_OUT", amount);
                logTx(to.getAccountNumber(),   "TRANSFER_IN",  amount);

                System.out.printf("Transferred %.2f from %s → %s%n",
                        amount, from.getAccountNumber(), to.getAccountNumber());
            }
        }
    }

    public double getBalance(int accountId) throws AccountNotFoundException {
        return store.getAccount(accountId).getBalance();
    }

    public void printStatement(int accountId) throws AccountNotFoundException {
        Account acc = store.getAccount(accountId);
        StatementFormatter.print(acc);
    }

    // ── Interest Operations ──────────────────────────────────────────────────
    public void applyInterest(int accountId) throws AccountNotFoundException {
        Account acc = store.getAccount(accountId);
        if (acc instanceof InterestBearing ib) {
            double prevBal = acc.getBalance();
            ib.applyInterest();
            double interestAmount = acc.getBalance() - prevBal;
            persistAccountQuietly(acc);
            recordTxQuietly(accountId, "INTEREST", interestAmount, acc.getBalance(), null, "SUCCESS",
                    String.format("Interest credit at %.1f%%", ib.getInterestRate() * 100));
        } else {
            System.out.println(acc.getAccountType() + " account does not bear interest.");
        }
    }

    public void applyInterestToAllEligible() {
        int count = 0;
        for (Account a : store.allAccounts()) {
            if (a instanceof InterestBearing ib && !a.isLocked()) {
                double prevBal = a.getBalance();
                ib.applyInterest();
                double interestAmount = a.getBalance() - prevBal;
                persistAccountQuietly(a);
                recordTxQuietly(a.getAccountId(), "INTEREST", interestAmount, a.getBalance(), null, "SUCCESS",
                        String.format("Interest credit at %.1f%%", ib.getInterestRate() * 100));
                count++;
            }
        }
        System.out.printf("Applied interest to %d eligible account(s).%n", count);
    }

    // ── Account Administration ───────────────────────────────────────────────
    public void lockAccount(int accountId) throws AccountNotFoundException {
        Account acc = store.getAccount(accountId);
        acc.lock();
        persistAccountQuietly(acc);
        System.out.println("Account " + acc.getAccountNumber() + " has been LOCKED.");
    }

    public void unlockAccount(int accountId) throws AccountNotFoundException {
        Account acc = store.getAccount(accountId);
        acc.unlock();
        persistAccountQuietly(acc);
        System.out.println("Account " + acc.getAccountNumber() + " has been UNLOCKED.");
    }

    public void setDailyLimit(int accountId, double limit) throws AccountNotFoundException, InvalidAmountException {
        if (limit <= 0) {
            throw new InvalidAmountException("Daily limit must be positive: " + limit);
        }
        Account acc = store.getAccount(accountId);
        acc.setDailyLimit(limit);
        persistAccountQuietly(acc);
        System.out.printf("Updated daily transaction limit for %s to ₹%,.2f%n", acc.getAccountNumber(), limit);
    }

    public boolean deleteAccount(int accountId) throws AccountNotFoundException {
        Account acc = store.getAccount(accountId);
        store.removeAccount(accountId);
        try {
            db.deleteAccount(accountId);
        } catch (SQLException e) {
            // Non-fatal
        }
        System.out.println("Deleted account: " + acc.getAccountNumber());
        return true;
    }

    // ── Listing & Views ──────────────────────────────────────────────────────
    public void listAllAccounts() {
        List<Account> list = store.allAccountsSortedById();
        if (list.isEmpty()) {
            System.out.println("No accounts available yet.");
            System.out.println("Please create a customer first, then create a bank account.");
            return;
        }
        System.out.printf("%-6s  %-14s  %-13s  %12s  %-8s  %s%n",
                "Acc-ID", "Account No", "Type", "Balance (₹)", "Status", "Cust-ID");
        System.out.println("-".repeat(75));
        list.forEach(a -> System.out.printf("%-6d  %-14s  %-13s  %12.2f  %-8s  %d%n",
                a.getAccountId(), a.getAccountNumber(),
                a.getAccountType(), a.getBalance(),
                a.isLocked() ? "LOCKED" : "ACTIVE",
                a.getCustomerId()));
        System.out.printf("%nTotal: %d account(s)%n", list.size());
    }

    // ── Reports & CSV Export ──────────────────────────────────────────────────
    public void generateReport() {
        try {
            TransactionLogger.generateReport(store.allAccounts());
        } catch (IOException e) {
            System.out.println("Report generation failed: " + e.getMessage());
        }
    }

    public void printFinancialSummaryReport() {
        List<Account> accounts = List.copyOf(store.allAccounts());
        long savingsCount = accounts.stream().filter(a -> a instanceof SavingsAccount).count();
        long currentCount = accounts.stream().filter(a -> a instanceof CurrentAccount).count();
        long fdCount      = accounts.stream().filter(a -> a instanceof FixedDepositAccount).count();
        long lockedCount  = accounts.stream().filter(Account::isLocked).count();

        double totalBalance = accounts.stream().mapToDouble(Account::getBalance).sum();

        System.out.println("\n══════════════════════════════════════════════════════════════════");
        System.out.println("                 MINIBANK FINANCIAL SUMMARY REPORT");
        System.out.println("══════════════════════════════════════════════════════════════════");
        System.out.printf("  Total Customers Registered : %d%n", store.allCustomers().size());
        System.out.printf("  Total Active Accounts      : %d%n", accounts.size() - lockedCount);
        System.out.printf("  Total Locked Accounts      : %d%n", lockedCount);
        System.out.printf("  Total Accounts (All)       : %d%n", accounts.size());
        System.out.println("  ----------------------------------------------------------------");
        System.out.printf("  • Savings Accounts         : %d%n", savingsCount);
        System.out.printf("  • Current Accounts         : %d%n", currentCount);
        System.out.printf("  • Fixed Deposit Accounts   : %d%n", fdCount);
        System.out.println("  ----------------------------------------------------------------");
        System.out.printf("  Total Bank Deposits/Liquidity : ₹%,.2f%n", totalBalance);
        System.out.println("══════════════════════════════════════════════════════════════════\n");
    }

    public File exportStatementToCsv(int accountId, File targetFile) throws AccountNotFoundException, IOException {
        Account acc = store.getAccount(accountId);
        List<Transaction> list = getTransactions(accountId);
        return CsvExporter.exportStatement(acc, list, targetFile);
    }

    // ── Transaction Queries ──────────────────────────────────────────────────
    public List<Transaction> getTransactions(int accountId) {
        try {
            return db.getTransactionsForAccount(accountId);
        } catch (SQLException e) {
            return Collections.emptyList();
        }
    }

    public List<Transaction> filterTransactions(int accountId, String type, LocalDate from, LocalDate to) {
        try {
            return db.filterTransactions(accountId, type, from, to);
        } catch (SQLException e) {
            return Collections.emptyList();
        }
    }

    public Optional<Transaction> findTransaction(String txId) {
        try {
            return db.findTransactionById(txId);
        } catch (SQLException e) {
            return Optional.empty();
        }
    }

    public boolean backupDatabase(File target) {
        return db.backupTo(target);
    }

    // ── Getters for Subsystems ───────────────────────────────────────────────
    public AuthService getAuthService() { return authService; }
    public DatabaseManager getDatabaseManager() { return db; }
    public TransactionProcessor getProcessor() { return processor; }
    public AccountStore getStore() { return store; }

    // ── Internal Helpers ─────────────────────────────────────────────────────
    private double getSpentToday(int accountId) {
        try {
            List<Transaction> todayTxs = db.filterTransactions(accountId, null, LocalDate.now(), LocalDate.now());
            return todayTxs.stream()
                    .filter(t -> "SUCCESS".equalsIgnoreCase(t.getStatus()))
                    .filter(t -> "WITHDRAW".equalsIgnoreCase(t.getType()) || "TRANSFER_OUT".equalsIgnoreCase(t.getType()))
                    .mapToDouble(Transaction::getAmount)
                    .sum();
        } catch (SQLException e) {
            return 0.0;
        }
    }

    private void persistAccountQuietly(Account a) {
        try {
            db.saveAccount(a);
        } catch (SQLException ignored) {
        }
    }

    private void recordTxQuietly(int accountId, String type, double amount, double balAfter,
                                 Integer targetAccountId, String status, String desc) {
        try {
            Transaction tx = Transaction.create(accountId, type, amount, balAfter, targetAccountId, status, desc);
            db.recordTransaction(tx);
        } catch (SQLException ignored) {
        }
    }

    private void logTx(String accountNumber, String type, double amount) {
        try {
            TransactionLogger.log(accountNumber, type, amount);
        } catch (IOException ignored) {
        }
    }
}
