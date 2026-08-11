# Database Evidence Capture Checklist

This directory is reserved for authentic, manually captured evidence from the
`feature/project-database` branch. This checklist does not claim that any
screenshot or terminal output has already been captured.

Capture the following evidence without editing or fabricating command output:

1. Seven-table SQLite schema initialization.
2. Canonical import counts for all seven datasets.
3. SQLite foreign-key enforcement and invalid-FK rejection.
4. Transaction rollback after a failed canonical import.
5. DAO persistent reads, inserts, and updates.
6. Validated audit-event persistence.
7. Planned-to-measured algorithm benchmark persistence using real measurements.
8. Database reload into `AdjacencyListGraph`.
9. Database reload into `AdjacencyMatrixGraph`.
10. Full Maven test-suite success.
11. Production database prohibited-collection scan.
12. Graph/algorithm database-separation scan.

Use numbered filenames consistent with the adjacent graph evidence folders.
Record the real branch, command, date, result, and relevant counts in each
capture. Do not add generated benchmark values until the algorithms have
actually been measured.
