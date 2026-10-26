// Lab 14 – JUnit 5 tests
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
 */
@DisplayName("MiniBank Unit Tests")
class MiniBankTest {

    private BankService svc;
    private int customerId;
    private int savingsId;

    @BeforeEach
    void setUp() throws Exception {
        svc = new BankService();
        Customer c = svc.createCustomer(
                "Shakshi Chaudhary", "shakshi@example.com", "9876543210",
                new Address("12 MG Road", "Ahmedabad", "Gujarat", "380001"));
        customerId = c.getCustomerId();
        SavingsAccount sa = svc.createSavingsAccount(customerId, 10_000.0);
        savingsId = sa.getAccountId();
    }

    // ── Validator ──────────────────────────────────────────────────────────────
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

    // ── Account creation ───────────────────────────────────────────────────────
    @Test @DisplayName("Savings account created with correct balance")
    void testSavingsAccountCreated() throws Exception {
        double bal = svc.getBalance(savingsId);
        assertEquals(10_000.0, bal, 0.01);
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

    // ── Deposit ────────────────────────────────────────────────────────────────
    @Test @DisplayName("Deposit increases balance")
    void testDeposit() throws Exception {
        svc.deposit(savingsId, 2_000.0);
        assertEquals(12_000.0, svc.getBalance(savingsId), 0.01);
    }

    @Test @DisplayName("Negative deposit throws InvalidAmountException")
    void testNegativeDepositThrows() {
        assertThrows(InvalidAmountException.class,
                () -> svc.deposit(savingsId, -500.0));
    }

    // ── Withdraw ───────────────────────────────────────────────────────────────
    @Test @DisplayName("Withdraw decreases balance")
    void testWithdraw() throws Exception {
        svc.withdraw(savingsId, 1_000.0);
        assertEquals(9_000.0, svc.getBalance(savingsId), 0.01);
    }

    @Test @DisplayName("Savings withdraw below min balance throws InsufficientFundsException")
    void testSavingsMinBalanceEnforced() {
        // balance is 10000, min is 500 → can withdraw up to 9500
        assertThrows(InsufficientFundsException.class,
                () -> svc.withdraw(savingsId, 9_600.0));
    }

    @Test @DisplayName("Unknown account throws AccountNotFoundException")
    void testUnknownAccount() {
        assertThrows(AccountNotFoundException.class,
                () -> svc.deposit(9999, 100.0));
    }

    // ── Transfer ───────────────────────────────────────────────────────────────
    @Test @DisplayName("Transfer moves funds between accounts")
    void testTransfer() throws Exception {
        CurrentAccount ca = svc.createCurrentAccount(customerId, 5_000.0);
        svc.transfer(savingsId, ca.getAccountId(), 2_000.0);
        assertEquals(8_000.0, svc.getBalance(savingsId), 0.01);
        assertEquals(7_000.0, svc.getBalance(ca.getAccountId()), 0.01);
    }

    // ── Interest ───────────────────────────────────────────────────────────────
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

    // ── AnnotationValidator ────────────────────────────────────────────────────
    @Test @DisplayName("AnnotationValidator passes on valid Customer")
    void testAnnotationValidatorPass() throws Exception {
        Customer c = svc.findCustomer(customerId).orElseThrow();
        assertDoesNotThrow(() -> AnnotationValidator.validate(c));
    }

    // ── BankInfo record ────────────────────────────────────────────────────────
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

    // ── Command record ─────────────────────────────────────────────────────────
    @Test @DisplayName("Command record parses verb and args")
    void testCommandRecord() {
        model.Command cmd = new model.Command("deposit", new String[]{"2001", "500"});
        assertEquals("deposit", cmd.verb());
        assertEquals(2, cmd.argCount());
        assertEquals("2001", cmd.arg(0));
    }

    // ── Address clone ──────────────────────────────────────────────────────────
    @Test @DisplayName("Address clone is a deep copy")
    void testAddressClone() {
        Address a = new Address("12 MG Road", "Ahmedabad", "Gujarat", "380001");
        Address b = a.clone();
        assertNotSame(a, b);
        assertEquals(a, b);
        b.setCity("Surat");
        assertNotEquals(a.getCity(), b.getCity());
    }
}
