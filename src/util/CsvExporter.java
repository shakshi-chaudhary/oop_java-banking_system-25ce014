package util;

import model.Account;
import model.Transaction;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Utility for exporting account statements and transaction histories
 * to standard RFC-4180 CSV files.
 */
public final class CsvExporter {

    private CsvExporter() {
        // Utility class
    }

    /**
     * Exports transactions for a given account to a CSV file.
     *
     * @param account      the account whose statement is being exported
     * @param transactions the list of transactions
     * @param targetFile   destination CSV file
     * @return the written file
     * @throws IOException if writing fails
     */
    public static File exportStatement(Account account, List<Transaction> transactions, File targetFile)
            throws IOException {
        if (targetFile == null) {
            String defaultName = "statement_" + (account != null ? account.getAccountNumber() : "export") + ".csv";
            targetFile = new File(defaultName);
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(targetFile))) {
            // Header
            writer.println("Transaction ID,Account ID,Account Number,Timestamp,Type,Amount,Balance After,Status,Description");

            if (transactions != null) {
                for (Transaction t : transactions) {
                    writer.printf("%s,%d,%s,%s,%s,%.2f,%.2f,%s,%s%n",
                            escapeCsv(t.getTransactionId()),
                            t.getAccountId(),
                            escapeCsv(account != null ? account.getAccountNumber() : "ACC-" + t.getAccountId()),
                            escapeCsv(t.getFormattedTimestamp()),
                            escapeCsv(t.getType()),
                            t.getAmount(),
                            t.getBalanceAfter(),
                            escapeCsv(t.getStatus()),
                            escapeCsv(t.getDescription())
                    );
                }
            }
        }
        return targetFile;
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
