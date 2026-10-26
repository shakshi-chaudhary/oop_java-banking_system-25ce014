// Lab 4 – StatementFormatter
package util;

import model.Account;

import java.util.List;

/**
 * Formats account transaction statements for console display.
 * Lab 4: demonstrates string formatting and StringBuilder.
 */
public final class StatementFormatter {

    private static final int LINE_WIDTH = 72;
    private static final String DIVIDER = "-".repeat(LINE_WIDTH);
    private static final String HEADER  =
            String.format("%-19s | %-8s | %10s | %10s",
                    "Date-Time", "Type", "Amount", "Balance");

    private StatementFormatter() {}

    /**
     * Produces a formatted mini-statement for the given account.
     */
    public static String format(Account account) {
        List<String> history = account.getTransactionHistory();
        StringBuilder sb = new StringBuilder();

        sb.append("\n").append(DIVIDER).append("\n");
        sb.append("  MINI-STATEMENT  |  ").append(account.getAccountNumber())
          .append("  |  ").append(account.getAccountType()).append("\n");
        sb.append(DIVIDER).append("\n");
        sb.append(HEADER).append("\n");
        sb.append(DIVIDER).append("\n");

        if (history.isEmpty()) {
            sb.append("  (no transactions)\n");
        } else {
            for (String entry : history) {
                sb.append(entry).append("\n");
            }
        }

        sb.append(DIVIDER).append("\n");
        sb.append(String.format("  Current Balance: %,.2f%n", account.getBalance()));
        sb.append(DIVIDER).append("\n");

        return sb.toString();
    }

    /** Prints the statement to stdout. */
    public static void print(Account account) {
        System.out.println(format(account));
    }
}
