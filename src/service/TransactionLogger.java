// Labs 11, 13 – TransactionLogger (NIO) + report generator
package service;

import model.Account;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

/**
 * Appends transaction events to a log file using NIO (Lab 11) and generates
 * an account summary report (Lab 13).
 */
public final class TransactionLogger {

    private static final Path LOG_FILE    = Paths.get("transactions.log");
    private static final Path REPORT_FILE = Paths.get("report.txt");
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private TransactionLogger() {}

    /** Appends a single log line using NIO Files.writeString (Lab 11). */
    public static void log(String accountNumber, String type, double amount)
            throws IOException {
        String line = String.format("%s | %s | %-8s | %,.2f%n",
                LocalDateTime.now().format(FMT), accountNumber, type, amount);
        Files.writeString(LOG_FILE, line,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /**
     * Generates a plain-text report of all accounts via NIO (Lab 13).
     */
    public static void generateReport(Collection<Account> accounts) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("====== MiniBank Account Report ======\n");
        sb.append("Generated: ").append(LocalDateTime.now().format(FMT)).append("\n\n");

        for (Account a : accounts) {
            sb.append(a.toString()).append("\n");
        }

        sb.append("\nTotal accounts: ").append(accounts.size()).append("\n");
        double total = accounts.stream().mapToDouble(Account::getBalance).sum();
        sb.append(String.format("Total deposits: %,.2f%n", total));

        Files.writeString(REPORT_FILE, sb.toString(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
        System.out.println("Report written to " + REPORT_FILE.toAbsolutePath());
    }
}
