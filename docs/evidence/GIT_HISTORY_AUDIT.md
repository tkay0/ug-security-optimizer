# Git History Audit

## Scope and method

This audit was reconstructed on 2026-08-21 from the local repository history, refreshed remote-tracking metadata, and authenticated GitHub CLI metadata for `tkay0/ug-security-optimizer`. It does **not** infer unrecorded work, meetings, or authorship from the final code alone.

- Repository: `https://github.com/tkay0/ug-security-optimizer`
- Default remote branch: `main`
- Current local branch at audit time: `fix/team3-optimization-integration`
- Current HEAD: `9299792` — `fix(perf): strengthen benchmark methodology evidence`
- Reachable commits inspected: **108** (`git rev-list --all --count`)
- Visible Git author identities: **10** (`git shortlog -sne --all`)
- GitHub PR metadata inspected: **37** PRs; **36 merged**, **1 open** (PR #21)
- Earliest project-development commit: **2026-07-30**, `2a6b5c3`, *Initial commit*
- Latest relevant development commit: **2026-08-21**, `9299792`, *fix(perf): strengthen benchmark methodology evidence*

## Branch and integration evidence

The local/remote graph contains `main`, `develop`, and feature/fix/integration branches. Major visible examples include:

- `feature/team1-shared-contracts`, `feature/team1-graph-list`, `feature/team1-graph-matrix`
- `feature/team1-bfs-dfs`, `fix/team1-bfs-dfs-review-v2`, `fix/team1-bfs-dfs-complexity-v3`
- `feature/team1-dijkstra-prim`, `feature/team1-disjoint-set-kruskal`, `feature/team1-linear-structures`
- `feature/project-dataset`, `feature/project-database`, `integration/database-linear-sync`, `integration/database-greedy-compat`
- `feature/team2-*` structure/GUI branches and `feature/team3-*` backend/frontend/optimisation branches
- `fix/team2-gui-backend-integration` and `fix/team3-optimization-integration`

The merge history supports feature-branch integration into `develop`. It also records a backend-foundation merge, reversion, and reapplication through PRs #29–#32; this is factual history, not evidence of a specific individual fault.

## Important milestone evidence

| Date (UTC) | Commit / PR | Verified change scope |
|---|---|---|
| 2026-07-30 | `2a6b5c3`, `eb57a99` | Initial repository and project structure. |
| 2026-08-04 | `95500bc`, PR #1 | Shared `Edge`, graph/result contracts, and tests. |
| 2026-08-05 | `8ca002b`, PR #2 | Adjacency-list graph, contract tests, and evidence. |
| 2026-08-05–06 | `81eea12`–`81ee55e`, PRs #3–#6 | Linear/binary search and selection/insertion sort, later package/review corrections. |
| 2026-08-06 | `5eeb5bc`, PR #8 | Adjacency-matrix graph and tests. |
| 2026-08-06–08 | `ad76d03`, `486862d`, `e0fd912`, PRs #14/#16 | Hash table, map/set, and B-tree with tests/fixes. |
| 2026-08-07 | `281eaf5`, PR #13 | BST and red-black-tree implementation/tests. |
| 2026-08-07–10 | `bf1e73e`, `a42791b`, PRs #11/#18/#24 | BFS/DFS initial implementation, review fix, and later complexity repair. |
| 2026-08-07 | `c298ad3` | Maven/JUnit layout migration. |
| 2026-08-08 | `ba0ff57`, PR #12; `57b2c1b`, PR #15 | Dataset, documentation, validation evidence, schema simplification. |
| 2026-08-09–10 | `a39d516`, `fe09f1c`, PRs #17/#19 | Queues/stack and linear structures with tests. |
| 2026-08-09–11 | `2e76534`, `0dfe231`, `5a99ec1`, `3e5c141`, PR #20 | SQLite schema, importer/parser, DAOs/mappers, graph loader, database integration. |
| 2026-08-10–14 | `4593355`, `a868a08`, `5dd0ad1`, `5e5ce6b`, PRs #23/#25/#26/#27 | Dijkstra/Prim, DSU/Kruskal, project parameters, optimisation integration. |
| 2026-08-14–18 | `514d401`, `d6ef89a`, `af370c8`, PRs #34–#36 | Backend workflow, bootstrap, end-to-end tests, frontend compatibility. |
| 2026-08-20 | `3bd83c7`, PR #37 | Backend connection for optimisation/reporting GUI features. |
| 2026-08-21 | `ae840a2`, `ed9d367`, `0707892`, `4b75bac` | README, UI metadata validation, background actions, scenarios/assignment details. |
| 2026-08-21 | `56bb286`, `9299792` | Examiner DSA demonstrations, correctness/performance artefacts, methodology refinement. |

## Verified late-stage commits

| Commit | Date (UTC) | Author | Verified diff summary |
|---|---|---|---|
| `ae840a2` | 2026-08-21 14:40:23 | Isaac Morrison Quaye | Updated README runtime/integration guidance. |
| `ed9d367` | 2026-08-21 14:43:36 | Isaac Morrison Quaye | Changed location UI validation and added `LocationInputTest`. |
| `0707892` | 2026-08-21 15:00:59 | Isaac Morrison Quaye | Added UI background-action/error helpers and tests; updated several menus. |
| `4b75bac` | 2026-08-21 15:01:10 | Isaac Morrison Quaye | Added routing-scenario and assignment-detail frontend/service support with tests. |
| `56bb286` | 2026-08-21 15:27:50 | Isaac Morrison Quaye | Added DSA demos, correctness generator, efficiency lab, documentation, representative results, and tests; removed a placeholder metrics class. |
| `9299792` | 2026-08-21 15:30:34 | Isaac Morrison Quaye | Refined benchmark methodology/evidence and efficiency-lab tests. |

## Contributor evidence and limitations

Git commit authorship is evidence that an account created a commit; it is not proof that one person alone designed or wrote every component later merged from a branch. The contribution assignments below are therefore stated only at the level supported by the available history.

| Recognised area | Git/GitHub support | Limitation |
|---|---|---|
| Isaac Morrison Quaye — dataset, database, graph representations, coordination | Direct authored commits for shared graph contracts (`95500bc`), adjacency list/matrix (`8ca002b`, `5eeb5bc`), dataset (`ba0ff57`), database stages (`2e76534`–`3e5c141`), and final examiner/performance work. | Git does not prove sole ownership of all coordination or any code integrated by others. |
| Eastwood Tweneboah Osei — repository coordination, shared integration, BST/RBT, backend | Direct setup/merge/integration commits; direct ordered-tree commit `281eaf5`; GitHub PR author/merger metadata for many integration PRs. | Merge authorship does not establish authorship of all merged feature code. |
| Selorm Sem — B-tree, hash/map/set, GUI | Direct commits `ad76d03`, `486862d`, `e0fd912`, Maven migration `c298ad3`; PR #28 metadata supports GUI-foundation contribution. | Final GUI work was integrated across several branches/authors. |
| Asiedu Messiah — linear/binary search, selection/insertion sort, algorithm review | Direct authored implementation/refactor commits for the four search/sort families; GitHub PR metadata for PRs #3–#7. | Trace/reviewer responsibility is not independently evidenced beyond commits. |
| Fauziya Adjeley Adjei — dynamic array, linked list, iterator | `fe09f1c` and PR #19 support this assignment. | No separate granular commits distinguish each structure. |
| Nana Kwasi Agyiri — binary heap/priority queue | `0d69c2d`, `b070ad8`, PR #10 support heap work. | Priority-dispatch integration occurred later and cannot be assigned solely from Git. |
| Jephthah Peprah — stack/queues/deque | `a39d516` and PR #17 support the implementation milestone. | Commit email contains a likely typo; account identity should be confirmed separately if needed. |
| Abukari Issah Kangre — DP/brute force | `63b45a4` is authored by `Issah-Kangre`; later production integration is `5e5ce6b`/PR #27. | The later production integration was committed by another account. |
| Owusu Kenneth Kwabena — Dijkstra/Prim | PR #26 and `4593355` establish the milestone. | Git records Isaac as commit/PR author; individual ownership by Owusu Kenneth is **not independently verifiable from repository history**. |
| Kumi Kwame Esua — greedy assignment/counterexamples | Greedy milestones exist (`3aec2d1`, `47df9c8`, PR #7). | Git records Asiedu Messiah for the implementation/refactor; Kumi’s individual ownership is **not independently verifiable from repository history**. |
| Owusu-Ansah John Nana Kwaku Bonsu — merge/quick sort | `b557f62` is authored by John-owusu-ansah; PR #9 is from the same account. | This supports the milestone, not sole ownership of all associated tests/reviews. |
| Amoh Derrick Kwaku — BFS/DFS | `bf1e73e` and `a42791b` are authored by `amohderrick`; PRs #11/#18 are authored by Derrick Amoh. | Later complexity correction PR #24 was integrated under a different account. |
| Adjapong Stephanie Kyerewaa — DSU/Kruskal | DSU/Kruskal milestone is documented by `a868a08` and PR #25. | Git records Isaac as author; Stephanie’s individual ownership is **not independently verifiable from repository history**. |
| Jimoh Damilola Alliyah / Awuku Duke Asare — no contribution recorded | No matching visible Git author identity or direct supporting commit was found in the inspected history. | Absence from this Git history is not proof of no offline/non-committed work. |

## GitHub pull-request evidence

GitHub CLI authentication was available. The repository showed 37 PRs in the returned metadata: 36 merged and one open PR (#21, `feature/team2-gui` to `main`). Confirmed merged examples include PRs #1–#20, #22–#37, with titles and branch names matching the milestones above. The local merge graph independently contains the corresponding merge commits where applicable.

## Historical limitations and discrepancies

1. Several branches have been merged and may no longer exist as local branches; remote-tracking branches preserve some feature tips, but branch deletion timing is not recoverable from this audit.
2. Squash/integration commits and merge commits obscure granular authorship. Git alone cannot validate every contribution allocation in the team list.
3. The history contains a backend-foundation merge, revert, and reapplication (PRs #29–#32). The audit records this sequence but cannot infer the operational reason beyond commit/PR titles.
4. Local untracked directories `docs/data/official-requirements/`, `docs/data/templates/`, and `results/full-efficiency-lab/` were present at audit time. They are not part of committed history and are deliberately excluded from milestone claims.
5. No Git tags were found. Exact release labels, meeting dates, oral-defence preparation, and uncommitted work are not independently verifiable from repository history.
