# Delta for view-modes

<!--
Provenance
- Capability: view-modes (NEW capability; ADDED-only delta — no other spec file in this change).
- Change: vista-sistema-matriz · Branch feature/tunable-parameters @ da55414 (106 tests / 14 classes green).
- procedure-trace is NOT modified: no delta file for it. R3.1 (openspec/specs/procedure-trace/spec.md:64-74)
  is preserved by construction (extraction returns byte-identical strings) and pinned headlessly for the
  FIRST time by the R2 pin test below.
- Verified pins: mostrarSistema builder SistemaFXController.java:459-483; formatear :526-543
  (NaN→"NaN", ±Infinity→"Infinity"/"-Infinity", 0.0→"0" incl. -0.0, else stripTrailingZeros().toPlainString()
  — never scientific notation); render sites :232-233 (file) / :330-331 (manual); visibility precedent
  aplicarVisibilidadParametros :159-169; limpiarCampos :792-822 (params reset :817); FXML zones
  Sistema.fxml:46-48 (ecuaciones) / :50-62 (matricial) / filaParametros :70-83.
- Canonical R3.1 fixture (cargarManual example, :260-264): A = [[4,1,1],[1,3,1],[1,1,3]], b = [6,5,5].
- Locked decisions carried: segmented ToggleButtons/ToggleGroup "Ecuaciones"/"Matricial"; single augmented
  [A|b] box with legacy A/b areas present-hidden; render-always; per-session mode; "Limpiar" resets mode and
  clears ALL targets; TextosVista pure helper in utils; pom untouched; no ControlsFX.
- Out of scope: augmented-box styling beyond alignment, parser, manual-dialog internals, persistence across
  restarts (session-only), CSS class design.
- Testing surface rule: headless tests import no JavaFX; anything requiring the live UI is a manual checklist
  row. Exact test/class counts are locked in design (proposal ≈ 110–112 tests; class-count delta 14→15 per
  proposal vs 16 per launch note — design reconciles).
-->

## ADDED Requirements

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
