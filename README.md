# MiniBank — Professional Java Banking System

A robust, enterprise-grade console banking system built in modern Java (JDK 17+), demonstrating object-oriented architecture, relational database persistence (SQLite via JDBC), role-based security with PBKDF2 password hashing, transactional integrity, and automated testing with JUnit 5 and GitHub Actions CI.

## Roll Number
**25ce014**

---

## Key Features

### 1. Core Banking Operations & Business Rules
- **Multi-Account Types**:
  - **Savings Account**: Enforces ₹500 minimum balance; earns 4% p.a. interest calculated via `BigDecimal`.
  - **Current Account**: Supports overdraft facility up to ₹10,000.
  - **Fixed Deposit Account**: 7% p.a. interest with withdrawal lockout until maturity date.
- **Transactional Integrity**:
  - Transfers are atomic: if withdrawal fails or destination rejects, balances are completely untouched (zero partial updates).
  - Rejection of negative, zero, and self-transfers (`fromAccount == toAccount`).
  - Configurable daily transaction limits (default ₹50,000/day).
  - Account locking and unlocking by administrators.

### 2. Relational Database Persistence (SQLite + JDBC)
- **Zero-Setup Database**: Uses embedded SQLite (`minibank.db`) with automatic table and index creation.
- **Foreign Key Constraints & Indices**: Enforced relational integrity across customers, accounts, transactions, and users.
- **Legacy Migration**: Automatically detects and migrates legacy serialized data (`accounts.dat`) into SQLite on startup without deleting original files.
- **Database Backup**: One-click database backup (`VACUUM INTO`) to timestamped files.

### 3. Role-Based Authentication & Security
- **Educational Security**: PBKDF2 with HMAC-SHA256, 65,536 iterations, 256-bit key length, and unique 16-byte cryptographically secure random salt per user.
- **Zero Plain-Text Storage**: Passwords are never stored, logged, or printed.
- **Brute-Force Lockout**: 5 consecutive failed login attempts automatically lock the user account.
- **Role Isolation**:
  - **Customer Portal**: Customers can only view and transact on their own accounts.
  - **Administrator Portal**: Admins manage customer accounts, lock/unlock accounts, apply interest, view system-wide liquidity reports, and inspect transaction audit logs.

### 4. Audit Trail, Reporting & Export
- **Transaction History**: Every transaction (deposit, withdrawal, transfer, interest credit, account opening) is permanently recorded with timestamp, type, amount, post-transaction balance, and status (`SUCCESS` / `FAILED`).
- **Date & Type Filtering**: Filter transaction history by type (`DEPOSIT`, `WITHDRAW`, `TRANSFER_OUT`, `ALL`) and custom date range (`YYYY-MM-DD`).
- **CSV Statement Export**: Export account statements to standard RFC-4180 CSV files (`statement_ACC002001.csv`).
- **Financial Summary Report**: Real-time aggregation of total bank liquidity, active vs locked accounts, and breakdown by account type.

---

## Prerequisites

- **Java Development Kit (JDK)**: JDK 17 or higher (tested on Java 17, 21, and 26).
- **Terminal / Shell**: bash/zsh on macOS/Linux, Command Prompt or PowerShell on Windows.
- **Internet Connection**: Only needed for the initial build so Maven Wrapper can download project dependencies (SQLite JDBC, JUnit 5). No manual Maven installation is required.

---

## Quick Start: Build, Test & Run

### 1. Clone the Repository
```bash
git clone https://github.com/shakshi-chaudhary/oop_java-banking_system-25ce014.git
cd oop_java-banking_system-25ce014
```

*(Alternative: Download the GitHub ZIP archive, extract it, and `cd` into the extracted folder.)*

### 2. macOS & Linux

Run automated unit and integration tests (45 tests):
```bash
./mvnw clean test
```

Package the self-contained executable JAR:
```bash
./mvnw package
```

Launch the application:
```bash
java -jar target/minibank-1.0.0.jar
```

### 3. Windows (Command Prompt or PowerShell)

Run automated unit and integration tests:
```cmd
mvnw.cmd clean test
```

Package the self-contained executable JAR:
```cmd
mvnw.cmd package
```

Launch the application:
```cmd
java -jar target/minibank-1.0.0.jar
```

---

## Initial Setup & Walkthrough

When launching MiniBank for the first time:

### Step 1: Initialize System Administrator
1. On the Main Portal, select **3. First-Time Setup (Create Administrator)**.
2. Enter your desired administrator username and password (e.g. `admin` / `AdminPass123!`).
3. MiniBank securely hashes the credentials with PBKDF2 and stores them in SQLite.

### Step 2: Administrator Operations
1. Select **2. Administrator Login** from the Main Portal.
2. Choose **1. Create Customer**: Enter customer details (Name, Email, Phone, Address).
3. Option to issue online login credentials for the customer.
4. Choose **2. Create Bank Account**: Enter Customer ID, select Account Type (Savings, Current, Fixed Deposit), and enter initial balance.
5. Save the generated Customer ID (e.g., `1001`) and Account ID (e.g., `2001`).

### Step 3: Customer Operations
1. Select **1. Customer Login** from the Main Portal.
2. Log in with the customer credentials created in Step 2.
3. Access the Customer Dashboard:
   - Check balances and statements.
   - Deposit and withdraw funds.
   - Transfer funds to other accounts.
   - Search and filter transaction logs.
   - Export official statements to CSV.

### Step 4: Quick Banking Terminal (Lab 1–14 Demonstration Mode)
Select **4. Quick Banking Terminal** from the Main Portal to run the classic 11-option banking menu with switch expressions for fast interactive evaluation.

---

## Project Architecture

```
oop_java-banking_system-25ce014/
├── .github/workflows/
│   └── ci.yml               – Automated GitHub Actions CI for Java 17 & 21
├── .mvn/wrapper/            – Maven Wrapper bootstrap
├── mvnw / mvnw.cmd          – Cross-platform Maven Wrapper scripts
├── pom.xml                  – Maven configuration (Java 17, SQLite JDBC, JUnit 5, Shade)
├── src/
│   ├── MiniBank.java        – Main console application entry point & portal navigation
│   ├── model/
│   │   ├── Account.java             – Abstract base class with BigDecimal arithmetic & locking
│   │   ├── SavingsAccount.java      – Savings account with min-balance rule & interest
│   │   ├── CurrentAccount.java      – Current account with overdraft facility
│   │   ├── FixedDepositAccount.java – FD account with maturity lock
│   │   ├── Customer.java            – Customer entity with Address & validation
│   │   ├── Transaction.java         – Transaction audit entity
│   │   ├── User.java                – Authentication & authorization entity
│   │   ├── Address.java             – Value object with deep cloning
│   │   ├── BankInfo.java            – Bank metadata record
│   │   ├── MenuOption.java          – Classic menu enumeration
│   │   └── annotation/              – Custom annotations (@Id, @Positive, @MaxLength)
│   ├── service/
│   │   ├── BankService.java         – Core business service & orchestration layer
│   │   ├── AuthService.java         – PBKDF2 authentication & lockout enforcement
│   │   ├── AccountStore.java        – In-memory concurrent store
│   │   ├── TransactionProcessor.java– Thread pool concurrency & deadlock demonstration
│   │   └── db/
│   │       └── DatabaseManager.java – SQLite JDBC connection, schema, transactions & migration
│   ├── util/
│   │   ├── SecurityUtil.java        – PBKDF2-HMAC-SHA256 hashing & constant-time compare
│   │   ├── CsvExporter.java         – RFC-4180 compliant CSV statement generator
│   │   ├── Validator.java           – Regex validators for email, phone, name
│   │   └── AnnotationValidator.java – Reflection-based custom annotation processor
│   └── test/
│       ├── MiniBankTest.java        – Original 34 unit tests covering Labs 1–14
│       └── EnhancedBankingTest.java – 11 tests covering persistence, atomicity, auth & limits
├── VIVA.md                  – Comprehensive Viva Q&A covering all 14 labs
└── README.md
```

---

## Automated Test Coverage

The test suite runs **45 automated tests** with 0 failures and 0 errors:
- **`MiniBankTest` (34 tests)**: Validates customer creation, auto-generated IDs, interest formulas, concurrency demo, command parser, reflection validator, deep cloning, and boundary checks.
- **`EnhancedBankingTest` (11 tests)**:
  - Atomic transfer rollback on insufficient funds.
  - Rejection of transfers to the same account.
  - Account locking and unlocking protection.
  - Daily withdrawal and transfer limits.
  - SQLite persistence survival across complete database reconnections.
  - PBKDF2 password verification and lockout after 5 consecutive failed attempts.
  - First-time administrator bootstrapping.
  - Customer authorization isolation (customers cannot see other customers' accounts).
  - Transaction history filtering by type and date range.
  - CSV statement export structure and content.

---

## Known Limitations & Security Disclaimer

- **Educational Purpose**: This project is developed as an academic and portfolio demonstration of Java OOP, concurrency, and persistence concepts. It is not intended for real-world financial transactions.
- **Local SQLite Database**: SQLite is an embedded single-file database. While ideal for desktop, local, and educational environments, production banking systems use clustered enterprise RDBMS (such as PostgreSQL or Oracle) with network encryption.
- **Console Interface**: The user interface is strictly terminal-based and does not use graphical frameworks (Swing/JavaFX) or web APIs.
