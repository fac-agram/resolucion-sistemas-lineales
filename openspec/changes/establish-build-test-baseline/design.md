# Design: Establish Build and Test Baseline

## Technical Approach

Build/tooling-only change; `src/**/*.java` stays byte-identical. Four mechanical moves:

1. Retarget the compiler to Java 17 and add JUnit 5 + surefire 3.5.4 using only the warm `~/.m2` cache.
2. Convert the 11 `main()` harnesses in `test/` into 52 JUnit 5 `@Test` methods, preserving every existing literal expectation and tolerance (the existing assertions are the oracle; no new expected values).
3. Delete the 13 untracked stale `.class` files under `src/` and exclude `**/*.class` from the resource phase.
4. Make `mvnw` mode `100755` in the Git index.

Delivered as three stacked PRs: 5 → 34 → 52 tests. Requirements R1/R7 ride on the pom pins; R2/R5 on conversion + discovery; R3 on surefire's default `testFailureIgnore=false` plus local mutation probes; R4 on deleting the harness protocol; R6 on the mode bit; R8 on a diff allowlist.

## Architecture Decisions

### Decision: exact `pom.xml` wiring

| Element | Final value | Evidence / rationale |
|---|---|---|
| `maven.compiler.release` | `17` | `pom.xml:15` currently `21`; installed JDK is `17.0.20.1`; JavaFX 21.0.13 bytecode is major 61 (explore) |
| `junit.version` | `5.11.4` | the only cached JUnit Jupiter version (`junit-jupiter{,-api,-engine,-params}/5.11.4` jars present) |
| `org.junit.jupiter:junit-jupiter` | `${junit.version}`, `<scope>test</scope>` | aggregate pulls api+params (compile) and engine (runtime); all cached |
| `maven-surefire-plugin` | `3.5.4`, no `<configuration>` | `surefire-junit-platform:3.5.4` is cached; Maven 3.9.11's default `3.2.5` has **no** cached platform provider, so an unpinned run fails offline. Defaults already match `**/*Test.java` and fail the build on assertion failure |
| `maven-compiler-plugin` | keep `3.14.1` | only cached version; Maven's default `3.13.0` is absent from `~/.m2` |
| `<resources>` excludes | add `<exclude>**/*.class</exclude>` next to `pom.xml:45` | `sh ./mvnw -o compile` copies 15 assets today (13 stale classes + 2 real); exclusion is the durable guard |
| `<source>` / `<target>` | omit | `release` applies to both `compile` and `testCompile`; the pair would be redundant/overridden |
| `junit-platform-launcher` | omit | `surefire-junit-platform:3.5.4` already declares it (1.11.4 is cached) |

No `<includes>`, no `failIfNoTests`, no `argLine`, no toolchains, no profiles. `javafx-maven-plugin:0.0.8` (`pom.xml:59-66`) stays untouched.

### Decision: harness → JUnit 5 conversion contract

| Harness idiom (evidence) | Replacement | Notes |
|---|---|---|
| `compara(String nombre, double obtenido, double esperado)`, abs diff `< 1e-6` (`test/NormasTest.java:109-116`, `CholeskyTest.java:109-128`, `CroutTest.java:85-104`, `EliminacionGaussianaTest.java:84-95`, `GaussJordanTest.java:84-103`, `GaussSeidelTest.java:69-76`, `JacobiTest.java:69-76`, `RelajacionSimpleTest.java:83-90`, `SORTest.java:105-112`) | `assertEquals(esperado, obtenido, 1e-6, nombre)` | **Arguments swap**: harness is `(message, actual, expected)`, JUnit is `(expected, actual, delta, message)`. Never write the 3-arg `assertEquals(a, b, msg)` — it binds to `assertEquals(double, double, String)`, which is exact equality with **no tolerance**, compiles silently, and fails every rounded value. Always pass the delta (4-arg form). |
| `compara` in `test/AnalisisSensibilidadTest.java:82-89` | `assertEquals(esperado, obtenido, 1e-9, nombre)` | Tolerance is **1e-9**, not 1e-6. Do not normalize. |
| `chequea(String nombre, boolean condicion)` (`CholeskyTest.java:131-140` and 9 more) | `assertTrue(condicion, nombre)` | Argument order already matches. Keep `if (x != null) { ... }` guards verbatim. |
| `esperarIae(String nombre, Runnable accion)` (`NormasTest.java:118-126`, `AnalisisSensibilidadTest.java:98-106`) | `assertThrows(IllegalArgumentException.class, accion, nombre)` | Harness accepts subclasses; `assertThrows` does too. |
| try/catch IAE + `chequea(..., true)` (`CholeskyTest.java:63-83`, `GaussSeidelTest.java:57-65`, `JacobiTest.java:57-65`, `RelajacionSimpleTest.java:71-79`, `SORTest.java:77-101`) | `assertThrows(IllegalArgumentException.class, () -> stmt, "msg")` | Keep the exact exception type; do not widen to `RuntimeException`. |
| try/catch `ArithmeticException` (`CholeskyTest.java:86-106`, `CroutTest.java:62-82`, `EliminacionGaussianaTest.java:61-81`, `GaussJordanTest.java:61-81`) | `assertThrows(ArithmeticException.class, ...)` | Same. |
| `comparaVector(String, double[] obtenido, double[] esperado)` (`SistemaControllerTest.java:182-201`) | `assertArrayEquals(esperado, obtenido, 1e-6, nombre)` | Fails on null, length mismatch, and element mismatch — same verdict as the harness. |
| `main`, `private static boolean ok`, `TEST_OK`/`TEST_FALLO`, `System.exit` | delete | R4; today `grep -rn "System.exit\|TEST_OK\|TEST_FALLO" test/` returns 29 matches. |
| success `System.out.printf` lines | delete | cosmetic; green runs become silent. |

Documented non-1:1 semantics (accepted, not blockers):

1. **Fail-fast vs accumulate.** Harness helpers set `ok=false` and continue; JUnit aborts the method at the first failed assertion. One broken method always maps to exactly one `<failure>`, so R3.1's `Failures: 1` holds; per-method verdict is unchanged.
2. **Boundary strictness.** Harness uses `< delta`, JUnit uses `<= delta`; only differs at exact boundary equality, which no expectation hits.
3. **`SORTest` oracle** (`SORTest.java:56-59`) compares SOR output against `GaussSeidel` output, not a hand literal. Keep as-is; it is the ω=1 equivalence check.
4. **`comparaVector` message text** changes; the verdict does not.

Per-file shape: add `import org.junit.jupiter.api.Test;` and `import static org.junit.jupiter.api.Assertions.*;`; convert `private static void verificaX()` to `@Test void verificaX()` (drop `private static`); keep bodies and literals identical. **No `package` declaration is added.**

### Decision: per-class counts and method names (total 52)

| Class | Slice | `@Test` methods | Count |
|---|---|---|---|
| `NormasTest` | PR1 | `verificaNormasVectoriales`, `verificaNormasMatriz2x2`, `verificaNormasMatriz3x3`, `verificaCasosEspeciales`, `verificaValidaciones` | 5 |
| `CholeskyTest` | PR2 | `verificaSolucion`, `verificaTraza`, `verificaMatrizNoSimetrica`, `verificaMatrizNoDefinidaPositiva` | 4 |
| `CroutTest` | PR2 | `verificaSolucion`, `verificaTraza`, `verificaPivoteNulo` | 3 |
| `EliminacionGaussianaTest` | PR2 | `verificaSolucion`, `verificaTraza`, `verificaSistemaSingular`, `verificaPivoteo` | 4 |
| `GaussJordanTest` | PR2 | `verificaSolucion`, `verificaTraza`, `verificaSistemaSingular`, `verificaPivoteo` | 4 |
| `SistemaControllerTest` | PR2 | `verificaJacobi`, `verificaGaussSeidel`, `verificaMetodoDesconocido`, `verificaMetodoNulo`, `verificaJacobiSinDominanciaDiagonal`, `verificaGaussDirecto`, `verificaGaussJordan`, `verificaCrout`, `verificaCholesky`, `verificaGaussSingular`, `verificaGaussJordanSingular`, `verificaCroutPivoteNulo`, `verificaCholeskyNoSimetrica`, `verificaCholeskyNoDefinidaPositiva` | 14 |
| `JacobiTest` | PR3 | `verificaSolucion`, `verificaConvergencia`, `verificaPrecondicionDiagonalEnCero` | 3 |
| `GaussSeidelTest` | PR3 | `verificaSolucion`, `verificaConvergencia`, `verificaPrecondicionDiagonalEnCero` | 3 |
| `SORTest` | PR3 | `verificaSolucion`, `verificaEquivalenciaConGaussSeidel`, `verificaConvergencia`, `verificaValidaciones` | 4 |
| `RelajacionSimpleTest` | PR3 | `verificaSolucion`, `verificaResidualPorFuera`, `verificaConvergencia`, `verificaPrecondicionDiagonalEnCero` | 4 |
| `AnalisisSensibilidadTest` | PR3 | `verificaCondicionIdentidad`, `verificaCondicionMatrizConocida`, `verificaMatrizCasiSingular`, `verificaValidaciones` | 4 |

`test/TrazaDemo.java` is untouched: it matches none of surefire's patterns (`**/Test*.java`, `**/*Test.java`, `**/*Tests.java`, `**/*TestCase.java`).

### Decision: default package

No `package` line anywhere in `test/`. Surefire selects by file name, not package; today's `target/surefire-reports/TEST-AnalisisSensibilidadTest.xml` (suite `name="AnalisisSensibilidadTest"`, no package prefix) proves surefire addresses the default-package class, and the explore probe ran a default-package `@Test` green. Residual risk is IDE-only (some IDEs warn or refuse default-package tests) — Maven is authoritative. Adding a package would require `test/unsa/...` paths, i.e. a layout change, explicitly out of scope (R8).

### Decision: PR slicing

3 stacked PRs fixed by session preflight; the 5 → 34 → 52 progression forces the composition: PR1 = plumbing + `NormasTest` (5), PR2 = direct methods + controller dispatch (29; cumulative 34), PR3 = iterative + matrix analysis (18; cumulative 52). PR2 ≈ 890 changed lines and PR3 ≈ 493 exceed the 400-line budget; the agreed split is recorded here, and per-class commits keep each review unit ≤ 378 lines.

## Data Flow

```
test/*.java ──maven-compiler-plugin 3.14.1 (release=17)──> target/test-classes
src/**/*.java ─────────────────same──────────────────────> target/classes
src/**/*.{fxml,css} ──resources (excl. **/*.java,**/*.class)──> target/classes

target/test-classes ──> maven-surefire-plugin 3.5.4
                          └─ surefire-junit-platform 3.5.4
                               └─ junit-jupiter-engine 5.11.4
                                    ├─ TEST-<Class>.xml per class
                                    └─ "Tests run: N" ──> Maven exit 0/1
```

## File Changes

| File | Action | Description |
|---|---|---|
| `pom.xml` | Modify | release 17, `junit.version`, `junit-jupiter` (test), surefire 3.5.4, `**/*.class` exclude |
| `test/NormasTest.java` | Modify | 5 `@Test` methods |
| `test/CholeskyTest.java`, `CroutTest.java`, `EliminacionGaussianaTest.java`, `GaussJordanTest.java`, `SistemaControllerTest.java` | Modify | 29 `@Test` methods |
| `test/JacobiTest.java`, `GaussSeidelTest.java`, `SORTest.java`, `RelajacionSimpleTest.java`, `AnalisisSensibilidadTest.java` | Modify | 18 `@Test` methods |
| `test/TrazaDemo.java` | Unchanged | demo; not discovered |
| `src/**/*.class` (13) | Delete (untracked, gitignored) | stale binaries; no Git surface |
| `mvnw` | Modify (mode) | `100644` → `100755` |
| `openspec/config.yaml` | Modify (PR3, conditional) | `testing.runner.command` / `apply.test_command` / `verify.test_command` = `sh ./mvnw -o test`; `strict_tdd` stays `false` |
| `src/**/*.java` (23) | Unchanged | zero production edits |

## Interfaces / Contracts

```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class NormasTest {
    @Test
    void verificaNormasVectoriales() {
        double[] v = {3, -4};
        assertEquals(7.0, Normas.norma1(v), 1e-6, "‖(3,-4)‖₁"); // expected, actual, delta, msg
    }

    @Test
    void verificaValidaciones() {
        assertThrows(IllegalArgumentException.class,
                () -> Normas.norma1((double[]) null), "norma1 vector null");
    }
}
```

## Delivery Plan (chained PRs)

Commit messages follow the repo's `type: descripcion` Spanish style (`git log`: `chore:`, `feat:`, `test:`, `refactor:`; subjects unaccented). Artifacts stay English.

### PR1 — plumbing + wiring proof

| # | Commit | Content | Verification |
|---|---|---|---|
| 1 | `build: fijar Java 17 y agregar JUnit 5 con surefire 3.5.4` | `pom.xml` delta above. The 13 `.class` deletions cannot be committed (untracked, `.gitignore:7`); they are a working-tree step of this PR, durable protection is the exclude line | `sh ./mvnw -o clean test` → `BUILD SUCCESS`, `No tests to run`, `Copying 2 resources from src`; `find src -name '*.class'` empty |
| 2 | `chore: hacer ejecutable el wrapper mvnw` | mode `100644` → `100755` | `git ls-files -s mvnw` → `100755`; `./mvnw -v` exits 0 |
| 3 | `test: convertir NormasTest a JUnit 5` | 5 tests | `sh ./mvnw -o clean test` → `Tests run: 5, Failures: 0`; one `TEST-NormasTest.xml` |

### PR2 — direct methods + controller dispatch (29 tests, cumulative 34)

One commit per class, direct family first, controller last so reviewers meet the established pattern before the largest file:

`test: convertir CholeskyTest a JUnit 5` (9) → `test: convertir CroutTest a JUnit 5` (12) → `test: convertir EliminacionGaussianaTest a JUnit 5` (16) → `test: convertir GaussJordanTest a JUnit 5` (20) → `test: convertir SistemaControllerTest a JUnit 5` (34).

One-commit-per-class rationale: each file is self-contained (every class carries its own `compara`/`chequea`), so each commit is independently reviewable and revertible without touching siblings; grouping by family would produce 500-line commits and couple rollbacks.

### PR3 — iterative + matrix analysis (18 tests, cumulative 52)

`test: convertir JacobiTest a JUnit 5` (37) → `test: convertir GaussSeidelTest a JUnit 5` (40) → `test: convertir SORTest a JUnit 5` (44) → `test: convertir RelajacionSimpleTest a JUnit 5` (48) → `test: convertir AnalisisSensibilidadTest a JUnit 5` (52) → optional `chore: actualizar configuracion de pruebas de SDD` (`openspec/config.yaml`, only if `openspec/` is tracked).

### Deliberate-red proof of R3 (never committed)

After a slice is green, mutate one literal in the working tree only:

```bash
# PR1 example; PR2 mutates CroutTest x1 1.0->1.1, PR3 mutates JacobiTest x1 0.8->0.9
sed -i 's/assertEquals(7.0, Normas.norma1(v)/assertEquals(8.0, Normas.norma1(v)/' test/NormasTest.java
sh ./mvnw -o test    # BUILD FAILURE, Tests run: 5, Failures: 1, exit 1
grep -c '<failure' target/surefire-reports/TEST-NormasTest.xml   # >= 1
git checkout -- test/NormasTest.java
sh ./mvnw -o test    # BUILD SUCCESS again
```

R3.2: in one converted `assertThrows(IllegalArgumentException.class, ...)`, change the expected type to `IllegalStateException.class`, confirm a `<error>`/`<failure>` naming the method, then `git checkout --`. No throwaway branch is needed; nothing red ever reaches a commit. If a paper trail is required, use a local branch `probe/red-<slice>` that is deleted and never opened as a PR.

## Verification Gates

| Gate | Command | Expected |
|---|---|---|
| PR1 mode | `git ls-files -s mvnw` / `./mvnw -v` | `100755`; Maven 3.9.11 + Java 17, exit 0 |
| PR1 resources | `sh ./mvnw -o clean compile` | `BUILD SUCCESS`; `Copying 2 resources from src`; `find src -name '*.class'` empty |
| PR1 suite | `sh ./mvnw -o clean test` | `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`; 1 `TEST-*.xml`; no `*.dump*` |
| PR2 suite | same | `Tests run: 34`; 6 `TEST-*.xml` |
| PR3 suite | same | `Tests run: 52`; 11 `TEST-*.xml`; no `TrazaDemo` entry |
| R4 | `grep -rn "System.exit\|TEST_OK\|TEST_FALLO" test/` | no matches (also `grep -rn "static void main" test/` → only `TrazaDemo`) |
| R6 | `sh ./mvnw -o package` | `BUILD SUCCESS`; `unzip -l target/*.jar \| grep -c '\.class'` equals `find target/classes -name '*.class' \| wc -l` (26 for 23 sources) |
| Forks | `find target/surefire-reports -maxdepth 1 -name '*.dump*'` | empty |
| R8 | diff allowlist below | no out-of-scope path |

## Risk Controls

| Risk | Control |
|---|---|
| Surefire naming | Defaults include `**/*Test.java`; all 11 names match, `TrazaDemo` matches none. Any new test must keep the `Test` suffix or be added to `<includes>`. Gate: 11 reports / 52 tests. |
| Leftover `System.exit` | Grep gate plus dump gate. A surefire fork death (`The forked VM terminated without saying goodbye` + `jvmRun*.dump`) means an exit survived; inspect the `.dumpstream`. |
| Stale `target/` | Always `clean` before reading reports. Today's `TEST-unsa.sistemas.*.xml` (previous layout) and the `tests="0"` `TEST-AnalisisSensibilidadTest.xml` are prior art, not this change's result. |
| Parallel Maven runs | Serialize builds; never run an IDE build and a terminal Maven run simultaneously, or `target/` is clobbered mid-copy. |
| `pruebas txt/` space | Not consumed; if ever referenced, quote `"pruebas txt/"`. Out of scope. |
| Offline resolution | All gates use `-o`; a resolution error means a pin is missing (unpinned surefire = 3.2.5 = no cached platform provider). |
| PR size | PR2/PR3 exceed the 400-line budget by the agreed 3-PR split; per-class commits bound each review unit. |
| Default package | IDE warnings only; do not "fix" it here. |

## Rollback

| Target | Procedure | Result |
|---|---|---|
| PR3 only | `git revert` the 5 PR3 test commits (or the PR3 merge commit) | 34 tests green; no other slice imports those files |
| PR2 only, PR3 unmerged | Revert the 5 PR2 commits; rebase PR3 | 5 tests green |
| PR2 only, PR3 merged | Revert the 5 PR2 commits | 23 tests green (Normas 5 + PR3 18); safe because helpers are per-file |
| Whole change | Revert in reverse order, or `git checkout <base> -- pom.xml test/`, `git update-index --chmod=-x mvnw`, `chmod -x mvnw`, `mvn clean` | Harness baseline restored; deleted `.class` files need no restore (untracked) |

## Out-of-Scope Guard (R8)

Per PR, `git diff --name-only origin/main...HEAD` must be a subset of the slice allowlist:

- PR1: `pom.xml`, `mvnw`, `test/NormasTest.java`
- PR2: PR1 set + `test/{Cholesky,Crout,EliminacionGaussiana,GaussJordan,SistemaController}Test.java`
- PR3: PR2 set + `test/{Jacobi,GaussSeidel,SOR,RelajacionSimple,AnalisisSensibilidad}Test.java`, `openspec/config.yaml`

Negative checks (must print nothing):

```bash
git diff --name-only origin/main...HEAD -- 'src/**/*.java' 'src/**/*.fxml' 'src/**/*.css' README.md 'pruebas txt/'
git diff --name-only origin/main...HEAD -- test/TrazaDemo.java
```

`find src -name '*.class'` must be empty and `git status --porcelain` must show no new `src` file. `.class` deletions and `target/` are invisible to `git diff`; verify them with `find` and `clean`, never by diff alone. `openspec/` is currently untracked, so its artifacts never appear in the allowlist diff.

## Testing Strategy

| Layer | What | Approach |
|---|---|---|
| Unit | 52 numerical/behavior assertions across 11 classes | Converted JUnit 5 tests run by surefire; oracle = existing literals and tolerances |
| Integration | Controller dispatch across 9 methods, warnings, singular cases (`SistemaControllerTest`) | Same suite, real `SistemaController`, no mocks |
| E2E | None | GUI/FXML is out of scope (pre-existing `"sistema.fxml"` vs `Sistema.fxml` defect) |

## Open Questions

None. All decisions are grounded in the current checkout; the only conditional item is the optional `openspec/config.yaml` commit, which is blocked on `openspec/` being tracked, not on design input.
