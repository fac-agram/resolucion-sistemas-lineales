# Archive Report: Establish Build and Test Baseline

- **Change**: `establish-build-test-baseline`
- **Capability**: `test-execution-baseline` (new)
- **Archived**: 2026-11-17
- **Archive path**: `openspec/changes/archive/2026-11-17-establish-build-test-baseline/`
- **Repository state at archive**: `main` @ `db0c95d`, base commit `e7bb7be`, 14 commits
- **Verification verdict**: `pass_with_warnings` — zero CRITICAL, two environmental UNVERIFIED scenarios (R1.2, R7.1)
- **Mode**: openspec (`strict_tdd: false`)

---

## 1. What Was Delivered

The repository moved from an unverifiable build to an honest, offline-capable build with a real test runner:

1. **Build retargeted to Java 17** — `maven.compiler.release` changed from 21 to 17 in `pom.xml`, so the project compiles on the installed JDK 17 and, by construction, on JDK 21+.
2. **JUnit 5 test execution** — `org.junit.jupiter:junit-jupiter:5.11.4` (test scope) plus `maven-surefire-plugin:3.5.4` pinned in `pom.xml`. Surefire auto-detects `JUnitPlatformProvider`; the previous `JUnit3Provider` / `Tests run: 0` false-green path is gone.
3. **52 JUnit 5 tests across 11 classes** — all eleven `main()` harnesses in `test/` were mechanically converted; every existing literal expectation, tolerance, and exception type was preserved as the oracle. No `System.exit`, `TEST_OK`, `TEST_FALLO`, or stdout pass markers remain.
4. **Resource-copy hygiene** — `<resources>` excludes `**/*.java` and `**/*.class`; the 13 untracked stale `.class` files under `src/` were deleted (working-tree step, gitignored). `clean compile` now copies exactly 2 assets.
5. **Wrapper fixed** — `mvnw` mode changed `100644` → `100755` in the Git index (mode-only change, no content change).
6. **Truthful SDD config** — `openspec/config.yaml` records `sh ./mvnw -o clean test` and the expected 52-test baseline.

Delivered as three stacked slices on `main`: PR1 (plumbing + NormasTest, 5 tests), PR2 (direct methods + controller, cumulative 34), PR3 (iterative + sensitivity, cumulative 52).

`src/**/*.java` (23 files), `README.md`, `pruebas txt/**`, FXML/CSS resources, `test/TrazaDemo.java`, and `MejoramientoIterativo` were not changed.

---

## 2. Commits (14 since `e7bb7be`)

| # | SHA | Message (repo convention: Spanish, `type: descripcion`) |
|---|-----|--------------------------------------------------------|
| 1 | `024cc63` | `build: fijar Java 17 y agregar JUnit 5 con surefire 3.5.4` |
| 2 | `7efc1f8` | `chore: hacer ejecutable el wrapper mvnw` |
| 3 | `a6fd55f` | `test: convertir NormasTest a JUnit 5` |
| 4 | `c358615` | `test: convertir CholeskyTest a JUnit 5` |
| 5 | `69b0b57` | `test: convertir CroutTest a JUnit 5` |
| 6 | `c02ac15` | `test: convertir EliminacionGaussianaTest a JUnit 5` |
| 7 | `f7050e3` | `test: convertir GaussJordanTest a JUnit 5` |
| 8 | `3c89d05` | `test: convertir SistemaControllerTest a JUnit 5` |
| 9 | `cfc9bb5` | `test: convertir JacobiTest a JUnit 5` |
| 10 | `63f8f75` | `test: convertir GaussSeidelTest a JUnit 5` |
| 11 | `c51f38e` | `test: convertir SORTest a JUnit 5` |
| 12 | `89fc060` | `test: convertir RelajacionSimpleTest a JUnit 5` |
| 13 | `6a1487e` | `test: convertir AnalisisSensibilidadTest a JUnit 5` |
| 14 | `db0c95d` | `docs: registrar el cambio establish-build-test-baseline` |

Composition: 1 `build:`, 1 `chore:`, 11 `test:`, 1 `docs:`. Diff surface `e7bb7be..HEAD` = 21 paths: `M mvnw`, `M pom.xml`, 11 `M test/*Test.java`, 7 `A openspec/**` change artifacts plus `A openspec/config.yaml` and `A openspec/specs/.gitkeep` / `A openspec/changes/archive/.gitkeep`. Zero production-source changes.

---

## 3. Final Gate Evidence

All gates were re-executed independently by `sdd-verify`; claims from `apply-progress` were treated as claims only.

| Gate | Command | Observed |
|------|---------|----------|
| Compile | `sh ./mvnw -o clean compile` | exit 0, `BUILD SUCCESS`, `Copying 2 resources from src to target/classes`, `Compiling 23 source files with javac [debug release 17]` |
| Stale binaries | `find src -name '*.class'` | 0 files; `git ls-files 'src/**/*.class'` → 0 |
| Test suite | `sh ./mvnw -o clean test` | exit 0, `Tests run: 52, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`, provider `org.apache.maven.surefire.junitplatform.JUnitPlatformProvider` |
| Reports | `ls target/surefire-reports/TEST-*.xml \| wc -l` | 11; `*.dump*` → 0; `JUnit3Provider` → 0; `tests="0"` → 0; no `TrazaDemo` entry |
| Package | `sh ./mvnw -o clean package` | exit 0, `BUILD SUCCESS`; jar `.class` count 26 == `target/classes` `.class` count 26 |
| Anti-false-green R3.1 | literal mutation `NormasTest 7.0 → 8.0`, then `sh ./mvnw -o test` | exit 1, `Failures: 1`, `BUILD FAILURE`, `<failure message="‖(3,-4)‖₁ ==> expected: <8.0> but was: <7.0>">`; reverted, suite back to 52 green |
| Anti-false-green R3.2 | `assertThrows` expected-type swap `IllegalArgumentException → IllegalStateException` | exit 1, `Failures: 1`, `<failure message="norma1 vector null ==> Unexpected exception type thrown, expected: <java.lang.IllegalStateException> but was: <java.lang.IllegalArgumentException>">` naming `verificaValidaciones`; reverted, tree clean |
| Process-kill / markers | `grep -rn "System.exit\|TEST_OK\|TEST_FALLO" test/` | no matches (exit 1) |
| Wrapper | `git ls-files -s mvnw`; `./mvnw -v` | `100755`; Maven 3.9.11 + Java 17.0.20.1, exit 0 |
| Scope | `git diff --name-status e7bb7be..HEAD` | 21 expected paths; negative diff for `src/**`, `README.md`, `pruebas txt`, `test/TrazaDemo.java` → empty |

No red state survived; final `git status --porcelain` at `db0c95d` was empty before this archive work.

---

## 4. Requirement Matrix

| Req | Status | Evidence summary |
|-----|--------|------------------|
| R1 Build Reproducibility | **PASS** (R1.1) / **UNVERIFIED** (R1.2) | Clean compile exit 0, 2 resources, 26 classes from 23 sources, 0 `.class` under `src/`. R1.2 not reproducible: only JDK 17 installed. |
| R2 Test Execution | **PASS** | 52/52 green, `JUnitPlatformProvider`, 11 `TEST-*.xml`, 0 dumps. |
| R3 Failure Propagation | **PASS** | Both red proofs (literal mutation, `assertThrows` type swap) produced `BUILD FAILURE`, exit 1, `Failures: 1`, and a `<failure>` entry; both reverted. |
| R4 No Process-Killing / Print Signals | **PASS** | Zero `System.exit` / `TEST_OK` / `TEST_FALLO` in `test/`; harness helpers absent; `System.out` only in untouched `TrazaDemo.java`. |
| R5 Test Discovery Hygiene | **PASS** | 11 `Running <Class>` lines; `TrazaDemo` absent from reports; blob hash identical to base. |
| R6 Wrappers and Tooling | **PASS** | Index mode `100755`, mode-only diff, `./mvnw -v` exit 0. |
| R7 Offline Capability | **PASS** (`-o` resolution) / **UNVERIFIED** (R7.1) | All gates ran with `-o` from the warm cache and exited 0; physical network-off not reproducible locally. |
| R8 Scope Exclusion Guard | **PASS** | Diff allowlist respected; negative diff empty; package jar has no stale classes. |

Tasks: 21 of 24 complete. T22–T24 are rollback procedures, intentionally unchecked and not executed.

---

## 5. Unverified Items Carried Forward

| Item | Why it is unverified | How to close it |
|------|----------------------|-----------------|
| **R1.2** — build on a JDK 21 machine | Only JDK 17 is installed on the verification host (`ls /usr/lib/jvm/` → `java-17-openjdk-amd64`; `update-alternatives --list java` → single entry). The compile log shows `release 17`, so risk is low. | CI matrix job with `actions/setup-java` 21 running `sh ./mvnw -o clean compile`. |
| **R7.1** — suite with networking physically disabled | Network isolation cannot be enforced in this environment. All `-o` runs resolved from the warm `~/.m2` cache, the strongest available proxy. | CI job that disables networking (container `--network none` or firewall) and runs `sh ./mvnw -o clean test`. |

Both scenarios remain normative in `openspec/specs/test-execution-baseline/spec.md`; they carry non-normative "Verification status" annotations pointing to pending CI verification. Neither was downgraded or removed.

---

## 6. Deviations Recorded by Verify

| ID | Deviation | Disposition |
|----|-----------|-------------|
| **S1** | `openspec/config.yaml` uses `sh ./mvnw -o clean test` while `tasks.md` T20 specifies `sh ./mvnw -o test`. | Benign documentation drift; both commands are truthful and the `clean` form matches the design gates. Documented in `apply-progress`. |
| **S2** | Rollback tasks T22–T24 remain unchecked. | Intentional: they are documented procedures, not deliverables. Leaving them unchecked correctly records that no rollback was executed. |
| **S3** | The orchestration preflight expected 17 commits; the repo has 14. | Preflight miscount. History is coherent with the 3-slice plan (1 build, 1 chore, 11 test conversions, 1 docs) and HEAD matches the expected tip `db0c95d`. |

Additional note: the R3.1 red probe was executed on the final committed tree, so it reported `Tests run: 52, Failures: 1` (the apply phase had recorded an intermediate `Tests run: 37, Failures: 1`). The assertion under test is slice-independent and the failure stayed confined to `TEST-NormasTest.xml`.

---

## 7. Follow-Up Backlog (not implemented here)

Ordered roughly by dependency and value; none of these is part of this change.

1. **Queued change `deliverable-packaging`** — fix `src/unsa/sistemas/view/MainApp.java:16` (`"sistema.fxml"` → `"Sistema.fxml"`), then add `jpackage`/`jlink` packaging so the application runs on any machine with a JRE or as a native bundle.
2. **CI pipeline** — matrix for JDK 17 + JDK 21, plus a network-disabled job. This closes the two UNVERIFIED scenarios (R1.2, R7.1).
3. **Layout migration** — move to standard Maven directories (`src/main/java`, `src/test/java`, `src/main/resources`) and add package declarations; removes IDE friction and the default-package constraint.
4. **`pruebas txt/` fixtures** — implement fixture parsing and fixture-driven tests (currently nothing reads that directory).
5. **Algorithm correctness gaps** — non-convergence and NaN currently reported as success; Jacobi/Gauss-Seidel/SOR diagonal preconditions need a stricter tolerance; trace index bugs; inverted equation labels.
6. **`MejoramientoIterativo`** — implement the stub.
7. **`Sustituciones` / `ConsoleView`** — implement the remaining stubs.
8. **README rewrite** — replace the stale JDK 14 claim and document the real build/test workflow.

---

## 8. Artifacts and Traceability

Filesystem (archived change):

- `proposal.md` — intent, scope, risks, rollback plan
- `explore.md` — exploration decisions and evidence
- `specs/test-execution-baseline/spec.md` — originating delta spec
- `design.md` — pom wiring, conversion contract, slicing, verification gates
- `tasks.md` — 24 tasks (21 complete; T22–T24 rollback procedures)
- `verify-report.md` — independent verification, requirement matrix, findings
- `archive-report.md` — this report

Source of truth updated:

- `openspec/specs/test-execution-baseline/spec.md` — new capability spec, R1–R8 with all scenarios preserved verbatim, provenance header added, two non-normative verification-status annotations.

Engram observations (project `resolucion-sistemas-lineales`):

| Topic | Observation |
|-------|-------------|
| `sdd/establish-build-test-baseline/explore` | #50 |
| `sdd/establish-build-test-baseline/proposal` | #51 |
| `sdd/establish-build-test-baseline/spec` | #52 |
| `sdd/establish-build-test-baseline/design` | #53 |
| `sdd/establish-build-test-baseline/tasks` | #55 |
| `sdd/establish-build-test-baseline/apply-progress` | #57 |
| `sdd/establish-build-test-baseline/verify-report` | #59 |
| `sdd/establish-build-test-baseline/archive-report` | #60 |

`openspec/specs/.gitkeep` was left in place: the convention does not require removing it from a populated directory, and it is harmless.
