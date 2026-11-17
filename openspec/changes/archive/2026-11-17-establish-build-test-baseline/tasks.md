# Tasks: Establish Build and Test Baseline

Change: `establish-build-test-baseline` · Store: openspec · Mode: Standard (strict_tdd: false)

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | PR1 ~180–220 · PR2 ~950–1,100 · PR3 ~520–650 · total ~1,650–1,950 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR1 (plumbing + NormasTest) → PR2 (direct methods + controller) → PR3 (iterative + analysis) |
| Delivery strategy | ask-always (`ask-on-risk`) |
| Chain strategy | stacked-to-main |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

PR2 and PR3 each exceed 400 changed lines even after the 3-PR split. Mitigation: one commit per class
(each review unit ≤378 lines) + review per commit; or an explicit maintainer-approved `size:exception`
per PR. PR1 is inside budget. Decision needed before apply is **Yes** (confirm split + exception policy).

### Suggested Work Units

| Unit | Goal | Base | Tests after | Commits |
|------|------|------|-------------|---------|
| PR1 | Honest build: Java 17 + JUnit 5 + surefire + wrapper bit + 1 converted class | `origin/main` | 5 (1 class) | 3 |
| PR2 | Direct methods + controller dispatch converted | PR1 head | 34 (6 classes) | 5 |
| PR3 | Iterative methods + sensitivity analysis converted | PR2 head | 52 (11 classes) | 5 (+1 conditional) |

## Apply Contract (all conversion tasks)

- Existing literals and tolerances ARE the oracle. Never invent an expected value; never edit
  `src/**`, `README.md`, `pruebas txt/`, `test/TrazaDemo.java`.
- Per file: add `import org.junit.jupiter.api.Test;` + `import static org.junit.jupiter.api.Assertions.*;`,
  convert `private static void verificaX()` → `@Test void verificaX()`; keep every literal, tolerance,
  comment and `verifica*` name. **No `package` line.**
- Replace idioms: `compara(n, got, exp)` → `assertEquals(exp, got, 1e-6, n)`; `comparaVector` →
  `assertArrayEquals(exp, got, 1e-6, n)`; `chequea(n, cond)` → `assertTrue(cond, n)`;
  `esperarIae`/try-catch IAE → `assertThrows(IllegalArgumentException.class, ..., "msg")`;
  `ArithmeticException` try-catch → `assertThrows(ArithmeticException.class, ...)`.
  **Never** the 3-arg `assertEquals(a, b, msg)` — exact equality, no tolerance.
- `AnalisisSensibilidadTest` tolerance is **1e-9** (its `compara` uses `< 1e-9`); all others **1e-6**.
  The `1e-8` there is a test matrix input, not a tolerance.
- Delete per file: `main`, `private static boolean ok`, harness helpers (`compara`, `chequea`,
  `esperarIae`, `comparaVector`), every `System.out` print, every `System.exit`.
- No red state is ever committed. Mutation probes run on the working tree only.

## PR1 — Plumbing + NormasTest (5 tests, 1 class)

- [x] **T1 — Delete stale `.class` binaries (working-tree step, NOT a commit)** · files: 13 untracked
  `src/**/*.class` · R1, R8 · no commit.
  - check: `find src -name '*.class'` → empty; `git status --porcelain` → only `?? openspec/`
  - They are untracked and gitignored (`.gitignore:7`). Do **not** `git add`/`git rm` them; nothing to stage.

- [x] **T2 — Pin Java 17 + JUnit 5 + surefire 3.5.4 (HIGHEST RISK)** · files: `pom.xml` · R1, R2, R7 ·
  commit: `build: fijar Java 17 y agregar JUnit 5 con surefire 3.5.4`
  - `maven.compiler.release` 21 → 17; add `junit.version` 5.11.4; add `org.junit.jupiter:junit-jupiter`
    `${junit.version}` scope `test`; pin `maven-surefire-plugin` 3.5.4 with **no** `<configuration>`;
    keep compiler 3.14.1 and javafx plugin 0.0.8; add `<exclude>**/*.class</exclude>` to `<resources>`.
  - check: `sh ./mvnw -o clean test` → `BUILD SUCCESS` + `No tests to run` (exit 0);
    `sh ./mvnw -o clean compile` → `BUILD SUCCESS` + `Copying 2 resources from src`, exit 0;
    `find src -name '*.class'` → empty. Unpinned surefire (3.2.5) has no cached platform provider → offline failure.

- [x] **T3 — Make the wrapper executable** · files: `mvnw` (index mode only) · R6 ·
  commit: `chore: hacer ejecutable el wrapper mvnw`
  - `chmod +x mvnw && git update-index --chmod=+x mvnw` (no content change).
  - check: `git ls-files -s mvnw` → `100755`; `./mvnw -v` → Maven 3.9.11 + Java 17, exit 0.

- [x] **T4 — Convert `test/NormasTest.java`** · files: `test/NormasTest.java` · R2, R4, R5.2 ·
  commit: `test: convertir NormasTest a JUnit 5`
  - 5 `@Test`: `verificaNormasVectoriales`, `verificaNormasMatriz2x2`, `verificaNormasMatriz3x3`,
    `verificaCasosEspeciales`, `verificaValidaciones`; 1e-6; `esperarIae` → `assertThrows(IllegalArgumentException.class, ...)`.
  - check: `sh ./mvnw -o clean test` → `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`,
    exactly 1 `target/surefire-reports/TEST-NormasTest.xml`.

- [x] **T5 — PR1 deliberate red (R3, working tree only)** · files: `test/NormasTest.java` (temporary) · R3
  - `sed -i 's/assertEquals(7.0, Normas.norma1(v)/assertEquals(8.0, Normas.norma1(v)/' test/NormasTest.java`
  - `sh ./mvnw -o test`; echo $? → `Failures: 1`, `BUILD FAILURE`, exit `1`;
    `grep -c '<failure' target/surefire-reports/TEST-NormasTest.xml` → ≥ 1.
  - R3.2: change one expected type to `IllegalStateException.class`, rerun, confirm `<error>`/`<failure>`
    naming the method; `git checkout -- test/NormasTest.java`.
  - Revert is part of acceptance: `git checkout -- test/NormasTest.java` then rerun → `Tests run: 5`,
    `BUILD SUCCESS`. `git status --porcelain` → no `test/` entry.

- [x] **T6 — PR1 guards + scope gate** · files: none · R1, R4, R8
  - `grep -rn "System.exit\|TEST_OK\|TEST_FALLO" test/` → no matches;
    `grep -rn "static void main" test/` → only `test/TrazaDemo.java` (untouched);
    `find src -name '*.class'` → empty; `find target/surefire-reports -name '*.dump*'` → empty.
  - `git diff --name-only origin/main...HEAD` ⊂ {`pom.xml`, `mvnw`, `test/NormasTest.java`} (diff of the
    three PR1 commits; `git ls-files openspec` → empty, so artifacts never appear).
  - Negative: `git diff --name-only origin/main...HEAD -- 'src/**/*.java' 'src/**/*.fxml' 'src/**/*.css' README.md 'pruebas txt/' test/TrazaDemo.java` → empty.
  - PR1 MUST end green with 5 tests: a config-only slice would reproduce the false green.

## PR2 — Direct methods + controller (29 tests, cumulative 34)

Each conversion: `sh ./mvnw -o clean test` → expected cumulative `Tests run`, `Failures: 0`,
`BUILD SUCCESS`, report count = number of converted classes. All 1e-6.

- [x] **T7 — `test/CholeskyTest.java`** · 4 tests · R2, R4, R5.1 · commit: `test: convertir CholeskyTest a JUnit 5`
  - `verificaSolucion`, `verificaTraza`, `verificaMatrizNoSimetrica`, `verificaMatrizNoDefinidaPositiva`;
    IAE and `ArithmeticException` paths via `assertThrows`.
  - check: `Tests run: 9`, 2 `TEST-*.xml`.

- [x] **T8 — `test/CroutTest.java`** · 3 tests · R2, R4, R5.1 · commit: `test: convertir CroutTest a JUnit 5`
  - `verificaSolucion`, `verificaTraza`, `verificaPivoteNulo`. check: `Tests run: 12`, 3 XML.

- [x] **T9 — `test/EliminacionGaussianaTest.java`** · 4 tests · R2, R4, R5.1 · commit: `test: convertir EliminacionGaussianaTest a JUnit 5`
  - `verificaSolucion`, `verificaTraza`, `verificaSistemaSingular`, `verificaPivoteo`. check: `Tests run: 16`, 4 XML.

- [x] **T10 — `test/GaussJordanTest.java`** · 4 tests · R2, R4, R5.1 · commit: `test: convertir GaussJordanTest a JUnit 5`
  - Same four shapes as T9. check: `Tests run: 20`, 5 XML.

- [x] **T11 — `test/SistemaControllerTest.java` (HIGHEST RISK)** · 14 tests · R2, R4, R5.1 ·
  commit: `test: convertir SistemaControllerTest a JUnit 5`
  - 14 methods per design; `comparaVector` → `assertArrayEquals(exp, got, 1e-6, n)`; keep null/unknown-method
    dispatch semantics and exact exception types. Largest file (377 lines) — commit last so the pattern is established.
  - check: `Tests run: 34, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`, 6 `TEST-*.xml`.

- [x] **T12 — PR2 deliberate red** · files: `test/CroutTest.java` (temporary) · R3
  - Mutate `assertEquals(1.0, x[0], 1e-6, "x1")` → `1.1`; `sh ./mvnw -o test` → `Tests run: 34, Failures: 1`,
    `BUILD FAILURE`, exit `1`, `<failure` in `TEST-CroutTest.xml`.
  - `git checkout -- test/CroutTest.java`; rerun → `Tests run: 34`, `BUILD SUCCESS`. No red committed.

- [x] **T13 — PR2 guards + scope gate** · files: none · R1, R4, R8
  - `Tests run: 34`; 6 `TEST-*.xml`; `grep -rn "System.exit\|TEST_OK\|TEST_FALLO" test/` → empty;
    `find target/surefire-reports -name '*.dump*'` → empty; `find src -name '*.class'` → empty.
  - `git diff --name-only origin/main...HEAD` ⊂ PR1 set + `test/{Cholesky,Crout,EliminacionGaussiana,GaussJordan,SistemaController}Test.java`.

## PR3 — Iterative + matrix analysis (18 tests, cumulative 52)

- [x] **T14 — `test/JacobiTest.java`** · 3 tests · R2, R4, R5.1 · commit: `test: convertir JacobiTest a JUnit 5`
  - `verificaSolucion`, `verificaConvergencia`, `verificaPrecondicionDiagonalEnCero`. check: `Tests run: 37`, 7 XML.

- [x] **T15 — `test/GaussSeidelTest.java`** · 3 tests · R2, R4, R5.1 · commit: `test: convertir GaussSeidelTest a JUnit 5`
  - Same three shapes. check: `Tests run: 40`, 8 XML.

- [x] **T16 — `test/SORTest.java`** · 4 tests · R2, R4, R5.1 · commit: `test: convertir SORTest a JUnit 5`
  - `verificaSolucion`, `verificaEquivalenciaConGaussSeidel`, `verificaConvergencia`, `verificaValidaciones`.
    Keep the SOR-vs-GaussSeidel ω=1 oracle verbatim (not a literal). check: `Tests run: 44`, 9 XML.

- [x] **T17 — `test/RelajacionSimpleTest.java`** · 4 tests · R2, R4, R5.1 · commit: `test: convertir RelajacionSimpleTest a JUnit 5`
  - `verificaSolucion`, `verificaResidualPorFuera`, `verificaConvergencia`, `verificaPrecondicionDiagonalEnCero`.
    check: `Tests run: 48`, 10 XML.

- [x] **T18 — `test/AnalisisSensibilidadTest.java`** · 4 tests · R2, R3, R4, R5.1 ·
  commit: `test: convertir AnalisisSensibilidadTest a JUnit 5`
  - `verificaCondicionIdentidad`, `verificaCondicionMatrizConocida`, `verificaMatrizCasiSingular`, `verificaValidaciones`.
    **Tolerance 1e-9** (4-arg form). `esperarIae` → `assertThrows(IllegalArgumentException.class, ...)`.
  - check: `Tests run: 52, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`, 11 `TEST-*.xml`.

- [x] **T19 — PR3 deliberate red** · files: `test/JacobiTest.java` (temporary) · R3
  - Mutate `assertEquals(0.8, x[0], 1e-6, "x1")` → `0.9`; `sh ./mvnw -o test` → `Tests run: 52, Failures: 1`,
    `BUILD FAILURE`, exit `1`, `<failure` in `TEST-JacobiTest.xml`.
  - `git checkout -- test/JacobiTest.java`; rerun → `Tests run: 52`, `BUILD SUCCESS`.

- [x] **T20 — Truthful SDD test commands (conditional, artifact-only)** · files: `openspec/config.yaml` · R2, R8
  - Set `testing.runner.command`, `apply.test_command`, `verify.test_command` = `sh ./mvnw -o test`;
    `strict_tdd` stays `false`. `openspec/` is untracked today (`git ls-files openspec` → empty), so this
    edit MUST NOT be committed; commit only if `git ls-files openspec | wc -l` > 0.

- [x] **T21 — PR3 final guards (release gate)** · files: none · R1, R2, R5, R7, R8
  - `sh ./mvnw -o clean test` → `Tests run: 52`, 11 `TEST-*.xml`, no `Tests run: 0`, no `JUnit3Provider`,
    no `*.dump*`; no report/entry names `TrazaDemo`.
  - `sh ./mvnw -o package` → `BUILD SUCCESS`; `unzip -l target/*.jar | grep -c '\.class'` ==
    `find target/classes -name '*.class' | wc -l` (26 for 23 sources).
  - `git diff --name-only origin/main...HEAD` ⊂ PR1+PR2 set + `test/{Jacobi,GaussSeidel,SOR,RelajacionSimple,AnalisisSensibilidad}Test.java`
    (+ `openspec/config.yaml` only if tracked).
  - Negative: same `src/**`, `README.md`, `pruebas txt/`, `test/TrazaDemo.java` diff → empty;
    `git status --porcelain` → no new `src` file.

## Rollback Tasks

- [ ] **T22 — Revert PR3 only** · `git revert` the 5 PR3 test commits (or the PR3 merge commit) · R2
  - check: `sh ./mvnw -o clean test` → `Tests run: 34`, `BUILD SUCCESS`. No sibling slice imports those files.

- [ ] **T23 — Revert PR2 only** · `git revert` the 5 PR2 commits; if PR3 is merged, rebase it · R2
  - check with PR3 unmerged: `Tests run: 5`, `BUILD SUCCESS`. With PR3 merged: `Tests run: 23` (5 + 18);
    safe because helpers are per-file.

- [ ] **T24 — Revert whole change** · `git checkout <base> -- pom.xml test/`, `git update-index --chmod=-x mvnw`,
  `chmod -x mvnw`, `mvn clean` · R1, R2, R6
  - check: `git diff origin/main --stat` → empty for tracked files; `find src -name '*.class'` stays empty
    (deleted files are untracked/gitignored, nothing to restore); `git ls-files -s mvnw` → `100644`.

## Risk Ranking (highest first)

1. **T2 — pom wiring (HIGHEST RISK)**: `release=17` gates every build; unpinned surefire 3.2.5 has no cached
   `surefire-junit-platform` → offline failure; `**/*.class` exclude is the durable stale-binary guard.
2. **T11 — SistemaControllerTest (HIGHEST RISK)**: 14 methods, 377 lines, array/null/dispatch semantics, 4-arg
   `assertArrayEquals`; a 3-arg `assertEquals` or widened exception type compiles but silently weakens the suite.
3. T18 — 1e-9 tolerance is unique to this file; normalizing it to 1e-6 hides real regressions.
4. T5/T12/T19 — red probes: a missed revert commits a red test; always `git status --porcelain` before commit.
5. T4/T7–T10/T14–T17 — mechanical conversion; risk is argument-order swap (expected, actual, delta, message).
6. T1/T3/T6/T13/T20/T21 — working-tree, index and guard steps; low risk, high verification value.

## Not in this change

Layout migration to `src/main/java` / `src/test/java` / `src/main/resources`; `pruebas txt/` parsing and
fixture-driven tests; algorithm fixes (NaN/Inf and convergence behavior); GUI/FXML fixes including the
`MainApp.java:16` `"sistema.fxml"` vs `Sistema.fxml` case mismatch; README rewrite (stale JDK 14 claim);
`MejoramientoIterativo`; `test/TrazaDemo.java` conversion. The queued future change `deliverable-packaging`
(FXML fix + jpackage/jlink) is NOT part of this change.
