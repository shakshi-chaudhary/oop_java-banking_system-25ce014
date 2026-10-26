// Labs 11, 12 – AccountStore
// HashMap-based in-memory store backed by serialization persistence.
// ConcurrentHashMap used for thread safety (Lab 12).
package service;

import exception.AccountNotFoundException;
import model.Account;
import model.Customer;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory store for customers and accounts.
 * Lab 11: object serialization (save/load).
 * Lab 12: ConcurrentHashMap, sorting.
 */
@SuppressWarnings("unchecked")
public class AccountStore {

    private static final String DATA_FILE = "accounts.dat";

    // Thread-safe maps (Lab 12)
    private final Map<Integer, Customer> customers = new ConcurrentHashMap<>();
    private final Map<Integer, Account>  accounts  = new ConcurrentHashMap<>();

    // ── Customer operations ───────────────────────────────────────────────────
    public void addCustomer(Customer c) {
        customers.put(c.getCustomerId(), c);
    }

    public Optional<Customer> findCustomer(int id) {
        return Optional.ofNullable(customers.get(id));
    }

    public Customer getCustomer(int id) throws AccountNotFoundException {
        return findCustomer(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    public Collection<Customer> allCustomers() {
        return Collections.unmodifiableCollection(customers.values());
    }

    public boolean removeCustomer(int id) {
        return customers.remove(id) != null;
    }

    // ── Account operations ────────────────────────────────────────────────────
    public void addAccount(Account a) {
        accounts.put(a.getAccountId(), a);
    }

    public Optional<Account> findAccount(int id) {
        return Optional.ofNullable(accounts.get(id));
    }

    public Account getAccount(int id) throws AccountNotFoundException {
        return findAccount(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    public boolean removeAccount(int id) {
        return accounts.remove(id) != null;
    }

    /** All accounts belonging to a specific customer. */
    public List<Account> accountsForCustomer(int customerId) {
        return accounts.values().stream()
                .filter(a -> a.getCustomerId() == customerId)
                .collect(Collectors.toList());
    }

    /** All accounts sorted by balance descending (Lab 12). */
    public List<Account> allAccountsSortedByBalance() {
        return accounts.values().stream()
                .sorted(Comparator.comparingDouble(Account::getBalance).reversed())
                .collect(Collectors.toList());
    }

    /** All accounts sorted by account ID ascending. */
    public List<Account> allAccountsSortedById() {
        return accounts.values().stream()
                .sorted(Comparator.comparingInt(Account::getAccountId))
                .collect(Collectors.toList());
    }

    public Collection<Account> allAccounts() {
        return Collections.unmodifiableCollection(accounts.values());
    }

    // ── Persistence (Lab 11) ──────────────────────────────────────────────────
    /** Serializes customers and accounts to {@value #DATA_FILE}. */
    public void save() throws IOException {
        try (ObjectOutputStream oos =
                     new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(new HashMap<>(customers));
            oos.writeObject(new HashMap<>(accounts));
            System.out.println("Data saved to " + DATA_FILE);
        }
    }

    /** Deserializes customers and accounts from {@value #DATA_FILE}. */
    public void load() throws IOException, ClassNotFoundException {
        File f = new File(DATA_FILE);
        if (!f.exists()) {
            System.out.println("No saved data found – starting fresh.");
            return;
        }
        try (ObjectInputStream ois =
                     new ObjectInputStream(new FileInputStream(f))) {
            Map<Integer, Customer> savedCustomers =
                    (Map<Integer, Customer>) ois.readObject();
            Map<Integer, Account>  savedAccounts  =
                    (Map<Integer, Account>)  ois.readObject();
            customers.putAll(savedCustomers);
            accounts.putAll(savedAccounts);
            System.out.println("Loaded " + customers.size() + " customer(s) and "
                               + accounts.size() + " account(s) from " + DATA_FILE);
        }
    }
}
