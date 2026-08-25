# UG Campus Security & Emergency Response Optimizer

A Java 17 academic application for modelling campus security and emergency-response work at the University of Ghana, Legon. It stores a validated localised seed dataset in SQLite, prioritises and assigns incidents, calculates routes, records workflow history, and presents both operational and examiner-facing functionality in one Swing desktop application.

## Requirements

- Java 17
- Maven 3.9 or later

Run commands from the repository root so the default `data/` directory is available.

## Build and test

```bash
mvn clean test
mvn package -DskipTests
```

The first command runs the JUnit 5 suite. The second packages the application after tests have already passed.

## Run the Swing application

Run `org.ugoptimizer.app.Main` from your IDE, or use Maven:

```bash
mvn compile exec:java -Dexec.mainClass=org.ugoptimizer.app.Main
```

The application starts the Swing `MainMenu` in **Operational Mode**. By default it creates or opens
`database/ug-security-optimizer.db`, initializes the SQLite schema, and imports
the canonical CSV dataset from `data/` only when the database has no locations.
Generated SQLite files are ignored by Git.

Optional command-line arguments let an IDE run use different paths:

```text
Main [database-file] [canonical-dataset-directory]
```

## Interface modes

The application uses one backend composition root, one database, and one runtime
state. Switching modes changes only the Swing presentation; it does not restart
the application or create another backend.

**Operational Mode** is the default operator workflow:

```text
Dashboard -> Incidents -> Dispatch -> Routes -> Resources -> Reports
```

- **Dashboard** shows compact live incident/resource totals, priority incidents, and quick actions.
- **Incidents** uses named campus locations and readable incident, urgency, response, and status labels. Its local search and filters do not change stored records.
- **Dispatch** presents the selected incident's current status and next valid workflow action, plus cancel and undo safeguards.
- **Routes** finds a recommended route under normal or named road conditions. Location and road maintenance remains available in the secondary **Campus Network** view.
- **Resources** lists and registers response resources using named home locations and readable operational values.
- **Reports** shows the current incident/resource summary and recent workflow audit history.

Select **DSA Lab** to open the examiner-facing academic workflow:

```text
Structures -> Search & Sort -> Graph Algorithms -> Optimization -> Correctness -> Efficiency Lab
```

The **Back to Operations** button returns to Operational Mode without replacing
the injected services or database state. Structures contains the queue, heap,
tree-index, and priority-queue demonstrations. Sort execution/recording is under
Search & Sort; BFS, DFS, and Dijkstra execution/recording is under Graph
Algorithms; recorded run timing/memory data is under Efficiency Lab. Correctness
exposes generated traces and the locations of retained proof, counterexample,
and test evidence.

## Available functionality

- Campus locations, roads, resources, service requests, workflow history, and reports persisted through SQLite DAOs.
- Custom structures and implementations for search, sorting, queues, stacks, trees, hashing, graphs, heaps, and disjoint sets.
- Deterministic BFS/DFS traversal, Dijkstra shortest paths, and Prim/Kruskal minimum-spanning-forest operations through the shared graph contracts.
- Priority, assignment, undo, and optimisation services, exposed to the Swing screens through frontend service contracts.
- Examiner-facing custom-structure demonstrations for FIFO, circular queue, deque, heap, BST, red-black tree, and B-tree behaviour using current request records.
- A deterministic efficiency lab with three raw trials, averages, environment metadata, and chart-ready CSV export.
- Canonical CSV validation and transactional database import.

Routing excludes baseline roads marked blocked. The backend can also construct
scenario-specific graphs from `road_scenarios`; Operational Mode exposes both
baseline and named-scenario shortest paths through **Routes**. Route values are
based on the selected project road scenario and are not live navigation data.

The DSA Lab can generate verified trace evidence and can run a small
representative efficiency suite or the complete official-size suite. Benchmarks
do not run automatically at startup. Equivalent command-line exporters are:

```bash
mvn exec:java -Dexec.mainClass=org.ugoptimizer.evidence.CorrectnessEvidenceMain
mvn exec:java -Dexec.mainClass=org.ugoptimizer.performance.EfficiencyLabMain -Dexec.args="--quick results/representative-efficiency-lab"
```

See [`docs/performance/README.md`](docs/performance/README.md) before collecting
final measurements; a full run is intentionally not part of normal tests.

## Data and provenance

The files in `data/` are seed data for an academic simulation. See
[`docs/data/README_DATASET.md`](docs/data/README_DATASET.md) and
[`docs/data/DATASET_PROVENANCE.md`](docs/data/DATASET_PROVENANCE.md) for the
dataset schema, validation, and provenance notes.
