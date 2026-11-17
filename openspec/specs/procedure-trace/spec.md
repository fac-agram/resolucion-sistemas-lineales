# procedure-trace

<!--
Provenance
- Capability: procedure-trace (new capability)
- Origin change: trace-label-fixes (retroactive formalization of landed 8b40d42 + remaining R4/R2.2 fixes)
- Source: openspec/changes/archive/2026-11-17-trace-label-fixes/specs/procedure-trace/spec.md
- Synced: 2026-11-17
- Origin verdict: pass_with_warnings (zero CRITICAL; commits c09f8ed + d83d63e; see archive-report.md in the archived change)
- Requirements R1-R6 and their scenarios are preserved verbatim from the delta spec; ids unchanged.
- R5 partial attestation (verify WARNING-1): Scenario R5.1 is COMPLIANT for the manual-load path
  (verified live via a JavaFX harness driving the real FXML + controller) but only STATICALLY verified
  for the file-load path (code block SistemaFXController.java:180-183 statically identical to the
  live-verified :278-281 block; FileChooser is a native dialog, not headlessly automatable).
  A ~30-second human click-through (solve A -> load file B -> panels empty, pane collapsed+disabled)
  remains PENDING as attestation; accepted at archive as static-identical per the verify disposition.
- Carried notes: verify WARNING-2 (apply-reported transient flake, unreproduced in 3 fresh full runs —
  flake-watch continues; capture target/surefire-reports BEFORE any clean if it ever reddens);
  WARNING-3 (TDD evidence in prose/commit-body form, substance independently reproduced — no spec impact);
  SUGGESTION (test contains("/ 1.0") substring could over-match a hypothetical "/ 1.0000000001" label;
  unreachable with current deterministic fixtures).
- Cross-change reconcile still open (NOT this capability): honest-results' Scenario R2.2 cites
  "11 TEST-*.xml"; reality is 12 — owned by that change's own archive, untouched here.
-->

## Purpose

Contract for honest, monotonic, pedagogically meaningful solution traces: step-index conventions, pivot normalization recording, equation label order, unified iteration counting, stale-trace clearing, and trace snapshot integrity. Formalizes landed commit `8b40d42` plus the remaining fixes delivered by `c09f8ed` (iteration count) and `d83d63e` (no-op suppression).

## Requirements

### Requirement: R1 Step-index monotonicity

Every recorded `Paso` MUST have `indice >= 0`, strictly increasing per execution. A row swap MUST consume the active index as a numbered step. The `-1` return of `pivoteo_simple` is an exception sentinel and MUST NOT surface in a trace; only the initial snapshot (`indice == 0`) MAY render as "Inicio".

> Test mapping: existing `GaussJordanTest.trazaSinIndicesNegativos`/`trazaIndicesMonotonicos`, `EliminacionGaussianaTest.trazaSinIndicesNegativos`; NEW swap-first assertion for `EliminacionGaussiana` (TDD).

#### Scenario: R1.1 Swap-first matrix

- GIVEN a system whose first pivot is zero (swap at column 0)
- WHEN the solver runs with a trace
- THEN the swap step has `indice == 1` and later indices strictly increase
- AND no step has `indice < 0` nor renders as "Inicio"

### Requirement: R2 Gauss-Jordan pivot normalization

Gauss-Jordan MUST normalize each pivot to 1 (within 1e-12) at the pivot position, recording steps without index collisions. A no-op normalization (`|pivote − 1| <= 1e-12`) MUST NOT be recorded (remaining: suppress cosmetic "F1 ← F1 / 1.0").

> Test mapping: existing `GaussJordanTest.verificaDiagonalUnitaria`; NEW no-op-suppression assertion (TDD).

#### Scenario: R2.1 Unit diagonal, no collisions

- GIVEN a non-singular system solved with a trace
- WHEN execution finishes
- THEN the final matrix holds 1 at each pivot position (tolerance 1e-12)
- AND swap/normalization/elimination indices strictly increase

#### Scenario: R2.2 No cosmetic no-op normalization (remaining)

- GIVEN a pivot already equal to 1 (`|pivote − 1| <= 1e-12`)
- WHEN Gauss-Jordan processes that column
- THEN the trace contains no "… / 1.0" normalization step

### Requirement: R3 Equation label order

The UI MUST label equations and solutions `x1..xn` left-to-right, consistent with coefficient column order.

> Test mapping: manual GUI checklist; data path covered by `SistemaControllerTest`.

#### Scenario: R3.1 3x3 label order

- GIVEN a 3x3 system loaded in the UI
- WHEN the equation panel renders
- THEN coefficients read `·x1 ·x2 ·x3` matching columns left-to-right and the solution list shows `x1, x2, x3` in order

### Requirement: R4 Unified iteration-count convention

Jacobi, Gauss-Seidel, SOR, and Relajación Simple MUST count the iteration that proves convergence: `getIteraciones()` MUST equal the pass index `k` where the stop criterion first holds. Relajación Simple MUST match (remaining: it breaks before assigning `iteraciones = k`). `MejoramientoIterativo`: out of scope (sibling change).

> Test mapping: NEW convergence/cap assertions in `RelajacionSimpleTest` + sibling-parity check (TDD); re-pin per R4.3.

#### Scenario: R4.1 Converging at pass k

- GIVEN Relajación Simple first measures residual norm < 1e-7 on pass k
- WHEN `resolver()` returns
- THEN `getIteraciones() == k` and `convergio() == true`

#### Scenario: R4.2 Iteration cap

- GIVEN a system never satisfying the tolerance
- WHEN `resolver()` returns
- THEN `getIteraciones() == 1000` and `convergio() == false`

#### Scenario: R4.3 Declared drift — intentional re-pin of RelajacionSimpleTest.java:45

- GIVEN `traza.totalPasos() == iteraciones + 1` pins the OLD uncounted-convergence convention
- WHEN the R4 fix lands (same commit, strict TDD)
- THEN it is re-pinned to "initial snapshot + one step per executed update" under the unified count
- AND RED evidence (failing pre-fix) is recorded in apply

### Requirement: R5 Stale trace cleared on load

Loading a new system (file or manual) MUST clear previous result and procedure panels and collapse/disable the procedure pane.

> Test mapping: manual GUI checklist (no UI tests).

#### Scenario: R5.1 Reload clears panels

- GIVEN system A's trace is displayed
- WHEN the user loads system B
- THEN procedure and result panels are empty and disabled until the next solve

> **Verification status (2026-11-17)**: PARTIAL. Manual-load path verified live through the real
> handler chain (JavaFX harness on DISPLAY=:1, automated dialog). File-load path statically identical
> (`SistemaFXController.java:180-183`) but the native FileChooser dialog is not headlessly automatable;
> ~30s human click-through attestation pending — accepted at archive as static-identical (verify
> WARNING-1). The normative requirement text above is unchanged.

### Requirement: R6 Trace deep-copy integrity

`Paso` MUST deep-copy matrix and solution vector at construction; later solver mutation MUST NOT alter recorded steps. (Already true; contractualized.)

> Test mapping: NEW snapshot-immunity test (TDD).

#### Scenario: R6.1 Snapshot immune to mutation

- GIVEN a solver records a `Paso`, then keeps mutating its matrix
- WHEN the trace is read after `resolver()`
- THEN each `Paso.matriz()` equals its record-time state
