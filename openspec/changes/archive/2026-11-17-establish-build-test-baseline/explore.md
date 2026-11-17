# Exploration: Establish Build and Test Baseline

- Change: `establish-build-test-baseline`
- Project: `resolucion-sistemas-lineales`
- Artifact store: `openspec` (this file is `explore.md` as requested by the orchestrator; the SDD convention alias is `exploration.md`)
- Phase: explore (decision-oriented, no code applied)
- Date: 2026-11-16

## Executive Summary

The repository cannot compile through Maven (compiler `release 21` against JDK 17) and has no test
execution path: no JUnit dependency, no configured surefire, and eleven `main()` harnesses that call
`System.exit`. A previous working state (reports dated 2026-11-11 under `target/surefire-reports/`)
proves a JUnit 5 conversion of these harnesses already ran green with **52 test methods**, which makes
the conversion a known-good, mechanical target rather than a design risk.

Recommendation: target **Java 17**, **convert all 11 harnesses to JUnit 5** (52 `@Test` methods, no
`System.exit`), pin **surefire 3.5.4** (the default 3.2.5 provider is not cached for offline builds),
**delete the 13 stale `.class` files under `src/` and add a defensive exclude**, fix the `mvnw` mode bit,
and keep the **existing `src/` + `test/` layout** for this change. Fixture integration is out of scope.

## Current State (verified)

### Build

- JDK installed: `openjdk version "17.0.20.1"`.
- `pom.xml:15` sets `maven.compiler.release=21`; `sh ./mvnw -o -q test-compile` fails with
  `Fatal error compiling: error: release version 21 not supported`.
- `pom.xml:19-33` declares only `javafx-controls` and `javafx-fxml` (`21.0.13`). No JUnit, no surefire.
- `pom.xml:37-38` remaps `sourceDirectory=src` and `testSourceDirectory=test`.
- `pom.xml:41-48` declares a `<resources>` block rooted at `src` that excludes only `**/*.java`.
- `pom.xml:50-67` pins only `maven-compiler-plugin:3.14.1` and `javafx-maven-plugin:0.0.8`.
- Maven wrapper: `distributionType=only-script`, Maven `3.9.11`; distribution already cached under
  `~/.m2/wrapper/dists/apache-maven-3.9.11/`, so offline wrapper runs work.
- `mvnw` is committed as `100644` (`git ls-files -s mvnw`), and `core.fileMode=true`.
- Sources compile cleanly under `javac --release 17` (23 main sources, 12 test sources, exit 0).
- No Java 21-only language features in the codebase: only `instanceof` pattern matching and `record`
  (`src/unsa/sistemas/controller/SistemaController.java:121,149`, `src/unsa/sistemas/model/trace/Paso.java:23`),
  both legal at release 17.

### JavaFX requirement

- `javafx-base`, `javafx-controls`, and `javafx-fxml` `21.0.13` jars contain class files with
  **major version 61 (Java 17)** — verified with `javap -verbose` on
  `javafx.beans.Observable`, `javafx.scene.control.Button`, `javafx.fxml.FXMLLoader`.
- Therefore JavaFX 21.0.13 runs on JDK 17; no JavaFX feature in use requires bytecode 21.

### Stale binaries under `src/`

- 13 `.class` files (including `SistemaController$Resultado.class`) are untracked (`.gitignore:7` = `*.class`).
- `sh ./mvnw -o compile` on this checkout logs
  `Copying 15 resources from src to target/classes` (13 classes + `estilo.css` + `Sistema.fxml`) and
  `Nothing to compile - all classes are up to date.`
- Reproduction of the real hazard in a `/tmp` copy: after removing `SOR.java` and running a clean
  `compile`, `target/classes/.../SOR.class` was still present (copied from `src/` as a resource while
  the source no longer exists).
- Counter-evidence worth keeping honest: a completely fresh `target/` compiles all 23 sources
  (`Compiling 23 source files`), so the "stale API forever" framing is too strong. The verified failure
  modes are (a) stale binaries shipped as resources into `target/classes` and any packaged jar, (b)
  orphaned classes surviving source deletion/rename, and (c) the up-to-date check being fed by
  resource-copied `.class` files rather than compiled output.

### Tests

- 11 harness classes in `test/` are default-package `main()` programs that end with
  `System.out.println(ok ? "TEST_OK" : "TEST_FALLO"); System.exit(ok ? 0 : 1);`
  (`test/CholeskyTest.java:16-17`). `test/TrazaDemo.java` is a demo (no `System.exit`, no assertions).
- 1,549 lines across `test/*.java`; **52 `verifica*` methods** total (4/4/3/4/4/3/3/5/4/14/4).
- Assertions are hand-derived literals with a `1e-6` tolerance
  (`test/NormasTest.java:109-116`), so expected values can map directly to JUnit assertions without
  re-deriving anything.
- No code reads `pruebas txt/` (10 fixtures); `grep` over `src`, `test`, `README.md` finds no reference.
- Prior state evidence in `target/surefire-reports/`:
  - `TEST-unsa.sistemas.*.xml` dated 2026-11-11: **52 tests, 0 failures, 0 errors**, e.g.
    `unsa.sistemas.CholeskyTest` with `tests="4"`. A JUnit 5 conversion already worked once.
  - `2026-11-16T20-39-48_842-jvmRun1.dump` yesterday: surefire fell back to `JUnit3Provider` and failed
    with `ClassNotFoundException: AnalisisSensibilidadTest`, while `AnalisisSensibilidadTest.txt` reports
    `Tests run: 0`. That is the false-green signature of `surefire:test` on this repo today.
- Offline dependency inventory in `~/.m2`: `junit-jupiter{,-api,-engine,-params}:5.11.4`,
  `junit-platform-{commons,engine,launcher}`, `maven-surefire-plugin:3.2.5` and `3.5.4`.
  Verified: Maven 3.9.11 defaults to surefire `3.2.5`, whose `surefire-junit-platform:3.2.5` provider is
  **absent** offline (`BUILD FAILURE: artifact ... could not be resolved`), while `3.5.4` resolves and
  runs.

## Affected Areas

- `pom.xml` — compiler release, JUnit dependency, surefire plugin, resource excludes.
- `test/*.java` (11 files, 52 methods) — harness to `@Test` conversion.
- `src/**/*.class` (13 files) — deletion; no tracked file is affected.
- `mvnw` — mode bit in the Git index.
- `openspec/config.yaml` — `testing.*` commands and `strict_tdd` can only be truthful after this change.
- Not affected: all 23 main `.java` files (expect zero code changes), `test/TrazaDemo.java`,
  `pruebas txt/`, `README.md`, `openspec/specs/`.

## Decision 1 — Java target: 17 vs 21

| Option | Pros | Cons | Effort |
|---|---|---|---|
| (a) `release=17` | Matches installed JDK; JavaFX 21.0.13 is bytecode 17 (major 61); no build-machine change; also builds on JDK 21+ via `--release 17` | Pins language level to 17; JDK 21 features unavailable | Low |
| (b) Require JDK 21 | Unlocks 21 features nobody currently uses | Blocks every machine/CI runner without 21; contradicts the installed toolchain; no functional gain | Medium |
| (c) Toolchains plugin | Both levels allowed | Extra config and per-machine `~/.m2/toolchains.xml`; toolchain downloads are not offline-friendly; unnecessary for an academic project | High |

**Recommendation: (a) `maven.compiler.release=17`.**

- Evidence: installed `17.0.20.1`; JavaFX `21.0.13` class files are major 61; no source construct above 17.
- Consequence for CI: a JDK 17 runner is sufficient (`temurin 17`, `-o` works with the cached wrapper
  distribution and `~/.m2`). A JDK 21 runner also builds because `javac --release 17` is supported there.
- Consequence for other machines: JDK 17+ required; JDK 8–16 cannot build (JavaFX 21 needs 17), which is
  already true today.
- Note: `README.md:76` claims "JDK 14 or superior" and a javac-only workflow — stale, and out of scope.

## Decision 2 — How tests become runnable

| Option | Pros | Cons | Effort |
|---|---|---|---|
| (a) Convert all 11 harnesses to JUnit 5 | One runner (`mvn test`), per-test reporting, IDE integration, real exit semantics, aligns with the existing green evidence (52 tests) | Touches 1,549 lines / 11 files, exceeds the 400-line review budget; `System.exit` must be removed (it kills the surefire fork) | Medium |
| (b) Keep `main()` and run via `exec-maven-plugin` | Tiny diff (pom only); zero test-code risk; exit code already means pass/fail | 11 duplicated plugin executions (~110 pom lines); `mvn test` still reports no tests (false-green pattern persists); `exec:java` would call `System.exit` inside the Maven JVM and kill the build JVM, so `exec:exec` forks are mandatory; first failure aborts later harnesses; no per-test reporting; every new test is a pom edit | Low |
| (c) Hybrid (JUnit for new, exec for legacy) | Small start | Two test systems to maintain; the baseline still does not execute 52 real tests; migration debt deferred indefinitely | Medium |

**Recommendation: (a) — convert all 11 harnesses to JUnit 5.**

Mechanical mapping (no semantic change, no algorithm edits):

1. Add `org.junit.jupiter.api.Test`, `org.junit.jupiter.api.Assertions.*`.
2. `private static void verificaX()` → `@Test void verificaX()` (method bodies unchanged apart from
   assertion calls).
3. Helper mapping:
   - `compara(nombre, obtenido, esperado)` → `assertEquals(esperado, obtenido, 1e-6, nombre)`
     (`test/NormasTest.java:109-116`, `test/CholeskyTest.java:109-128`).
   - `chequea(nombre, boolean)` → `assertTrue(condicion, nombre)`.
   - `esperarIae(nombre, Runnable)` → `assertThrows(IllegalArgumentException.class, accion, nombre)`.
   - try/catch + `chequea(false)` blocks (e.g. `test/CholeskyTest.java:63-106`) → `assertThrows(...)`.
4. Delete the static `ok` flag, the `main` method, the `TEST_OK`/`TEST_FALLO` lines, and every
   `System.exit`.
5. Keep the hand-derived literal expectations exactly as written; do not compute expectations with the
   code under test.
6. Keep existing identifiers (`verificaSolucion`, `compara`) or rename them freely; the project
   convention is Spanish domain vocabulary, and renaming is optional churn. Prefer keeping names.
7. `test/TrazaDemo.java` stays a demo: surefire will not pick it up (it matches none of the default
   patterns) and it must not be converted.
8. Optional cosmetic step: drop the `System.out.printf` success lines so a green run is silent. Not
   required for correctness.

Why a failing test now fails the build (verified with a `/tmp` probe using surefire 3.5.4 and
`junit-jupiter:5.11.4`):

```
[ERROR] FailingProbeTest.deliberatelyFails -- ... expected: <5> but was: <4>
[ERROR] Tests run: 2, Failures: 1, Errors: 0, Skipped: 0
[INFO] BUILD FAILURE      (Maven exit code 1)
```

`testFailureIgnore` stays at its default `false`, so a failed assertion fails the lifecycle.

Review-budget note: 11 files / 1,549 lines exceeds the 400-line PR budget. Recommend chained work
units: (1) pom + JUnit dependency + surefire + one smoke `@Test`; (2) direct methods; (3) iterative and
matrix/norm tests; (4) controller test (14 methods, largest file) — or an explicit `size:exception`.

## Decision 3 — Source layout: keep remapped dirs vs standard Maven layout

| Option | Pros | Cons | Effort |
|---|---|---|---|
| (a) Keep `src/` + `test/` + pom remap | Zero file moves; diff stays limited to pom + test conversion; prior working state used this shape | Non-standard; the `<resources>` hack keeps the `.class`-as-resource hazard alive; some tooling assumes `src/main/java` | Low |
| (b) Migrate to `src/main/java`, `src/test/java`, `src/main/resources` | Standard layout; `<resources>` block disappears, which removes the stale-`.class` vector by construction; better IDE/CI defaults | 37 tracked files moved (23 main + 12 test + 2 resources: `Sistema.fxml`, `estilos.css`); rename churn on top of an already large test conversion; must relocate resources to `src/main/resources/unsa/sistemas/view/` to keep `getResource("...")` resolution identical | Medium |

**Recommendation: (a) keep the current layout for this change; defer migration to a separate change.**

- Blast radius if migrated later: 35 `.java` files + 2 resources; **zero import changes** (package
  declarations stay `unsa.sistemas.*`; the 12 test files are default package and can stay so).
- Resource handling check: `src/unsa/sistemas/view/MainApp.java:16` loads `getResource("sistema.fxml")`,
  which is package-relative to `unsa/sistemas/view/`. Putting the file at
  `src/main/resources/unsa/sistemas/view/Sistema.fxml` preserves the same classpath-relative location, so
  migration would not change resource semantics.
- Rationale for deferring: this change already rewrites 1,549 lines of tests; adding 37 renames makes
  review harder and mixes two independent risks. Decision 4 fully neutralizes the `.class` hazard
  without a layout migration.

## Decision 4 — Stale `.class` strategy

| Option | Pros | Cons | Effort |
|---|---|---|---|
| (a) Delete the 13 files **and** add `<exclude>**/*.class</exclude>` | Removes current stale binaries; prevents recurrence (IDE/javac output into `src/`, partial checkouts); one line of defense | One extra pom line; hides future accidental `.class` placement rather than failing loudly | Low |
| (b) Delete only | Minimal | The pom keeps copying whatever binaries appear in `src/`; regression is silent | Low |

**Recommendation: (a).**

- Evidence: `Copying 15 resources from src` (13 classes + 2 assets) today; `/tmp` probe showed a class
  whose `.java` was deleted still landing in `target/classes`.
- The 13 files are untracked and matched by `.gitignore:7`, so deletion has no Git rollback surface and
  no risk to tracked work.
- Run `mvn clean` (or delete `target/`) once after removing them so `target/classes` is rebuilt from
  source only; otherwise stale copies remain until the next clean.

## Decision 5 — `mvnw` executable bit

- Current: `git ls-files -s mvnw` → `100644`; `./mvnw` → `Permiso denegado`; `core.fileMode=true`.
- Fix: `chmod +x mvnw` then `git update-index --chmod=+x mvnw`, commit the mode change. Verify with
  `git ls-files -s mvnw` → `100755`.
- Note: mode changes only reach other clones on a fresh checkout/pull of that commit; existing working
  copies need a local `chmod +x`. Keep `sh ./mvnw ...` in docs as the portable fallback (it works
  regardless of the bit and on Windows use `mvnw.cmd`).
- CI note: `./mvnw` in a pipeline fails today; after the mode fix it works. Add `sh ./mvnw` as the
  documented command so pipelines do not depend on the bit.

## Decision 6 — Fixture integration (`pruebas txt/`)

**Recommendation: out of scope for this change.**

- No code reads the 10 files (`grep` across `src`, `test`, `README.md` → no references), so they cannot
  break or fix the baseline.
- Reading them requires a parser plus a documented fixture contract (the files look like rows of
  coefficients with the independent term appended: `4 1 1 6`), which is new production code. Under TDD
  that means failing parser tests first — a separate change.
- Their scenarios (singular, pivot zero, non-convergent, `NaN`/`Inf`) belong with algorithm-behavior
  work, not with build plumbing.
- Hazard to record for that future change: the directory name contains a space (`pruebas txt/`), which
  breaks naive shell globs and requires quoting; relocating to `src/test/resources/fixtures/` is the
  natural fix once tests consume them.

## Minimal `pom.xml` diff (described, not applied)

1. `properties`:
   - change `maven.compiler.release` from `21` to `17`;
   - add `<junit.version>5.11.4</junit.version>` (mirrors the existing `javafx.version` style).
2. `dependencies`:
   - add `org.junit.jupiter:junit-jupiter:${junit.version}` with `<scope>test</scope>`.
3. `build/resources/resource/excludes`:
   - add `<exclude>**/*.class</exclude>` alongside the existing `**/*.java`.
4. `build/plugins`:
   - add `org.apache.maven.plugins:maven-surefire-plugin:3.5.4` (no `<configuration>` needed: defaults
     discover `**/*Test.java` and fail the build on assertion failures);
   - keep `maven-compiler-plugin:3.14.1` (raises the compiler version above the Maven 3.9.11 default
     `3.13.0`, which is not cached offline);
   - keep `javafx-maven-plugin:0.0.8` unchanged.
5. Do **not** add `<source>`/`<target>` separately: `release` already applies to `compile` and
   `testCompile`.
6. Do **not** add `junit-platform-launcher` explicitly: surefire 3.5.4 resolves the cached launcher.

## Verification Recipe

Prerequisite: JDK 17 and the cached wrapper distribution; commands assume the repo root.

1. Mode bit:
   - `git ls-files -s mvnw` → starts with `100755`.
   - `./mvnw -v` → Maven 3.9.11, Java 17 (no `Permiso denegado`).
2. Clean baseline:
   - `sh ./mvnw -o clean test`
   - expect `BUILD SUCCESS`, `Tests run: 52, Failures: 0, Errors: 0, Skipped: 0`
     (adjust only if a method is intentionally split or merged during conversion).
   - expect `target/surefire-reports/` to contain one `TEST-*.xml` per converted class and no
     `*.dump`/`*.dumpstream` files.
3. No stale binaries:
   - `find src -name '*.class'` → empty.
   - `sh ./mvnw -o clean compile` → resource phase reports `Copying 2 resources from src`
     (`Sistema.fxml` + `estilos.css`), not 15.
   - `find target/classes -name '*.class' | wc -l` → 26 for 23 sources (nested classes included);
     `test -f target/classes/unsa/sistemas/model/iterative/SOR.class` → present only because
     `SOR.java` exists.
4. Failure semantics (proves the baseline is not a false green):
   - mutate one literal expectation, e.g. in `test/NormasTest.java` change the expected `7.0` for
     `‖(3,-4)‖₁` to `8.0`;
   - `sh ./mvnw -o test` → `BUILD FAILURE`, `Tests run: 52, Failures: 1`, Maven exit code 1,
     `target/surefire-reports/TEST-NormasTest.xml` contains `<failure`;
   - revert the mutation (`git checkout -- test/NormasTest.java`) and re-run → `BUILD SUCCESS`.
5. No harness leftovers:
   - `grep -rn "System.exit" test/` → no matches.
   - `grep -rn "TEST_OK\|TEST_FALLO" test/` → no matches.
6. Regression guard for the whole lifecycle:
   - `sh ./mvnw -o package` → `BUILD SUCCESS`;
   - `unzip -l target/*.jar | grep -c '\\.class'` equals the `target/classes` count (no stale extras).
7. Known non-goal to avoid false alarms: do **not** validate by launching the GUI.
   `src/unsa/sistemas/view/MainApp.java:16` requests `"sistema.fxml"` while the file is
   `Sistema.fxml`; on Linux this resolves to `null` and the app fails. That is a pre-existing GUI defect
   outside this change's scope.

## Risks

- **Surefire discovery patterns.** Defaults are `**/Test*.java`, `**/*Test.java`, `**/*Tests.java`,
  `**/*TestCase.java`. All 11 classes match `*Test`; `TrazaDemo` matches none and stays out. New test
  classes must follow the pattern or be listed in `<includes>`.
- **Default package.** Verified that a default-package `@Test` is discovered and executed by surefire
  3.5.4 (probe: `Running ProbeTest`, `Tests run: 1`). Residual risk is tooling, not surefire: some IDEs
  refuse or warn on default-package tests, and package-private JUnit classes must stay non-private (the
  probe used a package-private class successfully). Keeping the conversion in the default package avoids
  import churn; a later layout change can move tests into `unsa.sistemas`.
- **`System.exit` inside tests.** If any harness keeps `System.exit` and becomes a surefire test, the
  fork dies and surefire reports `The forked VM terminated without saying goodbye` — the exact shape of
  the existing `jvmRun.dump`. Conversion must remove every `System.exit`.
- **Offline resolution.** Maven defaults to surefire `3.2.5`, whose `surefire-junit-platform` provider is
  not cached, so an unpinned surefire fails offline. Pin `3.5.4`. Likewise, do not drop the explicit
  `maven-compiler-plugin:3.14.1` (default `3.13.0` is not cached).
- **`pruebas txt/` space.** No current code path touches it, so the baseline is unaffected; any future
  shell glob or property referencing it must quote the path.
- **JavaFX headless.** The current suite touches no JavaFX classes (`src/unsa/sistemas/controller/
  SistemaController.java` has zero `javafx` imports), so headless CI is safe. Any future test that
  instantiates `Node`/`Stage` will need a virtual display (xvfb) or Monocle; treat UI tests as a separate
  concern.
- **Parallel Maven runs.** Two `mvn` processes in the same project clobber `target/` (resources copied
  while another process compiles), producing spurious `MojoExecutionException` or fork dumps. Serialize
  builds; do not run the wrapper from an IDE watcher and a terminal at the same time.
- **Prior-art report files.** `target/surefire-reports/TEST-unsa.sistemas.*.xml` are from a different
  (packaged) layout and a previous session. Run `clean` before reading reports, or they will suggest
  tests ran when they did not.
- **Scope creep pressure.** The stale binaries tempt a layout migration, and the GUI case bug tempts a
  GUI fix. Both are deliberately excluded; adding either inflates an already budget-breaking test diff.

## Recommended Scope

In scope:
- `pom.xml`: release 17, JUnit 5.11.4 test dependency, surefire 3.5.4, `**/*.class` exclude.
- Delete the 13 untracked `.class` files under `src/`; clean `target/`.
- Convert the 11 harnesses (52 methods) to JUnit 5; remove `main`/`System.exit`/`TEST_OK` protocol.
- `mvnw` mode bit via `git update-index --chmod=+x`.
- After conversion lands: update `openspec/config.yaml` `testing.runner.command` /
  `apply.test_command` / `verify.test_command` to `sh ./mvnw -o test`, and flip `testing.strict_tdd`
  based on the team's decision (it is truthful to enable it only once `mvn test` is real).

Out of scope:
- Layout migration to `src/main/java` / `src/test/java` / `src/main/resources` (separate change).
- `pruebas txt/` parsing and fixture-driven tests.
- Algorithm fixes, GUI fixes (including `MainApp.java:16` `"sistema.fxml"` case mismatch), README rewrite,
  `MejoramientoIterativo`.

## Ready for Proposal

Yes. The proposal should state: build/tooling change (per `openspec/config.yaml` proposal rules), the
rollback note (revert `pom.xml`, restore harnesses from Git, re-add mode bit — no runtime behavior
changes), and the delivery forecast that the conversion exceeds the 400-line review budget, so chained
PRs or an explicit size exception are needed.
