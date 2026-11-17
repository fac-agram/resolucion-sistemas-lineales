# Tasks: honest-results

- Change: `honest-results` · Store: `openspec` · Phase: tasks · Date: 2026-11-17
- Mode: **strict TDD** (`RED → GREEN`, no production line before its failing test)
- Baseline (verified): `sh ./mvnw -o clean test` → `Tests run: 52`, 11 `TEST-*.xml`, `SistemaControllerTest` = 14 methods
- Target: `Tests run: 59`, 11 classes, `SistemaControllerTest` = 21 methods
- Diff base for every scope guard: `3f2fd7e`
- `design.md` is normative for fixtures, precedence, mapper copy, view switch and commit plan. Do not re-derive anything below.

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | Slice 1 ~130 · Slice 2 ~120 · Slice 3 ~80 · **total ~330** |
| 400-line budget risk | Low |
| Chained PRs recommended | No — one PR, three ordered commits |
| Suggested split | Single PR: commit 1 (RED) → commit 2 (GREEN) → commit 3 (render) |
| Delivery strategy | ask-always |
| Chain strategy | stacked-to-main (declared; no chain required) |

Decision needed before apply: Yes
Chained PRs recommended: No
Chain strategy: stacked-to-main
400-line budget risk: Low

- Per-commit budget: **respected** (130 / 120 / 80, each < 400; total 330 < 400). No `size:exception` needed.
- `Decision needed before apply: Yes` is the ask-always confirmation of the single-PR, 3-commit plan — not a size exception.
- **Merge-safety**: commit 1 is tests-only and does not compile until commit 2, and commit 3 carries the `59` pin. Only the three commits together satisfy R2.1. Never open or merge slice 1 as its own PR.
- If a chain is imposed anyway, the only defensible pair is PR1 = slices 1+2 (~250 lines, tests green but config pin still stale) and PR2 = slice 3 (~80). Prefer the single PR.

### Suggested Work Units

| Unit | Goal | PR | Notes |
|------|------|----|-------|
| 1 | Tests pin the honest contract (RED) | PR 1 | Tests only; non-compiling until unit 2; not mergeable alone |
| 2 | Controller classifies by `Estado` (GREEN) | PR 1 | `Tests run: 59` green |
| 3 | View/CSS/config render by state | PR 1 | Includes `strict_tdd: true` + count pin; manual GUI sign-off |

## Contract Pins (from design — do not re-derive)

Precedence inside `Resultado.clasificar`: structural `ERROR` → `!Double.isFinite` → `NO_FINITO` (solution `null`, trace + convergence kept) → `convergencia != null && !convergencia.convergio()` → `NO_CONVERGIO` (last iterate kept) → `OK`.

| `Estado` | `titulo` | `barraEstado` | `claseCss` |
|---|---|---|---|
| `OK` | `SOLUCIÓN DEL SISTEMA` | `Sistema resuelto correctamente.` | `estado-ok` |
| `NO_CONVERGIO` | `ÚLTIMA APROXIMACIÓN (NO CONVERGIÓ)` | `El método no convergió. Se muestra la última aproximación.` | `estado-advertencia` |
| `NO_FINITO` | `NO SE PUDO RESOLVER EL SISTEMA` | `Error: el resultado no es finito.` | `estado-error` |
| `ERROR` | `NO SE PUDO RESOLVER EL SISTEMA` | `Error durante la resolución.` | `estado-error` |

**Documented deviation (record, do not "fix")**: OK + dominance note uses status `Sistema resuelto correctamente.` because spec R6.1 governs; the proposal's `Sistema resuelto con advertencias.` is superseded.

## Measured Fixtures (test oracle)

| Test | `A` | `b` | Method | Expected outcome |
|---|---|---|---|---|
| rewritten dominance test | `{{1,2},{3,4}}` | `{3,7}` | `JACOBI` | `NO_CONVERGIO`; finite iterate ≈`-1.1107837759465967E88`; it=1000, convergio=false |
| finite non-convergence | `{{1,0.999},{0.999,1}}` | `{1,2}` | `JACOBI` | `NO_CONVERGIO`; finite ≈`[-315.68, 316.63]`; it=1000 |
| non-finite iterative | `{{1,3},{2,1}}` | `{4,3}` | `JACOBI` | `NO_FINITO` (raw `[-Inf,-Inf]`) |
| non-finite iterative | `{{1,3},{2,1}}` | `{4,3}` | `SOR` | `NO_FINITO` (raw `[NaN,NaN]`) |
| non-finite direct | `{{1,2},{3,5}}` | `{1, NaN}` | `GAUSS` | `NO_FINITO`; `getConvergencia() == null` |
| OK + note | `{{1,1},{1,2}}` | `{2,3}` | `JACOBI` | `OK`; `{1,1}` @1e-6; it=50, converged; note appended |
| message gating | canonical `{{2,1},{1,3}}`, fixture 1, fixture 5, method `"NOPE"` | — | — | one result per state |
| mapper copy | 4 enum values | — | — | 8 exact strings + 3 CSS classes |

Tests assert finiteness, never the printed `1e88` literal; only the mapper test pins exact Spanish copy.

---

## Preflight

### 0.1 Confirm the 52-green baseline before touching anything
- Files: none (read-only).
- Reqs: test-execution-baseline R2.
- Run: `sh ./mvnw -o clean test`
- Expect: `Tests run: 52, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`; `grep -c "@Test" test/SistemaControllerTest.java` → `14`; `ls target/surefire-reports/TEST-*.xml | wc -l` → `11`. Stop and report if any differ.

---

## Slice 1 — Test-first contract (RED, tests only)

Files: `test/SistemaControllerTest.java` · ≈130 lines · commit message below.

### 1.1 RED-a — rewrite the dishonest assertion and add 5 tests against the OLD API
- Files: `test/SistemaControllerTest.java`.
- Do: rewrite `verificaJacobiSinDominanciaDiagonal` for fixture 1 using **only today's accessors**: `assertFalse(r.isExitoso(), "sin dominancia no debe ser exitoso")`, `assertTrue(r.isAdvertencia())`, solution non-null and every element `Double.isFinite`, `!r.getConvergencia().convergio()` with a null guard, message contains `"no es una solución del sistema"` and not `"Resuelto con éxito."`.
- Do: add 5 methods with old accessors — `verificaJacobiNoConvergeConAproximacionFinita` (`assertFalse(isExitoso, "no convergido no debe ser exitoso")`), `verificaJacobiValoresNoFinitos` (`"no finitos no debe ser exitoso"`), `verificaSORValoresNoFinitos` (`"SOR no finito no debe ser exitoso"`), `verificaMetodoDirectoNoFinito` (`"directo no finito no debe ser exitoso"`), `verificaJacobiConvergeConNotaSinDominancia` (`assertTrue(isExitoso)`, `assertFalse(isAdvertencia, "OK con nota no es advertencia")`, exact message, solution `{1,1}` @1e-6).
- Reqs: R1, R2, R3, R4, R5.
- Run: `sh ./mvnw -o test -Dtest=SistemaControllerTest`
- Expect: 19 methods run, **6 fail, 0 errors, 13 pass**; `BUILD FAILURE`. Each failure stops at its first assertion:

| Failing method | First failing assertion | Expected fragment |
|---|---|---|
| `verificaJacobiSinDominanciaDiagonal` | `assertFalse(isExitoso)` | `sin dominancia no debe ser exitoso ==> expected: <false> but was: <true>` |
| `verificaJacobiNoConvergeConAproximacionFinita` | `assertFalse(isExitoso)` | `no convergido no debe ser exitoso ==> expected: <false> but was: <true>` |
| `verificaJacobiValoresNoFinitos` | `assertFalse(isExitoso)` | `no finitos no debe ser exitoso ==> expected: <false> but was: <true>` |
| `verificaSORValoresNoFinitos` | `assertFalse(isExitoso)` | `SOR no finito no debe ser exitoso ==> expected: <false> but was: <true>` |
| `verificaMetodoDirectoNoFinito` | `assertFalse(isExitoso)` | `directo no finito no debe ser exitoso ==> expected: <false> but was: <true>` |
| `verificaJacobiConvergeConNotaSinDominancia` | `assertFalse(isAdvertencia)` | `OK con nota no es advertencia ==> expected: <false> but was: <true>` |

- Expect aggregate: `[ERROR] Tests run: 19, Failures: 6, Errors: 0, Skipped: 0`. Capture the transcript verbatim into `verify-report.md` later; a failure that is an error or a different message means the test is wrong, not the code.

### 1.2 RED-b — extend to the target API, add message and mapper tests (compile RED)
- Files: `test/SistemaControllerTest.java`.
- Do: replace old-API assertions with `SistemaController.Estado.*` + `r.getEstado()`; add `verificaMensajesPorEstado` (one result per state: canonical `OK`, fixture 1 `NO_CONVERGIO`, fixture 5 `NO_FINITO`, `"NOPE"` `ERROR` — assert each fixture's `getEstado()`, every message non-empty, only `OK` contains `"Resuelto con éxito."`, `ERROR.getSolucion() == null`) and `verificaTextosDeEstado` (8 exact strings + `estado-ok` / `estado-advertencia` / `estado-error`; `NO_CONVERGIO` heading ≠ `OK` heading; `NO_CONVERGIO` class ≠ `NO_FINITO` class). Use `var textos = SistemaController.textoDe(...)` so only three symbols are missing.
- Reqs: R1, R3, R6.
- Run: `sh ./mvnw -o test -Dtest=SistemaControllerTest`
- Expect: **no tests run**; `[ERROR] COMPILATION ERROR :` with `cannot find symbol` for `class Estado`, `method getEstado()`, `method textoDe(...)`; `Failed to execute goal ...maven-compiler-plugin...:testCompile ... Compilation failure`; `BUILD FAILURE`. Capture verbatim. Do not "fix" by adding production code in this slice.

### 1.3 Commit slice 1
- Files to stage: `test/SistemaControllerTest.java` only.
- Command: `git add test/SistemaControllerTest.java && git commit -m "test: exponer el estado real de los resultados del controlador"`
- Commit message (verbatim): `test: exponer el estado real de los resultados del controlador`
- Verify: `git show --name-only --format= HEAD` → only `test/SistemaControllerTest.java`; `git show --stat --oneline HEAD` → ~130 changed lines.
- Reqs: test-execution-baseline R2 (red commit is legal only inside this PR; never merge alone).
- Guard: never `git add -A` — `openspec/changes/` is untracked and would be swept in.

---

## Slice 2 — Controller classification (GREEN)

Files: `src/unsa/sistemas/controller/SistemaController.java` · ≈120 lines · commit message below.

### 2.1 Intermediate RED checkpoint — enum + derived flags + mapper only
- Files: `src/unsa/sistemas/controller/SistemaController.java`.
- Do: add `Estado`, `TextoEstado`, `textoDe` (final copy), `estado` field, `getEstado()`, and make `isExitoso()` / `isAdvertencia()` derive from it — temporarily deriving `estado` from the legacy flags (`exitoso`/`esAdvertencia`). No classifier, no message changes yet.
- Reqs: R1, R6.
- Run: `sh ./mvnw -o test -Dtest=SistemaControllerTest`
- Expect: `Tests run: 21, Failures: 7, Errors: 0` — red: `verificaJacobiSinDominanciaDiagonal` (message), `verificaJacobiNoConvergeConAproximacionFinita` (`expected: <NO_CONVERGIO> but was: <OK>`), `verificaJacobiValoresNoFinitos`, `verificaSORValoresNoFinitos`, `verificaMetodoDirectoNoFinito`, `verificaJacobiConvergeConNotaSinDominancia`, `verificaMensajesPorEstado`; `verificaTextosDeEstado` and 13 others green. Record all 7 names.

### 2.2 GREEN — classifier, state messages, factory removal
- Files: `SistemaController.java`.
- Do: add `clasificar(...)` + `contieneNoFinitos(...)` exactly as designed (precedence above), constants `MENSAJE_EXITO = "Resuelto con éxito."`, `NOTA_SIN_DOMINANCIA = "Nota: la matriz no es estrictamente diagonal dominante."`, `MENSAJE_NO_FINITO = "El método produjo valores no finitos (Infinity o NaN). No hay solución que mostrar."`, the `NO_CONVERGIO` message with `(N)`, `NOTA` wiring in `resolver()` for Jacobi/Gauss-Seidel without dominance; replace `resolver()` lines 121-137; delete `exito` / `exitoIterativo` / `exitoConAdvertencia`; keep `error(String)`. No finiteness guard outside the controller.
- Reqs: R1, R2, R3, R4, R5, R7.
- Run: `sh ./mvnw -o clean test`
- Expect: `Tests run: 59, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`; `grep -c "@Test" test/SistemaControllerTest.java` → `21`; 11 `TEST-*.xml`.

### 2.3 Mutation checks — each must produce red, then `git restore`
- Files: `SistemaController.java` (mutated then restored).
- Reqs: R2, R3, R4, R6.
- MUT-1 (mapper, R6): swap the `OK` and `NO_CONVERGIO` arms of `textoDe` → `sh ./mvnw -o test -Dtest=SistemaControllerTest` must fail `verificaTextosDeEstado` (heading/status/class mismatch). Restore.
- MUT-2 (finiteness, R2/R4): make `contieneNoFinitos` return `false` unconditionally → the three `NO_FINITO` tests must fail (state and `null` solution assertions). Restore.
- MUT-3 (copy, R3/R6): shorten `NO_CONVERGIO` `barraEstado` to `El método no convergió.` → `verificaTextosDeEstado` must fail on the exact string. Restore.
- After restore: `sh ./mvnw -o test` → `Tests run: 59, Failures: 0`. A mutation with no red test means the behavior is unprotected: fix the test, then repeat.

### 2.4 Commit slice 2
- Files to stage: `src/unsa/sistemas/controller/SistemaController.java` only.
- Commit message (verbatim): `feat: clasificar resultados por estado en el controlador`
- Verify: `git show --name-only --format= HEAD` → only that file; ~120 changed lines; `git status --porcelain` shows no `test/` change.
- Reqs: test-execution-baseline R2.

---

## Slice 3 — View, CSS and config

Files: `src/unsa/sistemas/view/SistemaFXController.java`, `src/unsa/sistemas/view/estilos.css`, `openspec/config.yaml` · ≈80 lines · commit message below.

### 3.1 State-first `mostrarResultado`
- Files: `src/unsa/sistemas/view/SistemaFXController.java` (`:550-635`).
- Do: reset the procedure pane; `Estado estado = resultado.getEstado()`; `var textos = SistemaController.textoDe(estado)`; single `switch (estado)` (`OK, NO_CONVERGIO` → `agregarSolucion`; `NO_FINITO, ERROR` → message only); `agregarSolucion` guards `getSolucion() == null`; trace block stays state-independent; `labelEstado.setText(textos.barraEstado())`; remove the three state classes then add `textos.claseCss()`. Delete the `:560` early return and the `:630-634` `isAdvertencia()` ternary.
- Reqs: R6, R4.
- Run: `sh ./mvnw -o clean test` → `Tests run: 59`, `BUILD SUCCESS`.
- Guard: `grep -n "isAdvertencia\|SOLUCIÓN DEL SISTEMA\|NO SE PUDO RESOLVER\|Resuelto con éxito\|Sistema resuelto" src/unsa/sistemas/view/SistemaFXController.java` → exit 1, no output (all copy comes from the mapper).

### 3.2 Append the three CSS state classes
- Files: `src/unsa/sistemas/view/estilos.css` (append at end; no new asset).
- Do: `.estado-ok { -fx-text-fill: #15803D; -fx-font-weight: bold; }`, `.estado-advertencia { -fx-text-fill: #B45309; -fx-font-weight: bold; }`, `.estado-error { -fx-text-fill: #B91C1C; -fx-font-weight: bold; }`; same specificity as `.texto-estado`, declared later.
- Reqs: R6, test-execution-baseline R1 (exactly 2 assets).
- Run: `sh ./mvnw -o clean compile` → `BUILD SUCCESS` + `Copying 2 resources from src`; `grep -c "estado-ok\|estado-advertencia\|estado-error" src/unsa/sistemas/view/estilos.css` → `3`.

### 3.3 Full green gate (source of the `59` pin)
- Files: none.
- Reqs: test-execution-baseline R2, R5.
- Run: `sh ./mvnw -o clean test`; `ls target/surefire-reports/TEST-*.xml | wc -l`
- Expect: `Tests run: 59, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`, `11`. Confirm **59** here; if the observed total differs, stop and report — do not edit tests to force it.

### 3.4 Flip `openspec/config.yaml` (tracked file — must be committed in slice 3)
- Files: `openspec/config.yaml`.
- Do (6 edits): line 15 `expect Tests run: 52` → `expect Tests run: 59 (confirm after apply)`; `strict_tdd: false` → `true`; rewrite `testing.reason` to `honest-results adds a new outcome contract (Estado/Resultado): RED-first tests were written and observed failing before the classification, messages and state-to-text mapper existed.`; runner comment `# expected: 52 tests across 11 classes` → `# expected: 59 tests across 11 classes (confirm after apply)`; replace the stale `enable_strict_tdd_requires` "stays false" block with `# Enabled by honest-results: first change to add production behavior; JUnit 5 + surefire prerequisites met by establish-build-test-baseline.`; `rules.apply` `- tdd: false` → `- tdd: true`.
- Reqs: test-execution-baseline R2; proposal strict-TDD adoption.
- Run: `grep -n "strict_tdd: true" openspec/config.yaml` → 1 match; `grep -n "tdd: true" openspec/config.yaml` → 2 lines (includes `strict_tdd`); `grep -n "59" openspec/config.yaml` → lines 15 and 26; `grep -n "52" openspec/config.yaml` → exit 1, no output.
- Note: the `59` pin is design-derived (52 + 7) but only trustworthy after 3.3.

### 3.5 R7 negative guard and diff scope
- Files: none.
- Reqs: R7, test-execution-baseline R2/R5.
- Run: `grep -rn "Double.isFinite\|Double.isNaN\|isInfinite" src/unsa/sistemas/model/` → exit 1, **no matches**.
- Run: `git diff --stat 3f2fd7e -- src/unsa/sistemas/model/` → empty.
- Run: `git diff --name-only 3f2fd7e -- src test openspec/config.yaml` → exactly `openspec/config.yaml`, `src/unsa/sistemas/controller/SistemaController.java`, `src/unsa/sistemas/view/SistemaFXController.java`, `src/unsa/sistemas/view/estilos.css`, `test/SistemaControllerTest.java`.
- Run: `git status --porcelain -- openspec/changes/` → only untracked `??` entries (change artifacts not staged).

### 3.6 Commit slice 3
- Files to stage: `src/unsa/sistemas/view/SistemaFXController.java`, `src/unsa/sistemas/view/estilos.css`, `openspec/config.yaml` only.
- Commit message (verbatim): `feat: renderizar el resultado según su estado en la vista`
- Verify: `git show --name-only --format= HEAD` → those 3 files; ~80 changed lines; `git status --porcelain -- src test` → clean.
- Reqs: test-execution-baseline R2 (the merged PR must carry the `59` pin).

### 3.7 Manual GUI acceptance (no TestFX on the classpath)
- Files: none; run `sh ./mvnw -o javafx:run` (FXML case fix is already at `3f2fd7e`).
- Reqs: R6, R4, R3.
- For each row: load the fixture, pick method type + method, run, record heading / status bar / status colour / solution lines.

| Fixture | Method | Expected `Estado` | Heading | Status bar | Colour | Panel |
|---|---|---|---|---|---|---|
| `prueba iterativo (Valor Inf).txt` | Jacobi | `NO_FINITO` | `NO SE PUDO RESOLVER EL SISTEMA` | `Error: el resultado no es finito.` | red `#B91C1C` | message only; no `xi` lines; trace visible |
| `prueba iterativo (Valor Inf).txt` | SOR | `NO_FINITO` | same | same | red | same |
| `prueba iterativo (Valor NaN).txt` | SOR | `NO_FINITO` | same | same | red | same |
| `prueba iterativo (Valor NaN).txt` | Jacobi | `NO_CONVERGIO` | `ÚLTIMA APROXIMACIÓN (NO CONVERGIÓ)` | `El método no convergió. Se muestra la última aproximación.` | amber `#B45309` | last iterate ≈1.07e301; `Convergió: No` |
| `prueba iterativo (matriz no diag. domin.).txt` | Jacobi | `NO_CONVERGIO` | same | same | amber | iterate ≈1e109; `Convergió: No` |
| `prueba iterativo (matriz no diag. domin.).txt` | Gauss-Seidel | `OK` + note | `SOLUCIÓN DEL SISTEMA` | `Sistema resuelto correctamente.` | green `#15803D` | ≈`(1,2,3)`; note in message |
| `prueba iterativo (converge).txt` | Jacobi | `OK` | `SOLUCIÓN DEL SISTEMA` | `Sistema resuelto correctamente.` | green | solution shown |
| `prueba directo (matriz singular).txt` | Gauss | `ERROR` | `NO SE PUDO RESOLVER EL SISTEMA` | `Error durante la resolución.` | red | message only |

- Assert horizontally: no `SOLUCIÓN DEL SISTEMA` heading on any non-`OK` row.
- The ≈1e88 `NO_CONVERGIO` iterate renders with an 89-digit mantissa; this is expected and **out of scope to format**.
- Row 6 status is the documented deviation (spec R6.1), not a defect.
- Sign off in `verify-report.md`; on failure, fix forward with a new commit (no amend).

---

## Risk Ranking

| Rank | Task | Risk | Why |
|---|---|---|---|
| 1 | 1.1 / 1.2 | **High** | Rewriting the test that encodes the lie; assertions must fail for the measured reason, not "bent" to fit code. Capture both RED transcripts before slice 2. |
| 2 | 3.1 | **High** | The only untested path. NPE on null solution and copy drift are invisible to the suite; only the mapper test + 3.7 catch them. |
| 3 | 2.1 | Medium | Compile-RED is weaker evidence than assertion-RED; the intermediate derived-flag step must be recorded, not skipped. |
| 4 | 3.4 | Medium | A stale/guessed count contradicts R2.1; only 3.3's real run authorises `59`. |
| 5 | 2.3 | Medium | A mutation that fails nothing means the test is tautological — fix the test, not the mutation. |

## Rollback (reverse dependency: 3 → 2 → 1)

| Slice | Action | Result |
|---|---|---|
| 3 | `git revert <slice-3 sha>` | Dishonest rendering and CSS return; config pin returns to `52` **while the suite stays at 59** because slice-1 tests remain → partial rollback leaves R2.1 inconsistent. Proceed to 2. |
| 2 | revert 3, then 2 | `getEstado`/`textoDe` disappear; slice-1 tests stop compiling → must also revert 1. |
| 1 | revert 1 | Original dishonest assertions return; `Tests run: 52`; no production file involved. |

- **Rollback correction (documented for verify)**: the design's slice-3 row claims a slice-3 revert restores `Tests run: 52`. It does not — the 7 new tests live in slice 1. The truth: full rollback (`git revert <3> <2> <1>`) returns `Tests run: 52`; a slice-3-only revert leaves 59 tests and a stale 52 pin. Do not "fix" the design; record this.
- No data, schema or build migration; `openspec/specs/` untouched until archive.

## Not in this change

- Solver math and any finiteness guard in `src/unsa/sistemas/model/**` (R7).
- `MejoramientoIterativo` implementation.
- Trace/label defects: `x3..x1` label order, `-1` step index, `RelajacionSimple` iteration-count convention.
- Parser extraction, `pruebas txt/` fixtures, packaging, `README.md`, `Sustituciones`, `ConsoleView`.
- Formatting the 89-digit iterates produced by `formatear`.
- TestFX/Monocle UI layer.
- Live-spec merge and archive (sdd-archive), FXML edits, new CSS assets.
- Committing `openspec/changes/honest-results/` or `openspec/changes/app-end-to-end/`; staging is explicit per slice.

## Verify Handoff

- RED-a transcript: 19 run / 6 failures with the six method names; RED-b transcript: 3 missing symbols.
- Final: `Tests run: 59`, 11 reports, `SistemaControllerTest` 21 methods.
- R7 greps empty; diff limited to the 5 files; mutations each produced red.
- 8 manual GUI rows signed off; deviation and rollback correction recorded above.
