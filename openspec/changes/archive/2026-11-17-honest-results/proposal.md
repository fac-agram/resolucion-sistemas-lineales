# Proposal: honest-results

- Change: `honest-results`
- Project: `resolucion-sistemas-lineales`
- Artifact store: `openspec`
- Phase: propose
- Date: 2026-11-17
- Classification: **behavioral change** (controller contract + UI rendering). Not build/tooling, not algorithm math.

## Intent

The application reports non-convergence and non-finite values as success (verified).

- `SistemaController.java:121-137` builds `Convergencia` and never branches on `iter.convergio()`.
- `SistemaController.java:200-216` hardcodes `"Resuelto con éxito."` for every success factory.
- `SistemaFXController.java:573-591` prints `SOLUCIÓN DEL SISTEMA` for any `isExitoso()`; `:630-634` prints `"Sistema resuelto correctamente."` whenever `!isAdvertencia()`.
- Reproduced: Jacobi on `prueba iterativo (Valor Inf).txt` → `x1 = -Infinity` under the success heading; SOR on the same file → `NaN` with `Resuelto con éxito.`; a 1000-iteration diverging run prints `Convergió: No` beneath `SOLUCIÓN DEL SISTEMA`.

Why now: item #2 of `app-end-to-end`; changes #3 (`mejoramiento-iterativo`) and #5 (`file-loading-fixtures`) consume this contract, and a `null`/false-success contract cannot be fixed downstream.

**Strict TDD is adopted by this change.** It writes new production behavior (an outcome contract), so the JUnit 5 + surefire 3.5.4 baseline qualifies. `openspec/config.yaml` `testing.strict_tdd: false` and its `reason` MUST flip to `true`/rewritten during apply. This phase does not touch config.

## Scope

### In Scope

- Outcome contract in `SistemaController`: `enum Estado { OK, NO_CONVERGIO, NO_FINITO, ERROR }`; `Resultado` carries `getEstado()`, convergence flag, iterations, last iterate (via existing `getConvergencia()` / `getSolucion()`).
- Remove the hardcoded success message; every message is derived from the state.
- Classify: finite + converged → `OK`; finite + not converged → `NO_CONVERGIO` + last iterate; any non-finite value (direct or iterative) → `NO_FINITO` with `getSolucion() == null`.
- Consume the state in `SistemaFXController.mostrarResultado`: branch on `getEstado()` before touching the solution; render three distinct panel texts and status-bar texts; toggle a CSS class in `estilos.css`.
- Rewrite `test/SistemaControllerTest.verificaJacobiSinDominanciaDiagonal` (the assertion that encodes the lie) and add coverage for the new branches.
- Update the `Tests run` pins in `openspec/config.yaml` (context line 15, runner comment line 26).

### Out of Scope

- Algorithm math. No finiteness guards inside `Jacobi`/`GaussSeidel`/`SOR`/`RelajacionSimple`/`Cholesky`: solvers keep returning `Infinity`/`NaN`, and that is correct — the controller catches it.
- `MejoramientoIterativo` implementation.
- Trace/label defects (`x3..x1` label order, `-1` step index, iteration-count convention in `RelajacionSimple`).
- Parser extraction, fixtures, packaging, README, `Sustituciones`, `ConsoleView`.

## Capabilities

### New Capabilities

- `result-honesty`: the outcome contract between controller and view — states, classification rules, and what each state may display.

### Modified Capabilities

- `test-execution-baseline`: **R2** pins `Tests run: 52` (and `config.yaml` mirrors it twice). This change alters the count, so R2 MUST be superseded by a delta requirement. **R5** also pins `11 classes / 52 methods`; it only needs superseding if design adds a *new* test class instead of extending `test/SistemaControllerTest.java` (default: extend the existing class → R5 stays intact).

## Approach

1. `Estado` enum nested in `SistemaController`; `Resultado` gains `estado` as the single source of truth. `isExitoso()` is retained as a derived accessor (`estado == OK`) so the seven pre-existing `!isExitoso()` error assertions (`:92,103,259,276,293,310,327`) stay valid; `isAdvertencia()` is removed (its only callers are the two sites this change rewrites).
2. One private classifier after `resolver()` returns: `solucion == null` → `ERROR`; any `!Double.isFinite(x)` → `NO_FINITO` (solution nulled); iterative && `!convergio()` → `NO_CONVERGIO`; else `OK`.
3. Dominance stays informational: a converged Jacobi/Gauss-Seidel run without dominance is `OK` plus a note. Non-convergence wins over the note; the note is never the primary message.
4. The view switches on the state first, then reads the solution — `NO_FINITO`/`ERROR` have no solution to format.

### Decision B semantics (already decided — not reopened)

- Non-converged but finite: WARNING plus the last iterate, explicitly labelled as not converged. Never hidden, never called success.
- Non-finite: ERROR, nothing displayed.
- No new physical state, no retry, no auto-switch of method.

## Behavior Table

| `Estado` | Condition | Panel heading + message (Spanish UI copy) | Solution shown | Status bar |
|---|---|---|---|---|
| `OK` | Finite, converged (or direct) | `SOLUCIÓN DEL SISTEMA` / `Resuelto con éxito.` | Yes | `Sistema resuelto correctamente.` |
| `OK` + note | Finite, converged, no diagonal dominance | Same heading / success text + `Nota: la matriz no es estrictamente diagonal dominante.` | Yes | `Sistema resuelto con advertencias.` |
| `NO_CONVERGIO` | Finite, `convergio() == false` | `ÚLTIMA APROXIMACIÓN (NO CONVERGIÓ)` / `El método no convergió en el máximo de iteraciones (N). Se muestra la última aproximación; no es una solución del sistema.` | Yes, labelled | `El método no convergió. Se muestra la última aproximación.` |
| `NO_FINITO` | Any `NaN` / `±Infinity` | `NO SE PUDO RESOLVER EL SISTEMA` / `El método produjo valores no finitos (Infinity o NaN). No hay solución que mostrar.` | No | `Error: el resultado no es finito.` |
| `ERROR` | Exception, null solution, unknown method, bad dimensions | `NO SE PUDO RESOLVER EL SISTEMA` / existing message | No | `Error durante la resolución.` |

`NO_CONVERGIO` and `NO_FINITO` MUST be visually distinguishable; today `labelEstado` has a single `texto-estado` class.

## Affected Areas

| Area | Impact | Description |
|---|---|---|
| `src/unsa/sistemas/controller/SistemaController.java` | Modified | `Estado`, `Resultado` fields/factories, classifier, message derivation |
| `src/unsa/sistemas/view/SistemaFXController.java` | Modified | `mostrarResultado` branches on state; no success framing outside `OK` |
| `src/unsa/sistemas/view/estilos.css` | Modified | warning/error status classes (same file — R1's "exactly 2 assets" holds) |
| `test/SistemaControllerTest.java` | Modified | dishonesty assertion rewritten; new branch coverage |
| `openspec/config.yaml` | Modified | `Tests run` pins, `strict_tdd: true` + `reason` |
| `openspec/specs/test-execution-baseline/spec.md` | Unchanged here | R2 superseded via delta spec in this change |

## Compatibility Impact

| Item | Current | After |
|---|---|---|
| `test/SistemaControllerTest.verificaJacobiSinDominanciaDiagonal:123` | `assertTrue(r.isExitoso())` for a diverging Jacobi | `assertEquals(NO_CONVERGIO, r.getEstado())` + `assertFalse(r.isExitoso())` |
| same test `:124-125` | `assertTrue(r.isAdvertencia())`, exact success-note message | `isAdvertencia()` removed; non-convergence message asserted via `getEstado()` |
| `verificaJacobi`…`verificaCholesky*` (13 tests) | `isExitoso()` / `getConvergencia() == null` | Unchanged — `OK` remains the only `isExitoso()` |
| `SistemaFXController:560,573,607,630` | `isExitoso()` / `isAdvertencia()` branching | `getEstado()` branching |
| `test-execution-baseline` R2 | `Tests run: 52` | **Superseded**; new expectation ≈**57** (estimate: 52 + 4–6 new tests) |
| `test-execution-baseline` R5 | 11 classes / 52 methods | Unchanged if tests go in the existing class (recommended) |
| `openspec/config.yaml` | `52` twice, `strict_tdd: false` | new count, `strict_tdd: true` |

## Review Workload Forecast

| Field | Value |
|---|---|
| Chained PRs recommended | **Yes** — 3 slices |
| 400-line budget risk | **Medium/borderline** — exploration sizes this change at ~250-320 lines |
| Decision needed before apply | **Yes** — (a) extend `SistemaControllerTest` vs new test class (decides whether R5 must also be superseded); (b) remove `isAdvertencia()` vs keep it derived; (c) CSS classes in the same slice as the controller |
| Estimated changed lines | ~250-320 total |

Suggested slices, each one commit under 400 lines:

1. Slice 1 — tests: rewrite `verificaJacobiSinDominanciaDiagonal` + add converged/non-converged/non-finite/direct-non-finite cases (~110-140 lines). Must be RED.
2. Slice 2 — `SistemaController`: enum, classifier, message derivation, factories (~90-120 lines). GREEN.
3. Slice 3 — view + CSS + config pins (~60-90 lines).

## Risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| The existing test asserts the lie; the diff looks like "tests bent to fit code" | High | Slice 1 is test-first and its RED output is recorded in the verify report; the delta spec names the changed assertion explicitly |
| UI status derivation is spread across `mostrarResultado` and the `labelEstado` ternary at `:630` | Med | Single `switch` on `getEstado()`; both texts derived from the same state, asserted by one test-visible mapping if design extracts it |
| `getSolucion()` becomes `null` for `NO_FINITO` → NPE if the view formats before branching | Med | State branch is the first statement after the trace reset; covered by a `NO_FINITO` test asserting `getSolucion() == null` |
| Spanish copy drifts between panel and status bar | Med | Both strings defined once per state and asserted verbatim in tests / manual checklist |
| No UI test layer (TestFX/Monocle absent) | High | Manual verification through the running GUI; `gui-launch-fix` is a prerequisite for that oracle only |
| Adding a third CSS *file* would break R1's "exactly 2 assets" | Low | New classes go in the existing `estilos.css` |

## Rollback Plan

Revert the commits of this change (slices 3 → 1). No data, schema, or build-file migration is involved: `SistemaController`, `SistemaFXController`, `estilos.css`, `test/SistemaControllerTest.java`, and the `config.yaml` pins return to `Tests run: 52` with the old (dishonest) contract restored. The change is additive at the API level — `getEstado()` disappears with it, so no external consumer breaks. Nothing in `openspec/specs/` is mutated until archive; dropping `openspec/changes/honest-results/` leaves the live specs untouched.

## Dependencies

- JUnit 5 + surefire 3.5.4 baseline (present, 52 green).
- Manual acceptance requires `gui-launch-fix` (FXML case mismatch) — not a code dependency for the headless tests.

## Success Criteria

- [ ] RED proof recorded: the rewritten `verificaJacobiSinDominanciaDiagonal` FAILS against the pre-change controller (`isExitoso()` returns `true` for the diverging Jacobi), for the expected reason, before implementation.
- [ ] `sh ./mvnw -o clean test` exits 0 with `Failures: 0, Errors: 0` and ≈57 tests (estimate; exact count fixed by design).
- [ ] `Jacobi` on a 1000-iteration diverging but finite system → `NO_CONVERGIO`, last iterate non-null and finite, and no success string in the result message.
- [ ] `Jacobi` on `prueba iterativo (Valor Inf).txt` and `SOR` on the same fixture → `NO_FINITO`, `getSolucion() == null`.
- [ ] A direct solver returning `NaN` (controller API, e.g. `Cholesky`) → `NO_FINITO`, not success.
- [ ] Converged Jacobi/Gauss-Seidel/direct runs still → `OK`; the 13 untouched assertions stay green.
- [ ] `git diff --stat` shows no file under `src/unsa/sistemas/model/` changed.
- [ ] Manual GUI check (after `gui-launch-fix`): the three states render with distinct headings and status-bar styling; the two fixtures above show warning/error respectively, never `SOLUCIÓN DEL SISTEMA`.
- [ ] `openspec/config.yaml` pins the new count and `strict_tdd: true`.
