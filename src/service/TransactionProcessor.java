// Labs 9, 10 – TransactionProcessor
// Demonstrates race condition (Lab 9), synchronized + thread pool (Lab 10),
// producer-consumer queue, and deadlock demo + fix.
package service;

import exception.BankException;
import model.Account;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Processes account transactions concurrently.
 * Lab 9:  race condition demo (unsynchronized counter).
 * Lab 10: thread pool, synchronized, producer-consumer, deadlock demo/fix.
 */
public class TransactionProcessor {

    private static final Logger LOG = Logger.getLogger(TransactionProcessor.class.getName());

    private final ExecutorService pool;
    private final BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();
    private final AtomicInteger successCount = new AtomicInteger();
    private final AtomicInteger failureCount = new AtomicInteger();

    public TransactionProcessor(int threadCount) {
        this.pool = Executors.newFixedThreadPool(threadCount, r -> {
            Thread t = new Thread(r, "TxnProcessor-" + r.hashCode());
            t.setDaemon(true);
            return t;
        });
    }

    /** Submit an async deposit task. */
    public Future<?> submitDeposit(Account account, double amount) {
        return pool.submit(() -> {
            try {
                account.deposit(amount);
                successCount.incrementAndGet();
                LOG.info(() -> "Deposited " + amount + " → " + account.getAccountNumber());
            } catch (BankException e) {
                failureCount.incrementAndGet();
                LOG.warning(e::toString);
            }
        });
    }

    /** Submit an async withdraw task. */
    public Future<?> submitWithdraw(Account account, double amount) {
        return pool.submit(() -> {
            try {
                account.withdraw(amount);
                successCount.incrementAndGet();
                LOG.info(() -> "Withdrew " + amount + " ← " + account.getAccountNumber());
            } catch (BankException e) {
                failureCount.incrementAndGet();
                LOG.warning(e::toString);
            }
        });
    }

    // ── Lab 9: Race condition demo ────────────────────────────────────────────
    /**
     * Demonstrates a race condition by letting multiple threads increment a
     * shared, un-synchronized counter.  The final value is usually < expected.
     */
    public static int raceConditionDemo(int threadCount, int iterations)
            throws InterruptedException {
        int[] counter = {0};   // array trick: effectively final for lambda capture
        ExecutorService exec = Executors.newFixedThreadPool(threadCount);

        CountDownLatch latch = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            exec.submit(() -> {
                for (int j = 0; j < iterations; j++) {
                    counter[0]++;  // NOT thread-safe – race condition here
                }
                latch.countDown();
            });
        }
        latch.await();
        exec.shutdownNow();
        return counter[0];
    }

    // ── Lab 10: Synchronized fix for the race condition ───────────────────────
    /** Demonstrates that AtomicInteger eliminates the race condition. */
    public static int synchronizedCounterDemo(int threadCount, int iterations)
            throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        ExecutorService exec = Executors.newFixedThreadPool(threadCount);

        CountDownLatch latch = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            exec.submit(() -> {
                for (int j = 0; j < iterations; j++) {
                    counter.incrementAndGet();
                }
                latch.countDown();
            });
        }
        latch.await();
        exec.shutdownNow();
        return counter.get();
    }

    // ── Lab 10: Producer-consumer demo ───────────────────────────────────────
    /**
     * Simple producer-consumer: producers enqueue tasks, one consumer drains.
     * Returns after all {@code taskCount} tasks are processed.
     */
    public static void producerConsumerDemo(int taskCount) throws InterruptedException {
        BlockingQueue<String> queue = new LinkedBlockingQueue<>(10);

        Thread producer = new Thread(() -> {
            for (int i = 1; i <= taskCount; i++) {
                try {
                    queue.put("task-" + i);
                    System.out.println("[Producer] queued task-" + i);
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
            }
        }, "Producer");

        Thread consumer = new Thread(() -> {
            int consumed = 0;
            while (consumed < taskCount) {
                try {
                    String task = queue.take();
                    System.out.println("[Consumer] processed " + task);
                    consumed++;
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
            }
        }, "Consumer");

        producer.start(); consumer.start();
        producer.join();  consumer.join();
    }

    // ── Lab 10: Deadlock demo and fix ─────────────────────────────────────────
    /** Demonstrates a deadlock between two threads (intentionally hangs briefly). */
    public static void deadlockDemo() {
        Object lockA = new Object();
        Object lockB = new Object();

        Thread t1 = new Thread(() -> {
            synchronized (lockA) {
                System.out.println("[T1] holds A, waiting for B...");
                try { Thread.sleep(50); } catch (InterruptedException ignored) {}
                synchronized (lockB) { System.out.println("[T1] acquired B"); }
            }
        }, "T1");

        Thread t2 = new Thread(() -> {
            synchronized (lockB) {
                System.out.println("[T2] holds B, waiting for A...");
                synchronized (lockA) { System.out.println("[T2] acquired A"); }
            }
        }, "T2");

        t1.setDaemon(true); t2.setDaemon(true);
        t1.start(); t2.start();
        System.out.println("[Demo] Deadlock threads started (daemon – will be killed on exit).");
    }

    /** Deadlock FIX: always acquire locks in the same order. */
    public static void deadlockFixed() throws InterruptedException {
        Object lockA = new Object();
        Object lockB = new Object();

        Thread t1 = new Thread(() -> {
            synchronized (lockA) { synchronized (lockB) {
                System.out.println("[T1-fixed] acquired A then B");
            }}
        });
        Thread t2 = new Thread(() -> {
            synchronized (lockA) { synchronized (lockB) {  // same order!
                System.out.println("[T2-fixed] acquired A then B");
            }}
        });
        t1.start(); t2.start();
        t1.join(); t2.join();
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    public void shutdown() {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(5, TimeUnit.SECONDS))
                pool.shutdownNow();
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public int getSuccessCount() { return successCount.get(); }
    public int getFailureCount() { return failureCount.get(); }
}
