# view-modes

<!--
Provenance
- Capability: view-modes (NEW capability; published from the ADDED-only delta of vista-sistema-matriz).
- Origin change: vista-sistema-matriz
- Source: openspec/changes/archive/2026-11-25-vista-sistema-matriz/specs/view-modes/spec.md
- Commits: C1 619ae38 (refactor: extraer los textos de vista a una clase pura) + C2 5c90456 (feat: alternar
  entre vista de ecuaciones y matricial) on branch feature/tunable-parameters, base da55414.
- Branch state at sync: feature/tunable-parameters is NOT merged to main (main @ 59f5777; merge decision
  C2-752 pending — user-owned). The behavior below is the reality of HEAD 5c90456.
- Synced: 2026-11-25 (archive execution date; the change was verified 2026-11-23). Note: the archive launch
  contract carried "2026-11-19", which is the parametros-ajustables verify-era date, not this change's —
  corrected per skill rule (archive date = date the sync ran) and audit honesty.
- Origin verdict: pass_with_warnings (0 CRITICAL; W1-W4; see archive-report.md and verify-report.md in the
  archived change).
- Suite at origin HEAD: 113 tests / 15 classes; openspec/config.yaml was re-pinned to this truth by this
  archive (task 3.3). The live test-execution-baseline R5 snapshot (12 classes / 82 methods) still describes
  merged mainline reality; its re-pin is deferred to a merge-time sync step (see archive-report.md, R5
  decision) — 113/15 supersedes the pending 105/14 duty of the unarchived parametros-ajustables T4.4.
- Requirements R1-R7 and their scenarios (R1.1-R7.3, 16 scenarios) are preserved verbatim from the delta
  spec; ids unchanged.
- Carried WARNING (verify W2): the selector/pane-adjacent UI surface (R1, R4, R5, R6) has zero automated
  coverage by spec design (no TestFX/Monocle in the toolset; pom must stay untouched). All six
  CHECKLIST-GUI rows were mechanically attested headful under Xephyr by the verify-phase harness
  (/tmp/opencode/verify-vsm — driver vsm.Driver, real input-robot events, byte assertions on live
  scene-graph state, 26 PNGs). User personal sign-off of CHECKLIST-GUI.txt (task 3.1) remains OPTIONAL
  backlog, not an archive gate — checklist rows 6/6 mechanically attested.
- Carried WARNING (verify W1): the explicit aplicarVista(vistaModo) call inside the D6 Limpiar trio is
  defense-in-depth; removing it alone is behavior-identical, so no test or checklist row can catch that
  minimal variant. Row (5) guards the observable trio variants (state + selection + visibility reset) only.
- Successor note (forward reference): active change vista-solucion-matriz plans a MODIFIED delta against
  this capability — R1 (mode state) and R6 (selector) become obsolete (machinery deleted), R4 survives as
  a content invariant with re-scoped targets, R5 is rewritten as the pane disable/collapse mirror, and
  R2/R3/R7 survive verbatim (its proposal, obsolete-by-design register). Until that change archives, this
  spec stays faithful to the shipped HEAD 5c90456.
-->

## Purpose

Equation ↔ matrix presentation modes for the system-input areas: a per-session view-mode state
(`ECUACIONES | MATRICIAL`), JavaFX-free pure text builders in `unsa.sistemas.utils.TextosVista`
(`formatear`, `textoEcuaciones`, `textoMatrizAumentada`), an aligned augmented `[A|b]` text contract,
render-always visibility semantics (visibility never gates rendering), and "Limpiar" full-reset behavior.
Presentation only: `matrizA`/`vectorB` remain the single source of truth and the data path stays
byte-identical (the `textoEcuaciones` byte-pin is the first-ever headless gate on procedure-trace R3.1).

## Requirements

### Requirement: R1 View-mode state

The controller MUST hold exactly one view mode with values `ECUACIONES | MATRICIAL`. It MUST default to `ECUACIONES` at application start and after `limpiarCampos`. Load operations (file or manual) MUST NOT change the mode. Mode MUST NOT persist across restarts — its lifetime is per-session only.

> Test mapping: manual rows only (no JavaFX harness exists); state transitions are observed via R6 checklist rows (6) and (5).

#### Scenario: R1.1 Initial mode

- GIVEN a fresh application start
- WHEN the UI renders
- THEN mode is `ECUACIONES`, the "Ecuaciones" button is selected, and only the equation zone is visible

#### Scenario: R1.2 Loads never change mode

- GIVEN mode `MATRICIAL` is active
- WHEN the user loads a system by file or manually
- THEN mode remains `MATRICIAL` and the augmented box shows the newly loaded system

#### Scenario: R1.3 "Limpiar" resets mode

- GIVEN mode `MATRICIAL` is active
- WHEN the user presses "Limpiar"
- THEN mode becomes `ECUACIONES`

### Requirement: R2 textoEcuaciones byte-identity (first headless R3.1 pin)

`unsa.sistemas.utils.TextosVista.textoEcuaciones(double[][] a, double[] b)` MUST be JavaFX-free and return a string byte-identical to the one `mostrarSistema` builds today (`SistemaFXController.java:459-482`): per row `a·x1 + b·x2 … = c` with sign tokens (`" + "`, `" - "`, leading `"-"`), coefficients formatted via `formatear` on their absolute value, labels in order `x1…xn` matching column order, and a trailing `\n` after each row. This is an extraction — the algorithm MUST NOT be rewritten. Rationale: this test is the first-ever headless pin of procedure-trace R3.1 (x1→xn order assertion included), replacing manual-checklist-only evidence.

> Test mapping: `TextosVistaTest` (new class) — pin test #1 is suite gate; design locks exact count.

#### Scenario: R2.1 Canonical 3x3 byte-pin

- GIVEN `A = [[4,1,1],[1,3,1],[1,1,3]]`, `b = [6,5,5]`
- WHEN `textoEcuaciones(a, b)` runs
- THEN the output equals `"4·x1 + 1·x2 + 1·x3 = 6\n1·x1 + 3·x2 + 1·x3 = 5\n1·x1 + 1·x2 + 3·x3 = 5\n"` exactly

#### Scenario: R2.2 Sign and zero branches

- GIVEN row `[-2, 0, 1.5]` with `b = 4`
- WHEN `textoEcuaciones` runs on that single-row system
- THEN the row renders `"-2·x1 + 0·x2 + 1.5·x3 = 4\n"` (zero shown; sign token precedes absolute-formatted value)

### Requirement: R3 textoMatrizAumentada format contract

`TextosVista.textoMatrizAumentada(double[][] a, double[] b)` MUST return one single text block: one line per equation; every cell rendered via `formatear`; per-COLUMN width = the maximum formatted-cell width within that column (sign included), so columns align vertically; exactly one bar separator between the A columns and the b column; alignment is pure text — no CSS may be relied on. The exact line template (brackets, pad side) MUST be locked as byte-exact literals by design-pinned tests.

> Test mapping: `TextosVistaTest` alignment pins.

#### Scenario: R3.1 Ragged magnitudes aligned

- GIVEN column cells that format to `"1"`, `"300000000000"` (`3e11` — `toPlainString` never yields scientific notation) and `"0.5"`
- WHEN `textoMatrizAumentada` renders
- THEN every row places each column's cells at the same character offsets (widths 1, 12, 3)

#### Scenario: R3.2 Square system

- GIVEN the canonical 3×3 fixture
- WHEN rendered
- THEN three lines of equal length appear, each with one separator before the b cell

#### Scenario: R3.3 NaN cell pins formatear behavior

- GIVEN a matrix entry `NaN` (defensive: the loader rejects non-finite values, but the pure helper MUST pin it)
- WHEN rendered
- THEN the cell is `"NaN"` (width 3), per `formatear` :528-530 — no exception, no padded variant

#### Scenario: R3.4 Single-column edge

- GIVEN `A = [[2],[3]]`, `b = [1,6]`
- WHEN rendered
- THEN each line holds one padded A cell, the separator, one b cell; consistent offsets

### Requirement: R4 Render-always across zones

Visibility changes MUST NOT gate rendering. On every successful load, all four targets — `areaSistema`, `areaMatrizA`, `areaVectorB`, `areaMatrizAumentada` — MUST receive `setText` with the current data regardless of which zone is visible. Existing render sites (:232-233, :330-331) MUST remain unconditional; hidden legacy A/b boxes keep receiving today's exact byte format.

> Test mapping: manual rows — verified by toggling zones without re-loading.

#### Scenario: R4.1 Hidden targets hold current data

- GIVEN mode `MATRICIAL` and a just-loaded system
- WHEN the user switches back to "Ecuaciones" without any re-load
- THEN the equation area instantly shows the current system (it was rendered while hidden)

#### Scenario: R4.2 Augmented box fresh while hidden

- GIVEN mode `ECUACIONES` and a completed load
- WHEN the user switches to "Matricial"
- THEN the augmented box shows that load's data with no re-render triggered by the switch

### Requirement: R5 "Limpiar" full reset

`limpiarCampos` MUST clear all four view targets including hidden ones, reset the mode to `ECUACIONES`, and update the selector so "Ecuaciones" shows selected (precedent: it already resets the parameters row, :817).

> Test mapping: manual row (matrix below).

#### Scenario: R5.1 Limpiar matrix

- GIVEN mode `MATRICIAL` with a loaded system
- WHEN the user presses "Limpiar"
- THEN all four TextAreas are empty — hidden emptiness proven by toggling back to "Matricial" after the reset — AND the selector shows "Ecuaciones" selected

### Requirement: R6 Selector UI (manual-only surface)

The UI MUST provide one segmented selector: two `ToggleButton`s labeled "Ecuaciones" and "Matricial" sharing a `ToggleGroup`, placed above the zones; exactly one is selected at all times, highlighting the current mode. Switching MUST toggle zone visibility only (`visible`/`managed`, per the :159-169 precedent) — never re-render, never re-parse. The selector MUST work independently of the C3 `filaParametros` row. No ControlsFX dependency; `pom.xml` untouched.

> Test mapping: this change's `CHECKLIST-GUI.txt` — explicit rows: (1) segmented look, exactly one button highlighted = current mode; (2) one click flips zones; exactly one zone visible; content equals last load; (3) switching causes no re-render and no status-message change; (4) `filaParametros` appears/disappears with method choice identically in both modes; (5) "Limpiar" resets selection visual to "Ecuaciones"; (6) file and manual loads in "Matricial" keep the selection and update the augmented box.

#### Scenario: R6.1 Manual checklist passes

- GIVEN the built application
- WHEN the operator executes rows (1)–(6)
- THEN all six pass

### Requirement: R7 Non-regression gates

The change MUST NOT break: procedure-trace R3.1 equation-string equality (live spec text unchanged — no delta), the `filaParametros`/`parametrosVisiblesPara` behavior, or R5.1 stale-clear semantics (result/procedure panels still clear on load, including while in `MATRICIAL` — render-always plus clear-always keeps hidden targets non-stale).

> Test mapping: suite-wide `sh ./mvnw -o clean test` green, unedited existing classes.

#### Scenario: R7.1 R3.1 pin is the first gate

- GIVEN the extraction commit
- WHEN the suite runs
- THEN the R2.1 byte-pin test passes before any UI work lands; any drift reddens the suite

#### Scenario: R7.2 Parameter-row parity

- GIVEN method selection drives `filaParametros`
- WHEN existing tests run after this change
- THEN all parameter-row assertions pass unedited in both view modes' code paths

#### Scenario: R7.3 R5.1 holds in MATRICIAL

- GIVEN a solved previous system and mode `MATRICIAL`
- WHEN a new system is loaded
- THEN result and procedure panels are cleared/disabled, AND the hidden view targets hold the new data (not stale text)
