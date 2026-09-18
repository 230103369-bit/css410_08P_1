# Java threading benchmarks

These programs estimate pi by generating random points in a 1 x 1 square. A point is a hit when `x*x + y*y <= 1`. The estimate is `4.0 * hits / points`.

## How to run

Install JDK 21 or newer, then run these commands in the repository folder:

```sh
javac PhantomBug.java SynchronizationTrap.java ReductionBenchmark.java
java PhantomBug
java SynchronizationTrap
java ReductionBenchmark
```

## Test environment

The results below were measured locally on a Mac with Apple Silicon using OpenJDK 21.0.12.1. Java reported 8 available processors. The programs were run one after another. Part 1 performs five runs; Parts 2 and 3 show one measured run per configuration. Part 3 includes two short warm-up runs. Times include worker creation, startup and joining where applicable. These are simple classroom measurements, so timings can change between runs.

## Part 1: The Phantom Bug

`PhantomBug.java` uses 4 threads and 50,000,000 points per run. All threads update the same `totalHits` counter with `totalHits++`.

| Run | Hits | Pi |
| --- | ---: | ---: |
| 1 | 12,322,494 | 0.985800 |
| 2 | 12,293,081 | 0.983446 |
| 3 | 12,981,286 | 1.038503 |
| 4 | 12,898,049 | 1.031844 |
| 5 | 12,877,593 | 1.030207 |

The results are wrong because `totalHits++` is not atomic. Two threads can read the same value and then overwrite each other's updates. This loses hits and lowers the estimate of pi. The exact result depends on how the threads execute; it does not have to fall within the example range in the assignment.

## Part 2: The Synchronization Trap

`SynchronizationTrap.java` compares a plain single-threaded loop with 4 threads sharing an `AtomicLong`. Both process 50,000,000 points.

| Version | Pi | Runtime (ms) |
| --- | ---: | ---: |
| Single thread | 3.141698 | 151.12 |
| 4 threads with AtomicLong | 3.141437 | 824.29 |

The four-thread version took 5.45 times as long. `incrementAndGet()` prevents lost updates, so both estimates are close to pi. However, the threads still compete to update one shared counter.

## Part 3: OpenMP-Style Reduction

`ReductionBenchmark.java` uses 100,000,000 points for each configuration. Each thread counts hits in a local variable and writes its result to a separate array element once. The main thread joins all workers and adds their results.

Speedup = single-thread runtime / current runtime.

Efficiency = speedup / number of threads * 100%.

| Threads | Runtime (ms) | Speedup | Efficiency | Pi |
| ---: | ---: | ---: | ---: | ---: |
| 1 | 289.11 | 1.00x | 100.00% | 3.141448 |
| 2 | 148.40 | 1.95x | 97.41% | 3.141393 |
| 4 | 77.63 | 3.72x | 93.11% | 3.141848 |
| 8 | 64.39 | 4.49x | 56.12% | 3.141493 |
| 16 | 60.72 | 4.76x | 29.76% | 3.141440 |
| 32 | 60.55 | 4.77x | 14.92% | 3.141508 |

The largest gains were from 1 to 4 threads. After 8 threads, adding more threads gave only a small improvement in this run. Efficiency is relative to ideal linear speedup, not CPU utilization.

## Questions

### 1. Why did 16 threads not run twice as fast as 8 threads on an 8-core CPU?

More threads do not create more CPU cores. With more runnable threads than available cores, threads must share execution resources. Scheduling and thread management also take time. Here, 16 threads took 60.72 ms compared with 64.39 ms for 8 threads, which is only about a 1.06x improvement, not 2x. Small timing differences can also come from measurement noise.

### 2. Why can the synchronized version be slower than a single-threaded loop?

With `synchronized`, threads must take turns entering the protected counter update. With `AtomicLong`, which this solution uses, updates are atomic without a Java monitor lock, but they still compete for the same memory location and require coordination between cores. Doing this for millions of hits can cost more than the parallel execution saves. A single-threaded loop updates its own counter without that contention. Local counters in Part 3 avoid shared updates inside the loop.
