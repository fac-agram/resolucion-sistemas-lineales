# Proposal: Establish Build and Test Baseline

- **Change type**: build/tooling (per `openspec/config.yaml` proposal rule). No algorithm or runtime behavior changes.
- **Change**: `establish-build-test-baseline`
- **Status**: proposed (explore complete, no code applied)

## Intent

The repo cannot be verified honestly. Maven fails to compile (`maven.compiler.release=21` against the installed JDK 17), and no test execution path exists: no JUnit dependency, no surefire configuration, eleven `main()` harnesses that call `System.exit`, and `mvn surefire:test` reporting `Tests run: 0` with `BUILD SUCCESS`. Every later change (algorithm fixes, layout migration, fixtures, GUI) is unverifiable until `mvn clean test` can fail and pass for real. This change buys that baseline once, cheaply, and mechanically.

## Scope

### In Scope

- `pom.xml`: `maven.compiler.release` 21 → 17; add `junit-jupiter:5.11.4` (test scope); pin `maven-surefire-plugin:3.5.4`; add `<exclude>**/*.class</exclude>` to the resources block.
- Convert the 11 harnesses in `test/` to JUnit 5 `@Test` (52 methods); delete `main`, the `ok` flag, `TEST_OK`/`TEST_FALLO` prints, and every `System.exit`.
- Delete the 13 untracked `.class` files under `src/` and rebuild `target/` clean.
- Fix `mvnw` exec bit (`git update-index --chmod=+x` → `100755`).
- After conversion lands, update `openspec/config.yaml` `testing.runner.command` / `apply.test_command` / `verify.test_command` to `sh ./mvnw -o test`; `strict_tdd` stays `false` until the team decides (not this proposal's call).

### Out of Scope

- Layout migration to `src/main/java` / `src/test/java` / `src/main/resources`.
- `pruebas txt/` fixture parsing and fixture-driven tests.
- Algorithm fixes and numerical behavior changes.
- GUI/FXML fixes (including the `MainApp.java:16` `"sistema.fxml"` vs `Sistema.fxml` case mismatch).
- README rewrite (stale JDK 14 claim).
- `MejoramientoIterativo` work.

## Capabilities

### New Capabilities

- `test-execution-baseline`: the suite is discoverable and executed by Maven, fails the build on assertion failure, and exercises the numerical algorithms without process exits. Requirements are verifiable and map to the success criteria below.

### Modified Capabilities

- None (no existing specs; `openspec/specs/` is empty).

## Approach

Six exploration decisions, applied in this order:

1. **Target Java 17** (`release=17`, no toolchains) — matches the installed JDK 17.0.20.1; JavaFX 21.0.13 jars are bytecode major 61, so compiling against them at 17 is safe.
2. **Convert all 11 harnesses to JUnit 5** (52 `@Test`) — one runner, per-test reporting, real exit semantics; the 2026-11-11 surefire reports prove this exact conversion already ran 52/52 green.
3. **Keep the `src/` + `test/` remapped layout** — isolates a tooling diff from 37 file renames; layout migration is its own change.
4. **Delete the 13 `.class` files and exclude `**/*.class`** — removes today's stale binaries and closes the "binary copied as a resource" vector by construction.
5. **Fix the `mvnw` mode bit** — the documented entry point currently fails with permission denied; keep `sh ./mvnw` as the portable form.
6. **Leave `pruebas txt/` alone** — nothing reads it; a parser is new production code that needs its own failing tests first.

## Why Now / Why Not Alternatives

**JUnit conversion vs alternatives.** `exec-maven-plugin` keeps the diff tiny but leaves `mvn test` reporting zero tests (the false-green pattern persists), needs ~110 lines of duplicated executions, aborts on the first failing harness, and gives no per-test reporting. A hybrid keeps two test systems alive and defers the real baseline indefinitely. Conversion is the only option where a red test makes the build red.

**Java 17 vs 21.** Requiring JDK 21 blocks every machine and runner that lacks it while unlocking features nobody uses. Toolchains add per-machine `~/.m2/toolchains.xml` and are not offline-friendly. `--release 17` also builds on JDK 21+.

**Why now.** Every other queued change (algorithm, fixtures, layout) needs a failing-capable test command; there is no cheaper moment than before the first one starts.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `pom.xml` | Modified | release 17, JUnit dependency, surefire 3.5.4, `.class` exclude |
| `test/*.java` (11 files) | Modified | harness → JUnit 5, 52 `@Test`, no `System.exit` |
| `src/**/*.class` (13 files) | Removed | untracked, gitignored stale binaries |
| `mvnw` | Modified | mode `100644` → `100755` in the Git index |
| `openspec/config.yaml` | Modified | truthful `test_command` values post-conversion |
| `test/TrazaDemo.java` | Unchanged | demo; matches no surefire pattern |
| `src/**/*.java` (23 files) | Unchanged | zero production edits expected |

## Review Workload Forecast

| Metric | Value |
|--------|-------|
| Test code rewritten | ~1,549 lines across 11 files |
| Config change | ~10 lines in `pom.xml`, 0 lines for the mode bit |
| Total changed surface | ~1,560 lines vs `review_budget_lines: 400` (≈3.9×) |
| **Chained PRs recommended** | **Yes** |
| **400-line budget risk** | **High** |
| **Decision needed before apply** | **Yes** (ask-always: chained vs size exception) |

Suggested split if chained: (1) pom + JUnit + surefire + one smoke `@Test`; (2) direct-method tests (Cholesky, Crout, Gauss, Gauss-Jordan); (3) iterative + norm/matrix tests; (4) `SistemaControllerTest` (14 methods, largest file).

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Surefire naming: a converted class is not discovered | Low | All 11 match `*Test`; keep the suffix; `TrazaDemo` intentionally excluded |
| Default package: IDE warnings or tooling limits | Med | Verified discovered and run by surefire 3.5.4; layout change is a later change |
| `System.exit` left in a test kills the fork | Med | Remove every exit; `grep -rn "System.exit" test/` must be empty |
| Offline provider resolution (default surefire 3.2.5 uncached) | High if unpinned | Pin surefire 3.5.4 and compiler 3.14.1, both cached |
| Parallel Maven runs clobber `target/` | Med | Serialize builds; no IDE watcher plus terminal at once |
| JavaFX headless requirement | Low | The suite imports no JavaFX today; UI tests stay a separate concern |
| Stale `target/surefire-reports/` suggest tests ran | Med | Always `clean` before reading reports; old reports are prior art from another layout |
| Renaming identifiers during conversion inflates the diff | Med | Keep existing `verifica*` names and helper names |

## Rollback Plan

Fully reversible; no runtime behavior is touched.

1. `git checkout -- pom.xml` (or revert the commit) to restore release 21 and drop JUnit/surefire/exclude.
2. `git checkout -- test/` to restore the 11 `main()` harnesses.
3. Re-create the 13 `.class` files only if you actually need them — they are untracked and gitignored, so nothing is lost from Git; `mvn clean` remains safe either way.
4. `git update-index --chmod=-x mvnw` (and `chmod -x mvnw`) if the bit must revert.
5. `git checkout -- openspec/config.yaml` to restore the pre-conversion testing block.
6. `mvn clean` to drop the converted `target/`.

## Dependencies

- JDK 17 installed and on `PATH` (17.0.20.1 present).
- Maven 3.9.11 wrapper distribution cached under `~/.m2/wrapper/dists/`.
- Offline cache: `junit-jupiter{,-api,-engine,-params}:5.11.4`, platform launcher, `maven-surefire-plugin:3.5.4`, `maven-compiler-plugin:3.14.1`.
- No network required with `-o`.

## Success Criteria

- [ ] `sh ./mvnw -o clean test` → `BUILD SUCCESS`, `Tests run: 52, Failures: 0, Errors: 0, Skipped: 0`, one `TEST-*.xml` per converted class, no `*.dump` files.
- [ ] Mutating one expected literal (e.g. `NormasTest` `7.0` → `8.0`) produces `BUILD FAILURE` with Maven exit code 1, `Tests run: 52, Failures: 1`, and a `<failure` entry in the corresponding surefire XML; reverting restores green.
- [ ] `find src -name '*.class'` → empty; `clean compile` logs `Copying 2 resources from src` (not 15).
- [ ] `grep -rn "System.exit" test/` → no matches; `TEST_OK`/`TEST_FALLO` absent from `test/`.
- [ ] `git ls-files -s mvnw` → `100755`; `./mvnw -v` runs without permission errors.
- [ ] `sh ./mvnw -o package` → `BUILD SUCCESS` and the jar contains no stale extra classes.
- [ ] `src/**/*.java` shows zero changes in the diff.
