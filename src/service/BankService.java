// Lab 14 – BankService (ties everything together)
package service;

import exception.*;
import model.*;
import util.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Application-level service layer.
 * Lab 14: orchestrates all operations; called by {@code MiniBank} main class.
 */
public class BankService {

    private final AccountStore store;
    private final TransactionProcessor processor;

    public BankService() {
        this.store     = new AccountStore();
        this.processor = new TransactionProcessor(4);
    }

    // ── Persistence (Lab 11) ──────────────────────────────────────────────────
    public void loadData() {
        try { store.load(); }
        catch (IOException | ClassNotFoundException e) {
            System.out.println("Warning: could not load saved data – " + e.getMessage());
        }
    }

    public void saveData() {
        try { store.save(); }
        catch (IOException e) {
            System.out.println("Warning: could not save data – " + e.getMessage());
        }
    }

    // ── Customer operations ───────────────────────────────────────────────────
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
        // Caller (MiniBank) prints the success banner with the generated ID
        return c;
    }

    public Optional<Customer> findCustomer(int id) {
        return store.findCustomer(id);
    }

    public List<Customer> allCustomers() {
        return List.copyOf(store.allCustomers());
    }

    /** True when at least one customer has been registered. */
    public boolean hasCustomers() {
        return !store.allCustomers().isEmpty();
    }

    /** Total number of registered customers. */
    public int customerCount() {
        return store.allCustomers().size();
    }

    /** True when at least one account exists. */
    public boolean hasAccounts() {
        return !store.allAccounts().isEmpty();
    }

    /** Total number of accounts. */
    public int accountCount() {
        return store.allAccounts().size();
    }

    // ── Account operations ────────────────────────────────────────────────────
    public SavingsAccount createSavingsAccount(int customerId, double initialBalance)
            throws AccountNotFoundException, ValidationException {
        store.getCustomer(customerId); // verify customer exists – throws if absent
        SavingsAccount acc = new SavingsAccount(customerId, initialBalance);
        store.addAccount(acc);
        return acc;   // caller prints the success banner
    }

    public CurrentAccount createCurrentAccount(int customerId, double initialBalance)
            throws AccountNotFoundException, ValidationException {
        store.getCustomer(customerId);
        CurrentAccount acc = new CurrentAccount(customerId, initialBalance);
        store.addAccount(acc);
        return acc;
    }

    public FixedDepositAccount createFixedDeposit(int customerId,
                                                   double amount, int months)
            throws AccountNotFoundException {
        store.getCustomer(customerId);
        FixedDepositAccount acc = new FixedDepositAccount(customerId, amount, months);
        store.addAccount(acc);
        return acc;
    }

    // ── Transactions ──────────────────────────────────────────────────────────
    public void deposit(int accountId, double amount)
            throws AccountNotFoundException, InvalidAmountException {
        Account acc = store.getAccount(accountId);
        acc.deposit(amount);
        logTx(acc.getAccountNumber(), "DEPOSIT", amount);
        System.out.printf("Deposited %.2f → %s. Balance: %.2f%n",
                amount, acc.getAccountNumber(), acc.getBalance());
    }

    public void withdraw(int accountId, double amount)
            throws AccountNotFoundException, InsufficientFundsException, InvalidAmountException {
        Account acc = store.getAccount(accountId);
        acc.withdraw(amount);
        logTx(acc.getAccountNumber(), "WITHDRAW", amount);
        System.out.printf("Withdrew %.2f ← %s. Balance: %.2f%n",
                amount, acc.getAccountNumber(), acc.getBalance());
    }

    public void transfer(int fromId, int toId, double amount)
            throws AccountNotFoundException, InsufficientFundsException, InvalidAmountException {
        Account from = store.getAccount(fromId);
        Account to   = store.getAccount(toId);
        // Lock in consistent order to avoid deadlock (Lab 10)
        Account first  = fromId < toId ? from : to;
        Account second = fromId < toId ? to   : from;
        synchronized (first) {
            synchronized (second) {
                from.withdraw(amount);
                to.deposit(amount);
            }
        }
        logTx(from.getAccountNumber(), "TRANSFER_OUT", amount);
        logTx(to.getAccountNumber(),   "TRANSFER_IN",  amount);
        System.out.printf("Transferred %.2f from %s → %s%n",
                amount, from.getAccountNumber(), to.getAccountNumber());
    }

    public double getBalance(int accountId) throws AccountNotFoundException {
        return store.getAccount(accountId).getBalance();
    }

    public void printStatement(int accountId) throws AccountNotFoundException {
        Account acc = store.getAccount(accountId);
        StatementFormatter.print(acc);
    }

    // ── Interest (Lab 5, 6) ───────────────────────────────────────────────────
    public void applyInterest(int accountId) throws AccountNotFoundException {
        Account acc = store.getAccount(accountId);
        if (acc instanceof InterestBearing ib) {
            ib.applyInterest();
        } else {
            System.out.println(acc.getAccountType() + " account does not bear interest.");
        }
    }

    // ── Listing ───────────────────────────────────────────────────────────────
    public void listAllAccounts() {
        List<Account> list = store.allAccountsSortedById();
        if (list.isEmpty()) {
            System.out.println("No accounts available yet.");
            System.out.println("Please create a customer first, then create a bank account.");
            return;
        }
        System.out.printf("%-6s  %-14s  %-13s  %12s  %s%n",
                "Acc-ID", "Account No", "Type", "Balance (₹)", "Cust-ID");
        System.out.println("-".repeat(65));
        list.forEach(a -> System.out.printf("%-6d  %-14s  %-13s  %12.2f  %d%n",
                a.getAccountId(), a.getAccountNumber(),
                a.getAccountType(), a.getBalance(), a.getCustomerId()));
        System.out.printf("%nTotal: %d account(s)%n", list.size());
    }

    public boolean deleteAccount(int accountId) throws AccountNotFoundException {
        Account acc = store.getAccount(accountId); // throws if not found
        store.removeAccount(accountId);
        System.out.println("Deleted account: " + acc.getAccountNumber());
        return true;
    }

    // ── Report (Lab 13) ───────────────────────────────────────────────────────
    public void generateReport() {
        try {
            TransactionLogger.generateReport(store.allAccounts());
        } catch (IOException e) {
            System.out.println("Report generation failed: " + e.getMessage());
        }
    }

    // ── Concurrency demos ─────────────────────────────────────────────────────
    public TransactionProcessor getProcessor() { return processor; }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void logTx(String accountNumber, String type, double amount) {
        try { TransactionLogger.log(accountNumber, type, amount); }
        catch (IOException e) { /* non-fatal */ }
    }

    public AccountStore getStore() { return store; }
}
