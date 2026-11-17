# test-execution-baseline

<!--
Provenance
- Capability: test-execution-baseline (new capability)
- Change: establish-build-test-baseline
- Source: openspec/changes/archive/2026-11-17-establish-build-test-baseline/specs/test-execution-baseline/spec.md
- Synced: 2026-11-17
- Origin verdict: pass_with_warnings (zero CRITICAL); see archive-report.md in the archived change
- Requirements R1-R8 and their scenarios are preserved verbatim from the delta spec.
- Synced: 2026-11-17 — R5 superseded by change trace-label-fixes (archive:
  openspec/changes/archive/2026-11-17-trace-label-fixes/; origin verdict pass_with_warnings, commits
  c09f8ed + d83d63e; see its archive-report.md). R5 re-pinned 11/52 -> 12 classes / 82 methods via the
  measured chain 67 (delta-writing HEAD) -> 76 (sibling mejoramiento-iterativo apply, k_s=9) -> 82
   (this change +6 tests); previously-note preserved inside the requirement. R1-R4, R6-R8 and the
   historical Delivery Mapping untouched.
- Synced: 2026-11-17 — R2 superseded by change honest-results (archive:
  openspec/changes/archive/2026-11-17-honest-results/; origin verdict pass_with_warnings, commits
  8e0f854 + 6ab9fb9 (squashed) on base 3f2fd7e, archived at HEAD 96f0b7d; see its archive-report.md).
  R2's literal `Tests run: 52` pinning is replaced by the unpinned single-source formulation: the
  expected total is maintained in openspec/config.yaml (currently 82 across 12 classes — already
  matching, no config edit at archive). Scenario R2.2 reconciled 11 -> 12 `TEST-*.xml` during this
  merge (measured at HEAD; verify WARNING-3; previously-note preserved inside the scenario). The
  sibling-attributed drift note: this change's own tip measured 59/11 at 6ab9fb9; 82/12 at HEAD.
  R1, R3-R8 (incl. the trace-label-fixes R5 re-pin 12/82) and the historical Delivery Mapping
  untouched.
-->

## Requirements

### Requirement: R1 Build Reproducibility

`sh ./mvnw -o clean compile` MUST succeed on JDK 17 with no property override. No `.class` SHALL remain under `src/`, and the resource phase MUST copy exactly 2 assets (`Sistema.fxml`, `estilos.css`). The build MUST also succeed on JDK 21+ via `maven.compiler.release=17`.

#### Scenario: R1.1 Clean build from empty target

- GIVEN `target/` is deleted and no `.class` exists under `src/`
- WHEN `sh ./mvnw -o clean compile` runs on JDK 17
- THEN it exits 0, logs `Copying 2 resources from src`, and prints nothing for `find src -name '*.class'`
- AND `target/classes` holds only classes compiled from current sources

#### Scenario: R1.2 Build on a JDK 21 machine

- GIVEN the same checkout runs under JDK 21
- WHEN `sh ./mvnw -o clean compile` runs without extra flags
- THEN it exits 0, proving `release=17` does not require a JDK 17 JVM

> **Verification status (2026-11-17)**: UNVERIFIED locally. The verification host has only JDK 17 installed, so this scenario could not be exercised. Pending CI verification (JDK 21 matrix job); not yet externally verified. The normative requirement text above is unchanged.

### Requirement: R2 Test Execution

`sh ./mvnw -o clean test` MUST execute the full discovered JUnit 5 test set, exit 0, and report `Failures: 0, Errors: 0, Skipped: 0`. It MUST write one `TEST-*.xml` per test class and leave no `*.dump`/`*.dumpstream`. Surefire MUST use its JUnit Platform provider; the `JUnit3Provider` fallback with `Tests run: 0` MUST NOT occur. The expected total test count MUST be maintained in `openspec/config.yaml`; this requirement SHALL NOT pin a literal count.
(Previously: pinned the literal `Tests run: 52` — the establish-build-test-baseline snapshot at conversion time.)

> Why the literal pin is removed: adding tests is legitimate behavior, and a literal pin turns the anti-false-green check into a change-detector that fails on every valid test addition. The count remains enforced, but against the single source in `openspec/config.yaml`.

#### Scenario: R2.1 Green baseline

- GIVEN the change is fully applied
- WHEN `sh ./mvnw -o clean test` runs
- THEN the output contains `Tests run: <expected>, Failures: 0, Errors: 0, Skipped: 0` and `BUILD SUCCESS`
- AND `<expected>` equals the test total declared in `openspec/config.yaml`

#### Scenario: R2.2 Real runner, per-class reports

- GIVEN a clean `target/`
- WHEN the same command runs
- THEN `target/surefire-reports/` holds 12 `TEST-*.xml` reports with no `Tests run: 0`, `JUnit3Provider`, or fork dump
(previously: 11 `TEST-*.xml` — the delta text written at this change's own era, 11 classes / 59 tests at `6ab9fb9`; reconciled to 12 during the honest-results archive merge because sibling `mejoramiento-iterativo` added the 12th class `MejoramientoIterativoTest`; measured 12 at HEAD `96f0b7d` — verify WARNING-3. If a future change adds a class, re-measure against `config.yaml`.)

### Requirement: R3 Failure Propagation (Anti-False-Green)

A single failing assertion MUST fail the build: no slice MAY read green while a test is red. Numerical assertions MUST compare with a tolerance (e.g. `1e-6`), never exact equality. Any wrong expectation MUST yield `BUILD FAILURE`, a non-zero exit code, a non-zero `Failures`/`Errors` count, and a `<failure>`/`<error>` in the matching surefire XML.

#### Scenario: R3.1 Assertion failure fails the build

- GIVEN the suite is green
- WHEN one expected literal (e.g. `NormasTest` `7.0` → `8.0`) is mutated
- THEN `sh ./mvnw -o test` exits non-zero with `BUILD FAILURE`, `Failures: 1`, and `<failure` in `TEST-NormasTest.xml`
- AND reverting the mutation restores `BUILD SUCCESS`

#### Scenario: R3.2 Exception expectation mismatch fails the build

- GIVEN a test asserts `assertThrows(IllegalArgumentException.class, ...)`
- WHEN the expected type is changed to one the code does not throw
- THEN the build fails with a non-zero count, an `<error>`/`<failure>` entry naming the test method

### Requirement: R4 No Process-Killing or Print-Based Signals

Tests MUST NOT call `System.exit` or use `TEST_OK`, `TEST_FALLO`, or any stdout marker as pass evidence. Results MUST flow only through the framework and Maven exit status.

#### Scenario: R4.1 Harness protocol removed

- GIVEN the change is fully applied
- WHEN `grep -rn "System.exit\|TEST_OK\|TEST_FALLO" test/` runs
- THEN it returns zero matches

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

### Requirement: R6 Wrappers and Tooling

`mvnw` MUST be mode `100755` in the Git index and executable in the working tree. `./mvnw -v` MUST run without `sh` and without a permission error.

#### Scenario: R6.1 Executable bit in the index

- GIVEN the change is applied
- WHEN `git ls-files -s mvnw` runs
- THEN the mode is `100755`

#### Scenario: R6.2 Direct invocation

- GIVEN a fresh checkout of the merged commit
- WHEN `./mvnw -v` runs
- THEN it prints the Maven and Java versions and exits 0

### Requirement: R7 Offline Capability

Every plugin and dependency version needed by `clean compile`, `clean test`, and `clean package` MUST be pinned in `pom.xml`. After one warm run, `-o` builds MUST resolve without network, including surefire's JUnit Platform provider.

#### Scenario: R7.1 Offline suite from a warm cache

- GIVEN the offline cache was warmed once with the pinned versions
- WHEN `sh ./mvnw -o clean test` runs with networking disabled
- THEN it exits 0 with no artifact or provider resolution error

> **Verification status (2026-11-17)**: UNVERIFIED locally. Networking cannot be physically disabled on the verification host; all `-o` gates resolved from the warm `~/.m2` cache and exited 0, which is the strongest available local proxy. Full network-off proof pending CI; not yet externally verified. The normative requirement text above is unchanged.

### Requirement: R8 Scope Exclusion Guard

The change MUST NOT migrate the layout, parse `pruebas txt/` fixtures, alter algorithms, fix the GUI/FXML case mismatch, rewrite `README.md`, or implement `MejoramientoIterativo`. Only `pom.xml`, `test/*.java`, untracked `src/**/*.class`, the `mvnw` mode, and `openspec/config.yaml` MAY change.

#### Scenario: R8.1 Diff stays in scope

- GIVEN the full change is applied
- WHEN `git diff --name-only` and `git status` are inspected
- THEN no `src/**/*.java`, `README.md`, `pruebas txt/**`, or resource file appears and `find src -name '*.class'` is empty

#### Scenario: R8.2 Deferred subsystems untouched

- GIVEN the same checkout
- WHEN `MainApp.java`, `MejoramientoIterativo.java`, and `README.md` are diffed against the base commit
- THEN they show no changes

## Delivery Mapping (historical, from the originating change)

Recorded for traceability. This section is not normative; the requirements above are the contract.

| Req | PR slice | Proposal criteria | Cumulative expectation |
|-----|----------|-------------------|------------------------|
| R1 | PR1 | SC1, SC3, SC7 | clean compile, `Copying 2 resources` |
| R2 | PR1 → PR2 → PR3 | SC1 | 5 → 34 → 52 tests, 1 → 6 → 11 classes |
| R3 | PR1, enforced through PR3 | SC2 | mutation fails in PR1 and in every later slice |
| R4 | PR1 → PR3 | SC4 | grep clean for each converted file |
| R5 | PR1 (R5.2), PR3 (R5.1) | SC1 | TrazaDemo never runs; 11 classes at PR3 |
| R6 | PR1 | SC5 | `100755`, `./mvnw -v` works |
| R7 | PR1, holds through PR3 | SC1, SC6 | pinned offline resolve |
| R8 | all slices | SC3, SC6, SC7 | no out-of-scope path in any slice |

Proposal criteria: SC1 green suite, SC2 mutation fails build, SC3 no stale `.class` / 2 resources, SC4 no exit/marker strings, SC5 `mvnw` mode, SC6 package without stale classes, SC7 zero `src/**/*.java` changes.
