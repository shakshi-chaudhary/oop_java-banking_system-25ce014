package service.db;

import model.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Manages SQLite JDBC database connections, schema lifecycle, transactions,
 * and data persistence for MiniBank.
 * Supports configurable database URLs, in-memory test databases, and automated
 * schema initialization with indices.
 */
public class DatabaseManager implements AutoCloseable {

    public static final String DEFAULT_DB_FILE = "minibank.db";
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final String jdbcUrl;
    private Connection connection;

    public DatabaseManager() {
        this(resolveDefaultJdbcUrl());
    }

    public DatabaseManager(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
        initSchema();
    }

    public static String resolveDefaultJdbcUrl() {
        String customPath = System.getProperty("minibank.db");
        if (customPath != null && !customPath.isBlank()) {
            return "jdbc:sqlite:" + customPath;
        }
        return "jdbc:sqlite:" + DEFAULT_DB_FILE;
    }

    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(jdbcUrl);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
            }
        }
        return connection;
    }

    /**
     * Initializes the relational database schema if tables do not exist.
     */
    public synchronized void initSchema() {
        try {
            Connection conn = getConnection();
            try (Statement stmt = conn.createStatement()) {
                // 1. Customers Table
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS customers (
                        customer_id INTEGER PRIMARY KEY,
                        name        TEXT NOT NULL,
                        email       TEXT NOT NULL,
                        phone       TEXT NOT NULL,
                        street      TEXT,
                        city        TEXT,
                        state       TEXT,
                        pin         TEXT,
                        created_at  TEXT NOT NULL
                    );
                """);

                // 2. Accounts Table
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS accounts (
                        account_id       INTEGER PRIMARY KEY,
                        customer_id      INTEGER NOT NULL,
                        account_number   TEXT UNIQUE NOT NULL,
                        account_type     TEXT NOT NULL,
                        balance          REAL NOT NULL,
                        interest_rate    REAL,
                        overdraft_limit  REAL,
                        maturity_date    TEXT,
                        is_locked        INTEGER DEFAULT 0,
                        daily_limit      REAL DEFAULT 50000.0,
                        created_at       TEXT NOT NULL,
                        FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
                    );
                """);

                // 3. Transactions Table
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS transactions (
                        transaction_id    TEXT PRIMARY KEY,
                        account_id        INTEGER NOT NULL,
                        type              TEXT NOT NULL,
                        amount            REAL NOT NULL,
                        balance_after     REAL NOT NULL,
                        target_account_id INTEGER,
                        timestamp         TEXT NOT NULL,
                        status            TEXT NOT NULL,
                        description       TEXT,
                        FOREIGN KEY (account_id) REFERENCES accounts(account_id) ON DELETE CASCADE
                    );
                """);

                // 4. Users (Authentication & Authorization) Table
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        username        TEXT PRIMARY KEY,
                        password_hash   TEXT NOT NULL,
                        salt            TEXT NOT NULL,
                        role            TEXT NOT NULL,
                        customer_id     INTEGER,
                        failed_attempts INTEGER DEFAULT 0,
                        is_locked       INTEGER DEFAULT 0,
                        created_at      TEXT NOT NULL,
                        FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE SET NULL
                    );
                """);

                // Indices for fast queries
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_accounts_cust ON accounts(customer_id);");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_tx_account ON transactions(account_id);");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_tx_timestamp ON transactions(timestamp);");
            }

            // Attempt safe migration from legacy accounts.dat if present and DB is empty
            migrateFromLegacyDataIfPresent();

        } catch (SQLException e) {
            System.err.println("Warning: Database initialization error: " + e.getMessage());
        }
    }

    /**
     * Safely migrates existing serialized data from accounts.dat into SQLite
     * without deleting or destroying the original accounts.dat file.
     */
    @SuppressWarnings("unchecked")
    private void migrateFromLegacyDataIfPresent() {
        // Only migrate into the primary production database, not temporary/in-memory test databases
        if (!jdbcUrl.contains(DEFAULT_DB_FILE)) {
            return;
        }

        File datFile = new File("accounts.dat");
        if (!datFile.exists() || datFile.length() == 0) {
            return;
        }

        try {
            // Check if customers table is already populated
            try (Statement s = getConnection().createStatement();
                 ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM customers")) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return; // Already populated, no migration needed
                }
            }

            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(datFile))) {
                Map<Integer, Customer> legacyCustomers = (Map<Integer, Customer>) ois.readObject();
                Map<Integer, Account>  legacyAccounts  = (Map<Integer, Account>) ois.readObject();

                if (legacyCustomers != null) {
                    for (Customer c : legacyCustomers.values()) {
                        saveCustomer(c);
                    }
                }
                if (legacyAccounts != null) {
                    for (Account a : legacyAccounts.values()) {
                        saveAccount(a);
                    }
                }
                System.out.println("Migrated " + (legacyCustomers != null ? legacyCustomers.size() : 0)
                        + " customer(s) and " + (legacyAccounts != null ? legacyAccounts.size() : 0)
                        + " account(s) from accounts.dat into SQLite.");
            }
        } catch (Exception e) {
            // Non-fatal warning; existing system continues
            System.out.println("Note on legacy data: " + e.getMessage());
        }
    }

    // ── Customer CRUD ────────────────────────────────────────────────────────
    public void saveCustomer(Customer c) throws SQLException {
        String sql = """
            INSERT INTO customers (customer_id, name, email, phone, street, city, state, pin, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(customer_id) DO UPDATE SET
                name   = excluded.name,
                email  = excluded.email,
                phone  = excluded.phone,
                street = excluded.street,
                city   = excluded.city,
                state  = excluded.state,
                pin    = excluded.pin;
        """;
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, c.getCustomerId());
            ps.setString(2, c.getName());
            ps.setString(3, c.getEmail());
            ps.setString(4, c.getPhone());
            Address a = c.getAddress();
            ps.setString(5, a != null ? a.getStreet() : "");
            ps.setString(6, a != null ? a.getCity() : "");
            ps.setString(7, a != null ? a.getState() : "");
            ps.setString(8, a != null ? a.getPincode() : "");
            ps.setString(9, LocalDateTime.now().format(ISO_FMT));
            ps.executeUpdate();
        }
    }

    public Optional<Customer> findCustomerById(int id) throws SQLException {
        String sql = "SELECT * FROM customers WHERE customer_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapCustomer(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Customer> loadAllCustomers() throws SQLException {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT * FROM customers ORDER BY customer_id ASC";
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapCustomer(rs));
            }
        }
        return list;
    }

    public boolean deleteCustomer(int id) throws SQLException {
        String sql = "DELETE FROM customers WHERE customer_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Customer mapCustomer(ResultSet rs) throws SQLException {
        int id       = rs.getInt("customer_id");
        String name  = rs.getString("name");
        String email = rs.getString("email");
        String phone = rs.getString("phone");
        Address addr = new Address(
                rs.getString("street"),
                rs.getString("city"),
                rs.getString("state"),
                rs.getString("pin")
        );
        return new Customer(id, name, email, phone, addr);
    }

    // ── Account CRUD ─────────────────────────────────────────────────────────
    public void saveAccount(Account a) throws SQLException {
        String sql = """
            INSERT INTO accounts (account_id, customer_id, account_number, account_type,
                                 balance, interest_rate, overdraft_limit, maturity_date,
                                 is_locked, daily_limit, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(account_id) DO UPDATE SET
                balance         = excluded.balance,
                is_locked       = excluded.is_locked,
                daily_limit     = excluded.daily_limit,
                interest_rate   = excluded.interest_rate,
                overdraft_limit = excluded.overdraft_limit;
        """;
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, a.getAccountId());
            ps.setInt(2, a.getCustomerId());
            ps.setString(3, a.getAccountNumber());
            ps.setString(4, a.getAccountType());
            ps.setDouble(5, a.getBalance());

            if (a instanceof SavingsAccount sa) {
                ps.setDouble(6, sa.getInterestRate());
                ps.setNull(7, Types.REAL);
                ps.setNull(8, Types.VARCHAR);
            } else if (a instanceof CurrentAccount ca) {
                ps.setNull(6, Types.REAL);
                ps.setDouble(7, ca.getOverdraftLimit());
                ps.setNull(8, Types.VARCHAR);
            } else if (a instanceof FixedDepositAccount fda) {
                ps.setDouble(6, fda.getInterestRate());
                ps.setNull(7, Types.REAL);
                ps.setString(8, fda.getMaturityDate() != null ? fda.getMaturityDate().toString() : null);
            } else {
                ps.setNull(6, Types.REAL);
                ps.setNull(7, Types.REAL);
                ps.setNull(8, Types.VARCHAR);
            }

            ps.setInt(9, a.isLocked() ? 1 : 0);
            ps.setDouble(10, a.getDailyLimit());
            ps.setString(11, LocalDateTime.now().format(ISO_FMT));
            ps.executeUpdate();
        }
    }

    public Optional<Account> findAccountById(int id) throws SQLException {
        String sql = "SELECT * FROM accounts WHERE account_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapAccount(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Account> loadAllAccounts() throws SQLException {
        List<Account> list = new ArrayList<>();
        String sql = "SELECT * FROM accounts ORDER BY account_id ASC";
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapAccount(rs));
            }
        }
        return list;
    }

    public boolean deleteAccount(int id) throws SQLException {
        String sql = "DELETE FROM accounts WHERE account_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Account mapAccount(ResultSet rs) throws SQLException {
        int id            = rs.getInt("account_id");
        int custId        = rs.getInt("customer_id");
        String acNo       = rs.getString("account_number");
        String type       = rs.getString("account_type");
        double balance    = rs.getDouble("balance");
        boolean locked    = rs.getInt("is_locked") == 1;
        double dailyLimit = rs.getDouble("daily_limit");

        return switch (type) {
            case "Savings" -> {
                double rate = rs.getDouble("interest_rate");
                yield new SavingsAccount(id, custId, acNo, balance, locked, dailyLimit, rate);
            }
            case "Current" -> {
                double overdraft = rs.getDouble("overdraft_limit");
                yield new CurrentAccount(id, custId, acNo, balance, locked, dailyLimit, overdraft);
            }
            case "FixedDeposit" -> {
                double rate = rs.getDouble("interest_rate");
                String matStr = rs.getString("maturity_date");
                LocalDate mat = matStr != null ? LocalDate.parse(matStr) : LocalDate.now().plusMonths(12);
                yield new FixedDepositAccount(id, custId, acNo, balance, locked, dailyLimit, rate, mat);
            }
            default -> throw new IllegalStateException("Unknown account type: " + type);
        };
    }

    // ── Transaction Logging & Audit ──────────────────────────────────────────
    public void recordTransaction(Transaction t) throws SQLException {
        String sql = """
            INSERT INTO transactions (transaction_id, account_id, type, amount, balance_after,
                                     target_account_id, timestamp, status, description)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, t.getTransactionId());
            ps.setInt(2, t.getAccountId());
            ps.setString(3, t.getType());
            ps.setDouble(4, t.getAmount());
            ps.setDouble(5, t.getBalanceAfter());
            if (t.getTargetAccountId() != null) {
                ps.setInt(6, t.getTargetAccountId());
            } else {
                ps.setNull(6, Types.INTEGER);
            }
            ps.setString(7, t.getTimestamp().format(ISO_FMT));
            ps.setString(8, t.getStatus());
            ps.setString(9, t.getDescription());
            ps.executeUpdate();
        }
    }

    public List<Transaction> getTransactionsForAccount(int accountId) throws SQLException {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE account_id = ? ORDER BY timestamp DESC";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapTransaction(rs));
                }
            }
        }
        return list;
    }

    public Optional<Transaction> findTransactionById(String transactionId) throws SQLException {
        String sql = "SELECT * FROM transactions WHERE transaction_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, transactionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapTransaction(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Transaction> filterTransactions(int accountId, String type, LocalDate fromDate, LocalDate toDate) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM transactions WHERE account_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(accountId);

        if (type != null && !type.isBlank() && !type.equalsIgnoreCase("ALL")) {
            sql.append(" AND type = ?");
            params.add(type.toUpperCase());
        }
        if (fromDate != null) {
            sql.append(" AND timestamp >= ?");
            params.add(fromDate.atStartOfDay().format(ISO_FMT));
        }
        if (toDate != null) {
            sql.append(" AND timestamp <= ?");
            params.add(toDate.atTime(23, 59, 59).format(ISO_FMT));
        }
        sql.append(" ORDER BY timestamp DESC");

        List<Transaction> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapTransaction(rs));
                }
            }
        }
        return list;
    }

    private Transaction mapTransaction(ResultSet rs) throws SQLException {
        String txId    = rs.getString("transaction_id");
        int acId       = rs.getInt("account_id");
        String type    = rs.getString("type");
        double amount  = rs.getDouble("amount");
        double bal     = rs.getDouble("balance_after");
        int targetId   = rs.getInt("target_account_id");
        Integer target = rs.wasNull() ? null : targetId;
        String tsStr   = rs.getString("timestamp");
        LocalDateTime ts = LocalDateTime.parse(tsStr, ISO_FMT);
        String status  = rs.getString("status");
        String desc    = rs.getString("description");

        return new Transaction(txId, acId, type, amount, bal, target, ts, status, desc);
    }

    // ── User Authentication CRUD ─────────────────────────────────────────────
    public void saveUser(User u) throws SQLException {
        String sql = """
            INSERT INTO users (username, password_hash, salt, role, customer_id, failed_attempts, is_locked, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(username) DO UPDATE SET
                password_hash   = excluded.password_hash,
                salt            = excluded.salt,
                failed_attempts = excluded.failed_attempts,
                is_locked       = excluded.is_locked;
        """;
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getPasswordHash());
            ps.setString(3, u.getSalt());
            ps.setString(4, u.getRole());
            if (u.getCustomerId() != null) {
                ps.setInt(5, u.getCustomerId());
            } else {
                ps.setNull(5, Types.INTEGER);
            }
            ps.setInt(6, u.getFailedAttempts());
            ps.setInt(7, u.isLocked() ? 1 : 0);
            ps.setString(8, LocalDateTime.now().format(ISO_FMT));
            ps.executeUpdate();
        }
    }

    public Optional<User> findUserByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapUser(rs));
                }
            }
        }
        return Optional.empty();
    }

    public boolean hasAdminUser() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'";
        try (Statement s = getConnection().createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        String user = rs.getString("username");
        String hash = rs.getString("password_hash");
        String salt = rs.getString("salt");
        String role = rs.getString("role");
        int custId  = rs.getInt("customer_id");
        Integer cId = rs.wasNull() ? null : custId;
        int fails   = rs.getInt("failed_attempts");
        boolean lock= rs.getInt("is_locked") == 1;

        return new User(user, hash, salt, role, cId, fails, lock);
    }

    // ── Backup Support ───────────────────────────────────────────────────────
    public boolean backupTo(File targetFile) {
        if (targetFile == null) return false;
        try {
            Connection conn = getConnection();
            try (Statement s = conn.createStatement()) {
                s.executeUpdate("VACUUM INTO '" + targetFile.getAbsolutePath().replace("'", "''") + "'");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Database backup error: " + e.getMessage());
            return false;
        }
    }

    @Override
    public synchronized void close() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException ignored) {
            }
            connection = null;
        }
    }
}
