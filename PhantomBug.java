import java.util.concurrent.ThreadLocalRandom;

public class PhantomBug {
    static long totalHits = 0;
    static final int POINTS = 50_000_000;
    static final int THREADS = 4;
    static final int RUNS = 5;

    public static void main(String[] args) throws InterruptedException {
        for (int run = 1; run <= RUNS; run++) {
            totalHits = 0;
            Thread[] threads = new Thread[THREADS];
            int points = POINTS / THREADS;

            for (int i = 0; i < THREADS; i++) {
                threads[i] = new Thread(() -> {
                    ThreadLocalRandom rand = ThreadLocalRandom.current();
                    for (int j = 0; j < points; j++) {
                        double x = rand.nextDouble();
                        double y = rand.nextDouble();
                        if (x * x + y * y <= 1.0) {
                            totalHits++;
                        }
                    }
                });
            }
            for (Thread thread : threads) thread.start();
            for (Thread thread : threads) thread.join();

            double pi = 4.0 * totalHits / POINTS;
            System.out.printf("Run %d: hits = %d, pi = %.6f%n", run, totalHits, pi);
        }
    }
}

// The counter is shared, but totalHits++ is not atomic.
// Threads can read the same value and overwrite each other's updates, so some hits are lost.
