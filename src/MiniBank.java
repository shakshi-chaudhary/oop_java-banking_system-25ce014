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
 */
public class MiniBank {

    // Bank details (Lab 1 – record)
    private static final BankInfo BANK = new BankInfo(
            "MiniBank", "Main Branch", "MINI0001234");

    private static final BankService svc = new BankService();
    private static final Scanner sc = new Scanner(System.in);

    // ─────────────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        System.out.println("══════════════════════════════════════════");
        System.out.println("  Welcome to " + BANK.name());
        System.out.println("  " + BANK);
        System.out.println("══════════════════════════════════════════");

        svc.loadData();               // Lab 11 – deserialise on start

        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt("Choice: ");

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
                System.out.println("Invalid choice – " + e.getMessage());
            } catch (BankException e) {
                System.out.println("Error: " + e);
            } catch (Exception e) {
                System.out.println("Unexpected error: " + e.getMessage());
            }
        }
    }

    // ── Menu display ──────────────────────────────────────────────────────────
    private static void printMenu() {
        System.out.println("\n──────────── MENU ────────────");
        MenuOption[] opts = MenuOption.values();
        for (int i = 0; i < opts.length; i++) {
            System.out.printf("  %2d. %s%n", i + 1, opts[i].getLabel());
        }
        System.out.println("──────────────────────────────");
    }

    // ── Handler methods (return false only for EXIT) ───────────────────────────

    private static boolean createCustomer() throws BankException {
        System.out.println("\n--- Create Customer ---");
        String name   = readString("Name: ");
        String email  = readString("Email: ");
        String phone  = readString("Phone (10-digit): ");
        String street = readString("Street: ");
        String city   = readString("City: ");
        String state  = readString("State: ");
        String pin    = readString("Pincode: ");

        Address addr = new Address(street, city, state, pin);
        svc.createCustomer(name, email, phone, addr);
        return true;
    }

    private static boolean createAccount() throws BankException {
        System.out.println("\n--- Create Account ---");
        int customerId = readInt("Customer ID: ");
        System.out.println("Type: 1=Savings  2=Current  3=FixedDeposit");
        int type = readInt("Account type: ");
        double balance = readDouble("Initial balance: ₹");

        switch (type) {
            case 1 -> svc.createSavingsAccount(customerId, balance);
            case 2 -> svc.createCurrentAccount(customerId, balance);
            case 3 -> {
                int months = readInt("Tenure (months): ");
                svc.createFixedDeposit(customerId, balance, months);
            }
            default -> System.out.println("Invalid account type.");
        }
        return true;
    }

    private static boolean deposit() throws BankException {
        System.out.println("\n--- Deposit ---");
        int    accountId = readInt("Account ID: ");
        double amount    = readDouble("Amount: ₹");
        svc.deposit(accountId, amount);
        return true;
    }

    private static boolean withdraw() throws BankException {
        System.out.println("\n--- Withdraw ---");
        int    accountId = readInt("Account ID: ");
        double amount    = readDouble("Amount: ₹");
        svc.withdraw(accountId, amount);
        return true;
    }

    private static boolean viewBalance() throws BankException {
        System.out.println("\n--- View Balance ---");
        int accountId = readInt("Account ID: ");
        double bal = svc.getBalance(accountId);
        System.out.printf("Balance: ₹%,.2f%n", bal);
        return true;
    }

    private static boolean viewStatement() throws BankException {
        System.out.println("\n--- View Statement ---");
        int accountId = readInt("Account ID: ");
        svc.printStatement(accountId);
        return true;
    }

    private static boolean transfer() throws BankException {
        System.out.println("\n--- Transfer ---");
        int    fromId  = readInt("From Account ID: ");
        int    toId    = readInt("To   Account ID: ");
        double amount  = readDouble("Amount: ₹");
        svc.transfer(fromId, toId, amount);
        return true;
    }

    private static boolean applyInterest() throws BankException {
        System.out.println("\n--- Apply Interest ---");
        int accountId = readInt("Account ID: ");
        svc.applyInterest(accountId);
        return true;
    }

    private static boolean listAccounts() {
        System.out.println("\n--- All Accounts ---");
        svc.listAllAccounts();
        svc.generateReport();
        return true;
    }

    private static boolean deleteAccount() throws BankException {
        System.out.println("\n--- Delete Account ---");
        int accountId = readInt("Account ID: ");
        svc.deleteAccount(accountId);
        return true;
    }

    private static boolean exit() {
        svc.saveData();               // Lab 11 – serialise on exit
        System.out.println("Goodbye from " + BANK.name() + "!");
        return false;                 // signals the while-loop to stop
    }

    // ── Input helpers ─────────────────────────────────────────────────────────
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
                System.out.print("Please enter a whole number – " + prompt);
            }
        }
    }

    private static double readDouble(String prompt) {
        System.out.print(prompt);
        while (true) {
            String line = sc.nextLine().strip();
            try {
                return Double.parseDouble(line);
            } catch (NumberFormatException e) {
                System.out.print("Please enter a number – " + prompt);
            }
        }
    }

    // ── Lab 9/10 demo runner (called via command-line arg "demo") ─────────────
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
