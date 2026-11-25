# Exploration: Vista Sistema ↔ Matricial (toggle between equation view and matrix view)

- Change: `vista-sistema-matriz`
- Project: `resolucion-sistemas-lineales`
- Artifact store: `hybrid` (this file + Engram `sdd/vista-sistema-matriz/explore`)
- Phase: explore (read-only investigation; no code changes)
- Date: 2026-11-23
- Baseline: branch `feature/tunable-parameters` @ `da55414` (base `main@59f5777`), 106 tests / 14 classes green. This change stacks on the **same branch** (single-front decision by user), on top of `parametros-ajustables` (C3, unmerged).

## User request (verbatim)

> "quiero poder alternar entre la vista de sistema de ecuacion y vista matricial"

## Decision Summary

| Aspect | Verdict | Rationale (verified) |
|---|---|---|
| Data model | **Zero impact** | `matrizA`/`vectorB` fields (`SistemaFXController.java:84-85`) are the single source; both renderers are read-only over them — no re-parse needed on toggle. |
| Mechanism | **visible/managed flip, both zones still rendered on load** | Exact pattern already shipped by C3 (`aplicarVisibilidadParametros`, `:159-169`; `filaParametros` FXML `:70-83`). Keeping both render calls (`:232-233`, `:330-331`) untouches R5.1 and avoids stale-text bugs. |
| Selector control | **Open question Q1** (segmented ToggleButtons vs ComboBox) | No ControlsFX in pom → no `ToggleSwitch`; both are base-JavaFX. FXML already uses 2 ComboBoxes (`:65`, `:68`) so combo is idiomatic; ToggleGroup is 1-click. |
| "Vista matricial" content | **Reuse existing separate `Matriz A` + `Vector b` boxes** (app's own label "Forma matricial", FXML `:50-62`) | Augmented single `[A|b]` block is a redesign, not a toggle → Q2. |
| Manual dialog | **Out of scope** | Dialog (`:251-355`) is coefficient-row entry in one editable TextArea (`:266-270`), not a viewer; nothing to toggle inside. |
| Tests | **+5–6 headless unit tests** via extracted pure renderers | Follows `ValidadorParametrosTest.ecoToleranciaUsaLaCopiaSeis` byte-exact precedent (`:150-154`) and `parametrosVisiblesPara` pattern (`SistemaController.java:192` tested `ParametrosTest.java:268-283`). Suite 106 → ~111–112. |
| Commits | **2 commits confirmed** | (1) pure helpers + tests + delegation; (2) FXML + listener. ~150–210 diff lines, well under the 400-line budget; no chained PR. |

## 1. Current layout map (what the user sees today)

**Both views are displayed SIMULTANEOUSLY, always.** There is no alternation today — every load populates all three read-only areas and all sit stacked in the left column.

`Sistema.fxml` (137 lines) — left VBox "Ingreso del sistema" (`:35-90`):

| FXML line | Node | Role |
|---|---|---|
| `:37` | `Label "Ingreso del sistema"` | section title |
| `:39-44` | buttons + `labelArchivo` | load entry points |
| `:46` | `Label "Sistema de ecuaciones"` | zone A header |
| `:48` | `TextArea fx:id="areaSistema"` (editable=false, `VBox.vgrow=ALWAYS`) | **equation view** |
| `:50` | `Label "Forma matricial"` | zone B header |
| `:51-62` | HBox → VBox(:53) + VBox(:58) | matrix zone wrapper |
| `:55` | `TextArea fx:id="areaMatrizA"` | **A box** (prefHeight 111, no wrap) |
| `:60` | `TextArea fx:id="areaVectorB"` | **b box** (prefHeight 111, no wrap) |
| `:64-68` | `comboTipoMetodo`, `comboMetodo` | method pickers |
| `:70-83` | `filaParametros` (C3; `visible=false managed=false`) | ω/ε/N row |
| `:85-89` | Resolver / Limpiar | actions |

Controller wiring (`SistemaFXController.java`, 840 lines). fx:id ↔ field parity confirmed — **no dead fields**: `areaSistema` FXML`:48`↔`:42`, `areaMatrizA` FXML`:55`↔`:45`, `areaVectorB` FXML`:60`↔`:48`, `filaParametros` FXML`:70`↔`:57`, `celdaOmega` FXML`:71`↔`:60.

Load flows (identical shape):
- **`cargarArchivo` `:176-249`** — parse `interpretarArchivo` `:209`, split into `nuevaA/nuevoB` `:211-224`, commit to fields `:227-228`, render BOTH: `mostrarSistema(); mostrarFormaMatricial();` `:232-233`, R5.1 clear `:235-238`.
- **`cargarManual` `:251-355`** — programmatic `Dialog<ButtonType>` `:254-293` with instructions `:273` + example `:260-264` + one editable `TextArea` `:266-270` for space-separated coefficient rows (`"4 1 1 6"` format — **not** equation text). Same parse/commit/render: `:305`, `:324-325`, `:330-331`, `:333-336`.
- **`interpretarArchivo` `:362-450`** — the real parser (orchestrator's "buscarMetodo/parsearMatriz" do not exist): `List<String>` → augmented `double[][]`, validates rectangular + square + finite, `IllegalArgumentException` with line numbers.
- **`mostrarSistema` `:457-484`** — builds `"a·x1 + b·x2 ... = c"` per row from fields; the R3 label code `·x(j+1)` is at **`:479`**; `areaSistema.setText` `:483`.
- **`mostrarFormaMatricial` `:491-520`** — builds `"[ v v ]"` rows for A (`:496-505`) and `"[ v ]"` rows for b (`:510-515`); sets the two areas `:518-519`. **Correction to the brief:** this method contains no equation labels — the x1..xn code is in `mostrarSistema:479` and the trace's augmented `| ` rendering is in `mostrarTraza:744-751`, not "~:418".
- **`formatear` `:526-543`** — private pure: NaN/Infinity/0 guards + `BigDecimal.stripTrailingZeros().toPlainString()`. Used by ALL renderers + `agregarSolucion` `:681` + `mostrarTraza`.
- `limpiarCampos` `:792-822` clears the three areas `:799-801`; `resolverSistema`/`mostrarResultado` `:570-672` never touch the view zones.

## 2. Toggle options (trade-offs)

Common recommendation frame: whichever control, the **content strategy is fixed and cheap** — keep populating all zones on load (both render calls unchanged), and make the toggle a pure `visible/managed` flip over two wrapper VBoxes. That reuses the exact C3 mechanism and keeps parsing/solving/rendering paths byte-identical.

| Option | Design | Changes | Reuses | Trade-off |
|---|---|---|---|---|
| **(a) ToggleButton** near `areaSistema` | One button; text mirrors target state ("Ver forma matricial" ↔ "Ver sistema de ecuaciones"); flips zone visibility | ~2 FXML edits (wrappers + button), ~10 controller lines | all 3 areas + both renderers | 1 click, but state is only inferable from button text (no "selected" affordance); label-swap string must be kept in sync. |
| **(b) Mode selector row** (segmented `ToggleButton`×2 in a `ToggleGroup`, or 2-item `ComboBox`) | Selector shows CURRENT mode; flips the same two zones | same wrappers; +1 HBox row; listener like `initialize:145-150` precedent | all 3 areas + both renderers | Self-documenting current view; segmented = 1 click; ComboBox = 2 clicks but matches existing combo idiom (`:65,:68`) and needs no CSS for segment styling. **ToggleSwitch unavailable — ControlsFX not in pom (verified).** |
| **(c) TabPane overlay** | Replace `:46-62` with `TabPane` (tabs "Ecuaciones"/"Matricial"), existing TextAreas moved into tab content | FXML restructure (import + nesting), controller unchanged | areas survive inside tabs | Native "which tab am I on" clarity, but: biggest FXML diff/reflow risk around `VBox.vgrow=ALWAYS` (`:48`), tab chrome fights `estilos.css` (no `.tab` classes exist), and semantics are less "mode" than desired. |

Matrix/vector boxes: in (a)/(b) the A and b boxes **remain exactly as today** inside the hidden-but-populated zone B; in (c) they move wholesale into tab 2. No option deletes an fx:id.

## 3. Recommended design

**Option (b), segmented form**: wrap FXML `:46-48` in `VBox fx:id="zonaEcuaciones"` and `:50-62` in `VBox fx:id="zonaMatricial"`; insert one `HBox` row (between `labelArchivo` row `:44` and the zones) holding two `ToggleButton`s "Ecuaciones" / "Matricial" sharing a `ToggleGroup` (Ecuaciones `selected=true` by default). Controller gains one listener doing exactly what `aplicarVisibilidadParametros:159-169` does for the param row: `setVisible/setManaged` on the two zones. Load flows keep calling both renderers `:232-233`/`:330-331` **untouched**.

Why: (1) it is the repo's own established pattern (C3 shipped last week — reviewers already know the mechanism); (2) presentation-only — data path byte-identical, zero re-parse (fields `:84-85` are the single source, verified read-only in both renderers); (3) the visible zone inherits the freed vertical space cleanly because `areaSistema` grows (`:48`) while the matrix HBox is fixed-height (`:55,:60`); (4) control type stays a one-line swap if the user prefers the ComboBox form (Q1), since only the listener wiring differs.

## 4. Pure-helper extraction + test surface (proposed)

New FX-free class `unsa.sistemas.utils.TextosVista` (presentation-string helpers already live in `utils` — `ValidadorParametros.ecoTolerancia` is the precedent):

| Signature | Extracted from | Notes |
|---|---|---|
| `static String formatear(double)` | `SistemaFXController:526-543` | controller's private copy delegates (single source; also used by `agregarSolucion`/`mostrarTraza`) |
| `static String textoEcuaciones(double[][] a, double[] b)` | `mostrarSistema:459-482` | body → pure; `mostrarSistema` becomes `areaSistema.setText(textoEcuaciones(matrizA, vectorB))` |
| `static String textoMatriz(double[][] a)` | `mostrarFormaMatricial:494-505` | ditto |
| `static String textoVector(double[] b)` | `mostrarFormaMatricial:508-515` | ditto |
| `static String textoMatrizAumentada(double[][] a, double[] b)` | only if Q2 = augmented | mirrors `mostrarTraza:744-751` `[ ... | v ]` style |

Test plan — new `test/TextosVistaTest.java`, byte-exact Spanish-string assertions (style of `ValidadorParametrosTest:150-154`), characterization-first (assert the CURRENT literal output so the extraction provably changes nothing):
1. 3×3 example block renders `4·x1 + 1·x2 + 1·x3 = 6` rows in x1→x3 order → **first ever headless pin of procedure-trace R3.1** (today manual-checklist-only, spec `:68`)
2. sign branches: leading `-`, inner ` - `, zero coefficients still shown
3. `textoMatriz` exact `"[ 4  1  1 ]"` lines; 4. `textoVector` exact `"[ 6 ]"` lines; 5. `formatear` edge set (0→"0", NaN, Infinity, stripTrailingZeros); 6. (optional) augmented row with ` | `.
**Estimate: +5–6 tests → suite 106 → ~111–112.** Plus one manual GUI checklist row (same as C3's `CHECKLIST-GUI.txt`): toggle flips zones, both pre-populated, choice survives re-solve, `Limpiar` behavior per design.

## 5. Regression risks

- **procedure-trace R3.1 (spec `:64-74`) — the pin that matters:** equation text semantics (`·x1 ·x2 ·x3` L→R, solution order) MUST stay byte-identical through extraction. Mitigation: the toggle changes **visibility, never rendering**; renderers stay invoked on every load; test #1 above pins the exact string. A rework that re-renders per-mode or reformulates equations would violate R3 — forbidden in this change.
- **R5.1 stale-clear (spec `:101-119`, attested live):** do NOT "optimize" by skipping `mostrarSistema`/`mostrarFormaMatricial` for the hidden zone — result/procedure clears at `:235-238`/`:333-336` must keep running and hidden-but-stale text is the bug to avoid. Rule: render always, show selectively.
- **C3 entanglement:** `filaParametros` `:70-83` and its listener `:145-150` are logically independent (params vs view mode); only placement is shared. Selector row goes above the method combos — combo/param logic untouched.
- **Layout reflow:** zone swap changes left-column height (grow-area `:48` vs fixed 111px boxes `:55,:60`); accepted precedent exists (C3 row appears/disappears mid-column). Do not alter `prefRowCount`/`vgrow` attributes.
- **fx:id parity:** maintained; the two new wrappers get fx:ids on BOTH sides (FXML + `@FXML` fields) or FXML load fails.
- **`limpiarCampos:799-801`:** clears all three areas regardless of visibility — correct as-is; recommend keeping current mode selection on clear (reset choice = open Q3, cosmetic).

## 6. Interaction with pending decisions

- `parametros-ajustables` merge decision **C2=752 untouched** — this change stacks on `feature/tunable-parameters` per the user's single-front decision; its untracked openspec folder already sits alongside; ours adds exactly one more (`openspec/changes/vista-sistema-matriz/`).
- **Archive re-pin note (for that change's T4.4-style task):** `openspec/config.yaml:15,26` still say "expect 82 / 12 classes" (stale at 106/14). After this change the truth is **~111–112 tests / 15 classes** — whoever re-pins next should read the post-change number, not 82 or 106.

## 7. Effort + commit shape

~150–210 diff lines: FXML (~15) + controller (~35: 2 wrapper fields, listener, delegations) + `TextosVista` (~60) + tests (~70–90). Effort Low–Medium (focused half-day). **2 work-unit commits confirmed** (matches the C1/C3 shape on this branch):
1. `refactor: extraer el renderizado de texto a un auxiliar puro` — `TextosVista` + `TextosVistaTest` (RED: tests against missing class compile-fail first, per strict TDD) + controller delegation; zero visual change.
2. `feat: permitir alternar entre vista de ecuaciones y vista matricial` — FXML zones + selector + flip listener + checklist row.

Single PR under the 400-line budget → no chained-PR.

## 8. Open questions for the user

- **Q1 (control form):** one-click segmented toggle ["Ecuaciones" | "Matricial"] (recommended) vs a ComboBox like the method pickers? Both are base JavaFX; only the listener differs.
- **Q2 (matrix view content):** keep today's split `Matriz A` + `Vector b` boxes (recommended — that IS what the app labels "Forma matricial"), or also/instead show a single augmented `[A|b]` block in trace style? Augmented = +1 helper, +1 test, slightly more scope.
- **Q3 (choice lifetime):** is the default "Ecuaciones" on every app start and after `Limpiar` fine, or should the chosen mode persist across loads/clears within the session? (Recommend: within-session keep; reset only on fresh start — fewest new rules.)

## 9. Ready for proposal?

**Yes.** Presentation-only change, mechanism precedented on this very branch, data-model impact verified zero, regression surface bounded to two pinned specs (R3.1, R5.1) that the design explicitly protects. Resolve Q1–Q3 during proposal; defaults recommended above.
