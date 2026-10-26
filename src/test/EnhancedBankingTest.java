import exception.*;
import model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import service.AuthService;
import service.BankService;
import service.db.DatabaseManager;
import util.CsvExporter;
import util.SecurityUtil;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 6 – Expanded automated test suite for MiniBank.
 * Tests SQLite persistence across restarts, transaction atomicity,
 * role-based authentication, brute-force lockout, account locking,
 * daily limits, CSV export, and customer account isolation.
 */
@DisplayName("Enhanced Banking & Security Tests")
class EnhancedBankingTest {

    @TempDir
    Path tempDir;

    private DatabaseManager db;
    private BankService svc;
    private Customer cust1;
    private Customer cust2;
    private SavingsAccount acc1;
    private SavingsAccount acc2;

    @BeforeEach
    void setUp() throws Exception {
        String dbPath = tempDir.resolve("test_bank.db").toAbsolutePath().toString();
        db = new DatabaseManager("jdbc:sqlite:" + dbPath);
        svc = new BankService(db);

        cust1 = svc.createCustomer("Alice Smith", "alice@example.com", "9876543210",
                new Address("10 Park Ave", "Mumbai", "MH", "400001"));
        cust2 = svc.createCustomer("Bob Jones", "bob@example.com", "9123456780",
                new Address("20 Lake View", "Pune", "MH", "411001"));

        acc1 = svc.createSavingsAccount(cust1.getCustomerId(), 5_000.0);
        acc2 = svc.createSavingsAccount(cust2.getCustomerId(), 3_000.0);
    }

    @AfterEach
    void tearDown() {
        if (db != null) {
            db.close();
        }
    }

    @Test
    @DisplayName("Transfer to same account fails and leaves balance unchanged")
    void testTransferToSameAccountFails() {
        double before = acc1.getBalance();
        assertThrows(InvalidAmountException.class,
                () -> svc.transfer(acc1.getAccountId(), acc1.getAccountId(), 500.0));
        assertEquals(before, acc1.getBalance(), 0.001);
    }

    @Test
    @DisplayName("Failed transfer due to insufficient funds leaves both balances unchanged")
    void testFailedTransferLeavesBalancesUnchanged() {
        double bal1Before = acc1.getBalance();
        double bal2Before = acc2.getBalance();

        // Account 1 has 5,000; attempting to transfer 6,000 should fail
        assertThrows(InsufficientFundsException.class,
                () -> svc.transfer(acc1.getAccountId(), acc2.getAccountId(), 6_000.0));

        assertEquals(bal1Before, acc1.getBalance(), 0.001, "Source balance must not change on failure");
        assertEquals(bal2Before, acc2.getBalance(), 0.001, "Destination balance must not change on failure");

        // Verify that a FAILED transaction record was logged in the database audit
        List<Transaction> txs = svc.getTransactions(acc1.getAccountId());
        boolean hasFailedTx = txs.stream().anyMatch(t -> "FAILED".equalsIgnoreCase(t.getStatus()));
        assertTrue(hasFailedTx, "Audit log must record the failed transaction attempt");
    }

    @Test
    @DisplayName("Successful transfer moves funds atomically and records transactions")
    void testSuccessfulTransferAtomicity() throws Exception {
        svc.transfer(acc1.getAccountId(), acc2.getAccountId(), 1_000.0);

        assertEquals(4_000.0, acc1.getBalance(), 0.001);
        assertEquals(4_000.0, acc2.getBalance(), 0.001);

        List<Transaction> txs1 = svc.getTransactions(acc1.getAccountId());
        List<Transaction> txs2 = svc.getTransactions(acc2.getAccountId());

        assertTrue(txs1.stream().anyMatch(t -> "TRANSFER_OUT".equals(t.getType()) && "SUCCESS".equals(t.getStatus())));
        assertTrue(txs2.stream().anyMatch(t -> "TRANSFER_IN".equals(t.getType()) && "SUCCESS".equals(t.getStatus())));
    }

    @Test
    @DisplayName("Account locking blocks deposits and withdrawals until unlocked")
    void testAccountLockingAndUnlocking() throws Exception {
        svc.lockAccount(acc1.getAccountId());
        assertTrue(acc1.isLocked());

        // Attempting deposit must throw AccountLockedException
        assertThrows(AccountLockedException.class,
                () -> svc.deposit(acc1.getAccountId(), 500.0));

        // Attempting withdraw must throw AccountLockedException
        assertThrows(AccountLockedException.class,
                () -> svc.withdraw(acc1.getAccountId(), 200.0));

        // Unlock account and verify normal operations resume
        svc.unlockAccount(acc1.getAccountId());
        assertFalse(acc1.isLocked());

        svc.deposit(acc1.getAccountId(), 500.0);
        assertEquals(5_500.0, acc1.getBalance(), 0.001);
    }

    @Test
    @DisplayName("Enforce daily transaction limits on withdrawals")
    void testDailyTransactionLimit() throws Exception {
        // Set daily limit to 2,000 on acc1
        svc.setDailyLimit(acc1.getAccountId(), 2_000.0);

        // First withdrawal of 1,500 succeeds
        svc.withdraw(acc1.getAccountId(), 1_500.0);
        assertEquals(3_500.0, acc1.getBalance(), 0.001);

        // Second withdrawal of 1,000 would exceed daily limit (1,500 + 1,000 = 2,500 > 2,000)
        assertThrows(InsufficientFundsException.class,
                () -> svc.withdraw(acc1.getAccountId(), 1_000.0));

        assertEquals(3_500.0, acc1.getBalance(), 0.001, "Balance must not change when limit exceeded");
    }

    @Test
    @DisplayName("Data persists across database connection restarts")
    void testDatabasePersistenceAcrossRestarts() throws Exception {
        String dbPath = tempDir.resolve("restart_test.db").toAbsolutePath().toString();

        int savedCustId;
        int savedAccId;

        // Session 1: Create customer, account, and deposit funds
        try (DatabaseManager db1 = new DatabaseManager("jdbc:sqlite:" + dbPath)) {
            BankService s1 = new BankService(db1);
            Customer c = s1.createCustomer("Charlie Brown", "charlie@example.com", "9988776655",
                    new Address("32 Market Rd", "Delhi", "DL", "110001"));
            savedCustId = c.getCustomerId();

            SavingsAccount a = s1.createSavingsAccount(savedCustId, 7_500.0);
            savedAccId = a.getAccountId();
            s1.deposit(savedAccId, 2_500.0);
            assertEquals(10_000.0, a.getBalance(), 0.001);
        }

        // Session 2: Re-open the database from the same file
        try (DatabaseManager db2 = new DatabaseManager("jdbc:sqlite:" + dbPath)) {
            Customer loadedCust = db2.findCustomerById(savedCustId).orElse(null);
            assertNotNull(loadedCust, "Customer must survive database restart");
            assertEquals("Charlie Brown", loadedCust.getName());

            Account loadedAcc = db2.findAccountById(savedAccId).orElse(null);
            assertNotNull(loadedAcc, "Account must survive database restart");
            assertEquals(10_000.0, loadedAcc.getBalance(), 0.001, "Account balance must survive database restart");

            // Verify transactions also survived
            List<Transaction> txs = db2.getTransactionsForAccount(savedAccId);
            assertFalse(txs.isEmpty(), "Transactions must survive database restart");
        }
    }

    @Test
    @DisplayName("PBKDF2 authentication, password verification, and lockout after 5 failed attempts")
    void testAuthAndBruteForceLockout() throws Exception {
        AuthService auth = svc.getAuthService();

        // 1. Register a customer user
        auth.registerCustomerUser(cust1.getCustomerId(), "alice_user", "MySecurePass123!");

        // 2. Correct credentials succeed
        User user = auth.login("alice_user", "MySecurePass123!");
        assertNotNull(user);
        assertEquals(cust1.getCustomerId(), user.getCustomerId());
        assertFalse(user.isLocked());

        // 3. Four incorrect attempts warn with remaining attempts
        for (int i = 1; i <= 4; i++) {
            assertThrows(BankException.class, () -> auth.login("alice_user", "WrongPass!"));
        }

        // 4. Fifth incorrect attempt locks the account
        BankException ex = assertThrows(BankException.class,
                () -> auth.login("alice_user", "WrongPass!"));
        assertTrue(ex.getMessage().contains("locked") || ex.getErrorCode().contains("LOCKED"));

        // 5. Subsequent attempt is blocked immediately
        assertThrows(BankException.class,
                () -> auth.login("alice_user", "MySecurePass123!"));
    }

    @Test
    @DisplayName("First-time administrator initialization")
    void testFirstAdminInitialization() throws Exception {
        String dbPath = tempDir.resolve("admin_test.db").toAbsolutePath().toString();
        try (DatabaseManager dbAdmin = new DatabaseManager("jdbc:sqlite:" + dbPath)) {
            AuthService auth = new AuthService(dbAdmin);

            assertFalse(auth.hasAdminUser(), "Fresh database must not have an admin");

            auth.createFirstAdmin("sysadmin", "SuperAdminPass123!");
            assertTrue(auth.hasAdminUser(), "Admin must now exist");

            User admin = auth.login("sysadmin", "SuperAdminPass123!");
            assertNotNull(admin);
            assertTrue(admin.isAdmin());
            assertNull(admin.getCustomerId(), "Admin is not bound to a customer ID");
        }
    }

    @Test
    @DisplayName("Customer authorization isolation: customer only accesses their own accounts")
    void testCustomerAccountIsolation() {
        List<Account> aliceAccounts = svc.getAccountsForCustomer(cust1.getCustomerId());
        List<Account> bobAccounts   = svc.getAccountsForCustomer(cust2.getCustomerId());

        assertEquals(1, aliceAccounts.size());
        assertEquals(acc1.getAccountId(), aliceAccounts.get(0).getAccountId());

        assertEquals(1, bobAccounts.size());
        assertEquals(acc2.getAccountId(), bobAccounts.get(0).getAccountId());

        // Ensure Alice's account list does not contain Bob's account
        assertFalse(aliceAccounts.contains(acc2));
        assertFalse(bobAccounts.contains(acc1));
    }

    @Test
    @DisplayName("Transaction search and date/type filtering")
    void testTransactionSearchAndFiltering() throws Exception {
        svc.deposit(acc1.getAccountId(), 1_000.0);
        svc.withdraw(acc1.getAccountId(), 500.0);

        List<Transaction> deposits = svc.filterTransactions(acc1.getAccountId(), "DEPOSIT", LocalDate.now(), LocalDate.now());
        List<Transaction> withdrawals = svc.filterTransactions(acc1.getAccountId(), "WITHDRAW", LocalDate.now(), LocalDate.now());

        assertFalse(deposits.isEmpty(), "Must find deposit transactions");
        assertEquals("DEPOSIT", deposits.get(0).getType());

        assertFalse(withdrawals.isEmpty(), "Must find withdrawal transactions");
        assertEquals("WITHDRAW", withdrawals.get(0).getType());
    }

    @Test
    @DisplayName("Export account statement to CSV format")
    void testCsvStatementExport() throws Exception {
        svc.deposit(acc1.getAccountId(), 1_200.0);
        svc.withdraw(acc1.getAccountId(), 300.0);

        File csvFile = tempDir.resolve("test_statement.csv").toFile();
        File exported = svc.exportStatementToCsv(acc1.getAccountId(), csvFile);

        assertTrue(exported.exists());
        assertTrue(exported.length() > 0);

        List<String> lines = Files.readAllLines(exported.toPath());
        assertFalse(lines.isEmpty());
        // Header verification
        assertTrue(lines.get(0).contains("Transaction ID") && lines.get(0).contains("Amount"));
        // At least header + open + deposit + withdraw lines
        assertTrue(lines.size() >= 4);
    }
}
