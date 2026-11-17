# Delta for test-execution-baseline

<!--
Provenance
- Change: trace-label-fixes — OWNS the supersession of live R5 (delegated by orchestrator; the proposal's "joint with sibling" delta is written once, here).
- Collisions checked (2026-11-17): openspec/changes/mejoramiento-iterativo/ has NO specs/ directory — sibling did not write this delta. openspec/changes/honest-results/ modifies R2 only — R5 untouched there.
- Scope discipline: R5 ONLY is modified here. R2's unpinned count lives in openspec/config.yaml (per honest-results); R1/R3/R4/R6-R8 unchanged.
- Destination evidence (HEAD, 2026-11-17): 12 `*Test` classes / 67 `@Test` methods by static count; surefire-reports hold exactly 12 `TEST-*.xml` summing `tests="67"`; openspec/config.yaml already expects "Tests run: 67" across 12 classes.
- Reconciled at apply (2026-11-17, strict TDD measurement): baseline before this change 12/76 (sibling `mejoramiento-iterativo` landed first, k_s=9, c_s=0); this change adds +6 @Test, 0 classes → final 12/82, gates observed 76 → 78 (commit c09f8ed) → 82 (commit d83d63e), all `Failures: 0, Errors: 0, Skipped: 0`. openspec/config.yaml :15/:26 updated to 82 inside commit d83d63e.
- verify must reconcile: honest-results' Scenario R2.2 still cites "11 TEST-*.xml" (stale in that change's own delta; not edited here — different change's artifact).
-->

## MODIFIED Requirements

### Requirement: R5 Test Discovery Hygiene

Surefire MUST discover all 12 `*Test` classes (82 methods) via its default `**/*Test.java` pattern. `test/TrazaDemo.java` MUST NOT run as a test, nor be converted.
(Previously: pinned 11 classes / 52 methods — the establish-build-test-baseline snapshot, stale since suite growth added `MejoramientoIterativoTest` and later methods; re-pinned to the verified HEAD reality 12 classes / 67 methods at delta-writing time. Final applied reality, locked at apply by measurement: 12 classes / 82 methods — 67 → 76 with sibling `mejoramiento-iterativo` apply (k_s=9) → 82 with this change's +6 tests.)

#### Scenario: R5.1 All twelve classes discovered

- GIVEN the change is fully applied
- WHEN `sh ./mvnw -o clean test` runs
- THEN 12 distinct test classes appear in the surefire output
- AND the run reports `Tests run: 82, Failures: 0, Errors: 0, Skipped: 0`

#### Scenario: R5.2 Demo excluded

- GIVEN `TrazaDemo.java` is present and unchanged
- WHEN the suite runs
- THEN no report or run entry names `TrazaDemo`
