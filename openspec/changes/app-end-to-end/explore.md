# Exploration: App End-to-End

- Change: `app-end-to-end`
- Project: `resolucion-sistemas-lineales`
- Artifact store: `openspec` (this file is `explore.md` as requested by the orchestrator; the SDD convention alias is `exploration.md`)
- Phase: explore (decomposition only, no implementation)
- Date: 2026-11-17
- Baseline: `establish-build-test-baseline` is DONE and archived; `sh ./mvnw -o clean test` → 11 surefire reports, **52 tests, 0 failures, 0 errors** (re-confirmed during this exploration).

## Executive Summary

The application is **one line away from starting** and **several semantic decisions away from being honest**.

Verified facts:

1. The GUI cannot start on Linux/macOS. `MainApp.java:16` asks for `sistema.fxml` while the resource is `Sistema.fxml`; on a case-sensitive filesystem `getResource` returns `null` and `FXMLLoader` throws `IllegalStateException: Location is not set.` (reproduced by running `MainApp`/`ConsoleView` against `target/classes`).
2. Everything behind that load works: with the correct resource name the FXML loads on JavaFX 21 (root `BorderPane`, controller injected, 15 `@FXML` fields bound, `stylesheets="@estilos.css"` resolved to an absolute file URL, `initialize()` populated both combo boxes). The `http://javafx.com/javafx/26` namespace is **not** a blocker — it produces only `Loading FXML document with JavaFX API of version 26 by JavaFX runtime of version 21.0.13`.
3. The numerical screen lies. Verified through the real controller/UI: a 1000-iteration non-converged Jacobi run prints `SOLUCIÓN DEL SISTEMA` + `Resuelto con éxito.` and the status bar says `Sistema resuelto correctamente.`; `Jacobi` on `prueba iterativo (Valor Inf).txt` returns `x1 = -Infinity, x2 = -Infinity` with the same success framing; `SOR` returns `NaN` with `Resuelto con éxito.`. The controller never inspects `convergio()` and never checks finiteness.
4. There is **no distributable entry point**: the jar has no `Main-Class`, `java -jar` fails, and a classpath launch of `MainApp` dies with `faltan los componentes de JavaFX runtime`. `mvn javafx:run` cannot run offline here because the plugin's own dependencies are not in `~/.m2`.
5. There are **zero UI tests**. The 52 green tests prove the math surface and cannot see any of the above — that is the structural risk this roadmap must respect.

Recommendation: **six ordered changes**, starting with `gui-launch-fix` (1 production line + one resource-resolution test), because the running GUI is the only oracle available for every later user-visible change. Then `honest-results` (the semantic core), then `mejoramiento-iterativo`, then `trace-and-label-fixes`, then `file-loading-fixtures`, and finally `deliverable-packaging` (jar/runtime/README) when the behavior it documents is final.

## Current State (verified)

### Build and test baseline

- `pom.xml:13-18` — `maven.compiler.release=17`, `javafx.version=21.0.13`, `junit.version=5.11.4`.
- `pom.xml:44-58` — `sourceDirectory=src`, `testSourceDirectory=test`, resources copied from `src` excluding `*.java`/`*.class`; the built jar therefore contains `unsa/sistemas/view/Sistema.fxml` and `estilos.css`.
- `pom.xml:60-84` — compiler 3.14.1, surefire 3.5.4, `javafx-maven-plugin:0.0.8` with `mainClass=unsa.sistemas.view.MainApp`.
- `target/resolucion-sistemas-lineales-1.0-SNAPSHOT.jar` manifest (verified with `unzip -p`): `Manifest-Version`, `Created-By: Maven JAR Plugin 3.4.1`, `Build-Jdk-Spec: 17` — **no `Main-Class`**, no `Class-Path`.
- Test inventory: 11 classes / 52 tests (`SistemaControllerTest` 14, `NormasTest` 5, and 4/3/3/4/4/4/4/4/3 across the remaining 9). `test/TrazaDemo.java` is a `main()` demo, not a test.
- `test/SistemaControllerTest.java:121-131` **encodes the current dishonest contract**: `assertTrue(r.isExitoso())` together with `assertTrue(!r.getConvergencia().convergio())`. Any change to the success semantics must rewrite this assertion deliberately.
- `test/RelajacionSimpleTest.java:45` pins `traza.totalPasos() == getIteraciones() + 1`; the iteration-count convention for `RelajacionSimple` cannot change without touching this invariant.

### User journey map

Legend: **OK** = works today, **BROKEN** = cannot complete, **PARTIAL** = completes but the output is wrong or misleading.

| # | Step | Status | Evidence / what breaks it |
|---|------|--------|---------------------------|
| 1 | Start the app (double-click, `java -jar`, `mvn javafx:run`) | **BROKEN** | `unzip -p target/*.jar META-INF/MANIFEST.MF` has no `Main-Class` → `java -jar` prints `no hay ningún atributo de manifiesto principal`. `mvn -o javafx:run` fails: `maven-plugin-api:3.6.0`, `plexus-java`, `plexus-archiver`, `commons-exec`, `maven-core:3.6.0` not in `~/.m2`. |
| 2 | JavaFX boot | **BROKEN (classpath)** | `java -cp target/classes:<javafx jars> unsa.sistemas.view.MainApp` → `Error: faltan los componentes de JavaFX runtime...` because `MainApp extends Application` (JDK launcher guard). |
| 3 | FXML load + controller injection | **BROKEN** | `MainApp.java:15-17` requests `"sistema.fxml"`; `Class.getResource` returns `null` (verified: lower-case `null`, `Sistema.fxml` resolves). Real failure: `IllegalStateException: Location is not set.` at `FXMLLoader.loadImpl`. |
| 4 | Same load with the correct name | **OK** | Probe on `target/classes`: `LOADED root=javafx.scene.layout.BorderPane controller=unsa.sistemas.view.SistemaFXController@…`; stylesheet resolved; `initialize()` ran (`comboTipoMetodo` = `[Directo, Iterativo]`). Namespace 26 warning only. |
| 5 | Manual entry | **OK (once 3 is fixed)** | `SistemaFXController.java:192-294` dialog → `interpretarArchivo` → same validation as files; malformed input raises `Alert` (`:282-293`). |
| 6 | File load | **OK (once 3 is fixed)** | `SistemaFXController.java:120-191`; all 10 fixtures in `pruebas txt/` parse (verified 10/10, `3x4` or `2x3`); errors are Spanish and specific (`:301-389`). |
| 7 | Method selection | **OK** | `SistemaFXController.java:68-114` populates 4 direct + 5 iterative entries; `obtenerMetodoSeleccionado` maps all 9 (`:488-507`). |
| 8 | Solve | **PARTIAL** | Dispatch works for 8 of 9 methods. `MEJORAMIENTO` always returns `Resultado.error("Método aún no implementado: MEJORAMIENTO…")` (`MejoramientoIterativo.java:26-28`, `SistemaController.java:117-119`). |
| 9 | Result panel | **PARTIAL (semantically broken)** | `SistemaFXController.java:573-591` prints `SOLUCIÓN DEL SISTEMA` whenever `isExitoso()`, even when `convergencia.convergio() == false`; `:607-608` prints `Convergió: No` under that same success heading; `:630-634` chooses the status bar from `isAdvertencia()` only. |
| 10 | Trace panel | **PARTIAL** | Renders, but equation labels are reversed (`:418`), row swaps from `EliminacionGaussiana` render as `Inicio` (`:650-654` + `EliminacionGaussiana.java:117`), and `GaussJordan` emits duplicate step numbers. |
| 11 | Load a new system while a trace is shown | **PARTIAL** | `:180` and `:275` clear only `areaResultado`; the previous procedure stays enabled/populated until the next solve. |

### File loading path

- Parser: `SistemaFXController.interpretarArchivo(List<String>)` at `:301-389` — private method of the FX controller. Rules: trim, skip blanks, skip `#` comments, split on `\s+`, equal column count per row, `Double.parseDouble`, reject non-finite (`:361-366`), reject empty, require `rows == columns-1` (square).
- Fixture compatibility: all 10 files under `pruebas txt/` are plain whitespace-separated rows and parse successfully. Negative cases verified: `4 five 6` → `Valor numérico inválido en la línea 2: five`; `Infinity 1 2` → `Se encontró un valor no finito en la línea 1.`; `1, 2, 3` → `Valor numérico inválido en la línea 1: 1,` (comma-separated input is unsupported by design).
- **Fixture defect:** `prueba iterativo (Valor Inf).txt` and `prueba iterativo (Valor NaN).txt` contain ordinary 2x3 integer systems — they contain no `Infinity`/`NaN` token and cannot exercise the non-finite validator. Their *names* are the only thing testing that branch.
- Testability: the parser is reachable from JUnit only through reflection on a JavaFX class (`new SistemaFXController()` needs no toolkit, but the coupling is accidental). No test currently reads the fixtures; `grep` finds no reference to `pruebas txt` in `src/`, `test/` or `pom.xml`.

### Result honesty — every place a wrong answer is presented as success

| Location | Behavior (verified) |
|---|---|
| `SistemaController.java:121-137` | Iterative branch builds `Convergencia` but **never** branches on `iter.convergio()`. |
| `SistemaController.java:200-216` | `Resultado.exito` / `exitoIterativo` hardcode the message `"Resuelto con éxito."` regardless of convergence. |
| `SistemaController.java:127-133` | `isAdvertencia()` is set **only** for Jacobi/Gauss-Seidel without strict diagonal dominance — never for non-convergence, never for `NaN`/`Infinity`. |
| `SistemaController.java:112-137` | No finiteness check on the solution before reporting success. |
| `SistemaFXController.java:573-591` | Success heading + `getMensaje()` printed for any `isExitoso()`. |
| `SistemaFXController.java:630-634` | `"Sistema resuelto correctamente."` whenever not an advertencia. |
| `Cholesky.java:141-154` + `:56` | `esSimetrica()` tolerates `NaN` (`Math.abs(NaN - NaN) > EPS` is `false`) and `valor <= EPS` is `false` for `NaN`, so `L` becomes `NaN` and the method returns `x = [NaN, NaN, NaN]`. |
| `Jacobi.java:85-112`, `GaussSeidel.java:88-114`, `SOR.java:141-168`, `RelajacionSimple.java:111-139` | No finiteness guard on the iterate; overflow to `±Infinity`/`NaN` is returned as the "solution" with `convergio=false`. |

Reproduced outcomes on shipped fixtures through the real controller + real UI text (status bar quoted verbatim):

- `Jacobi` on `prueba directo (matriz singular).txt`: `x1 = -2.14e308…`, `Convergió: No`, status `Sistema resuelto con advertencias.`, heading `SOLUCIÓN DEL SISTEMA`.
- `Jacobi` on `prueba iterativo (Valor Inf).txt`: `x1 = -Infinity, x2 = -Infinity`, heading `SOLUCIÓN DEL SISTEMA`.
- `SOR` on the same file: `x1 = NaN, x2 = NaN`, `Resuelto con éxito.`, status `Sistema resuelto correctamente.`
- `Relajación Simple` on the singular fixtures: `x1 = 0, x2 = 0, x3 = 6` presented as `Resuelto con éxito.`
- Diagonally dominant 2x2 `[[1, 0.999],[0.999, 1]]`: Jacobi/Gauss-Seidel/Relajación/SOR all hit `Iteraciones: 1000` with `convergió=false` and were still reported as `Sistema resuelto correctamente.` (Jacobi's answer `0.3163` vs true `0.50025`).
- `Cholesky` with a `NaN` entry returns `x = NaN` and `Resuelto con éxito.` — but note reachability: the file/manual parser rejects non-finite **input** tokens (`:361-366`), so this is a controller-API/defense-in-depth hole, not a UI repro. Non-finite **results** are reachable through the UI (row 2 above).

### Trace and label defects

- `SistemaFXController.java:418` — `matrizA[i].length - j` labels the first coefficient `x3`. Verified UI text for a 3x3 system: `1·x3 + 1·x2 + 1·x1 = 6`. Correct is `j + 1`.
- `EliminacionGaussiana.java:117-124` — swap records `new Paso(-1, …)`; verified trace `idx=-1 | Intercambio F1 ↔ F2`; the UI renders every non-positive index as `Inicio` (`SistemaFXController.java:650-654`), so a row swap is presented as the start of the procedure.
- `GaussJordan.java:42` + `:126-150` — `pivoteo_simple` receives `indice` by value, records with it, and never increments the caller's counter; verified trace: `idx=1 | Intercambio F1 ↔ F2` immediately followed by `idx=1 | F2 ← F2 - (0.0) F1` (a shared number and a registered no-op elimination).
- `RelajacionSimple.java:106-109` vs `:122` — the convergence check breaks **before** `iteraciones = k`, so the iteration that proves convergence is never counted; the other iterative solvers assign `iteraciones = k` after producing the iterate. Consequence: the "iterations" convention is inconsistent across methods, and the `traza.totalPasos() == getIteraciones() + 1` invariant (`test/RelajacionSimpleTest.java:45`) is tied to the current definition.

### Missing vs dead code

**Genuinely missing (required for the promised behavior):**

| Item | Evidence |
|---|---|
| Correct FXML resource reference | `MainApp.java:16` vs `src/unsa/sistemas/view/Sistema.fxml` |
| Honest outcome model (convergence + finiteness consumed by UI) | `SistemaController.java:121-137`, `:200-216`; `SistemaFXController.java:630-634` |
| `MejoramientoIterativo` algorithm **and** convergence surface | `MejoramientoIterativo.java:26-28` returns `null`; class implements only `Resolutor`, not `ResolutorIterativo`, so even when implemented the controller's `instanceof ResolutorIterativo` branch (`:121`) would report no convergence data |
| Distributable entry point | jar manifest has no `Main-Class`; `MainApp` cannot be the main class of a classpath/shaded launch |
| Testable parser entry point | `interpretarArchivo` is private inside `SistemaFXController` (`:301`) |
| Matrix inversion (only if condition number is exposed) | `Matriz` offers `multiplicar` (`:129`), `multiplicarPorVector` (`:147`), `transpuesta` (`:163`), `clonar` (`:174`), `submatrizPrincipal` (`:187`), `determinante` (`:204`) — no identity/inverse; `AnalisisSensibilidad.calcularCondicion` requires the caller to pass `A⁻¹` (`:57-91`) |

**Implemented but dead (do not "complete" them):**

| Item | Evidence | Verdict |
|---|---|---|
| `Normas` (307 lines) | Fully implemented, covered by `NormasTest` (5 tests), zero production callers | Dead but correct; usable as-is if the condition number is ever exposed |
| `AnalisisSensibilidad` (92 lines) | Implemented + tested, zero callers; needs `A⁻¹` | Dead but correct; blocking dependency is the missing inversion |
| `Sustituciones` (4 lines) | Empty class, zero references (`grep` across `src/`, `test/`, `pom.xml`) | Dead placeholder; substitution logic is duplicated inside `EliminacionGaussiana`, `Crout`, `Cholesky`, `GaussJordan` |
| `ConsoleView` (7 lines) | Forwards to `MainApp.main` (`ConsoleView.java:4-6`) — not a console | Not a CLI; **but** it is exactly the non-`Application` launcher a classpath launch requires, so repurpose/rename rather than delete blindly |
| `test/TrazaDemo.java` | `main()` demo, no assertions, not matched by surefire `*Test` | Dead test artifact |
| 10 fixtures in `pruebas txt/` | No code reads them | Dead but parseable; the cheapest source of acceptance evidence |
| `ResolutorDirecto` | Interface duplicates `Resolutor.resolver()/getTraza()` | Redundant, harmless; out of scope |

### Delivery requirements

1. **Resource name must match on a case-sensitive filesystem.** Verified fix: `getResource("Sistema.fxml")` loads the full UI (controller + stylesheet + `initialize()`); `"sistema.fxml"` returns `null`.
2. **A run path that works outside an IDE.** Options, with what was actually verified:
   - `mvn javafx:run` — configured (`pom.xml:76-83`) but **fails offline today** (plugin dependencies absent from `~/.m2`). Works with network + display. Not a self-contained hand-in.
   - **jlink runtime image (recommended):** built offline from the cached `-linux` modular jars in 87 MB; `rt/bin/java --add-modules javafx.controls,javafx.fxml -cp target/classes unsa.sistemas.view.MainApp` gets past the launcher guard and reaches the FXML load (failing only on the `:16` defect). Proven viable without JavaFX jmods (`jlink` accepts the modular jars; natives live inside `javafx-graphics-…-linux.jar`).
   - Shaded/assembly uber-jar: needs a non-`Application` launcher class, otherwise the JDK refuses to start it (`faltan los componentes de JavaFX runtime` — verified); bundles natives for one OS only; `maven-shade-plugin` is **not** cached, `maven-assembly-plugin` is.
   - `jpackage` app-image: JDK 17 ships `jpackage`/`jlink` (verified), but it is per-OS and should be a design-phase decision layered on the jlink image.
3. **JDK prerequisite:** JDK 17+ to build (`release=17`); a bundled jlink runtime removes the Java requirement for the end user, but the build still needs JDK 17 + the cached offline Maven repo.
4. **README rewrite is mandatory, not cosmetic.** `README.md:13` says the entry point does not exist, `:18` says to run from `ConsoleView`, `:52-53` calls `Normas`/`AnalisisSensibilidad` empty, `:69` calls `Sustituciones` empty (the only true statement), `:76-84` claims "no external dependencies / javac only" and "no execution command yet". It also pre-dates the Maven/JUnit baseline.

## Proposed Changes

Sizes are rough production+test line counts for the whole change. Budget reference: 400 lines per commit; "borderline" means plan multiple commits inside the change.

| # | Name | Intent | In scope | Out of scope | Size (files / lines) | Depends on | Needs strict TDD |
|---|------|--------|----------|--------------|----------------------|------------|------------------|
| 1 | `gui-launch-fix` | Make the app start on case-sensitive filesystems | `MainApp` resource reference; optional namespace normalization `javafx/26` → `javafx/21`; a resource-resolution test that fails today | Packaging, UI behavior, FXML structure | 2-3 files / ~25 lines | — | Yes (bug fix; the failing test is the reproduction) |
| 2 | `honest-results` | Never present a non-converged or non-finite answer as success | Outcome model in `SistemaController` (consume `convergio()`, reject non-finite input/solution), UI rendering of that outcome, status bar text, update the assertion that encodes the old contract | New solver behavior, packaging, trace cosmesis | `SistemaController`, `SistemaFXController` (+ maybe a small pure status mapper), `test/SistemaControllerTest` / new tests — ~250-320 lines (borderline) | — (but manual verification needs #1) | Yes (semantic change) |
| 3 | `mejoramiento-iterativo` | Turn the registered dead-end method into a working method | Iterative refinement: residual `r = b − Ax`, correction solve reusing a direct solver, `x += d`, stop on `‖r‖∞ < tol` or max iterations; implement `ResolutorIterativo`; trace steps | New math utilities (`Matriz` inversion), UI changes | `MejoramientoIterativo` (+ new test class) — ~200 lines | #2 (honest convergence contract) | Yes (new production behavior) |
| 4 | `trace-and-label-fixes` | Make the pedagogical output correct | Equation label order (`:418`), monotonic step numbering in `EliminacionGaussiana`/`GaussJordan`, no-op elimination suppression, `RelajacionSimple` iteration convention, clear stale trace on load; extract pure text formatting so it is testable without JavaFX | Trace redesign, new panels | 5-6 files / ~180-230 lines (borderline) | — (manual verification needs #1) | Yes (bug fixes + extraction) |
| 5 | `file-loading-fixtures` | Make input parsing testable and the shipped fixtures real acceptance evidence | Extract the parser from `SistemaFXController` into a plain class; wire the controller to it; parametrized tests over all 10 fixtures; replace the two misleading "Valor Inf"/"Valor NaN" fixtures with genuine cases (asserting rejection) | CSV/other formats, file dialogs, drag & drop | 1 new class + `SistemaFXController` + 2 fixtures + 1 test class — ~260-300 lines (borderline) | #2 (fixture expectations encode honest outcomes) | Yes for extraction/validation (characterize first) |
| 6 | `deliverable-packaging` | Let a user run the app on another machine and document the truth | jlink runtime (or jpackage app-image) + launcher (`ConsoleView` repurposed/renamed to `Launcher`, non-`Application`) + jar `Main-Class` + `run.sh`; README rewrite (status, prerequisites, build/test/run) | CI, installers for multiple OSes, code signing | `pom.xml`/scripts ~40-60, launcher ~10, README ~120 — ~200 lines | #1-#5 (packages/documents final behavior) | No (config/docs; verification is a manual packaging smoke test) |
| (opt) | `sustituciones-extraction` | Remove duplicated forward/back substitution | Move the 5 duplicated loops into `Sustituciones` | Any behavior change | 6 files / ~120 lines | #4 (same files) | Yes (refactor with tests) — but no user-visible value |
| (opt) | `norms-sensitivity-ui` | Expose norms/condition number | `Matriz.inversa()` (new), controller wiring, UI panel | — | 5+ files / ~300-400 lines | #2, #5 | Yes (new behavior) |

Notes on the two optional changes: `sustituciones-extraction` is a textbook refactor (the `java-refactoring-extract-method` skill applies) with zero end-to-end value — it should be deferred unless the rubric grades code structure. `norms-sensitivity-ui` is the largest scope on the board and its hard prerequisite (matrix inversion) does not exist; it is only justified if the deliverable must demonstrate condition-number analysis.

## Recommendation: Change to Do First

**`gui-launch-fix`.**

Technical justification, in order of weight:

1. **It is a total blocker, not a quality issue.** Every other change is unobservable by the user until the window opens. Severity is binary.
2. **It unblocks the only oracle the project has.** There are zero UI tests and no cached TestFX/Monocle, so manual verification through the running GUI is the only way to accept changes #2-#6. Fixing the launch is a prerequisite of the *verification capability*, not a convenience.
3. **It removes a confounder.** While the app cannot start, any later defect would be indistinguishable from the FXML layer; fixing it now means every subsequent failure has one cause.
4. **It is the cheapest and least risky change on the board** (one string + one test), and it does not touch the 52 green tests.
5. It is a bug fix, so strict TDD applies: a test asserting that `MainApp` can resolve its FXML must fail first (`getResource` returns `null`), then the one-line fix turns it green.

Why not `honest-results` first, despite it being the most severe *semantic* defect: it is fully verifiable headlessly through `SistemaControllerTest`, so it is not blocked by the launch; and it must be designed (outcome model decision) before it can be implemented. Ordering here is about verification capability, not about which bug is worst.

## Roadmap Order

1. `gui-launch-fix` — entry gate; unblocks all manual verification.
2. `honest-results` — defines the outcome contract that #3 consumes and #5 asserts.
3. `mejoramiento-iterativo` — fills the one selectable dead-end method; inherits the honest contract so it is never born lying.
4. `trace-and-label-fixes` — independent, low risk, high pedagogical visibility; best done once the GUI is runnable.
5. `file-loading-fixtures` — converts dead fixtures into executable acceptance evidence; done after #2 so expectations encode honest outcomes rather than the current false success.
6. `deliverable-packaging` — must be last: the manifest, runtime and README describe behavior that only exists after #1-#5.

Deferred: `sustituciones-extraction` (no user-visible value), `norms-sensitivity-ui` (needs new inversion routine + new UI surface).

## Risks

| ID | Risk | Why it matters | Mitigation |
|----|------|----------------|------------|
| R1 | Runtime-only defects are invisible to the suite | 52 tests are green while the app cannot start — proven. FXML path, controller injection, stylesheet, `initialize()`, dialogs, labels have no test coverage | After #1, add a toolkit-based FXML smoke test only if a headless strategy exists; otherwise document the manual checklist per change |
| R2 | No UI test layer | TestFX/Monocle are not in `~/.m2`; adding them needs network and breaks the offline baseline; CI may lack a display (this host has `DISPLAY=:1`) | Do not pretend checkbox-level coverage; use the probe pattern (FXMLLoader + `Platform.startup` on the module path) for one-off evidence and keep it out of the suite until a headless plan exists |
| R3 | Offline Maven constraints | `javafx:run` dependencies are absent, `maven-shade-plugin` is absent, JavaFX jmods are absent | Use `jlink` with the cached modular jars (verified); prefer JDK tools over new plugins; if a plugin is unavoidable, declare the one-time network step explicitly |
| R4 | `honest-results` intentionally changes an existing assertion | `test/SistemaControllerTest.java:123` asserts success for a diverging Jacobi | The spec delta MUST state the old contract is being replaced, and name the assertion; do not let the diff look like "tests bent to fit code" |
| R5 | Iteration-count convention is pinned by a test | `test/RelajacionSimpleTest.java:45` ties `traza.totalPasos()` to `getIteraciones()+1` | Decide the convention in the spec before coding; if the convention changes, update the invariant deliberately in the same commit |
| R6 | Overstating the `Cholesky`/`NaN` repro | The UI parser rejects non-finite tokens, so `NaN` input is not reachable from the file dialog | Frame it as controller-API hardening (defense in depth); use the reachable `-Infinity`/`NaN` **result** cases as the UI acceptance evidence |
| R7 | Packaging is platform-specific | JavaFX natives ship per OS; one artifact serves one OS | State the target OS in the packaging spec before building; document the Linux/Windows/macOS difference |
| R8 | FXML namespace 26 noise | Currently benign (verified load on 21 with a warning) | Normalize to the runtime version as hygiene inside #1; do not treat as a blocker, do not let Scene Builder regenerate it unnoticed |
| R9 | Compute on the FX thread | 1000 iterations × O(n²) run inside the handler (`SistemaFXController.java:535`); large `n` freezes the UI | Out of scope for the minimum; note it as a known limitation instead of a change |

## Open Questions

1. **Outcome model.** Should non-convergence be an error (no solution shown), a warning with the last iterate explicitly labeled "no convergió", or a new explicit status enum (`OK | NO_CONVERGIO | NO_FINITO | ERROR`) mapped by the UI? Recommendation: the enum; it is the only option that is both honest and still useful academically. This decision rewrites `test/SistemaControllerTest.java:123`.
2. **Is `Mejoramiento Iterativo` required by the rubric?** Implementing it is ~1 change; removing it from the menu/handler is two lines. Either is defensible; "registered but always failing" is not.
3. **Are `Normas` / `AnalisisSensibilidad` required in the UI?** They are implemented and tested but unreachable; exposing the condition number requires a new matrix-inversion routine.
4. **Target OS for the deliverable**, which decides the jlink/jpackage artifact and whether `run.sh` is enough.
5. **Fixture strategy:** test resources via `pom.xml` or read `pruebas txt/` from the project root (path contains a space)? Reading the tracked folder avoids a pom change and keeps the offline build untouched.
6. **`Sustituciones` in or out of the minimum path?** Evidence says out (pure refactor, 5 solvers at risk, zero user-visible value).

## Skill Resolution

| Skill | Read | Used for |
|---|---|---|
| `sdd-explore` | Yes (`~/.config/opencode/skills/sdd-explore/SKILL.md`) | Phase contract, artifact persistence, envelope |
| `_shared/openspec-convention.md` | Yes | Artifact path `openspec/changes/app-end-to-end/explore.md`, no delta specs written in explore |
| `test-driven-development/SKILL.md` | Yes (project `.agents/skills/…`) | Deciding `needs_strict_tdd` per change; bug fixes must start with a failing test |
| `test-driven-development/writing-good-tests.md` | Yes | Test design for #1 (resource resolution) and #2 (hand-derived literals, no mirror assertions); avoided change-detector tests (e.g. asserting manifest text) |
| `java-refactoring-extract-method/SKILL.md` | Yes | Sizing/sequencing the two extractions (`Sustituciones`, parser); baseline-green-first rule and one-extraction-per-step |

Not loaded: `_shared/sdd-phase-common.md` (referenced by the skill but not present in the paths provided; retrieval/persistence were driven by the orchestrator prompt and the openspec convention file).

## Ready for Proposal

Yes. The decomposition is evidence-backed and the first change is decided. Before `sdd-propose` writes the delta specs, question 1 (outcome model) must be answered, because it determines the contract of change #2 and therefore the acceptance criteria of #3 and #5.
