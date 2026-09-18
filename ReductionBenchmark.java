import java.util.concurrent.ThreadLocalRandom;

public class ReductionBenchmark {
    static final int POINTS = 100_000_000;

    public static void main(String[] args) throws InterruptedException {
        int[] counts = {1, 2, 4, 8, 16, 32};
        System.out.println("Available processors: " + Runtime.getRuntime().availableProcessors());
        runExperiment(1, 5_000_000);
        runExperiment(4, 5_000_000);
        double time1 = 0;
        System.out.printf("%-8s %-14s %-12s %-16s %-12s%n",
                "Threads", "Runtime(ms)", "Speedup", "Efficiency(%)", "Pi");
        for (int n : counts) {
            long start = System.nanoTime();
            long totalHits = runExperiment(n, POINTS);
            double time = (System.nanoTime() - start) / 1_000_000.0;
            if (n == 1) time1 = time;
            double speedup = time1 / time;
            double efficiency = speedup / n * 100.0;
            double pi = 4.0 * totalHits / POINTS;
            System.out.printf("%-8d %-14.2f %-12.2f %-16.2f %-12.6f%n",
                    n, time, speedup, efficiency, pi);
        }
    }

    static long runExperiment(int n, int points) throws InterruptedException {
        Thread[] threads = new Thread[n];
        long[] results = new long[n];
        int perThread = points / n;
        int extra = points % n;
        for (int i = 0; i < n; i++) {
            final int index = i;
            final int myPoints = perThread + (i < extra ? 1 : 0);
            threads[i] = new Thread(() -> {
                ThreadLocalRandom rand = ThreadLocalRandom.current();
                long myHits = 0;
                for (int j = 0; j < myPoints; j++) {
                    double x = rand.nextDouble();
                    double y = rand.nextDouble();
                    if (x * x + y * y <= 1.0) myHits++;
                }
                results[index] = myHits;
            });
        }
        for (Thread thread : threads) thread.start();
        for (Thread thread : threads) thread.join();
        long totalHits = 0;
        for (long hits : results) totalHits += hits;
        return totalHits;
    }
}
