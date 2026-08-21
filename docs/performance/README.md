# Efficiency Lab

The efficiency lab times the project's real implementations with deterministic inputs derived
from `ProjectParameters.BENCHMARK_SEED` (`7497`). Input creation, graph construction, cloning,
post-run correctness checks, and height/statistic collection happen outside timing. Each algorithm/size group records three raw
`System.nanoTime()` trials and their arithmetic mean.

## Official plan

| Experiment | Algorithms | Input sizes |
|---|---|---|
| Search | Linear, Binary | 100, 500, 1,000, 5,000, 10,000 |
| Sort | Selection, Insertion, Merge, Quick | 100, 500, 1,000, 5,000, 10,000 |
| Hash | custom `HashTable`, three initial capacities | 100, 500, 1,000, 5,000, 10,000, 20,000 |
| Trees | BST/RB insertion, search, and height | 100, 500, 1,000, 5,000, 10,000 |
| Heap | custom heap request-priority insertion/extraction | 100, 500, 1,000, 5,000, 10,000, 20,000 |
| Graph | BFS, DFS, Dijkstra, Prim, Kruskal | 50, 100, 200, 500 vertices |
| Exact optimization | DP and brute force, identical inputs | 8, 12, 16 items |
| Assignment | actual greedy selector and candidates | 100, 500, 1,000, 5,000, 10,000, 20,000 |

Binary Search receives a sorted clone and records `sorted=true`. Sorting algorithms receive
equivalent deterministic clones. Graphs are connected; each Prim/Kruskal measurement is preceded
by an untimed equality check. DP and brute force likewise must agree before either measurement is
accepted. Greedy results contain an actual selected resource ID and response time—no placeholder
metrics are used.

## Run

From the Swing application, open **Efficiency Lab**. The representative run is small; the full
official run can take several minutes because the quadratic sorts and ordered BST degradation are
intentional.

Command-line equivalents:

```bash
# Small verification run
mvn exec:java -Dexec.mainClass=org.ugoptimizer.performance.EfficiencyLabMain \
  -Dexec.args="--quick results/representative-efficiency-lab"

# Final official-size collection
mvn exec:java -Dexec.mainClass=org.ugoptimizer.performance.EfficiencyLabMain \
  -Dexec.args="--full results/full-efficiency-lab"
```

Each directory contains `benchmark-results.csv` and `environment.txt`. The CSV columns are:

```text
experiment,algorithm,input_size,trial,runtime_ns,average_runtime_ns,memory_kb_approx,seed,parameters,result_metric,date_run
```

`memory_kb_approx` is a used-heap delta sampled consistently around the timed operation. It is not
a profiler measurement; negative deltas caused by garbage collection are reported as zero. The
environment file captures OS, architecture, Java/JVM, processor count, and JVM heap. Record the
exact CPU model and physical RAM manually on the final benchmark machine.

## Charts

The CSV is chart-ready. In LibreOffice Calc or Excel:

1. Import as UTF-8 comma-separated data.
2. Filter to `trial = 1` so the repeated group average appears once.
3. Create an XY line chart with `input_size` on X and `average_runtime_ns` on Y.
4. Use one series per `algorithm` and one chart per `experiment`.
5. Keep the seed, parameters, environment file, raw CSV, and chart together as evidence.

Do not copy the representative timings into the final report as full-scale results. Run the full
plan once on the declared submission machine and preserve its generated files unchanged.
