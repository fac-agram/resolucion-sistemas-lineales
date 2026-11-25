# Design: Vista Sistema ↔ Matricial (mode toggle)

- Change: `vista-sistema-matriz` · Artifact store: `hybrid` (this file + Engram `sdd/vista-sistema-matriz/design`).
- Baseline: `feature/tunable-parameters` @ `da55414` (verified: 106 `@Test` across 14 `*Test` classes; `TrazaDemo.java` has 0).
- All literals below were **produced by probes in `/tmp/opencode/vistaprobe/`**, not typed by hand: `Orig.java` is a verbatim `sed` extraction of `mostrarSistema`/`mostrarFormaMatricial`/`formatear` bodies from the current controller; `TextosVista.java` is the designed final class; every locked string is actual probe stdout. Reusable at apply/verify as the byte-diff harness.

## Technical Approach

Two work-unit commits, per proposal: (C1) extract the view's presentation strings into FX-free `unsa.sistemas.utils.TextosVista` and delegate, byte-identically — probe-proven zero drift; (C2) add the segmented selector + zone wrappers + `visible`/`managed` flip, reusing the C3 mechanism (`aplicarVisibilidadParametros:159-169`). Rendering stays unconditional on every load (render-always, R4); the toggle NEVER re-renders or re-parses. `procedure-trace` live spec untouched; R3.1 gets its first headless pin via the R2.1 test.

## Probe Evidence (locked, this phase)

1. **Byte-identity (extraction):** `TextoVista.textoEcuaciones` vs the current `mostrarSistema` builder — `IDENTICAL` on 3 fixtures (canonical 3×3 len 69; sign/zero row len 26; mixed magnitudes len 92). `formatear` vs current private copy — `IDENTICAL` on a 12-value edge set.
2. **`formatear` ACTUAL edge outputs:** `0.0→"0"`, `-0.0→"0"`, `NaN→"NaN"`, `+∞→"Infinity"`, `−∞→"-Infinity"`, `1e-7→"0.0000001"`, `0.5→"0.5"`, `3e11→"300000000000"`, `100.0→"100"`, `2.50→"2.5"`, `1e15→"1000000000000000"`, `1e-5→"0.00001"`. Note: the sibling `ValidadorParametros.formatearNumero` renders `1e-7→"1E-7"` (scientific) — a DIFFERENT contract; the view helper MUST NOT be merged into it (pinned by test T7).
3. **Toggle semantics (live JavaFX 21 probe on Xephyr :99):** (a) programmatic `setSelected(true)` that CHANGES selection fires `ToggleGroup.selectedToggleProperty` listeners exactly once; (b) redundant `setSelected(true)` (already selected) fires NOTHING; (c) ToggleGroup forces the previous button off (`true→false` observed); (d) user re-click of the selected button does not deselect. API is `selectedToggleProperty()` — there is NO `selectedValueProperty()` (compile error captured; that method is on `ListView`/choices).
4. **Legacy format reference (unchanged code):** `areaMatrizA` canonical = `"[ 4  1  1  ]\n..."`, `areaVectorB` = `"[ 6 ]\n..."` — locked only to document what the hidden boxes keep receiving; `mostrarFormaMatricial` is not touched at all.

## Architecture Decisions

| # | Fork | Options | Decision | Rationale |
|---|------|---------|----------|-----------|
| D1 | ViewMode representation | nested private enum in `SistemaFXController` / public nested in `SistemaController` / top-level type | **`private enum VistaModo { ECUACIONES, MATRICIAL }` nested in `SistemaFXController`, field `vistaModo` default `ECUACIONES`** | Code evidence: nested public enums exist (`Estado:163`, `ParametroUi:175`) but they live in `SistemaController` **because the FX controller imports them and headless tests pin them**. `VistaModo` has zero external consumers — the spec itself maps R1 to manual rows only. Math controller MUST NOT learn about view zones (layering); top-level type is YAGNI. Public→private: no cross-class use. |
| D2 | Selector mechanism | pure `zonasVisiblesPara(VistaModo)→EnumSet<ZonaVista>` (parametrosVisiblesPara shape) / single-boolean `aplicarVista` | **`aplicarVista(VistaModo)` with ONE equality + structural negation** | The precedent predicate exists because 9 métodos × 3 parámetros is a non-trivial subset policy WITH headless tests (`ParametrosTest:268-283`). Mode→zone is a 2-value bijection; an EnumSet + second enum would decide nothing, be untestable under the 15-class plan, and either pollute the math controller or force headless tests to class-load the FX controller (fragile). The predicate SPIRIT survives: listener decides nothing — mapping lives in one pure-expression line, mutual exclusion is structural (`!verEcuaciones`). |
| D3 | Zone strategy | present-hidden legacy boxes + new augmented box, render-always | **Locked by proposal** — third renderer `mostrarMatrizAumentada()` called unconditionally at both load sites; flip touches `visible`/`managed` only | Keeps `fx:id` parity, R5.1 paths and hidden-but-current data (R4.1). |
| D4 | Augmented line template | brackets+ASCII `'\|'` / bars-only U+2502 / no brackets | **`"[ <A cells> \| <b cell> ]"`, ASCII `'\|'` (U+007C), cells RIGHT-aligned per column (left-pad), single space between columns, one `" \| "` before b, trailing `" ]"`, `\n` per row** | Trace renderer `mostrarTraza:742-753` already emits `"[ … \| v ]"` ASCII — consistency with the app's own augmented vocabulary and the user-locked `"[A|b]"`. ASCII bar is font-agnostic (Notepad/console/TextArea safe); U+2502 risks missing glyphs in non-Monospaced fallbacks. Right-align puts ones/column and decimal points at stable offsets (R3.1 requires equal offsets; left-align would scatter decimal points). |
| D5 | FXML group wiring | `fx:define` + `$grupoVista` / group nested in first button + `toggleGroup="$grupoVista"` on second / programmatic wiring in `initialize` | **Nested `<toggleGroup><ToggleGroup fx:id="grupoVista"/></toggleGroup>` inside `botonVistaEcuaciones`; second button `toggleGroup="$grupoVista"`; `selected="true"` on Ecuaciones** | Canonical FXML idiom; ids register during parse of the first subtree, so the later `$`-ref resolves. `selected="true"` is applied pre-`initialize()` → initial visibility must come from FXML defaults (`zonaMatricial visible=false managed=false`, mirroring `filaParametros:70`), not from the listener. |
| D6 | `limpiarCampos` reset | rely on listener firing / explicit visibility call / both | **Both: `vistaModo = ECUACIONES; botonVistaEcuaciones.setSelected(true); aplicarVista(vistaModo);`** | Probe 3b: redundant select is SILENT, so listener-only would leave the from-ECUACIONES path without a visibility re-assert (already-correct, but the double write is idempotent and makes correctness independent of listener-fire semantics); probe 3a: when coming from MATRICIAL the listener fires → an extra `aplicarVista` runs → harmless. Single visibility writer remains `aplicarVista`. |
| D7 | CSS | new `.selector-*` / reuse | **None new.** Plain `ToggleButton` defaults; `estilos.css` has no `.toggle-button` rule to fight; `.boton-principal` deliberately NOT applied (selector is not an action) | Proposal: styles out of scope unless strictly needed — not needed; selected-state highlight is native modena. |

## Locked Literals (actual probe output — copy verbatim into tests)

R2.1 canonical `textoEcuaciones` (expected, `'·'` = U+00B7, file is UTF-8):

```java
"4·x1 + 1·x2 + 1·x3 = 6\n1·x1 + 3·x2 + 1·x3 = 5\n1·x1 + 1·x2 + 3·x3 = 5\n"
```

R2.2 signs/zero (`A=[[-2,0,1.5]]`, `b=[4]`): `"-2·x1 + 0·x2 + 1.5·x3 = 4\n"`

R3.2 canonical augmented (`A=[[4,1,1],[1,3,1],[1,1,3]]`, `b=[6,5,5]`) — 3 lines, each length 13, one ASCII `|` per line:

```java
"[ 4 1 1 | 6 ]\n[ 1 3 1 | 5 ]\n[ 1 1 3 | 5 ]\n"
```

R3.1 ragged column (`A=[[1],[3e11],[0.5]]`, `b=[3,4,5]`; formatted cell widths 1, 12, 3; column width 12):

```java
"[            1 | 3 ]\n[ 300000000000 | 4 ]\n[          0.5 | 5 ]\n"
```

R3.1b mixed magnitudes/negatives (`A=[[-2,0,1.5],[1,3e11,0.5],[7,-3,12.25]]`, `b=[4,3,-0.5]`) — same test, second fixture:

```java
"[ -2            0   1.5 |    4 ]\n[  1 300000000000   0.5 |    3 ]\n[  7           -3 12.25 | -0.5 ]\n"
```

R3.3 NaN cell (`A=[[1,NaN],[3,4]]`, `b=[5,6]`) — `"NaN"` width 3, no exception:

```java
"[ 1 NaN | 5 ]\n[ 3   4 | 6 ]\n"
```

R3.4 single column (`A=[[2],[3]]`, `b=[1,6]`):

```java
"[ 2 | 1 ]\n[ 3 | 6 ]\n"
```

## Interfaces / Contracts

New `src/unsa/sistemas/utils/TextosVista.java` (final draft lives at `/tmp/opencode/vistaprobe/src/unsa/sistemas/utils/TextosVista.java`; Spanish javadoc per `ValidadorParametros` precedent):

```java
public final class TextosVista {                    // no instances
    public static String formatear(double valor);                 // :526-543 body verbatim
    public static String textoEcuaciones(double[][] a, double[] b); // mostrarSistema:459-482 verbatim (matrizA→a, vectorB→b)
    public static String textoMatrizAumentada(double[][] a, double[] b); // new; rectangular precondition (parser guarantees)
}
```

Controller surface additions (C2):

```java
private enum VistaModo { ECUACIONES, MATRICIAL }
private VistaModo vistaModo = VistaModo.ECUACIONES;

private void aplicarVista(VistaModo modo) {          // sole visibility writer for zones
    boolean verEcuaciones = modo == VistaModo.ECUACIONES;
    zonaEcuaciones.setVisible(verEcuaciones);  zonaEcuaciones.setManaged(verEcuaciones);
    zonaMatricial.setVisible(!verEcuaciones);  zonaMatricial.setManaged(!verEcuaciones);
}

private void mostrarMatrizAumentada() {
    areaMatrizAumentada.setText(TextosVista.textoMatrizAumentada(matrizA, vectorB));
}
```

Listener (in `initialize()`, after the `comboMetodo` listener — note the property name from probe 3):

```java
grupoVista.selectedToggleProperty().addListener((observable, anterior, nuevo) -> {
    vistaModo = (nuevo == botonVistaMatricial) ? VistaModo.MATRICIAL : VistaModo.ECUACIONES;
    aplicarVista(vistaModo);                          // null -> ECUACIONES: safe default
});
```

## Data Flow

```
click selector ─→ grupoVista.selectedToggleProperty ─→ listener ─→ vistaModo + aplicarVista
                                       (visible/managed ONLY — no setText, no parse)
load (file/manual) ─→ interpretarArchivo ─→ commit matrizA/vectorB ─→ mostrarSistema()
                     ─→ mostrarFormaMatricial() ─→ mostrarMatrizAumentada()   [all unconditional]
                     ─→ result/procedure clear (R5.1) ─→ labelEstado        [mode untouched]
Limpiar ─→ clear 5 view areas ─→ vistaModo=ECUACIONES + setSelected(Ecuaciones)
          [+ listener fires iff mode was MATRICIAL; explicit aplicarVista covers the silent case]
```

## FXML Diff Plan (line-anchored, current `Sistema.fxml` numbering)

| Anchor | Action |
|--------|--------|
| after `:8` (`<?import TextField?>`) | +`<?import javafx.scene.control.ToggleButton?>` +`<?import javafx.scene.control.ToggleGroup?>` |
| after `:44` (close of load-buttons HBox), before `:46` | +selector HBox: `<HBox alignment="CENTER_LEFT" spacing="10">` with `botonVistaEcuaciones` (text `"Ecuaciones"`, `selected="true"`, nested `<ToggleGroup fx:id="grupoVista"/>`) and `botonVistaMatricial` (text `"Matricial"`, `toggleGroup="$grupoVista"`) |
| `:46-48` | wrap as-is into `<VBox fx:id="zonaEcuaciones" spacing="10" VBox.vgrow="ALWAYS">` — the grow flag moves the `areaSistema:48` growth into the chain (new attribute on a NEW node; existing `VBox.vgrow` on `:48` untouched) |
| `:50-62` | wrap as-is into `<VBox fx:id="zonaMatricial" spacing="10" visible="false" managed="false" VBox.vgrow="ALWAYS">`; append NEW `<TextArea fx:id="areaMatrizAumentada" editable="false" prefRowCount="6" promptText="Matriz aumentada..." wrapText="false" VBox.vgrow="ALWAYS"/>` after `:62`. `wrapText="false"` is part of the alignment contract (wrapping would break locked offsets) |
| everything else | untouched (`filaParametros:70-83`, combos, actions) |

## Controller Wiring Map (file changes only; exact lines emerge post-edit)

| Site | Change | Commit |
|------|--------|--------|
| imports `:8`-ish | `+import unsa.sistemas.utils.TextosVista;`, `−import java.math.BigDecimal;` (becomes unused; only `formatear` used it — verified by grep: consumers are the 9 private-method call sites `:479,481,501,513,681,698,750,769,778`) | C1 |
| `mostrarSistema :457-484` | body → `areaSistema.setText(TextosVista.textoEcuaciones(matrizA, vectorB));` | C1 |
| `formatear :526-543` | body → `return TextosVista.formatear(valor);` (keeps all 9 call sites byte-unchanged; SORTest/trace outputs unaffected) | C1 |
| fields after `:48` | `+@FXML VBox zonaEcuaciones; +@FXML VBox zonaMatricial; +@FXML TextArea areaMatrizAumentada;` and after `:60` `+@FXML ToggleGroup grupoVista; +@FXML ToggleButton botonVistaEcuaciones; +@FXML ToggleButton botonVistaMatricial;` + imports `javafx.scene.control.ToggleButton/ToggleGroup` | C2 |
| after `:85` | `VistaModo` enum + `vistaModo` field | C2 |
| end of `initialize()` (`:150`) | selector listener (above) | C2 |
| after `aplicarVisibilidadParametros :169` | `aplicarVista` + `mostrarMatrizAumentada` | C2 |
| `:233` / `:331` | `+ mostrarMatrizAumentada();` after `mostrarFormaMatricial();` — loads NEVER touch mode/visibility | C2 |
| `limpiarCampos :801` | `+ areaMatrizAumentada.clear();` (all-5 clear targets) | C2 |
| `limpiarCampos :817`-ish | D6 trio after the params reset | C2 |

## Testing Strategy — FINAL LOCK

New flat test class `test/TextosVistaTest.java` (no package, JUnit 5, Spanish test-method names, byte-exact `assertEquals` per `ValidadorParametrosTest` house style). **7 tests → suite 106→113, classes 14→15** (spec delegated the count to design; proposal "~110–112" was an estimate; launch-note "16 classes" rejected — mode/visibility is manual-only per spec R1/R4/R6 mapping, so no second test class exists).

| # | Test (name sketch) | Scenario | Fixture → locked literal above |
|---|--------------------|----------|-------------------------------|
| T1 | `textoEcuaciones3x3EsIdenticoAlRenderActual` | R2.1 / pins trace-spec R3.1 (x1→x3 order) | canonical block |
| T2 | `textoEcuacionesMuestraSignosYceros` | R2.2 | sign/zero row |
| T3 | `textoMatrizAumentadaAlineaColumnasDesiguales` | R3.1 | ragged block + mixed block (2 asserts) |
| T4 | `textoMatrizAumentada3x3TieneLineasIgualesConBarra` | R3.2 | canonical block + length/`split("\n")` + one-`|` per line asserts |
| T5 | `textoMatrizAumentadaCelulaNaNUsaNan` | R3.3 | NaN block |
| T6 | `textoMatrizAumentadaUnaColumna` | R3.4 | single-column block |
| T7 | `formatearNuncaUsaNotacionCientifica` | proposal edge-set bullet | probe-4 table (incl. `1e-7→"0.0000001"` contrast vs `formatearNumero` `"1E-7"`) |

**RED-first (strict_tdd=true):** write `TextosVistaTest.java` first and run the suite — compile-RED on missing class `unsa.sistemas.utils.TextosVista` (C1 precedent on this branch: tests precede the class within the same commit story); then add `TextosVista` + delegations → GREEN 113/15. No JavaFX import anywhere in tests; T1–T7 are pure.

**Manual rows — `openspec/changes/vista-sistema-matriz/CHECKLIST-GUI.txt` (C3 format), 1:1 with spec R6 (1)–(6):** (1) segmented look, exactly one button highlighted, start = Ecuaciones, matricial zone hidden without gap (`managed=false`); (2) one click flips zones, exactly one visible, content = last load; (3) switching re-renders nothing, status bar unchanged, hidden-but-current proof: switch back and forth after one load, both boxes always hold the same fresh data (R4.1/R4.2); (4) `filaParametros` appears/disappears with method choice identically in both modes (R7.2); (5) Limpiar from MATRICIAL: all five areas empty (re-toggle proves hidden emptiness) and selector shows Ecuaciones (R5.1); (6) file + manual loads while in MATRICIAL keep the mode and refresh the augmented box, result/procedure panels cleared (R1.2/R7.3).

## Commit Plan (work-unit, each <400 changed lines)

| Commit | Message (repo style: Spanish, conventional, no scope) | Content | Est. size |
|--------|--------------------------------------------------------|---------|-----------|
| C1 | `refactor: extraer los textos de vista a una clase pura` | `TextosVista` + `TextosVistaTest` (7 tests, RED first) + `mostrarSistema`/`formatear` delegations + import swap. **Zero FXML diff; zero UI-visible change** — byte-pins prove identity | ~285 lines |
| C2 | `feat: alternar entre vista de ecuaciones y matricial` | FXML (selector+wrappers+augmented area) + fields/listener/`aplicarVista`/`mostrarMatrizAumentada`/load-site calls/`limpiarCampos` trio + CHECKLIST-GUI.txt + README.md view section (Spanish, matches README voice) | ~140 lines |

## Verification Gates

- **C1:** `sh ./mvnw -o clean test` → `Tests run: 113, Failures: 0, Errors: 0`, 15 classes; `git diff` over `test/` shows ONLY the new file (existing 106 untouched → R7.1, SORTest/trace pins ride along green); `git diff -w` for the moved `formatear` shows the body unchanged except the signature (`private String`→`public static String`) — indentation-only normalization; `git diff` of `Sistema.fxml` empty.
- **C2:** 113/15 still green (no test touches C2 code); fx:id parity: each of `zonaEcuaciones zonaMatricial areaMatrizAumentada grupoVista botonVistaEcuaciones botonVistaMatricial` appears exactly once in FXML `fx:id=` and ≥1× as `@FXML` field in controller (grep loop in apply log); render-always invariant: grep `setText(` in controller → hits only inside `mostrar*`/`mostrarResultado`/`mostrarTraza`, and the three `mostrar…();` calls at both load sites are adjacent unconditional lines with no `if`/visibility around them (cite line numbers in verify-report); app boots under `sh ./mvnw -o javafx:run` (Xephyr/DISPLAY available at `/tmp/opencode/attest` precedent) before manual rows run.
- **End of change:** 6/6 checklist rows signed; `pom.xml`, `estilos.css`, `openspec/specs/procedure-trace/spec.md` zero-diff.

## Migration / Rollback

No migration. **C2 revert** → simultaneous stacked rendering returns; `TextosVista` extraction remains as harmless pure helpers. **C1 revert after C2 kept** is NOT valid order (C2 wiring calls `TextosVista`) — revert newest-first; each commit is individually bistable/testable at its gate.

## Risks

| Risk | Status | Mitigation |
|------|--------|------------|
| Extraction drifts R3.1 text | Neutralized by probe | T1/T2 byte-pins + re-runnable `/tmp/opencode/vistaprobe` harness at apply |
| Redundant programmatic `setSelected` silent (probe 3b) | Designed around | D6 explicit `aplicarVista` in `limpiarCampos`; semantics javadoc'd in Spanish at the trio |
| `wrapText` collapses alignment | Real | `wrapText="false"` locked in FXML plan; T3–T6 pin offsets |
| Vertical reflow / grow chain on wrapper | Med | `VBox.vgrow="ALWAYS"` on both wrappers + `areaMatrizAumentada`; manual rows 1–2 observe; `prefRowCount` of existing areas untouched |
| U+00B7 `·` mojibake in test literals | Low | pom `sourceEncoding=UTF-8`; probe compiled/ran the identical char; verify byte-pin green on first RED→GREEN run |
| Headless tests accidentally loading FX | Zero by construction | T1–T7 import only `TextosVista` |

## Open Questions

- None blocking. (Non-blocking, for the next archive re-pin task: `openspec/config.yaml:15,26` stale at "82/12" — post-change truth is **113 tests / 15 classes**.)
