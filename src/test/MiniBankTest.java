// Lab 14 – JUnit 5 tests
// Updated: added 8 new guard / ID-visibility / navigation scenarios (§7 of spec)
import exception.*;
import model.*;
import service.BankService;
import util.Validator;
import util.AnnotationValidator;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MiniBank.
 * Lab 14: demonstrates JUnit 5 annotations and assertions.
 *
 * NEW (§7): scenarios 1-8 verify the guard flows and ID visibility
 * introduced in Phase 2.
 */
@DisplayName("MiniBank Unit Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MiniBankTest {

    private BankService svc;
    private Customer    customer;
    private int         customerId;
    private int         savingsId;

    @BeforeEach
    void setUp() throws Exception {
        svc = new BankService();
        customer = svc.createCustomer(
                "Shakshi Chaudhary", "shakshi@example.com", "9876543210",
                new Address("12 MG Road", "Ahmedabad", "Gujarat", "380001"));
        customerId = customer.getCustomerId();
        SavingsAccount sa = svc.createSavingsAccount(customerId, 10_000.0);
        savingsId = sa.getAccountId();
    }

    // ════════════════════════════════════════════════════════════════════════
    // §7 Scenario 1: creating a customer auto-generates a unique ID
    // ════════════════════════════════════════════════════════════════════════
    @Test @Order(1)
    @DisplayName("§7-1  Customer creation auto-generates a unique positive ID")
    void testCustomerAutoId() throws Exception {
        Customer c2 = svc.createCustomer(
                "Riya Patel", "riya@example.com", "8765432109",
                new Address("5 Ring Road", "Surat", "Gujarat", "395001"));

        // both IDs must be positive and distinct
        assertTrue(customer.getCustomerId() > 0,  "ID must be positive");
        assertTrue(c2.getCustomerId() > 0,         "Second ID must be positive");
        assertNotEquals(customer.getCustomerId(), c2.getCustomerId(),
                "IDs must be unique");
    }

    // ════════════════════════════════════════════════════════════════════════
    // §7 Scenario 2: generated Customer ID can immediately open an account
    // ════════════════════════════════════════════════════════════════════════
    @Test @Order(2)
    @DisplayName("§7-2  Freshly generated Customer ID works for account creation")
    void testFreshIdCanCreateAccount() throws Exception {
        Customer fresh = svc.createCustomer(
                "Kiran Mehta", "kiran@example.com", "9012345678",
                new Address("7 Canal Rd", "Baroda", "Gujarat", "390001"));

        SavingsAccount acc = svc.createSavingsAccount(fresh.getCustomerId(), 2_000.0);
        assertNotNull(acc);
        assertEquals(fresh.getCustomerId(), acc.getCustomerId());
        assertTrue(acc.getAccountId() > 0);
    }

    // ════════════════════════════════════════════════════════════════════════
    // §7 Scenario 3: account creation is rejected when no customers exist
    // ════════════════════════════════════════════════════════════════════════
    @Test @Order(3)
    @DisplayName("§7-3  Account creation with unknown ID throws AccountNotFoundException")
    void testCreateAccountNoCustomer() {
        BankService fresh = new BankService();   // empty store – no customers
        // Any customer ID tried on the empty service must throw
        assertThrows(AccountNotFoundException.class,
                () -> fresh.createSavingsAccount(9999, 5_000.0),
                "Should reject account creation when customer does not exist");
    }

    // ════════════════════════════════════════════════════════════════════════
    // §7 Scenario 4: unknown Customer ID does not create an account
    // ════════════════════════════════════════════════════════════════════════
    @Test @Order(4)
    @DisplayName("§7-4  Unknown Customer ID is rejected – account count stays unchanged")
    void testUnknownCustomerIdNoAccountCreated() {
        int before = svc.accountCount();
        assertThrows(AccountNotFoundException.class,
                () -> svc.createSavingsAccount(99999, 1_000.0));
        assertEquals(before, svc.accountCount(),
                "No account must be created for an unknown customer");
    }

    // ════════════════════════════════════════════════════════════════════════
    // §7 Scenario 5: invalid account ID is handled safely
    // ════════════════════════════════════════════════════════════════════════
    @Test @Order(5)
    @DisplayName("§7-5  Invalid account ID on deposit throws AccountNotFoundException")
    void testInvalidAccountIdHandledSafely() {
        assertThrows(AccountNotFoundException.class,
                () -> svc.deposit(99999, 100.0),
                "Deposit on non-existent account must throw, not crash");
    }

    // ════════════════════════════════════════════════════════════════════════
    // §7 Scenario 6: valid operations (deposit, withdraw, transfer) still work
    // ════════════════════════════════════════════════════════════════════════
    @Test @Order(6)
    @DisplayName("§7-6a  Deposit increases balance correctly")
    void testDepositStillWorks() throws Exception {
        svc.deposit(savingsId, 3_000.0);
        assertEquals(13_000.0, svc.getBalance(savingsId), 0.01);
    }

    @Test @Order(6)
    @DisplayName("§7-6b  Withdraw decreases balance correctly")
    void testWithdrawStillWorks() throws Exception {
        svc.withdraw(savingsId, 1_000.0);
        assertEquals(9_000.0, svc.getBalance(savingsId), 0.01);
    }

    @Test @Order(6)
    @DisplayName("§7-6c  Transfer moves funds between two accounts")
    void testTransferStillWorks() throws Exception {
        CurrentAccount ca = svc.createCurrentAccount(customerId, 5_000.0);
        svc.transfer(savingsId, ca.getAccountId(), 2_000.0);
        assertEquals(8_000.0, svc.getBalance(savingsId), 0.01);
        assertEquals(7_000.0, svc.getBalance(ca.getAccountId()), 0.01);
    }

    // ════════════════════════════════════════════════════════════════════════
    // §7 Scenario 7: invalid / bad input does not crash
    // ════════════════════════════════════════════════════════════════════════
    @Test @Order(7)
    @DisplayName("§7-7a  Negative deposit amount throws InvalidAmountException")
    void testNegativeAmountThrows() {
        assertThrows(InvalidAmountException.class,
                () -> svc.deposit(savingsId, -100.0));
    }

    @Test @Order(7)
    @DisplayName("§7-7b  Zero amount throws InvalidAmountException")
    void testZeroAmountThrows() {
        assertThrows(InvalidAmountException.class,
                () -> svc.deposit(savingsId, 0.0));
    }

    @Test @Order(7)
    @DisplayName("§7-7c  Savings over-withdrawal throws InsufficientFundsException")
    void testOverWithdrawThrows() {
        // balance 10000, minBalance 500 → max withdraw = 9500
        assertThrows(InsufficientFundsException.class,
                () -> svc.withdraw(savingsId, 9_600.0));
    }

    @Test @Order(7)
    @DisplayName("§7-7d  Blank customer name throws ValidationException")
    void testBlankCustomerNameThrows() {
        assertThrows(ValidationException.class,
                () -> svc.createCustomer("", "a@b.com", "9000000000",
                        new Address("x", "y", "z", "123456")));
    }

    @Test @Order(7)
    @DisplayName("§7-7e  Invalid email throws ValidationException")
    void testInvalidEmailThrows() {
        assertThrows(ValidationException.class,
                () -> svc.createCustomer("Test", "not-an-email", "9000000000",
                        new Address("x", "y", "z", "123456")));
    }

    // ════════════════════════════════════════════════════════════════════════
    // §7 Scenario 8: IDs displayed after creation (return value carries them)
    // ════════════════════════════════════════════════════════════════════════
    @Test @Order(8)
    @DisplayName("§7-8a  createCustomer returns object with non-zero ID")
    void testCreateCustomerReturnsIdInObject() throws Exception {
        Customer c = svc.createCustomer(
                "Dev Joshi", "dev@example.com", "9111222333",
                new Address("A Block", "Rajkot", "Gujarat", "360001"));
        assertTrue(c.getCustomerId() > 0,
                "Returned Customer must carry the auto-generated ID");
    }

    @Test @Order(8)
    @DisplayName("§7-8b  createSavingsAccount returns object with Account ID and Account Number")
    void testCreateAccountReturnsIdAndNumber() throws Exception {
        SavingsAccount acc = svc.createSavingsAccount(customerId, 5_000.0);
        assertTrue(acc.getAccountId() > 0, "Account ID must be positive");
        assertNotNull(acc.getAccountNumber(), "Account number must not be null");
        assertFalse(acc.getAccountNumber().isBlank(), "Account number must not be blank");
    }

    @Test @Order(8)
    @DisplayName("§7-8c  Customer ID is linked correctly on the created account")
    void testAccountLinkedToCustomer() throws Exception {
        SavingsAccount acc = svc.createSavingsAccount(customerId, 8_000.0);
        assertEquals(customerId, acc.getCustomerId(),
                "Account must be linked to the customer who created it");
    }

    // ════════════════════════════════════════════════════════════════════════
    // Existing tests – preserved from original suite
    // ════════════════════════════════════════════════════════════════════════
    @Test @DisplayName("Valid email is accepted")
    void testValidEmail() {
        assertTrue(Validator.isValidEmail("shakshi@example.com"));
    }

    @Test @DisplayName("Invalid email is rejected")
    void testInvalidEmail() {
        assertFalse(Validator.isValidEmail("not-an-email"));
    }

    @Test @DisplayName("Valid Indian phone is accepted")
    void testValidPhone() {
        assertTrue(Validator.isValidPhone("9876543210"));
    }

    @Test @DisplayName("Phone starting with 0 is rejected")
    void testInvalidPhone() {
        assertFalse(Validator.isValidPhone("0876543210"));
    }

    @Test @DisplayName("Savings account created with correct balance")
    void testSavingsAccountBalance() throws Exception {
        assertEquals(10_000.0, svc.getBalance(savingsId), 0.01);
    }

    @Test @DisplayName("Current account created successfully")
    void testCurrentAccountCreated() throws Exception {
        CurrentAccount ca = svc.createCurrentAccount(customerId, 5_000.0);
        assertNotNull(ca);
        assertEquals(5_000.0, ca.getBalance(), 0.01);
    }

    @Test @DisplayName("Fixed deposit created with correct balance")
    void testFixedDepositCreated() throws Exception {
        FixedDepositAccount fd = svc.createFixedDeposit(customerId, 20_000.0, 12);
        assertNotNull(fd);
        assertEquals(20_000.0, fd.getBalance(), 0.01);
    }

    @Test @DisplayName("Unknown account throws AccountNotFoundException")
    void testUnknownAccount() {
        assertThrows(AccountNotFoundException.class,
                () -> svc.deposit(9999, 100.0));
    }

    @Test @DisplayName("Savings interest applied correctly at 4%")
    void testSavingsInterest() throws Exception {
        svc.applyInterest(savingsId);
        assertEquals(10_400.0, svc.getBalance(savingsId), 0.01);
    }

    @Test @DisplayName("FD cannot withdraw before maturity")
    void testFDWithdrawBeforeMaturity() throws Exception {
        FixedDepositAccount fd = svc.createFixedDeposit(customerId, 10_000.0, 12);
        assertThrows(InsufficientFundsException.class,
                () -> svc.withdraw(fd.getAccountId(), 1_000.0));
    }

    @Test @DisplayName("AnnotationValidator passes on valid Customer")
    void testAnnotationValidatorPass() throws Exception {
        Customer c = svc.findCustomer(customerId).orElseThrow();
        assertDoesNotThrow(() -> AnnotationValidator.validate(c));
    }

    @Test @DisplayName("BankInfo record stores fields correctly")
    void testBankInfoRecord() {
        BankInfo b = new BankInfo("MiniBank", "Main", "MINI0001234");
        assertEquals("MiniBank",    b.name());
        assertEquals("Main",        b.branch());
        assertEquals("MINI0001234", b.ifscCode());
    }

    @Test @DisplayName("BankInfo record rejects blank name")
    void testBankInfoRejectsBlankName() {
        assertThrows(IllegalArgumentException.class,
                () -> new BankInfo("", "Main", "MINI0001234"));
    }

    @Test @DisplayName("Command record parses verb and args")
    void testCommandRecord() {
        model.Command cmd = new model.Command("deposit", new String[]{"2001", "500"});
        assertEquals("deposit", cmd.verb());
        assertEquals(2, cmd.argCount());
        assertEquals("2001", cmd.arg(0));
    }

    @Test @DisplayName("Address clone is a deep copy")
    void testAddressClone() {
        Address a = new Address("12 MG Road", "Ahmedabad", "Gujarat", "380001");
        Address b = a.clone();
        assertNotSame(a, b);
        assertEquals(a, b);
        b.setCity("Surat");
        assertNotEquals(a.getCity(), b.getCity());
    }

    @Test @DisplayName("hasCustomers returns true after creation, false on fresh service")
    void testHasCustomers() {
        assertTrue(svc.hasCustomers());
        assertFalse(new BankService().hasCustomers());
    }

    @Test @DisplayName("hasAccounts returns true after creation, false on fresh service")
    void testHasAccounts() {
        assertTrue(svc.hasAccounts());
        assertFalse(new BankService().hasAccounts());
    }

    @Test @DisplayName("customerCount increments with each registration")
    void testCustomerCount() throws Exception {
        int before = svc.customerCount();
        svc.createCustomer("New User", "new@ex.com", "9800000001",
                new Address("1 A", "B", "C", "400001"));
        assertEquals(before + 1, svc.customerCount());
    }
}
