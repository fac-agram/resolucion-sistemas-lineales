# Design: honest-results

- Change: `honest-results`
- Project: `resolucion-sistemas-lineales`
- Artifact store: `openspec` (+ Engram `sdd/honest-results/design`)
- Phase: design · Date: 2026-11-17
- Inputs: `proposal.md`, delta specs `result-honesty` and `test-execution-baseline`, live `test-execution-baseline` (R1/R2/R5), `openspec/config.yaml`, controller, view, `estilos.css`, `SistemaControllerTest` (14 methods), `Jacobi`/`SOR`/`ResolutorIterativo`.
- Baseline verified today: `sh ./mvnw -o clean test` → `Tests run: 52`, 11 `TEST-*.xml`, `SistemaControllerTest` = 14 methods.

## Technical Approach

Add a four-value outcome state to the controller's `Resultado`, computed once after the solver returns; derive every message, panel heading, status-bar text and CSS class from that state. Solvers keep returning `NaN`/`±Infinity` untouched (R7). The view stops asking "exitoso?" and asks "which state?", branching before it touches the solution. Tests extend the existing `SistemaControllerTest` (11-class pin holds) and were written RED-first.

**Convergence assumptions (unchanged numerics).** Jacobi/SOR/GS: `MAX_ITERACIONES = 1000`, `TOLERANCIA = 1e-7` on successive-iterate difference; `convergio()` is false when the cap is hit. Spectral radius of `D⁻¹(L+U)` explains every measurement below: `{{1,2},{3,4}}` → √1.5 ≈ 1.2247 (growth 1.2247¹⁰⁰⁰ ≈ 1e88, **finite**); `{{1,3},{2,1}}` → √6 ≈ 2.449 (overflows before the cap → ±Inf); `[[1,0.999],[0.999,1]]` → 0.999 (contraction 0.368 in 1000 iterations → **finite, not converged**).

## Measured Outcomes

Probe: `javac -cp target/classes -d /tmp/opencode/honest-probe HonestProbe.java && java -cp target/classes:/tmp/opencode/honest-probe HonestProbe` (JavaFX-free path; temp sources live only in `/tmp/opencode/`). Pre-change results:

| Fixture | Method | `isExitoso` / `isAdvertencia` | Solution | Convergence | Correct state |
|---|---|---|---|---|---|
| `A={{1,2},{3,4}}, b={3,7}` | JACOBI | true / true | `[-1.1107837759465967E88, -1.1107837759465946E88]` — **0 non-finite** | it=1000, err=2.5918e88, convergio=false | `NO_CONVERGIO` |
| `A={{1,2},{3,4}}, b={3,7}` | GAUSS_SEIDEL | true / true | `[1.6451207958748997E176, -1.2338405969061748E176]` — finite | it=1000, convergio=false | `NO_CONVERGIO` |
| `A=[[1,0.999],[0.999,1]], b={1,2}` | JACOBI | true / false | `[-315.6778219502637, 316.6267532787718]` — finite | it=1000, err=0.73613, convergio=false | `NO_CONVERGIO` |
| `A=[[1,0.999],[0.999,1]], b={1,1}` | JACOBI | true / false | `[0.3163104428359359, 0.3163104428359359]` — finite | it=1000, err=0.36807, convergio=false | `NO_CONVERGIO` |
| `prueba iterativo (Valor Inf).txt` → `A={{1,3},{2,1}}, b={4,3}` | JACOBI | true / true | `[-Infinity, -Infinity]` — 2 non-finite | it=1000, err=Infinity | `NO_FINITO` |
| same | SOR (ω=1.5) | true / false | `[NaN, NaN]` with `"Resuelto con éxito."` | it=1000, err=NaN | `NO_FINITO` |
| same | GAUSS_SEIDEL | true / true | `[Infinity, -Infinity]` | it=1000, err=NaN | `NO_FINITO` |
| `prueba iterativo (Valor NaN).txt` → `A={{1,2},{2,1}}, b={3,3}` | JACOBI | true / true | `[-1.0715086071862672E301, …]` — **finite** | it=1000, convergio=false | `NO_CONVERGIO` |
| same | SOR | true / false | `[NaN, NaN]` | it=1000 | `NO_FINITO` |
| `A={{1,2},{3,5}}, b={1, NaN}` | GAUSS | true / false | `[NaN, NaN]`, convergencia=null | — | `NO_FINITO` |
| `A={{1,1},{1,2}}, b={2,3}` (non-dominant) | JACOBI | true / true | `[0.9999999701976776, 0.9999999701976776]` | it=50, convergio=true | `OK` + note |

**Fixture mapping correction (spec/proposal implied otherwise).** `{{1,2},{3,4}}` — the matrix of the existing dishonest test — is **finite** (`1e88`), so it proves `NO_CONVERGIO`, **not** `NO_FINITO`. `"prueba iterativo (Valor NaN).txt"` under Jacobi is also finite (`1.07e301`) and proves `NO_CONVERGIO`; only SOR/GS on it are non-finite. `NO_FINITO` is proven by (a) Jacobi/GS/SOR on the *Valor Inf* fixture, and (b) the direct path `GAUSS` with `b={1, NaN}` (`[NaN, NaN]`, convergencia `null`) — the cheapest deterministic fixture. `OK` + no-dominance note is proven by `{{1,1},{1,2}}, b={2,3}` (converges in 50 iterations, non-dominant).

## Architecture Decisions

| # | Decision | Alternatives rejected | Rationale |
|---|---|---|---|
| D1 | `Estado` enum nested in `SistemaController`; `Resultado.estado` is the single source of truth; `isExitoso()`/`isAdvertencia()` become derivations | boolean pair / third top-level class | matches existing nested-type convention (`Convergencia`, `Resultado`); kills flag drift |
| D2 | Classify in `Resultado.clasificar(...)`, called once after the solver returns | per-factory states / guards in solvers | R7 forbids solver guards; one place to read precedence |
| D3 | Non-converged + finite → `NO_CONVERGIO` with the last iterate kept | treat as `OK`+note / as `ERROR` | Decision B: show the iterate, label it not-a-solution; hiding it is dishonest the other way |
| D4 | Non-finite → `NO_FINITO`, `getSolucion() == null`, trace and convergence kept | return the `NaN` vector | R4 forbids exposing non-finite values as a solution; trace still documents the divergence |
| D5 | Keep `getConvergencia()` as the only carrier of `iteraciones`/`convergio`; no new primitive accessors | add `getIteraciones()`/`isConvergido()` | second source of truth; a null/0 policy for direct methods is ambiguous; 13 tests already read the record |
| D6 | Pure mapper `SistemaController.textoDe(Estado)` returning `TextoEstado(titulo, barraEstado, claseCss)` | mapper in `view`; heading/status literals in the view | reachable from the existing test class (R5), controller has zero JavaFX imports, and state→CSS distinctness becomes headlessly testable |
| D7 | OK + non-dominance collapses into `OK`: note appended to `mensaje`, no fourth state, no fourth rendering | fourth state / status driven by `isAdvertencia()` | spec R5 makes `isAdvertencia()` mean `NO_CONVERGIO`; R6.1 pins OK's status; flag-driven status is what this change deletes |
| D8 | Remove success factories `exito`/`exitoIterativo`/`exitoConAdvertencia`; keep `error(String)` | keep them public | a public factory that hardcodes `"Resuelto con éxito."` re-opens the lie; no consumer outside the controller (grep-verified) |
| D9 | Trace block stays state-independent (renders for `NO_FINITO` too) | render trace only on success | the trace is the evidence of divergence; `formatear` already handles `Infinity`/`NaN` |

**Deviation to confirm at verify:** the proposal's behavior table gives `OK`+note the status `"Sistema resuelto con advertencias."`; delta spec R6.1 fixes every `OK` status to `"Sistema resuelto correctamente."` and R5 removes the flag. The spec governs (D7): status is `"Sistema resuelto correctamente."`, the note lives in the panel message. This is a deliberate, documented change of status-bar text for converging non-dominant runs.

## Estado Precedence

Effective order (R2), all inside the existing `try` so the `catch` remains the last resort:

1. **Structural `ERROR`** — `metodo == null` (`:91`), bad dimensions (`:99`), unknown method (`:104`), `solucion == null` (`:117`), any exception from construction/`resolver()` (`:138`). All return `Resultado.error(...)` → `Estado.ERROR`, `solucion == null`.
2. **`NO_FINITO`** — `contieneNoFinitos(solucion)`: any element where `!Double.isFinite(x)` (covers `NaN`, `+Inf`, `-Inf`). Precedes convergence.
3. **`NO_CONVERGIO`** — `convergencia != null && !convergencia.convergio()`. Direct methods have `convergencia == null` and can never land here.
4. **`OK`** — everything else, including direct methods and converged iteratives with or without the note.

```java
// SistemaController.resolver(), replacing lines 121-137
Convergencia convergencia = (r instanceof ResolutorIterativo iter)
        ? new Convergencia(iter.getIteraciones(), iter.getErrorFinal(), iter.convergio())
        : null;
String nota = convergencia != null
        && (JACOBI.equals(metodo) || GAUSS_SEIDEL.equals(metodo))
        && !ValidadorMatriz.esDiagonalDominante(A)
        ? Resultado.NOTA_SIN_DOMINANCIA : null;
return Resultado.clasificar(solucion, metodo, traza, convergencia, nota);

// inside Resultado — private, single decision point
private static Resultado clasificar(double[] solucion, String metodo, Traza traza,
                                    Convergencia convergencia, String nota) {
    if (contieneNoFinitos(solucion)) {                       // null-safe: solucion non-null here
        return new Resultado(Estado.NO_FINITO, null, MENSAJE_NO_FINITO, metodo, traza, convergencia);
    }
    if (convergencia != null && !convergencia.convergio()) {
        String mensaje = "El método no convergió en el máximo de iteraciones ("
                + convergencia.iteraciones()
                + "). Se muestra la última aproximación; no es una solución del sistema.";
        return new Resultado(Estado.NO_CONVERGIO, solucion, mensaje, metodo, traza, convergencia);
    }
    String mensaje = nota == null ? MENSAJE_EXITO : MENSAJE_EXITO + " " + nota;
    return new Resultado(Estado.OK, solucion, mensaje, metodo, traza, convergencia);
}

private static boolean contieneNoFinitos(double[] solucion) {
    for (double v : solucion) { if (!Double.isFinite(v)) return true; }
    return false;
}
```

`null` safety: `clasificar` is only reached after the `solucion == null` guard; `contieneNoFinitos` never receives `null`. The view's `null` guard is separate (see View Integration). Messages: `MENSAJE_EXITO = "Resuelto con éxito."`, `NOTA_SIN_DOMINANCIA = "Nota: la matriz no es estrictamente diagonal dominante."`, `MENSAJE_NO_FINITO = "El método produjo valores no finitos (Infinity o NaN). No hay solución que mostrar."`; `ERROR` keeps today's specific text (existing tests assert `contains("no reconocido")`).

## Resultado Shape

| Member | Type | Semantics |
|---|---|---|
| `estado` | `Estado` (final) | one of `OK`, `NO_CONVERGIO`, `NO_FINITO`, `ERROR` |
| `solucion` | `double[]`, nullable (final) | finite last iterate for `OK`/`NO_CONVERGIO`; `null` for `NO_FINITO`/`ERROR` |
| `mensaje` | `String` (final) | state-derived, non-empty (R3) |
| `metodoUsado` | `String`, nullable (final) | `null` on `ERROR` (unchanged) |
| `traza` | `Traza`, nullable (final) | unchanged; kept on `NO_FINITO` |
| `convergencia` | `Convergencia`, nullable (final) | unchanged record `(iteraciones, errorFinal, convergio)`; `null` for direct methods and `ERROR` |
| `getEstado()` | `Estado` | new, R1 |
| `isExitoso()` | `estado == Estado.OK` | retained; 13 tests and R5.1 depend on it |
| `isAdvertencia()` | `estado == Estado.NO_CONVERGIO` | retained per decision 6; **note:** was `true` for OK+note, now `false` |
| `getSolucion()` / `getMensaje()` / `getMetodoUsado()` / `getTraza()` / `getConvergencia()` | unchanged signatures/semantics | the 13 passing tests read them; only `getSolucion()`'s nullability expands to `NO_FINITO` |
| `static error(String)` | `Resultado` | public, `Estado.ERROR`, `solucion == null` |
| `private static clasificar(...)` | `Resultado` | replaces the three success factories |

No primitive `getIteraciones()`/`isConvergido()` (D5).

## Mapper Spec

`sistemaController.textoDe(Estado)` → `SistemaController.TextoEstado`, a pure, JavaFX-free, allocation-only record `(String titulo, String barraEstado, String claseCss)` implemented as an exhaustive `switch` expression with no `default` (Java 17 enum exhaustiveness). Placement: nested in `SistemaController` (D6). Both strings are asserted verbatim by `SistemaControllerTest.verificaTextosDeEstado` — the spec's exact copy:

| `Estado` | `titulo` | `barraEstado` | `claseCss` |
|---|---|---|---|
| `OK` | `SOLUCIÓN DEL SISTEMA` | `Sistema resuelto correctamente.` | `estado-ok` |
| `NO_CONVERGIO` | `ÚLTIMA APROXIMACIÓN (NO CONVERGIÓ)` | `El método no convergió. Se muestra la última aproximación.` | `estado-advertencia` |
| `NO_FINITO` | `NO SE PUDO RESOLVER EL SISTEMA` | `Error: el resultado no es finito.` | `estado-error` |
| `ERROR` | `NO SE PUDO RESOLVER EL SISTEMA` | `Error durante la resolución.` | `estado-error` |

Three renderings (ok / warning / error) over four states: `NO_FINITO` and `ERROR` share the error rendering but differ in status text. `NO_CONVERGIO`'s heading contains no `SOLUCIÓN DEL SISTEMA` (R6.2).

## View Integration

`SistemaFXController.mostrarResultado` (`:550-635`) becomes state-first: branch, then format. The current early return at `:560` and the `isAdvertencia()` ternary at `:630` disappear; no success literal lives in the view.

```java
private void mostrarResultado(SistemaController.Resultado resultado) {
    areaProcedimiento.clear();
    paneProcedimiento.setExpanded(false);
    paneProcedimiento.setDisable(true);

    SistemaController.Estado estado = resultado.getEstado();
    SistemaController.TextoEstado textos = SistemaController.textoDe(estado);
    StringBuilder texto = new StringBuilder(textos.titulo()).append("\n\n");

    switch (estado) {                                   // the only state branch
        case OK, NO_CONVERGIO -> agregarSolucion(texto, resultado);
        case NO_FINITO, ERROR  -> texto.append(resultado.getMensaje());
    }
    areaResultado.setText(texto.toString());

    Traza traza = resultado.getTraza();                 // state-independent (D9), :620-628 unchanged
    if (traza != null && !traza.estaVacia()) { areaProcedimiento.setText(...); paneProcedimiento.setDisable(false); }

    labelEstado.setText(textos.barraEstado());
    labelEstado.getStyleClass().removeAll("estado-ok", "estado-advertencia", "estado-error");
    labelEstado.getStyleClass().add(textos.claseCss());
}

private void agregarSolucion(StringBuilder texto, SistemaController.Resultado resultado) {
    double[] solucion = resultado.getSolucion();
    if (solucion != null) {                              // NPE guard: state first, null check second
        for (int i = 0; i < solucion.length; i++) {
            texto.append("x").append(i + 1).append(" = ").append(formatear(solucion[i])).append("\n");
        }
    }
    texto.append("\nMétodo utilizado: ").append(resultado.getMetodoUsado());
    texto.append("\n\n").append(resultado.getMensaje());
    var convergencia = resultado.getConvergencia();       // :594-609 block moves here verbatim
    if (convergencia != null) { /* Iteraciones / Error final / Convergió */ }
}
```

No TestFX on the classpath → the view has no automated oracle; the mapper test plus the manual checklist below carry R6.

## CSS Classes

Additive rules at the end of the existing `src/unsa/sistemas/view/estilos.css` (R1's "exactly 2 assets" holds; `.texto-estado` from `Sistema.fxml:117` stays):

```css
.estado-ok         { -fx-text-fill: #15803D; -fx-font-weight: bold; }  /* OK */
.estado-advertencia{ -fx-text-fill: #B45309; -fx-font-weight: bold; }  /* NO_CONVERGIO */
.estado-error      { -fx-text-fill: #B91C1C; -fx-font-weight: bold; }  /* NO_FINITO + ERROR */
```

Same specificity as `.texto-estado`, declared later → wins for `-fx-text-fill`; font size stays 12px. Gives `NO_CONVERGIO` (amber) and `NO_FINITO` (red) distinct visuals, satisfying the proposal's requirement with no script test.

## Data Flow

```
SistemaFXController → SistemaController.resolver(A, b, metodo)
                         ├─ structural guards ──────────────────────► ERROR
                         ├─ Resolutor.resolver() → double[]  (may hold NaN/±Inf)
                         ├─ Convergencia (iterative only)
                         └─ Resultado.clasificar(...) → estado
                    → mostrarResultado(resultado)
                         ├─ textoDe(estado) → titulo | barraEstado | claseCss
                         ├─ switch(estado) → panel body (solution only for OK/NO_CONVERGIO)
                         └─ labelEstado: barraEstado + claseCss
```

## File Changes

| File | Action | Description |
|---|---|---|
| `src/unsa/sistemas/controller/SistemaController.java` | Modify | `Estado`, `TextoEstado`, `textoDe`, `estado` field, `clasificar`, state messages, factory removal |
| `src/unsa/sistemas/view/SistemaFXController.java` | Modify | `mostrarResultado` state-first refactor; delete success/error literals and `isAdvertencia()` use |
| `src/unsa/sistemas/view/estilos.css` | Modify | 3 additive state classes |
| `test/SistemaControllerTest.java` | Modify | 1 rewritten + 7 new methods (14 → 21) |
| `openspec/config.yaml` | Modify | test count, `strict_tdd`, stale comment |
| `src/unsa/sistemas/model/**` | **Unchanged** | R7 |

## Testing Strategy (strict TDD)

Red-first sequence is binding; no production line before its failing test.

**Slice 1 — `test: exponer el estado real de los resultados del controlador`** (files: `test/SistemaControllerTest.java`, ≈130 lines). Order inside the commit:
1. Rewrite `verificaJacobiSinDominanciaDiagonal` using **only today's accessors** and add tests 2-6 with today's accessors: `assertFalse(r.isExitoso())`, `assertNull(r.getSolucion())`, `assertFalse(getMensaje().contains("Resuelto con éxito."))`, `assertFalse(r.isAdvertencia())` for the OK+note fixture. Run `sh ./mvnw -o test` → **6 failing test methods**, each stopping at its first assertion with a measured failure: `isExitoso()` is `true` where it must be `false`, a non-finite vector is exposed as a solution, and the OK+note message is the dominance note. Record the output.
2. Extend the same tests to the target API (`getEstado()`, `Estado.*`) and add `verificaMensajesPorEstado` + `verificaTextosDeEstado`. Run again → **compile error** `cannot find symbol: class Estado` / `getEstado()` / `textoDe` — the wished-for API, as prescribed for an API extension.
Verification: both RED artifacts captured; test file has 8 touched methods.

| Test | Fixture (measured) | Assertions (hand-derived) | Fails today because |
|---|---|---|---|
| `verificaJacobiSinDominanciaDiagonal` (rewritten) | `{{1,2},{3,4}}`, `{3,7}`, JACOBI | `NO_CONVERGIO`; `!isExitoso`; `isAdvertencia`; solution non-null, all `Double.isFinite`; `!convergencia.convergio()`; message contains `"no es una solución del sistema"`, not `"Resuelto con éxito."` | `isExitoso()` true, message is the note |
| `verificaJacobiNoConvergeConAproximacionFinita` | `[[1,0.999],[0.999,1]]`, `{1,2}` | `NO_CONVERGIO`; finite solution; `iteraciones() > 0`; `convergio()` false | state absent; today `OK` (no note) |
| `verificaJacobiValoresNoFinitos` | `{{1,3},{2,1}}`, `{4,3}` | `NO_FINITO`; `!isExitoso`; `getSolucion() == null`; message contains `"no finitos"` | success with `[-Inf,-Inf]` |
| `verificaSORValoresNoFinitos` | same | `NO_FINITO`; `getSolucion() == null` | `NaN` under `"Resuelto con éxito."` |
| `verificaMetodoDirectoNoFinito` | `{{1,2},{3,5}}`, `{1, NaN}`, GAUSS | `NO_FINITO`; `getSolucion() == null`; `getConvergencia() == null` | `[NaN,NaN]` reported as success |
| `verificaJacobiConvergeConNotaSinDominancia` | `{{1,1},{1,2}}`, `{2,3}` | `OK`; `isExitoso`; `!isAdvertencia`; message equals `"Resuelto con éxito. Nota: la matriz no es estrictamente diagonal dominante."`; solution `{1,1}` @1e-6 | `isAdvertencia()` true, message is the note |
| `verificaMensajesPorEstado` | one per state (`"NOPE"` for `ERROR`) | all messages non-empty; only `OK` contains `"Resuelto con éxito."` (R3); `ERROR.getSolucion() == null` | text is state-independent |
| `verificaTextosDeEstado` | 4 enum values | the 8 exact strings + 3 CSS classes; `NO_CONVERGIO` heading ≠ `OK` heading; `NO_CONVERGIO` class ≠ `NO_FINITO` class | `textoDe` missing |

**Slice 2 — `feat: clasificar resultados por estado en el controlador`** (files: controller only, ≈120 lines). Minimal code to satisfy slice 1: enum, `estado` field, classifier, messages, `textoDe`/`TextoEstado`, factory removal. First run after adding only the enum + `getEstado()` derived from legacy flags must show the state assertions failing with `expected: <NO_CONVERGIO> but was: <OK>`; then add the classifier. Verification: `Tests run: 59, Failures: 0, Errors: 0, Skipped: 0`, 11 reports.

**Slice 3 — `feat: renderizar el resultado según su estado en la vista`** (files: view, `estilos.css`, `openspec/config.yaml`, ≈80 lines). No new test is possible (JavaFX UI, no TestFX); the mapper test from slice 1 already pins the copy. Verification: full suite green + manual checklist.

Commit budget: ≈130 / ≈120 / ≈80 changed lines — each far below the 400-line review budget. No chained PR needed; the three commits stack directly to `main`.

## Config Changes

| Location | From | To |
|---|---|---|
| `config.yaml:15` | `expect Tests run: 52, Failures: 0, Errors: 0.` | `expect Tests run: 59, Failures: 0, Errors: 0.` |
| `config.yaml:19` | `strict_tdd: false` | `strict_tdd: true` |
| `config.yaml:20-23` | mechanical-conversion `reason` | `honest-results adds a new outcome contract (Estado/Resultado): RED-first tests were written and observed failing before the classification, messages and state-to-text mapper existed.` |
| `config.yaml:26` | `# expected: 52 tests across 11 classes` | `# expected: 59 tests across 11 classes` |
| `config.yaml:43-49` | "strict_tdd stays false …" block | `# Enabled by honest-results: first change to add production behavior; JUnit 5 + surefire prerequisites met by establish-build-test-baseline.` |
| `config.yaml:69` | `- tdd: false` | `- tdd: true` |

Count derivation: 52 + 7 new methods = **59**; `SistemaControllerTest` 14 → 21 methods, class count stays 11 (R5 intact). **Confirm 59 after apply** — it is the value to verify against `Tests run:`.

## Verification Gates

1. `sh ./mvnw -o clean test` → `Tests run: 59, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`, 11 `TEST-*.xml`.
2. RED evidence: slice-1 run shows the 6 failing test methods, then the compile error for `Estado`/`getEstado`/`textoDe`.
3. `git diff --name-only` = the 5 files above; `git diff --stat -- src/unsa/sistemas/model` empty.
4. `grep -rn "Double.isFinite\|Double.isNaN\|Double.isInfinite" src/unsa/sistemas/model/` → no matches (verified today: none).
5. Mutation check: flipping `NO_CONVERGIO`/`OK` in the classifier, dropping the `contieneNoFinitos` loop, or changing one mapper string must each fail at least one test.
6. Manual GUI (after `gui-launch-fix`; no automated UI oracle): `prueba iterativo (Valor Inf).txt` + Jacobi → red `NO SE PUDO RESOLVER EL SISTEMA`, no solution lines; same file + SOR → same; `prueba iterativo (matriz no diag. domin.).txt` + Jacobi → amber `ÚLTIMA APROXIMACIÓN (NO CONVERGIÓ)` with the ≈1e109 iterate; same file + Gauss-Seidel → green `SOLUCIÓN DEL SISTEMA` with `≈(1,2,3)` and the note; `prueba iterativo (converge).txt` + Jacobi → green.

## Rollback

| Slice | Revert | Result |
|---|---|---|
| 3 | `git revert <view commit>` | view/CSS/config return to the dishonest rendering and `Tests run: 52`; controller contract stays |
| 2 | revert slice-3, then `<controller commit>` | `getEstado`/`textoDe` disappear; slice-1 tests no longer compile → revert slice 1 too |
| 1 | revert slice-1 | `Tests run: 52`, original assertions restored; no production file involved |

Order is 3 → 2 → 1 (reverse dependency). No data/schema/build migration; `openspec/specs/` untouched until archive.

## Risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| OK+note status text diverges from the proposal table (spec R6.1 wins) | High (deliberate) | documented above; flagged for verify; mapper test pins the spec string |
| `isAdvertencia()` flips to `false` for OK+note | Med | no consumer outside view+tests (grep-verified); view no longer reads it |
| Compile-RED is weaker evidence than assertion-RED | Med | two-step RED in slice 1: assertion failures first, API compile errors second |
| `NO_CONVERGIO` renders 89-digit values (`1.1e88`) via `formatear` | Med | honest and labelled; number formatting is out of scope |
| No UI test layer | High | mapper test pins copy; manual checklist with measured per-state fixtures |
| A future caller re-introducing a hardcoded success message | Low | success factories removed (D8) |

## Open Questions

None blocking. Every fixture, precedence rule, string, placement and count above is resolved with measured evidence.

## Next

Ready for `sdd-tasks`.
