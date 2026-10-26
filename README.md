# MiniBank – OOP Java Banking System

A console-based banking application built incrementally across 14 labs, demonstrating core Java OOP concepts: encapsulation, inheritance, interfaces, annotations, concurrency, I/O, and generics.

## Roll Number
25ce014

---

## Prerequisites

- **Java Development Kit (JDK)**: JDK 17 or higher (tested on Java 17, 21, and 26). Ensure `java -version` and `javac -version` are accessible in your terminal.
- **Internet Connection**: Required on the first run so the Maven Wrapper can automatically download Maven and project dependencies (JUnit 5, plugins). No manual Maven installation is required.

---

## Getting Started

### Option A: Clone with Git

Run the clone command in your terminal:

```bash
git clone https://github.com/shakshi-chaudhary/oop_java-banking_system-25ce014.git
```

Then navigate into the project directory:

```bash
cd oop_java-banking_system-25ce014
```

### Option B: Download as ZIP (Alternative)

1. On GitHub, click the green **Code** button and select **Download ZIP** (or go to `https://github.com/shakshi-chaudhary/oop_java-banking_system-25ce014/archive/refs/heads/main.zip`).
2. Extract the downloaded ZIP archive on your computer.
3. Open a terminal / command prompt and navigate to the extracted folder:
   ```bash
   cd oop_java-banking_system-25ce014-main
   ```
   *(or the folder name where you extracted the project)*

---

## Build, Test, and Run

The repository includes the Maven Wrapper (`mvnw` / `mvnw.cmd`), allowing cross-platform builds without installing Apache Maven manually.

### macOS / Linux

Run tests:
```bash
./mvnw clean test
```

Build the executable JAR:
```bash
./mvnw package
```

Run the application:
```bash
java -jar target/minibank-1.0.0.jar
```

### Windows (Command Prompt or PowerShell)

Run tests:
```cmd
mvnw.cmd clean test
```

Build the executable JAR:
```cmd
mvnw.cmd package
```

Run the application:
```cmd
java -jar target/minibank-1.0.0.jar
```

---

## Repository Structure

```
├── .mvn/wrapper/        – Maven Wrapper configuration and bootstrap JAR
├── mvnw                 – Maven Wrapper shell script (macOS/Linux)
├── mvnw.cmd             – Maven Wrapper batch script (Windows)
├── pom.xml              – Maven project definition, JUnit 5 dependencies & plugins
├── src/
│   ├── MiniBank.java    – Application entry point, console looping menu
│   ├── model/           – Domain classes (Customer, Account subtypes, Address, Command)
│   ├── model/annotation – Custom annotations (@Id, @Positive, @MaxLength)
│   ├── util/            – Validator, CommandParser, StatementFormatter, AnnotationValidator
│   ├── service/         – BankService, TransactionProcessor, AccountStore, Repository
│   ├── exception/       – Custom banking exception hierarchy
│   └── test/            – JUnit 5 automated unit test suite (MiniBankTest.java)
├── VIVA.md              – Comprehensive Q&A covering all 14 labs
└── README.md
```

---

## Labs Covered

| Lab | Topic |
|:---:|-------|
| 1   | Setup, BankInfo record, MenuOption enum, looping menu with switch expressions |
| 2   | Customer & Account classes, encapsulation, static ID counters |
| 3   | toString, equals/hashCode, nested Address, deep clone |
| 4   | Validator (regex), Command record, CommandParser, StatementFormatter |
| 5   | Abstract Account, SavingsAccount, CurrentAccount, FixedDepositAccount |
| 6   | Interfaces: Transactable, InterestBearing, WithdrawRule, Premium; packages & JAR |
| 7   | Custom annotations @Id, @Positive, @MaxLength; reflection AnnotationValidator |
| 8   | Custom exception hierarchy replacing boolean returns |
| 9   | Race condition demo |
| 10  | synchronized methods, thread pool TransactionProcessor, producer-consumer, deadlock demo/fix |
| 11  | Object serialization persistence, transaction log, NIO report |
| 12  | AccountStore with HashMap, balance/ID sorting, ConcurrentHashMap |
| 13  | Generic Repository&lt;T&gt; with @Id reflection |
| 14  | BankService orchestration, menu wiring, load/save, JUnit 5 test suite, executable JAR |

---

## Git Tags
Each lab is tagged: `lab-01` through `lab-14`.

## Viva Q&A
See [`VIVA.md`](VIVA.md) for answers to all lab viva questions.
