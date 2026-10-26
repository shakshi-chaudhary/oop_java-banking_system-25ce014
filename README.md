# MiniBank – OOP Java Banking System

A console-based banking application built incrementally across 14 labs,
demonstrating core Java OOP concepts: encapsulation, inheritance, interfaces,
annotations, concurrency, I/O, and generics.

## Roll Number
25ce014

## Repository Structure
```
src/
  model/           – Domain classes (Customer, Account subtypes, Address, Command)
  model/annotation – Custom annotations (@Id, @Positive, @MaxLength)
  util/            – Validator, CommandParser, StatementFormatter, AnnotationValidator
  service/         – BankService, TransactionProcessor, AccountStore, Repository
  exception/       – Custom exception hierarchy
  test/            – JUnit 5 unit tests
```

## Labs Covered
| Lab | Topic |
|-----|-------|
| 1   | Setup, BankInfo record, MenuOption enum, looping menu |
| 2   | Customer & Account classes, encapsulation, static counters |
| 3   | toString, equals/hashCode, nested Address, clone |
| 4   | Validator (regex), Command record, CommandParser, StatementFormatter |
| 5   | Abstract Account, SavingsAccount, CurrentAccount, FixedDepositAccount |
| 6   | Interfaces: Transactable, InterestBearing, WithdrawRule, Premium; packages & JAR |
| 7   | Custom annotations @Id, @Positive, @MaxLength; reflection AnnotationValidator |
| 8   | Custom exception hierarchy replacing boolean returns |
| 9   | Race condition demo |
| 10  | synchronized, thread pool TransactionProcessor, producer-consumer, deadlock demo/fix |
| 11  | Serialization, transaction log, NIO report |
| 12  | AccountStore with HashMap, sorting, ConcurrentHashMap |
| 13  | Generic Repository<T> with @Id reflection |
| 14  | BankService integration, full menu wiring, load/save, JUnit tests, JAR |

## Build & Run
```bash
# Compile
find src -name "*.java" | xargs javac -d out

# Run
java -cp out MiniBank

# Run tests (requires JUnit 5 jars in lib/)
java -cp out:lib/junit-platform-console-standalone.jar \
     org.junit.platform.console.ConsoleLauncher --scan-classpath
```

## Git Tags
Each lab is tagged: `lab-01` through `lab-14`.

## Viva Q&A
See `VIVA.md` for answers to all lab viva questions.
