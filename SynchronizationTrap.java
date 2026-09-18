import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

public class SynchronizationTrap {
    static final int POINTS = 50_000_000;
    static final int THREADS = 4;
    static final AtomicLong totalHits = new AtomicLong(0);

    public static void main(String[] args) throws InterruptedException {
        long start1 = System.nanoTime();
        long hits1 = countSingleThread();
        long end1 = System.nanoTime();
        double pi1 = 4.0 * hits1 / POINTS;
        double time1 = (end1 - start1) / 1_000_000.0;
        totalHits.set(0);
        long start2 = System.nanoTime();
        countFourThreads();
        long end2 = System.nanoTime();
        double pi2 = 4.0 * totalHits.get() / POINTS;
        double time2 = (end2 - start2) / 1_000_000.0;

        System.out.printf("Single thread: pi = %.6f, time = %.2f ms%n", pi1, time1);
        System.out.printf("Four threads (AtomicLong): pi = %.6f, time = %.2f ms%n", pi2, time2);
        System.out.printf("Time ratio (four / single): %.2f%n", time2 / time1);
    }

    static long countSingleThread() {
        long hits = 0;
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        for (int i = 0; i < POINTS; i++) {
            double x = rand.nextDouble();
            double y = rand.nextDouble();
            if (x * x + y * y <= 1.0) hits++;
        }
        return hits;
    }

    static void countFourThreads() throws InterruptedException {
        Thread[] threads = new Thread[THREADS];
        int points = POINTS / THREADS;
        for (int i = 0; i < THREADS; i++) {
            threads[i] = new Thread(() -> {
                ThreadLocalRandom rand = ThreadLocalRandom.current();
                for (int j = 0; j < points; j++) {
                    double x = rand.nextDouble();
                    double y = rand.nextDouble();
                    if (x * x + y * y <= 1.0) {
                        totalHits.incrementAndGet();
                    }
                }
            });
        }
        for (Thread thread : threads) thread.start();
        for (Thread thread : threads) thread.join();
    }
}

// AtomicLong prevents lost updates, so the count is correct.
// All threads still update one counter. Coordinating these updates can make it slower than one thread.
