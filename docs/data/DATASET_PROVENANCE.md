# Dataset Provenance and Localisation Note

## Project
University of Ghana Campus Security and Emergency Response Optimizer

## Purpose
Coursework seed data for graph routing, reachability, MST algorithms, queue/priority scheduling, searching, sorting, resource assignment, database loading, testing, and later performance experiments.

## Real/localised content
`locations.csv` uses real University of Ghana/Legon place and facility names verified from University of Ghana web pages and the University-affiliated Radio Univers campus guide. Every location row includes a `source_url`.

## Central synthetic-data and privacy statement
The `x_coord` and `y_coord` values are synthetic schematic local coordinates in metres with the Main University Gate as a project origin; they are not GPS or survey coordinates. Road connectivity, distances, travel times, traffic levels and condition weights are project-generated. Service requests, resources, road scenarios and audit events are synthetic academic records. No real incidents, people, victims, personal information, live assets or live security operations are represented.

## Roads and derived values
The 100 graph links are suitable for coursework algorithms, not real navigation.

The application calculates routing cost at runtime as `travel_time_min * condition_weight`. A derived routing cost is not stored in `roads.csv`.

Request priority is calculated by the application/algorithm layer from the project rules and index-number-derived parameters. It is not stored as a precomputed `priority_score` in `service_requests.csv`.

## Algorithm-run dataset
`algorithm_runs.csv` contains 30 planned benchmark rows. `timeNs`, `memoryKb`, and `dateRun` are intentionally blank. Fill them only from real runs of the team's Java implementation. Do not submit fabricated timings as empirical evidence.

## Official template alignment
The first columns of `locations.csv`, `roads.csv`, `service_requests.csv`, and `resources.csv` use the exact lecturer-supplied template names and ordering. Existing project-specific extension columns follow those required columns. `algorithm_runs.csv`, `road_scenarios.csv`, and `audit_events.csv` retain their purpose-specific project schemas.

## Sources
- https://www.ug.edu.gh/campus/main
- https://old1.ug.edu.gh/about/overview
- https://univers.ug.edu.gh/finding-your-way-on-the-streets-of-legon-a-level-100-guide/
- https://www.ug.edu.gh/aad/accomodation
- https://old1.ug.edu.gh/academics/colleges
- https://law.ug.edu.gh/about-us
- https://www.ug.edu.gh/careers/contact-us
- https://www.ug.edu.gh/announcement/vacancies-senior-level-administrative-positions-safety-and-security-services
- https://ugcs.ug.edu.gh/service-catalogue/academic-computing/design-library-systems-and-research

## Generation date
2026-08-08


## Road-model upgrade
The baseline `roads.csv` now contains:
- a descriptive `route_label` constructed from the endpoint location names;
- varied synthetic `travel_time_min` values based on route distance, baseline traffic class, and a deterministic junction/turn delay;
- the inputs needed to calculate routing cost at runtime without duplicating derived data.

The route labels are descriptive project labels, not claims about official street names.

## Controlled road scenarios
`road_scenarios.csv` contains 12 controlled what-if records across three scenarios:
- `ACCESS_BLOCKAGE_DRILL` — temporary blocked links chosen so the 50-location graph remains connected;
- `EVENT_CROWD` — elevated travel-time multipliers;
- `RAINY_EVENING` — increased condition/travel penalties.

These scenarios let the team test rerouting and changing constraints without corrupting the baseline seed graph.
