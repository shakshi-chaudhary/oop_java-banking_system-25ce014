// Lab 1 – Main entry point with looping menu and switch expression
// Lab 14 – Fully wired to BankService; load on start, save on exit
import model.*;
import model.BankInfo;
import model.MenuOption;
import service.BankService;
import service.TransactionProcessor;
import exception.*;

import java.util.Scanner;

/**
 * MiniBank – Console Banking Application.
 * Roll No: 25ce014
 *
 * Entry point wiring together all 14 labs.
 * Lab 1:  BankInfo record, MenuOption enum, looping menu with switch expression.
 * Lab 14: calls BankService for all real operations; load on start, save on exit.
 *
 * UX rules (as of Phase 2 fix):
 *   - Customer ID is always auto-generated; never typed by the user.
 *   - Account creation is blocked when no customers exist.
 *   - Every prerequisite failure returns safely to the main menu.
 *   - No operation crashes on a bad numeric ID.
 */
public class MiniBank {

    // Bank details (Lab 1 – record)
    private static final BankInfo BANK = new BankInfo(
            "MiniBank", "Main Branch", "MINI0001234");

    private static final BankService svc = new BankService();
    private static final Scanner sc = new Scanner(System.in);

    // Divider widths
    private static final String THIN  = "─".repeat(44);
    private static final String THICK = "═".repeat(44);

    // ─────────────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        System.out.println(THICK);
        System.out.println("  Welcome to " + BANK.name());
        System.out.println("  " + BANK);
        System.out.println(THICK);

        svc.loadData();               // Lab 11 – deserialise on start

        boolean running = true;
        while (running) {
            printMenu();
            int choice = readIntSafe("Choice: ");
            if (choice == -1) {       // non-numeric input
                System.out.println("  Please enter a number between 1 and "
                        + MenuOption.values().length + ".");
                continue;
            }

            try {
                // Lab 1 – switch expression (arrow form)
                running = switch (MenuOption.fromIndex(choice)) {
                    case CREATE_CUSTOMER  -> createCustomer();
                    case CREATE_ACCOUNT   -> createAccount();
                    case DEPOSIT          -> deposit();
                    case WITHDRAW         -> withdraw();
                    case VIEW_BALANCE     -> viewBalance();
                    case VIEW_STATEMENT   -> viewStatement();
                    case TRANSFER         -> transfer();
                    case APPLY_INTEREST   -> applyInterest();
                    case LIST_ACCOUNTS    -> listAccounts();
                    case DELETE_ACCOUNT   -> deleteAccount();
                    case EXIT             -> exit();
                };
            } catch (IllegalArgumentException e) {
                System.out.println("  Invalid choice – " + e.getMessage());
            } catch (BankException e) {
                System.out.println("  Error: " + e);
            } catch (Exception e) {
                System.out.println("  Unexpected error: " + e.getMessage());
            }
        }
    }

    // ── Menu display ──────────────────────────────────────────────────────────
    private static void printMenu() {
        System.out.println("\n" + THIN);
        System.out.println("             MINIBANK MENU");
        System.out.println(THIN);
        MenuOption[] opts = MenuOption.values();
        for (int i = 0; i < opts.length; i++) {
            System.out.printf("  %2d.  %s%n", i + 1, opts[i].getLabel());
        }
        System.out.println(THIN);
    }

    // ── 1. Create Customer ────────────────────────────────────────────────────
    private static boolean createCustomer() throws BankException {
        System.out.println("\n" + THIN);
        System.out.println("         CREATE CUSTOMER");
        System.out.println(THIN);
        System.out.println("  (Customer ID is generated automatically)\n");

        String name  = readString("  Full name   : ");
        String email = readString("  Email       : ");
        String phone = readString("  Phone (10-d): ");
        String street= readString("  Street      : ");
        String city  = readString("  City        : ");
        String state = readString("  State       : ");
        String pin   = readString("  Pincode     : ");

        Address addr = new Address(street, city, state, pin);
        Customer c   = svc.createCustomer(name, email, phone, addr);

        // ── Success banner ────────────────────────────────────────────────────
        System.out.println("\n  ✔  Customer created successfully!");
        System.out.println(THIN);
        System.out.printf ("  Your Customer ID  :  %d%n", c.getCustomerId());
        System.out.printf ("  Name              :  %s%n", c.getName());
        System.out.printf ("  Email             :  %s%n", c.getEmail());
        System.out.printf ("  Phone             :  %s%n", c.getPhone());
        System.out.println(THIN);
        System.out.println("  IMPORTANT: Save your Customer ID.");
        System.out.println("  You will need it to create a bank account.");
        System.out.println(THIN);
        return true;
    }

    // ── 2. Create Account ─────────────────────────────────────────────────────
    private static boolean createAccount() throws BankException {
        System.out.println("\n" + THIN);
        System.out.println("          CREATE ACCOUNT");
        System.out.println(THIN);

        // Guard: at least one customer must exist
        if (!svc.hasCustomers()) {
            System.out.println("  No customers registered yet!");
            System.out.println();
            System.out.println("  Please create a customer first.");
            System.out.println("  Choose option 1 – Create Customer.");
            System.out.println();
            System.out.println("  Returning to the main menu...");
            return true;
        }

        // List known customer IDs to help the user
        System.out.println("  Registered customers:");
        svc.allCustomers().stream()
           .sorted(java.util.Comparator.comparingInt(Customer::getCustomerId))
           .forEach(cu -> System.out.printf("    ID %-6d  %s%n",
                   cu.getCustomerId(), cu.getName()));
        System.out.println();

        // Read and validate Customer ID
        int customerId = readInt("  Customer ID : ");
        if (svc.findCustomer(customerId).isEmpty()) {
            System.out.printf("%n  Error: No customer found with ID: %d%n", customerId);
            System.out.println("  Please enter a valid Customer ID.");
            System.out.println("  You can create a customer using option 1.");
            System.out.println("\n  Returning to the main menu...");
            return true;
        }

        System.out.println("  Account type:");
        System.out.println("    1  Savings       (min balance ₹500, 4% interest)");
        System.out.println("    2  Current        (overdraft up to ₹10,000)");
        System.out.println("    3  Fixed Deposit  (7% interest, locked until maturity)");
        int type = readInt("  Type (1/2/3): ");

        double balance = readDouble("  Initial balance (₹): ");

        Account acc;
        switch (type) {
            case 1 -> acc = svc.createSavingsAccount(customerId, balance);
            case 2 -> acc = svc.createCurrentAccount(customerId, balance);
            case 3 -> {
                int months = readInt("  Tenure (months): ");
                acc = svc.createFixedDeposit(customerId, balance, months);
            }
            default -> {
                System.out.println("  Invalid account type. Returning to menu.");
                return true;
            }
        }

        // ── Success banner ────────────────────────────────────────────────────
        System.out.println("\n  ✔  Account created successfully!");
        System.out.println(THIN);
        System.out.printf ("  Account ID     :  %d%n",   acc.getAccountId());
        System.out.printf ("  Account Number :  %s%n",   acc.getAccountNumber());
        System.out.printf ("  Account Type   :  %s%n",   acc.getAccountType());
        System.out.printf ("  Customer ID    :  %d%n",   acc.getCustomerId());
        System.out.printf ("  Balance        :  ₹%,.2f%n", acc.getBalance());
        System.out.println(THIN);
        System.out.println("  IMPORTANT: Save your Account ID.");
        System.out.println("  You will need it for deposits, withdrawals, and statements.");
        System.out.println(THIN);
        return true;
    }

    // ── 3. Deposit ────────────────────────────────────────────────────────────
    private static boolean deposit() throws BankException {
        System.out.println("\n" + THIN);
        System.out.println("              DEPOSIT");
        System.out.println(THIN);

        if (!svc.hasAccounts()) {
            noAccountsGuidance();
            return true;
        }
        int    accountId = readInt("  Account ID : ");
        double amount    = readDouble("  Amount (₹) : ");
        svc.deposit(accountId, amount);
        return true;
    }

    // ── 4. Withdraw ───────────────────────────────────────────────────────────
    private static boolean withdraw() throws BankException {
        System.out.println("\n" + THIN);
        System.out.println("             WITHDRAW");
        System.out.println(THIN);

        if (!svc.hasAccounts()) {
            noAccountsGuidance();
            return true;
        }
        int    accountId = readInt("  Account ID : ");
        double amount    = readDouble("  Amount (₹) : ");
        svc.withdraw(accountId, amount);
        return true;
    }

    // ── 5. View Balance ───────────────────────────────────────────────────────
    private static boolean viewBalance() throws BankException {
        System.out.println("\n" + THIN);
        System.out.println("           VIEW BALANCE");
        System.out.println(THIN);

        if (!svc.hasAccounts()) {
            noAccountsGuidance();
            return true;
        }
        int accountId = readInt("  Account ID : ");
        double bal = svc.getBalance(accountId);
        System.out.printf("%n  Balance: ₹%,.2f%n", bal);
        return true;
    }

    // ── 6. View Statement ─────────────────────────────────────────────────────
    private static boolean viewStatement() throws BankException {
        System.out.println("\n" + THIN);
        System.out.println("          VIEW STATEMENT");
        System.out.println(THIN);

        if (!svc.hasAccounts()) {
            noAccountsGuidance();
            return true;
        }
        int accountId = readInt("  Account ID : ");
        svc.printStatement(accountId);
        return true;
    }

    // ── 7. Transfer ───────────────────────────────────────────────────────────
    private static boolean transfer() throws BankException {
        System.out.println("\n" + THIN);
        System.out.println("             TRANSFER");
        System.out.println(THIN);

        if (!svc.hasAccounts()) {
            noAccountsGuidance();
            return true;
        }
        int    fromId = readInt("  From Account ID : ");
        int    toId   = readInt("  To   Account ID : ");
        if (fromId == toId) {
            System.out.println("  Error: Source and destination accounts must be different.");
            return true;
        }
        double amount = readDouble("  Amount (₹)      : ");
        svc.transfer(fromId, toId, amount);
        return true;
    }

    // ── 8. Apply Interest ─────────────────────────────────────────────────────
    private static boolean applyInterest() throws BankException {
        System.out.println("\n" + THIN);
        System.out.println("          APPLY INTEREST");
        System.out.println(THIN);

        if (!svc.hasAccounts()) {
            noAccountsGuidance();
            return true;
        }
        int accountId = readInt("  Account ID : ");
        svc.applyInterest(accountId);
        return true;
    }

    // ── 9. List Accounts ──────────────────────────────────────────────────────
    private static boolean listAccounts() {
        System.out.println("\n" + THIN);
        System.out.println("          ALL ACCOUNTS");
        System.out.println(THIN);
        svc.listAllAccounts();
        if (svc.hasAccounts()) {
            svc.generateReport();
        }
        return true;
    }

    // ── 10. Delete Account ────────────────────────────────────────────────────
    private static boolean deleteAccount() throws BankException {
        System.out.println("\n" + THIN);
        System.out.println("          DELETE ACCOUNT");
        System.out.println(THIN);

        if (!svc.hasAccounts()) {
            noAccountsGuidance();
            return true;
        }
        int accountId = readInt("  Account ID : ");
        svc.deleteAccount(accountId);
        return true;
    }

    // ── 11. Exit ──────────────────────────────────────────────────────────────
    private static boolean exit() {
        svc.saveData();               // Lab 11 – serialise on exit
        System.out.println("\n  Goodbye from " + BANK.name() + "!");
        System.out.println("  Thank you for banking with us.\n");
        return false;                 // signals the while-loop to stop
    }

    // ── Shared empty-state guidance ───────────────────────────────────────────
    private static void noAccountsGuidance() {
        System.out.println("  No accounts available yet.");
        System.out.println("  Please create a customer first (option 1),");
        System.out.println("  then create a bank account (option 2).");
        System.out.println("\n  Returning to the main menu...");
    }

    // ── Input helpers ─────────────────────────────────────────────────────────

    private static String readString(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().strip();
    }

    /**
     * Reads an integer, retrying on bad input (never throws).
     * Returns {@code -1} if the very first token is not a number and
     * {@code retryOnError} is false — used for the main menu choice.
     */
    private static int readInt(String prompt) {
        System.out.print(prompt);
        while (true) {
            String line = sc.nextLine().strip();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.print("  Please enter a whole number – " + prompt);
            }
        }
    }

    /**
     * Menu-safe integer read: returns {@code -1} instead of retrying
     * so the outer loop can show a clean "enter 1–11" hint.
     */
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
                System.out.print("  Please enter a number – " + prompt);
            }
        }
    }

    // ── Lab 9/10 demo runner (triggered via -Dminibank.demo JVM property) ─────
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
