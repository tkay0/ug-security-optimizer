# User-Mode Report Update Notes

## Status and update gate

These notes define the technical-report updates required after the implemented
two-mode GUI has been visually reviewed and approved. The application now uses
a CardLayout shell with Operational Mode as the default and Academic / DSA Lab
as the examiner-facing workspace. The README has been aligned with the checked-in
implementation, but the frozen report must wait for visual approval and final
screenshots.

The frozen submission artifacts must not be edited, regenerated, replaced, or
deleted during GUI development:

- `report/technical-report.docx`
- `report/technical-report.pdf`

After GUI approval, regenerate the report through the project's controlled
report workflow so that the DOCX, PDF, prose, figure numbering, captions, and
screenshots remain synchronized.

## Implemented navigation to document after approval

Operational Mode:

`Dashboard -> Incidents -> Dispatch -> Routes -> Resources -> Reports`

Academic / DSA Lab:

`DSA Lab -> Structures -> Search & Sort -> Graph Algorithms -> Optimization -> Correctness -> Efficiency Lab -> Back to Operations`

Verified final operator-facing details to use in revised prose and screenshots:

- the incident form is titled **Report New Incident** and uses **Incident
  Location**, **Response Destination**, **Incident Type**, **Urgency**,
  **Required Response**, and **Description**;
- campus locations are displayed by name while their existing numeric IDs remain
  the stored/service values;
- Dispatch displays the current status and the next deterministic workflow step,
  with confirmation for cancellation and undo;
- Routes displays a named **Recommended Route**, **Modeled Travel Cost**, and
  selected **Road Conditions**; and
- Operational Reports contains the live incident summary, resource summary, and
  recent workflow/audit history with **Refresh Report**. It contains no algorithm
  run controls or benchmark columns.

The former technical report controls now appear only in the DSA Lab: sorting
run/record controls under **Search & Sort**, BFS/DFS/Dijkstra run/record controls
under **Graph Algorithms**, and recorded timing/memory results under
**Efficiency Lab**.

## 1. Existing report sections requiring textual updates

| Report section | Required update after GUI approval |
| --- | --- |
| Executive Summary | Briefly identify Operational Mode and Academic / DSA Lab as the two audience-oriented entry points. Do not imply that they use separate backends. |
| 1.3 Inputs | Distinguish operational input through Incidents, Dispatch, Routes, and Resources from examiner interaction through the DSA Lab. |
| 1.4 Outputs | Associate operational outputs with Operational Mode and correctness, trace, and efficiency evidence with the DSA Lab. |
| 1.5 Assumptions and Boundaries | Replace any generic statement that the Swing interface is merely "simple" with the verified rationale for separating operational and assessment-facing functions. |
| 3.1 Layers and Responsibilities | Replace the current flat list of Swing tabs with the two navigation modes. Preserve the existing service, algorithm, data-structure, persistence, and database descriptions. |
| 3.2 Dispatch and Optimization Workflow | Explain that the dispatch workflow is reached from Operational Mode, while algorithm inspection and optimization evidence are available through the DSA Lab. Do not change the underlying workflow unless implementation changes it. |
| 4.1 Scheduling Engine | Replace references to the old Priority Queue or DSA Demonstrations navigation with the verified DSA Lab location for the scheduling demonstration. |
| 4.2 Indexing Engine | Replace references to the old DSA Demonstrations screen with the verified Structures area of the DSA Lab. |
| 5.1 Search and Sort | State that examiner-facing demonstrations are accessed through `DSA Lab -> Search & Sort`. |
| 5.2 Graph Algorithms | Replace references to the old Routing screen where appropriate: operational route work belongs under Routes, while graph-algorithm demonstrations belong under `DSA Lab -> Graph Algorithms`. |
| 5.3 Optimization Algorithms | Identify `DSA Lab -> Optimization` as the academic entry point without changing the algorithm descriptions or results. |
| 6 Correctness | Identify `DSA Lab -> Correctness` as the interface for existing correctness evidence, if that screen is confirmed by visual review. |
| 7 Performance Evaluation | Identify `DSA Lab -> Efficiency Lab` as the interface for the existing experiment workflow. Keep the benchmark methodology and results unchanged. |
| 8 Database Integration | Update only UI entry-point names used in examples. Preserve the schema, transaction, DAO, importer, and persistence descriptions. |
| 9 Responsible Algorithm Selection | Update navigation terminology only where the old screen names appear. The algorithm-selection analysis remains unchanged. |
| Appendices and evidence references | Update the GUI screenshot inventory, figure cross-references, and any UI verification checklist after replacement screenshots are captured. |

## 2. Architecture descriptions requiring updates

After visual approval, revise the presentation-layer description and
architecture diagram to show the implemented two navigation surfaces:

- Operational Mode for Dashboard, Incidents, Dispatch, Routes, Resources, and
  Reports. The Routes section also retains location and road maintenance in the
  secondary nested **Campus Network** view.
- Academic / DSA Lab for Structures, Search & Sort, Graph Algorithms,
  Optimization, Correctness, and Efficiency Lab.

The architecture must continue to show that both modes call the same frontend
adapters, application services, custom data structures and algorithms,
persistence layer, database, and runtime state. The diagram must not imply a
duplicate database, duplicate service layer, or separate algorithm
implementation for either mode. If mode switching introduces a navigation
controller or shell, show it only in the presentation layer.

Figure 2, the layered architecture and operational/evidence-flow diagram,
should be updated at the presentation layer. Figure 3, the emergency dispatch
workflow, remains technically valid unless GUI implementation changes the
underlying workflow; its UI labels may need to be aligned with the new
navigation.

## 3. Existing GUI figures to review or replace

The following figures use the old single-tab navigation or old screen names and
must be visually reviewed after the new GUI is approved:

| Existing figure | Required action |
| --- | --- |
| Figure 1 - Roads module | Replace with the approved Campus Routes view if the old module is no longer directly exposed. |
| Figure 4 - Dispatch Workflow module | Replace with the approved Dispatch view. |
| Figure 5 - Priority Queue module | Replace with the corresponding DSA Lab Structures or scheduling view, if implemented there. |
| Figure 6 - DSA Demonstrations module | Replace with the DSA Lab landing/navigation view or the corresponding Structures view. |
| Figure 7 - Dijkstra shortest-path view | Recapture in Campus Routes or Graph Algorithms according to the final placement of the feature. |
| Figure 8 - Optimization module | Recapture under `DSA Lab -> Optimization`. |
| Figure 9 - Correctness trace view | Recapture under `DSA Lab -> Correctness` if that is the verified implementation. |
| Figure 19 - Locations module | Replace or recapture only if the old screen is removed or materially redesigned. |
| Figure 20 - Reports module | Replace with `Operational Mode -> Reports`, showing the incident/resource summary and recent audit history without technical algorithm controls. |
| Figure 21 - Efficiency Lab | Recapture under `DSA Lab -> Efficiency Lab` if the surrounding navigation changes. |

Retain any figure whose visible content and caption remain accurate, but crop or
replace screenshots that display obsolete tabs or navigation labels.

## 4. Recommended new screenshots

Capture these only from the implemented, approved application and use realistic
repository data already supported by the runtime:

1. Operational Mode Dashboard.
2. Incidents, showing the final named location selectors, readable urgency and response options, and search/status/urgency filters.
3. Dispatch.
4. Campus Routes.
5. Resources, showing the named Home Location selector and readable type/status values.
6. DSA Lab landing page and navigation.
7. One representative DSA Lab feature; Correctness is recommended because it
   directly demonstrates assessment evidence, but use Structures instead if it
   better represents the approved GUI.

Reports and Efficiency Lab may also be recaptured when their surrounding
navigation has visibly changed, even if their internal content is unchanged.

## 5. Suggested figure captions

Use or adapt these captions only after confirming that each screenshot visibly
supports the wording:

- **Operational Mode Dashboard:** "Operational Mode dashboard providing the
  primary entry point to campus security and emergency-response functions."
- **Incidents:** "Incidents view for reporting and filtering service requests
  using named campus locations and operator-facing terminology."
- **Dispatch:** "Dispatch view connecting service requests, available resources,
  and the established assignment workflow."
- **Campus Routes:** "Campus Routes view showing the recommended campus route,
  modeled travel cost and selected project road conditions."
- **Resources:** "Resources view for reviewing availability and registering
  response resources against named campus home locations."
- **DSA Lab:** "Academic / DSA Lab navigation exposing the assessed structures,
  algorithms, correctness evidence, and efficiency experiments."
- **Representative DSA feature:** "Correctness view presenting reproducible
  algorithm evidence from the shared application runtime." If Structures is
  shown instead: "Structures view demonstrating the custom data structures used
  by the application."

## 6. Design-decision paragraph for the revised report

Add the following paragraph only after the implemented two-mode interface is
visually approved:

> The application now separates operational functionality from examiner-facing academic functionality. Operational Mode presents incidents, dispatch, routing, resources and reports using user-facing terminology. Academic / DSA Lab exposes the custom data structures, algorithms, correctness evidence and performance experiments required for assessment. Both modes use the same backend services, database and runtime state.

## 7. Existing claims made inaccurate by the new navigation

When regenerating the report, locate and correct claims that:

- describe the application as one flat set of top-level tabs;
- list Locations & Roads, Routing, Requests & Resources, Search & Sort,
  Dispatch Workflow, Priority Queue, Optimization, DSA Demonstrations,
  Efficiency Lab, and Reports as peer navigation items;
- identify DSA Demonstrations as the sole entry point for custom scheduling and
  indexing demonstrations;
- describe the old Routing screen as the only presentation location for graph
  work without distinguishing operational Routes from academic Graph
  Algorithms;
- refer to Priority Queue, Dispatch Workflow, or Optimization as top-level tab
  names when their final approved locations are inside a mode;
- omit the route back from the DSA Lab to Operational Mode; or
- imply that academic demonstrations use a separate backend or separate data
  state.

The README's interface overview, available-functionality list, and run guidance
must be updated at the same time. Until then, it must continue to describe the
interface that the checked-in source actually launches.

## 8. Report sections that remain technically unchanged

The navigation redesign does not by itself change the following technical
content:

- dataset composition, provenance, validation, and localisation;
- database and schema design;
- custom data-structure implementations;
- algorithm implementations;
- correctness evidence and trace contents;
- benchmark methodology and benchmark results;
- time- and space-complexity analysis;
- index-derived parameters; and
- contribution and attendance records.

These sections require only cross-reference or screenshot-caption updates if
they explicitly name an old screen. Their technical results must not be
rewritten merely because navigation changes.

## Final report-refresh checklist

Before regenerating the frozen report artifacts:

1. Verify every documented destination exists in the approved GUI.
2. Verify mode switching preserves the same database and runtime state.
3. Confirm the final user-facing labels and navigation order.
4. Capture screenshots at a consistent window size using current repository
   data and no fabricated states.
5. Replace obsolete screenshots and update all figure numbers, captions, and
   in-text references.
6. Update the README and report prose together so they describe the same
   released application.
7. Re-run the project's test and report-generation checks before replacing the
   frozen DOCX/PDF in a separately approved task.
