// Lab 1 – Main entry point with looping menu and switch expression
// Lab 14 – Fully wired to BankService; load on start, save on exit
import exception.*;
import model.*;
import service.AuthService;
import service.BankService;
import service.TransactionProcessor;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * MiniBank – Console Banking Application.
 * Roll No: 25ce014
 *
 * Professional Enhancement:
 *   - Role-based separation: Customer Portal vs Administrator Portal
 *   - Secure PBKDF2 authentication with brute-force lockout
 *   - Daily transaction limit enforcement
 *   - Account locking & unlocking
 *   - Transaction search, filtering, and CSV export
 *   - Financial summary reports & database backup
 *   - Confirmation before destructive actions
 *   - Quick Banking Terminal retaining the classic 11-option Lab 1 switch expression
 */
public class MiniBank {

    private static final BankInfo BANK = new BankInfo(
            "MiniBank", "Main Branch", "MINI0001234");

    private static final BankService svc = new BankService();
    private static final Scanner sc = new Scanner(System.in);

    private static final String THIN  = "─".repeat(50);
    private static final String THICK = "═".repeat(50);

    public static void main(String[] args) {
        System.out.println(THICK);
        System.out.println("  Welcome to " + BANK.name());
        System.out.println("  " + BANK);
        System.out.println(THICK);

        svc.loadData(); // Load saved database & legacy records on startup

        boolean running = true;
        while (running) {
            printGatewayMenu();
            int choice = readIntSafe("  Choice: ");
            switch (choice) {
                case 1 -> customerLoginFlow();
                case 2 -> adminLoginFlow();
                case 3 -> firstTimeAdminSetup();
                case 4 -> runQuickBankingTerminal();
                case 5 -> running = exitApplication();
                default -> System.out.println("  Please choose a valid option (1 - 5).");
            }
        }
    }

    // ── Main Gateway Menu ────────────────────────────────────────────────────
    private static void printGatewayMenu() {
        System.out.println("\n" + THIN);
        System.out.println("               MINIBANK MAIN PORTAL");
        System.out.println(THIN);
        System.out.println("   1.  Customer Login");
        System.out.println("   2.  Administrator Login");
        System.out.println("   3.  First-Time Setup (Create Administrator)");
        System.out.println("   4.  Quick Banking Terminal (Classic Menu)");
        System.out.println("   5.  Exit Application");
        System.out.println(THIN);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 1. CUSTOMER PORTAL & DASHBOARD
    // ═════════════════════════════════════════════════════════════════════════
    private static void customerLoginFlow() {
        System.out.println("\n" + THIN);
        System.out.println("                 CUSTOMER LOGIN");
        System.out.println(THIN);

        String username = readString("  Username: ");
        String password = readString("  Password: ");

        try {
            User user = svc.getAuthService().login(username, password);
            if (!user.isCustomer() && !user.isAdmin()) {
                System.out.println("  Access denied: Account is not authorized as Customer.");
                return;
            }
            if (user.getCustomerId() == null) {
                System.out.println("  Notice: This user is not linked to a customer record.");
                return;
            }
            System.out.println("\n  ✔ Login successful! Welcome, " + user.getUsername() + ".");
            runCustomerDashboard(user);
        } catch (BankException e) {
            System.out.println("  Login Error: " + e.getMessage());
        }
    }

    private static void runCustomerDashboard(User user) {
        int custId = user.getCustomerId();
        boolean active = true;

        while (active) {
            System.out.println("\n" + THIN);
            System.out.println("         CUSTOMER DASHBOARD (" + user.getUsername() + ")");
            System.out.println(THIN);
            System.out.println("   1.  View My Accounts & Balances");
            System.out.println("   2.  Deposit Funds");
            System.out.println("   3.  Withdraw Funds");
            System.out.println("   4.  Transfer Funds");
            System.out.println("   5.  View Account Statement");
            System.out.println("   6.  Search & Filter My Transactions");
            System.out.println("   7.  Export Statement to CSV");
            System.out.println("   8.  Change Password");
            System.out.println("   9.  Logout");
            System.out.println(THIN);

            int choice = readIntSafe("  Choice: ");
            try {
                switch (choice) {
                    case 1 -> viewCustomerAccounts(custId);
                    case 2 -> customerDeposit(custId);
                    case 3 -> customerWithdraw(custId);
                    case 4 -> customerTransfer(custId);
                    case 5 -> customerStatement(custId);
                    case 6 -> customerFilterTransactions(custId);
                    case 7 -> customerExportCsv(custId);
                    case 8 -> changePasswordFlow(user);
                    case 9 -> {
                        System.out.println("  Logged out successfully.");
                        active = false;
                    }
                    default -> System.out.println("  Please choose an option between 1 and 9.");
                }
            } catch (BankException e) {
                System.out.println("  Error: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("  Unexpected Error: " + e.getMessage());
            }
        }
    }

    private static void viewCustomerAccounts(int customerId) {
        List<Account> myAccounts = svc.getAccountsForCustomer(customerId);
        if (myAccounts.isEmpty()) {
            System.out.println("  No accounts found for your customer profile.");
            return;
        }
        System.out.println("\n  Your Bank Accounts:");
        System.out.printf("  %-6s  %-14s  %-13s  %12s  %-8s%n",
                "Acc-ID", "Account No", "Type", "Balance (₹)", "Status");
        System.out.println("  " + "-".repeat(58));
        for (Account a : myAccounts) {
            System.out.printf("  %-6d  %-14s  %-13s  %12.2f  %-8s%n",
                    a.getAccountId(), a.getAccountNumber(), a.getAccountType(),
                    a.getBalance(), a.isLocked() ? "LOCKED" : "ACTIVE");
        }
    }

    private static void customerDeposit(int customerId) throws BankException {
        Account acc = selectCustomerAccount(customerId);
        if (acc == null) return;

        double amount = readDouble("  Deposit Amount (₹): ");
        svc.deposit(acc.getAccountId(), amount);
    }

    private static void customerWithdraw(int customerId) throws BankException {
        Account acc = selectCustomerAccount(customerId);
        if (acc == null) return;

        double amount = readDouble("  Withdraw Amount (₹): ");
        svc.withdraw(acc.getAccountId(), amount);
    }

    private static void customerTransfer(int customerId) throws BankException {
        System.out.println("  Select source account:");
        Account from = selectCustomerAccount(customerId);
        if (from == null) return;

        int toId = readInt("  Destination Account ID: ");
        if (from.getAccountId() == toId) {
            System.out.println("  Error: Source and destination accounts must be different.");
            return;
        }

        double amount = readDouble("  Transfer Amount (₹): ");
        svc.transfer(from.getAccountId(), toId, amount);
    }

    private static void customerStatement(int customerId) throws BankException {
        Account acc = selectCustomerAccount(customerId);
        if (acc == null) return;

        svc.printStatement(acc.getAccountId());
    }

    private static void customerFilterTransactions(int customerId) {
        Account acc = selectCustomerAccount(customerId);
        if (acc == null) return;

        System.out.println("  Filter by Type (DEPOSIT, WITHDRAW, TRANSFER_IN, TRANSFER_OUT, or ALL):");
        String type = readString("  Type: ").toUpperCase();
        LocalDate fromDate = readDateOptional("  Start Date (YYYY-MM-DD or blank): ");
        LocalDate toDate   = readDateOptional("  End Date   (YYYY-MM-DD or blank): ");

        List<Transaction> list = svc.filterTransactions(acc.getAccountId(), type, fromDate, toDate);
        if (list.isEmpty()) {
            System.out.println("  No transactions matched your criteria.");
            return;
        }

        System.out.println("\n  Matching Transactions:");
        System.out.println("  " + THIN);
        list.forEach(t -> System.out.println("  " + t));
        System.out.printf("  Total: %d transaction(s)%n", list.size());
    }

    private static void customerExportCsv(int customerId) {
        Account acc = selectCustomerAccount(customerId);
        if (acc == null) return;

        String filename = "statement_" + acc.getAccountNumber() + ".csv";
        File file = new File(filename);
        try {
            svc.exportStatementToCsv(acc.getAccountId(), file);
            System.out.println("\n  ✔ Statement successfully exported to: " + file.getAbsolutePath());
        } catch (Exception e) {
            System.out.println("  CSV Export Failed: " + e.getMessage());
        }
    }

    private static Account selectCustomerAccount(int customerId) {
        List<Account> myAccounts = svc.getAccountsForCustomer(customerId);
        if (myAccounts.isEmpty()) {
            System.out.println("  You do not have any registered bank accounts.");
            return null;
        }
        viewCustomerAccounts(customerId);
        int accId = readInt("  Enter Account ID: ");
        return myAccounts.stream()
                .filter(a -> a.getAccountId() == accId)
                .findFirst()
                .orElseGet(() -> {
                    System.out.println("  Error: Account ID not found among your accounts.");
                    return null;
                });
    }

    private static void changePasswordFlow(User user) {
        System.out.println("\n--- Change Password ---");
        String oldPass = readString("  Current Password : ");
        String newPass = readString("  New Password     : ");
        String confirm = readString("  Confirm Password : ");

        if (!newPass.equals(confirm)) {
            System.out.println("  Error: Passwords do not match.");
            return;
        }

        try {
            svc.getAuthService().changePassword(user.getUsername(), oldPass, newPass);
            System.out.println("  ✔ Password updated successfully!");
        } catch (BankException e) {
            System.out.println("  Error: " + e.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 2. ADMINISTRATOR PORTAL & DASHBOARD
    // ═════════════════════════════════════════════════════════════════════════
    private static void adminLoginFlow() {
        System.out.println("\n" + THIN);
        System.out.println("             ADMINISTRATOR LOGIN");
        System.out.println(THIN);

        if (!svc.getAuthService().hasAdminUser()) {
            System.out.println("  No administrator account found in the system!");
            System.out.println("  Please choose Option 3 (First-Time Setup) from the main menu.");
            return;
        }

        String username = readString("  Admin Username: ");
        String password = readString("  Admin Password: ");

        try {
            User admin = svc.getAuthService().login(username, password);
            if (!admin.isAdmin()) {
                System.out.println("  Access denied: User does not possess ADMINISTRATOR privileges.");
                return;
            }
            System.out.println("\n  ✔ Admin authentication granted. Welcome, " + admin.getUsername() + "!");
            runAdminDashboard(admin);
        } catch (BankException e) {
            System.out.println("  Login Error: " + e.getMessage());
        }
    }

    private static void runAdminDashboard(User admin) {
        boolean active = true;
        while (active) {
            System.out.println("\n" + THIN);
            System.out.println("         ADMINISTRATOR COMMAND CENTER");
            System.out.println(THIN);
            System.out.println("   1.  Create Customer (and issue user login)");
            System.out.println("   2.  Create Bank Account");
            System.out.println("   3.  List All Customers & Accounts");
            System.out.println("   4.  Lock Account");
            System.out.println("   5.  Unlock Account");
            System.out.println("   6.  Set Account Daily Limit");
            System.out.println("   7.  Apply Interest (All Eligible Accounts)");
            System.out.println("   8.  Financial Summary Report");
            System.out.println("   9.  Search Transaction Audit Log");
            System.out.println("  10.  Backup Database");
            System.out.println("  11.  Delete Account (Destructive)");
            System.out.println("  12.  Unlock Locked User Account");
            System.out.println("  13.  Logout");
            System.out.println(THIN);

            int choice = readIntSafe("  Choice: ");
            try {
                switch (choice) {
                    case 1 -> createCustomerWithLogin();
                    case 2 -> createAccount();
                    case 3 -> listAccounts();
                    case 4 -> lockAccountFlow();
                    case 5 -> unlockAccountFlow();
                    case 6 -> setDailyLimitFlow();
                    case 7 -> svc.applyInterestToAllEligible();
                    case 8 -> svc.printFinancialSummaryReport();
                    case 9 -> searchTransactionFlow();
                    case 10 -> backupDatabaseFlow();
                    case 11 -> deleteAccountWithConfirmation();
                    case 12 -> unlockUserFlow();
                    case 13 -> {
                        System.out.println("  Admin session ended.");
                        active = false;
                    }
                    default -> System.out.println("  Please choose an option between 1 and 13.");
                }
            } catch (BankException e) {
                System.out.println("  Error: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("  Unexpected Error: " + e.getMessage());
            }
        }
    }

    private static void createCustomerWithLogin() throws BankException {
        createCustomer();
        System.out.println("\n  Optional: Set up customer online login credentials?");
        System.out.print("  Create user account now? (y/N): ");
        String ans = sc.nextLine().strip();
        if (ans.equalsIgnoreCase("y") || ans.equalsIgnoreCase("yes")) {
            int custId = readInt("  Customer ID: ");
            String user = readString("  Desired Username: ");
            String pass = readString("  Temporary Password: ");
            svc.getAuthService().registerCustomerUser(custId, user, pass);
            System.out.println("  ✔ Customer login created successfully for username: " + user);
        }
    }

    private static void lockAccountFlow() throws BankException {
        int accId = readInt("  Account ID to Lock: ");
        System.out.print("  Confirm locking account ID " + accId + "? (yes/no): ");
        if (!"yes".equalsIgnoreCase(sc.nextLine().strip())) {
            System.out.println("  Operation cancelled.");
            return;
        }
        svc.lockAccount(accId);
    }

    private static void unlockAccountFlow() throws BankException {
        int accId = readInt("  Account ID to Unlock: ");
        svc.unlockAccount(accId);
    }

    private static void setDailyLimitFlow() throws BankException {
        int accId = readInt("  Account ID: ");
        double limit = readDouble("  New Daily Limit (₹): ");
        svc.setDailyLimit(accId, limit);
    }

    private static void searchTransactionFlow() {
        String txId = readString("  Enter Transaction ID (e.g. TXN-12345678): ");
        Optional<Transaction> tx = svc.findTransaction(txId.trim());
        if (tx.isPresent()) {
            System.out.println("\n  ✔ Transaction Found:");
            System.out.println("  " + THIN);
            Transaction t = tx.get();
            System.out.println("  ID          : " + t.getTransactionId());
            System.out.println("  Account ID  : " + t.getAccountId());
            System.out.println("  Type        : " + t.getType());
            System.out.println("  Amount      : ₹" + String.format("%.2f", t.getAmount()));
            System.out.println("  Balance Post: ₹" + String.format("%.2f", t.getBalanceAfter()));
            System.out.println("  Timestamp   : " + t.getFormattedTimestamp());
            System.out.println("  Status      : " + t.getStatus());
            System.out.println("  Description : " + t.getDescription());
            System.out.println("  " + THIN);
        } else {
            System.out.println("  No transaction found with ID: " + txId);
        }
    }

    private static void backupDatabaseFlow() {
        String backupName = "minibank_backup_" + LocalDate.now() + ".db";
        File file = new File(backupName);
        boolean ok = svc.backupDatabase(file);
        if (ok) {
            System.out.println("\n  ✔ Database successfully backed up to: " + file.getAbsolutePath());
        } else {
            System.out.println("  Database backup failed. Ensure SQLite is active.");
        }
    }

    private static void deleteAccountWithConfirmation() throws BankException {
        int accId = readInt("  Account ID to DELETE: ");
        System.out.println("\n  WARNING: This will permanently delete Account ID " + accId + "!");
        System.out.print("  Type 'yes' to confirm permanent deletion: ");
        String confirm = sc.nextLine().strip();
        if (!"yes".equalsIgnoreCase(confirm)) {
            System.out.println("  Deletion cancelled. Account was NOT modified.");
            return;
        }
        svc.deleteAccount(accId);
    }

    private static void unlockUserFlow() throws BankException {
        String username = readString("  Username to unlock: ");
        svc.getAuthService().unlockUser(username);
        System.out.println("  ✔ User '" + username + "' has been unlocked and failed login attempts reset.");
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 3. FIRST-TIME SETUP
    // ═════════════════════════════════════════════════════════════════════════
    private static void firstTimeAdminSetup() {
        System.out.println("\n" + THIN);
        System.out.println("            FIRST-TIME ADMINISTRATOR SETUP");
        System.out.println(THIN);

        if (svc.getAuthService().hasAdminUser()) {
            System.out.println("  An administrator account already exists!");
            System.out.println("  Please use Option 2 from the main menu to login.");
            return;
        }

        System.out.println("  Create your initial primary administrator account:");
        String username = readString("  Admin Username : ");
        String password = readString("  Admin Password : ");
        String confirm  = readString("  Confirm Pass   : ");

        if (!password.equals(confirm)) {
            System.out.println("  Error: Passwords do not match. Setup aborted.");
            return;
        }

        try {
            svc.getAuthService().createFirstAdmin(username, password);
            System.out.println("\n  ✔ Administrator created successfully!");
            System.out.println("  You may now login using Option 2 on the main menu.");
        } catch (BankException e) {
            System.out.println("  Setup Error: " + e.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 4. QUICK BANKING TERMINAL (Classic 11-Option Menu)
    // ═════════════════════════════════════════════════════════════════════════
    private static void runQuickBankingTerminal() {
        boolean active = true;
        while (active) {
            printClassicMenu();
            int choice = readIntSafe("  Choice: ");
            if (choice == -1) {
                System.out.println("  Please enter a number between 1 and " + MenuOption.values().length + ".");
                continue;
            }

            try {
                // Lab 1 – switch expression with MenuOption enum
                active = switch (MenuOption.fromIndex(choice)) {
                    case CREATE_CUSTOMER  -> createCustomer();
                    case CREATE_ACCOUNT   -> createAccount();
                    case DEPOSIT          -> deposit();
                    case WITHDRAW         -> withdraw();
                    case VIEW_BALANCE     -> viewBalance();
                    case VIEW_STATEMENT   -> viewStatement();
                    case TRANSFER         -> transfer();
                    case APPLY_INTEREST   -> applyInterest();
                    case LIST_ACCOUNTS    -> listAccounts();
                    case DELETE_ACCOUNT   -> deleteAccountWithConfirmationClassic();
                    case EXIT             -> false; // returns to Main Portal
                };
            } catch (IllegalArgumentException e) {
                System.out.println("  Invalid choice – " + e.getMessage());
            } catch (BankException e) {
                System.out.println("  Error: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("  Unexpected Error: " + e.getMessage());
            }
        }
    }

    private static void printClassicMenu() {
        System.out.println("\n" + THIN);
        System.out.println("         QUICK BANKING TERMINAL (LAB 1 - 14)");
        System.out.println(THIN);
        MenuOption[] opts = MenuOption.values();
        for (int i = 0; i < opts.length; i++) {
            System.out.printf("  %2d.  %s%n", i + 1, opts[i].getLabel());
        }
        System.out.println(THIN);
    }

    // ── Classic Handlers ─────────────────────────────────────────────────────
    private static boolean createCustomer() throws BankException {
        System.out.println("\n" + THIN);
        System.out.println("             CREATE CUSTOMER");
        System.out.println(THIN);
        System.out.println("  (Customer ID is generated automatically)\n");

        String name   = readString("  Full name   : ");
        String email  = readString("  Email       : ");
        String phone  = readString("  Phone (10-d): ");
        String street = readString("  Street      : ");
        String city   = readString("  City        : ");
        String state  = readString("  State       : ");
        String pin    = readString("  Pincode     : ");

        Address addr = new Address(street, city, state, pin);
        Customer c   = svc.createCustomer(name, email, phone, addr);

        System.out.println("\n  ✔  Customer created successfully!");
        System.out.println(THIN);
        System.out.printf ("  Your Customer ID  :  %d%n", c.getCustomerId());
        System.out.printf ("  Name              :  %s%n", c.getName());
        System.out.printf ("  Email             :  %s%n", c.getEmail());
        System.out.printf ("  Phone             :  %s%n", c.getPhone());
        System.out.println(THIN);
        System.out.println("  IMPORTANT: Save your Customer ID to open bank accounts.");
        System.out.println(THIN);
        return true;
    }

    private static boolean createAccount() throws BankException {
        System.out.println("\n" + THIN);
        System.out.println("              CREATE ACCOUNT");
        System.out.println(THIN);

        if (!svc.hasCustomers()) {
            System.out.println("  No customers registered yet!");
            System.out.println("  Please create a customer first (Option 1).");
            return true;
        }

        System.out.println("  Registered Customers:");
        svc.allCustomers().stream()
                .sorted(java.util.Comparator.comparingInt(Customer::getCustomerId))
                .forEach(cu -> System.out.printf("    ID %-6d  %s%n", cu.getCustomerId(), cu.getName()));
        System.out.println();

        int customerId = readInt("  Customer ID : ");
        if (svc.findCustomer(customerId).isEmpty()) {
            System.out.printf("%n  Error: No customer found with ID: %d%n", customerId);
            System.out.println("  Please enter a valid Customer ID.");
            return true;
        }

        System.out.println("  Account type:");
        System.out.println("    1  Savings        (min balance ₹500, 4% interest)");
        System.out.println("    2  Current        (overdraft facility up to ₹10,000)");
        System.out.println("    3  Fixed Deposit  (7% interest, locked until maturity)");
        int type = readInt("  Type (1/2/3): ");

        double balance = readDouble("  Initial balance (₹): ");

        Account acc = switch (type) {
            case 1 -> svc.createSavingsAccount(customerId, balance);
            case 2 -> svc.createCurrentAccount(customerId, balance);
            case 3 -> {
                int months = readInt("  Tenure (months): ");
                yield svc.createFixedDeposit(customerId, balance, months);
            }
            default -> {
                System.out.println("  Invalid account type.");
                yield null;
            }
        };

        if (acc != null) {
            System.out.println("\n  ✔  Account created successfully!");
            System.out.println(THIN);
            System.out.printf ("  Account ID     :  %d%n",   acc.getAccountId());
            System.out.printf ("  Account Number :  %s%n",   acc.getAccountNumber());
            System.out.printf ("  Account Type   :  %s%n",   acc.getAccountType());
            System.out.printf ("  Customer ID    :  %d%n",   acc.getCustomerId());
            System.out.printf ("  Balance        :  ₹%,.2f%n", acc.getBalance());
            System.out.println(THIN);
        }
        return true;
    }

    private static boolean deposit() throws BankException {
        if (!svc.hasAccounts()) { noAccountsGuidance(); return true; }
        int accountId = readInt("  Account ID : ");
        double amount = readDouble("  Amount (₹) : ");
        svc.deposit(accountId, amount);
        return true;
    }

    private static boolean withdraw() throws BankException {
        if (!svc.hasAccounts()) { noAccountsGuidance(); return true; }
        int accountId = readInt("  Account ID : ");
        double amount = readDouble("  Amount (₹) : ");
        svc.withdraw(accountId, amount);
        return true;
    }

    private static boolean viewBalance() throws BankException {
        if (!svc.hasAccounts()) { noAccountsGuidance(); return true; }
        int accountId = readInt("  Account ID : ");
        double bal = svc.getBalance(accountId);
        System.out.printf("%n  Balance: ₹%,.2f%n", bal);
        return true;
    }

    private static boolean viewStatement() throws BankException {
        if (!svc.hasAccounts()) { noAccountsGuidance(); return true; }
        int accountId = readInt("  Account ID : ");
        svc.printStatement(accountId);
        return true;
    }

    private static boolean transfer() throws BankException {
        if (!svc.hasAccounts()) { noAccountsGuidance(); return true; }
        int fromId = readInt("  From Account ID : ");
        int toId   = readInt("  To   Account ID : ");
        if (fromId == toId) {
            System.out.println("  Error: Source and destination accounts must be different.");
            return true;
        }
        double amount = readDouble("  Amount (₹)      : ");
        svc.transfer(fromId, toId, amount);
        return true;
    }

    private static boolean applyInterest() throws BankException {
        if (!svc.hasAccounts()) { noAccountsGuidance(); return true; }
        int accountId = readInt("  Account ID : ");
        svc.applyInterest(accountId);
        return true;
    }

    private static boolean listAccounts() {
        svc.listAllAccounts();
        return true;
    }

    private static boolean deleteAccountWithConfirmationClassic() throws BankException {
        if (!svc.hasAccounts()) { noAccountsGuidance(); return true; }
        int accountId = readInt("  Account ID : ");
        System.out.print("  Are you sure you want to delete Account ID " + accountId + "? (yes/no): ");
        String confirm = sc.nextLine().strip();
        if (!"yes".equalsIgnoreCase(confirm)) {
            System.out.println("  Deletion cancelled.");
            return true;
        }
        svc.deleteAccount(accountId);
        return true;
    }

    private static boolean exitApplication() {
        svc.saveData();
        System.out.println("\n  Goodbye from " + BANK.name() + "!");
        System.out.println("  All account and customer data securely preserved.\n");
        return false;
    }

    private static void noAccountsGuidance() {
        System.out.println("  No accounts available yet.");
        System.out.println("  Please create a customer first (Option 1), then create an account (Option 2).");
    }

    // ── Input Utilities ──────────────────────────────────────────────────────
    private static String readString(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().strip();
    }

    private static int readInt(String prompt) {
        System.out.print(prompt);
        while (true) {
            String line = sc.nextLine().strip();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.print("  Please enter a valid whole number – " + prompt);
            }
        }
    }

    private static int readIntSafe(String prompt) {
        System.out.print(prompt);
        String line = sc.nextLine().strip();
        try {
            return Integer.parseInt(line);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static double readDouble(String prompt) {
        System.out.print(prompt);
        while (true) {
            String line = sc.nextLine().strip();
            try {
                double v = Double.parseDouble(line);
                if (v <= 0) {
                    System.out.print("  Amount must be positive – " + prompt);
                    continue;
                }
                return v;
            } catch (NumberFormatException e) {
                System.out.print("  Please enter a valid amount – " + prompt);
            }
        }
    }

    private static LocalDate readDateOptional(String prompt) {
        System.out.print(prompt);
        String line = sc.nextLine().strip();
        if (line.isBlank()) return null;
        try {
            return LocalDate.parse(line, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            System.out.println("  Invalid date format. Ignoring date filter.");
            return null;
        }
    }

    // ── Lab 9/10 Concurrency Demo Runner ─────────────────────────────────────
    static {
        if (System.getProperty("minibank.demo") != null) {
            runConcurrencyDemo();
        }
    }

    private static void runConcurrencyDemo() {
        System.out.println("\n=== Concurrency Demo ===");
        try {
            int tc = 10, itr = 1000;
            System.out.println("Race condition result (expected " + (tc * itr) + "): "
                    + TransactionProcessor.raceConditionDemo(tc, itr));
            System.out.println("Synchronized result  (expected " + (tc * itr) + "): "
                    + TransactionProcessor.synchronizedCounterDemo(tc, itr));
            TransactionProcessor.producerConsumerDemo(5);
            TransactionProcessor.deadlockFixed();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
