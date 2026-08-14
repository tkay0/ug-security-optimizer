# Index-Derived Project Parameters

## Purpose

The project brief requires at least three algorithm parameters to be derived
reproducibly from team-member index numbers.

This project derives three parameters from all 15 group members:

1. Incident priority weight
2. Optimization budget
3. Benchmark random seed

## Team index numbers

| Index Number | Member |
|---|---|
| 22079872 | Isaac Morrison Quaye |
| 22040957 | Eastwood Tweneboah Osei |
| 22243032 | Selorm Sem |
| 22153370 | Fauziya Adjeley Adjei |
| 22136496 | Nana Kwasi Agyiri |
| 22036173 | Jephthah Peprah |
| 22404379 | Abukari Issah Kangre |
| 22401243 | Owusu Kenneth Kwabena |
| 22392636 | Asiedu Messiah |
| 22396052 | Kumi Kwame Esua |
| 22409852 | Owusu-Ansah John Nana Kwaku Bonsu |
| 22367855 | Amoh Derrick Kwaku |
| 22381560 | Adjapong Stephanie Kyerewaa |
| 22325573 | Jimoh Damilola Alliyah |
| 22400447 | Awuku Duke Asare |

## Parameter 1 — Priority Weight

Take the final two digits of every member index number:

72, 57, 32, 70, 96, 73, 79, 43, 36, 52, 52, 55, 60, 73, 47

Their sum is:

897

Formula:

PRIORITY_WEIGHT = 1 + (897 mod 5)

Therefore:

PRIORITY_WEIGHT = 3

This parameter is reserved for the project's incident/request priority
calculation.

## Parameter 2 — Optimization Budget

Use the same final-two-digit total.

Formula:

OPTIMIZATION_BUDGET = 50 + (897 mod 51)

Since:

897 mod 51 = 30

Therefore:

OPTIMIZATION_BUDGET = 80

This parameter is reserved for the Dynamic Programming / Brute Force
constrained optimization problem.

## Parameter 3 — Benchmark Seed

Take the final three digits of every member index number:

872, 957, 032, 370, 496, 173, 379, 243, 636, 052, 852, 855, 560, 573, 447

Their sum is:

7497

Formula:

BENCHMARK_SEED = sum of the final three digits of all team index numbers

Therefore:

BENCHMARK_SEED = 7497

This value will be used as the deterministic seed for reproducible performance
experiments and generated benchmark inputs.

## Final values

| Parameter | Value | Intended Use |
|---|---:|---|
| PRIORITY_WEIGHT | 3 | Incident/request prioritization |
| OPTIMIZATION_BUDGET | 80 | DP and brute-force optimization |
| BENCHMARK_SEED | 7497 | Reproducible benchmark generation |

## Integration rule

These values are project-level configuration parameters.

They must not be substituted into unrelated algorithms merely to satisfy the
index-number requirement.

Their intended integrations are:

- PRIORITY_WEIGHT -> priority calculation
- OPTIMIZATION_BUDGET -> DP / brute-force constrained optimization
- BENCHMARK_SEED -> empirical performance experiment input generation

The formulas and values must remain consistent in the implementation,
correctness traces, performance evidence, final report, and oral defense.
