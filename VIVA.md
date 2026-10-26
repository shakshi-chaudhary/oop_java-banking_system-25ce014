# MiniBank – Viva Q&A (All Labs)
**Roll No: 25ce014**

---

## Lab 1 – Setup, Records, Enums, Menu

**Q1. What is a Java record and when should you use it?**
A record is an immutable, transparent data carrier introduced in Java 16. Use it when you need a class whose only purpose is to hold data — it auto-generates `equals`, `hashCode`, `toString`, getters, and an all-args constructor. Example: `BankInfo(name, branch, ifscCode)`.

**Q2. What is an enum and what advantage does it have over plain integer constants?**
An enum is a type-safe set of named constants. Unlike integers, the compiler rejects invalid values at compile time, enums can have fields/methods, and `switch` on an enum is exhaustive — you get a warning if a case is missing.

**Q3. How does the switch expression differ from a switch statement?**
A switch *expression* returns a value and can use arrow (`->`) labels that do not fall through. It must be exhaustive. A switch *statement* is imperative, allows fall-through (unless `break` is used), and does not return a value.

---

## Lab 2 – Customer & Account, Encapsulation, Static Counter

**Q1. Why are fields private in encapsulation?**
To control access and maintain class invariants. External code can only interact through the validated public API (getters/setters), preventing direct corruption of internal state.

**Q2. What is the purpose of a static field/counter?**
A static field belongs to the class, not to any instance, so all objects share the same counter. Incrementing it in the constructor guarantees each instance gets a unique, auto-assigned ID.

**Q3. Why do deposit and withdraw use `synchronized` on the method?**
`synchronized` ensures only one thread can execute the method at a time, preventing race conditions on the balance field when multiple threads perform transactions concurrently.

---

## Lab 3 – toString, equals/hashCode, Address, clone

**Q1. What is the contract between equals and hashCode?**
If `a.equals(b)` is true, then `a.hashCode()` must equal `b.hashCode()`. Violating this breaks HashMap/HashSet behaviour because objects with equal logical identity would land in different buckets.

**Q2. Why is a deep clone needed for Customer?**
Customer contains an `Address` reference. Shallow clone (`super.clone()`) copies the reference, so both original and clone would share the same Address object. Mutating one would affect the other. Deep clone creates a new Address.

**Q3. What is the difference between Cloneable and a copy constructor?**
`Cloneable` + `super.clone()` uses native memory copying and requires explicit depth handling. A copy constructor is explicit Java code and is generally preferred for clarity and safety (immutable fields, inheritance complications).

---

## Lab 4 – Validator (Regex), Command Record, CommandParser, StatementFormatter

**Q1. How does regex pattern matching work in Java?**
`Pattern.compile(regex)` compiles the expression once. `pattern.matcher(input)` creates a `Matcher`. Calling `matcher.matches()` tests whether the entire input matches. Pre-compiling as a static field avoids re-compilation on every call.

**Q2. Why is the Command class a record rather than a regular class?**
Command is immutable data with no behaviour — a perfect fit for a record. The record syntax is 90% less boilerplate and communicates intent clearly.

**Q3. What is the purpose of `StringBuilder` in StatementFormatter?**
String concatenation inside a loop creates many intermediate `String` objects. `StringBuilder` appends in-place and is converted to a `String` only once at the end, which is far more memory-efficient.

---

## Lab 5 – Abstract Account, Account Hierarchy

**Q1. Why is Account declared abstract?**
You should never instantiate a generic "Account" — only concrete types (Savings, Current, FD) make sense. Making it abstract enforces that subclasses implement `getAccountType()` and prevents accidental instantiation.

**Q2. What is a template method pattern?**
A superclass defines the skeleton of an algorithm with some steps left abstract or overrideable. Here, `withdraw` calls `validateWithdraw` — subclasses override `validateWithdraw` to add their own rules without duplicating the balance-update logic.

**Q3. How does FixedDeposit prevent withdrawal before maturity?**
`FixedDepositAccount.validateWithdraw()` overrides the base hook and throws `InsufficientFundsException` if `LocalDate.now().isBefore(maturityDate)`.

---

## Lab 6 – Interfaces: Transactable, InterestBearing, WithdrawRule, Premium

**Q1. When should you use an interface vs an abstract class?**
Use an interface when you want to define a contract that unrelated classes can implement (multiple inheritance of type). Use an abstract class when you want to share implementation code among closely related classes. Java allows one superclass but many interfaces.

**Q2. What is a default method in an interface?**
A default method has an implementation in the interface itself (`default` keyword), introduced in Java 8. It lets you add new methods to an interface without breaking all existing implementations. Example: `InterestBearing.interestDescription()`.

**Q3. What is a functional interface and how is it used?**
A functional interface has exactly one abstract method (marked with `@FunctionalInterface`). It can be implemented with a lambda. `WithdrawRule` is functional, so you can write: `WithdrawRule rule = (bal, amt) -> bal - amt >= 500;`.

---

## Lab 7 – Custom Annotations, Reflection, AnnotationValidator

**Q1. What is `@Retention(RUNTIME)` and why is it needed?**
By default, annotations are discarded by the compiler. `@Retention(RetentionPolicy.RUNTIME)` tells the JVM to keep annotation metadata in the bytecode so it can be read at runtime via the Reflection API.

**Q2. How does `AnnotationValidator` use reflection?**
It calls `clazz.getDeclaredFields()` to get all fields, calls `field.setAccessible(true)` to bypass private access, reads annotations via `field.getAnnotation(Positive.class)`, then inspects the field's runtime value with `field.get(obj)`.

**Q3. Why walk the class hierarchy in `AnnotationValidator`?**
`getDeclaredFields()` only returns fields declared in that specific class, not inherited ones. Walking up via `clazz.getSuperclass()` ensures inherited annotated fields (e.g., `balance` in `Account`) are also validated.

---

## Lab 8 – Custom Exception Hierarchy

**Q1. Why create a custom exception hierarchy instead of using built-in exceptions?**
Custom exceptions carry domain context (error code, structured message) and let callers write type-specific catch blocks (`catch (InsufficientFundsException e)`) instead of generic ones. They clearly communicate the intent.

**Q2. What is the difference between checked and unchecked exceptions?**
Checked exceptions (`extends Exception`) must be declared with `throws` or caught. They represent recoverable conditions (e.g., insufficient funds). Unchecked exceptions (`extends RuntimeException`) signal programming errors and are not required to be handled.

**Q3. Why do `deposit` and `withdraw` throw checked exceptions?**
They represent expected, recoverable conditions during normal business operation. Forcing callers to handle them via `try/catch` or `throws` ensures error paths are not silently ignored.

---

## Lab 9 – Race Condition Demo

**Q1. What is a race condition?**
A race condition occurs when two or more threads access shared mutable state concurrently without synchronisation, and the outcome depends on the unpredictable order of thread scheduling. The result is non-deterministic and often wrong.

**Q2. Why does `counter[0]++` cause a race condition?**
`counter[0]++` is a read-modify-write composed of three bytecode instructions: LOAD, ADD 1, STORE. Two threads can both read the same old value, both add 1, and both store the same new value — losing one increment.

---

## Lab 10 – Synchronization, Thread Pool, Producer-Consumer, Deadlock

**Q1. What does `synchronized` do?**
It acquires the intrinsic lock (monitor) on the object before entering the block/method. Only one thread can hold a lock at a time, so concurrent access to shared state serialises correctly.

**Q2. What is a thread pool and why is it preferred over creating raw threads?**
A thread pool (`ExecutorService`) reuses a fixed set of threads for multiple tasks, avoiding the overhead of creating/destroying threads per task. It also provides task queuing, future-based result handling, and orderly shutdown.

**Q3. How is deadlock prevented in the `transfer` method?**
By always acquiring the two account locks in a consistent global order (lower account ID first). This breaks the circular-wait condition because no two threads will ever hold the first lock and wait for the second in opposite order.

---

## Lab 11 – Serialization, Transaction Log, NIO

**Q1. What is Java object serialisation?**
Serialisation converts an object graph to a byte stream (`ObjectOutputStream`) so it can be stored or transmitted. Deserialisation (`ObjectInputStream`) reconstructs the object. Classes must implement `Serializable` and should declare `serialVersionUID`.

**Q2. Why is `serialVersionUID` important?**
It is a version stamp. If a class changes (fields added/removed) but the UID stays the same, the JVM can still deserialise old data. Without it, the JVM auto-generates a UID from class structure — any change would cause `InvalidClassException`.

**Q3. What is the advantage of NIO `Files.writeString` over classic `FileWriter`?**
`Files.writeString` is concise, supports explicit charset, accepts `OpenOption` flags (CREATE, APPEND, TRUNCATE), and is part of the modern `java.nio.file` API which also supports path manipulation, file watching, and memory-mapped I/O.

---

## Lab 12 – HashMap, ConcurrentHashMap, Sorting

**Q1. Why is `HashMap` not thread-safe?**
`HashMap` does not synchronise structural modifications. Concurrent puts/resizes can cause infinite loops or data loss. Use `ConcurrentHashMap`, which partitions the map into segments and locks only the affected segment.

**Q2. How does sorting work with `Stream.sorted`?**
`Stream.sorted(Comparator)` uses `TimSort` on the stream elements. `Comparator.comparingDouble(Account::getBalance).reversed()` chains a key-extractor and reversal without writing explicit comparison logic.

---

## Lab 13 – Generic Repository\<T\>

**Q1. What are generics and why do they help?**
Generics parameterise a class/method with a type placeholder (`<T>`). The compiler enforces type safety — `Repository<Account>` rejects a `Customer` at compile time. This eliminates casts and makes code reusable across types.

**Q2. How does `Repository<T>` find the key without knowing the concrete type?**
It calls `AnnotationValidator.getIdValue(entity)` which uses reflection to find the field annotated `@Id` and returns its value. This decouples the repository from any specific type.

---

## Lab 14 – BankService Integration, JUnit, JAR

**Q1. What is the role of a service layer?**
It sits between the UI (MiniBank menu) and the data layer (AccountStore), orchestrates business logic (validate → operate → log → persist), and provides a clean API that hides implementation details from callers.

**Q2. What are JUnit 5's key annotations?**
`@Test` marks a test method; `@BeforeEach` runs before each test; `@DisplayName` labels the test; `assertThrows` checks that an exception is thrown; `assertEquals`/`assertNotNull` verify values.

**Q3. How do you create an executable JAR in Java?**
Compile all sources to a directory (`-d out`), create a `MANIFEST.MF` with `Main-Class: MiniBank`, then run `jar cfm minibank.jar MANIFEST.MF -C out .`. Run with `java -jar minibank.jar`.
