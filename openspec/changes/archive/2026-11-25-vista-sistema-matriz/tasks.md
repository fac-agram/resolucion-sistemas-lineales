# Tasks: Vista Sistema ↔ Matricial (mode toggle)

Baseline `feature/tunable-parameters` @ `da55414` (106/14). Store `hybrid`; strict TDD: RED precedes the class in C1.

## Review Workload Forecast

C1 ~285 lines, C2 ~140; each commit < 400. 2 work-unit commits stack on `feature/tunable-parameters`; no PR slice; `ask-on-risk`, nothing to ask.

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: feature-branch-chain
400-line budget risk: Low

## Phase 0: Baseline gate

- [x] 0.1 Verify branch `feature/tunable-parameters`, HEAD `da55414`, `sh ./mvnw -o clean test` = `Tests run: 106, Failures: 0` / 14 classes. Stop on miss.
- [x] 0.2 C1/C2 stage only `src/`, `test/`, `README.md` — never `openspec/` (prior pattern `0a7824f`, `68fb4a8`); change dir stays untracked.

## Phase 1: C1 `refactor: extraer los textos de vista a una clase pura`

- [x] 1.1 RED FIRST — write `test/TextosVistaTest.java` (no package, JUnit 5, Spanish method names, byte-exact `assertEquals`, imports only `TextosVista`). Tests T1–T7 per design `test_plan_final`; T7 edges `0.0→"0"`, `-0.0→"0"`, `NaN→"NaN"`, `±∞→"Infinity"/"-Infinity"`, `3e11→"300000000000"`, `1e-5→"0.00001"`, divergence pin `1e-7→"0.0000001"` vs `formatearNumero` `"1E-7"` — never merge helpers). Run suite → compile-RED `cannot find symbol unsa.sistemas.utils.TextosVista`; capture transcript in apply log. Locked literals verbatim, no improvising:

```java
// T1 R2.1 / T2 R2.2
"4·x1 + 1·x2 + 1·x3 = 6\n1·x1 + 3·x2 + 1·x3 = 5\n1·x1 + 1·x2 + 3·x3 = 5\n"
"-2·x1 + 0·x2 + 1.5·x3 = 4\n"
// T3 R3.1 ragged (2 fixtures) / T4 R3.2 / T5 R3.3 NaN / T6 R3.4
"[            1 | 3 ]\n[ 300000000000 | 4 ]\n[          0.5 | 5 ]\n"
"[ -2            0   1.5 |    4 ]\n[  1 300000000000   0.5 |    3 ]\n[  7           -3 12.25 | -0.5 ]\n"
"[ 4 1 1 | 6 ]\n[ 1 3 1 | 5 ]\n[ 1 1 3 | 5 ]\n"
"[ 1 NaN | 5 ]\n[ 3   4 | 6 ]\n"
"[ 2 | 1 ]\n[ 3 | 6 ]\n"
```

- [x] 1.2 Create `src/unsa/sistemas/utils/TextosVista.java` (final, private ctor, Spanish Javadoc per `ValidadorParametros`; draft `/tmp/opencode/vistaprobe/src/.../TextosVista.java`): `formatear` verbatim move of `:526-543` (signature only); `textoEcuaciones` byte-identical extraction of `mostrarSistema` builder `:459-482`; `textoMatrizAumentada` new — `"[ "` cells `" | "` b `" ]\n"`, ASCII `|`, right-align per column to max `formatear` width.
- [x] 1.3 Controller delegates only: `mostrarSistema` → `areaSistema.setText(TextosVista.textoEcuaciones(matrizA, vectorB));`; `formatear` → `return TextosVista.formatear(valor);`. 9 callsites (`:479,:481,:501,:513,:681,:698,:750,:769,:778`) unchanged; `+import ...TextosVista`, `−import java.math.BigDecimal` (`:14`).
- [x] 1.4 C1 gates: `113 / 15 classes`; `git diff --name-only da55414 -- test/` = `TextosVistaTest.java` only; `Sistema.fxml` diff EMPTY; `git diff -w` proves `formatear` body unchanged; **R3.1 pin green — T1 is the first-ever headless pin of `procedure-trace` R3.1**; `grep -c "300000000000" test/TextosVistaTest.java` ≥ 2.
- [x] 1.5 Commit #1 `refactor: extraer los textos de vista a una clase pura` (~285 lines; zero FXML, zero visible change).

## Phase 2: C2 `feat: alternar entre vista de ecuaciones y matricial`

- [x] 2.1 FXML per `fxml_diff_plan`: Toggle imports after `:8`; selector HBox after `:44` — `botonVistaEcuaciones` (`text="Ecuaciones"`, `selected="true"`, nested `<toggleGroup><ToggleGroup fx:id="grupoVista"/></toggleGroup>`) + `botonVistaMatricial` (`text="Matricial"`, `toggleGroup="$grupoVista"`); wrap `:46-48` as-is → `VBox fx:id="zonaEcuaciones" VBox.vgrow="ALWAYS"`; `:50-62` → `VBox fx:id="zonaMatricial" visible="false" managed="false" VBox.vgrow="ALWAYS"`; NEW `TextArea fx:id="areaMatrizAumentada"` `wrapText="false" prefRowCount="6"` after `:62`; `filaParametros:70-83` untouched.
- [x] 2.2 Controller: `@FXML` `zonaEcuaciones`/`zonaMatricial`/`areaMatrizAumentada` after `:48`, `grupoVista`/`botonVistaEcuaciones`/`botonVistaMatricial` after `:60`; `private enum VistaModo { ECUACIONES, MATRICIAL }` + field after `:85`; listener on `grupoVista.selectedToggleProperty()` (**not** `selectedValue` — probe-verified) at end of `initialize()`; `aplicarVista(VistaModo)` (sole visibility writer) + `mostrarMatrizAumentada()` after `:169`; `+mostrarMatrizAumentada();` after `:233` and `:331`; `limpiarCampos`: `+areaMatrizAumentada.clear()` @`:801` + trio `vistaModo=ECUACIONES; botonVistaEcuaciones.setSelected(true); aplicarVista(vistaModo)` @`:817` — probe 3b: redundant `setSelected` is SILENT → apply visibility explicitly (pin in Spanish Javadoc).
- [x] 2.3 README Spanish view section, short user copy ("Botones Ecuaciones/Matricial…"); write `openspec/changes/vista-sistema-matriz/CHECKLIST-GUI.txt` (C3 format), 6 rows 1:1 with spec R6 (1)–(6).
- [x] 2.4 C2 gates: `113 / 15`; parity — each of `zonaEcuaciones zonaMatricial areaMatrizAumentada grupoVista botonVistaEcuaciones botonVistaMatricial` = 1× FXML `fx:id=` AND ≥1× `@FXML` (log grep loop); render-always — no `visible`/`managed` wraps any `setText` (cite hit lines); `test/` diff still only `TextosVistaTest.java`; boots `sh ./mvnw -o javafx:run` (Xephyr).
- [x] 2.5 Commit #2 `feat: alternar entre vista de ecuaciones y matricial` (~140 lines; CHECKLIST-GUI untracked until archive).

## Phase 3: User-owned follow-up

- [ ] 3.1 Run the 6 CHECKLIST rows manually (harness `/tmp/opencode/vistaprobe` re-runnable); sign each.
- [ ] 3.2 Merge decision `C2-752` (`parametros-ajustables`) — now +2 stacked commits same branch.
- [ ] 3.3 Archive re-pin `openspec/config.yaml:15,26`: 105/14 → 106/14 → **now 113/15**.
- [ ] 3.4 Package v1.3 AFTER the merge decision.

## Highest risk (ranked)

1. R3.1 drift → T1/T2 byte-pins + probe harness. 2. Silent redundant `setSelected` → explicit `aplicarVista` trio. 3. `wrapText` breaks offsets → `wrapText="false"` + T3–T6. 4. Wrapper reflow → `vgrow` on new nodes only. 5. `·` mojibake → UTF-8, verify first GREEN.

## Out of scope

Parser, solver numerics, manual-dialog internals, `estilos.css`, `pom.xml`, cross-restart persistence, R5.1 clear-logic content, removing `fx:id`s, `procedure-trace` edits. Amendments honored: nested enum no predicate (D1/D2); ASCII bar (D4).
