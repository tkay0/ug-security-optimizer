# UG Campus Security & Emergency Response Optimizer

A Java 17 academic application for modelling campus security and emergency-response work at the University of Ghana, Legon. It stores a validated localised seed dataset in SQLite, prioritises and assigns requests, calculates routes, records workflow history, and presents the available operations in a Swing desktop interface.

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

The application starts the Swing `MainMenu`. By default it creates or opens
`database/ug-security-optimizer.db`, initializes the SQLite schema, and imports
the canonical CSV dataset from `data/` only when the database has no locations.
Generated SQLite files are ignored by Git.

Optional command-line arguments let an IDE run use different paths:

```text
Main [database-file] [canonical-dataset-directory]
```

## Available functionality

- Campus locations, roads, resources, service requests, workflow history, and reports persisted through SQLite DAOs.
- Custom structures and implementations for search, sorting, queues, stacks, trees, hashing, graphs, heaps, and disjoint sets.
- Deterministic BFS/DFS traversal, Dijkstra shortest paths, and Prim/Kruskal minimum-spanning-forest operations through the shared graph contracts.
- Priority, assignment, undo, and optimisation services, exposed to the Swing screens through frontend service contracts.
- Canonical CSV validation and transactional database import.

Routing excludes baseline roads marked blocked. The backend can also construct
scenario-specific graphs from `road_scenarios`; the current Swing routing screen
uses the baseline graph and does not yet offer a scenario selector.

## Data and provenance

The files in `data/` are seed data for an academic simulation. See
[`docs/data/README_DATASET.md`](docs/data/README_DATASET.md) and
[`docs/data/DATASET_PROVENANCE.md`](docs/data/DATASET_PROVENANCE.md) for the
dataset schema, validation, and provenance notes.
