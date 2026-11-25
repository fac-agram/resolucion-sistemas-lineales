# Verification Report — `vista-sistema-matriz`

**Change**: vista-sistema-matriz (capability `view-modes`, ADDED-only delta)
**Version**: specs at commit 5c90456 · branch `feature/tunable-parameters` (base `da55414`; C1 `619ae38`, C2 `5c90456`; `main` untouched at `59f5777`)
**Mode**: Strict TDD (config `strict_tdd: true`, runner `sh ./mvnw -o clean test`)
**Verifier**: adversarial re-execution — full suite, all mutation gates, byte-pin programmatic diff, and a new headful Xephyr attestation harness (real mouse/keyboard events via input robot, live scene-graph state introspection). Nothing was taken from the apply-progress claim (#128) at face value.

**Verdict: PASS WITH WARNINGS** — implementation matches specs, design and tasks; the warnings are about the accepted absence of automated coverage for the C2 UI surface and one overstatement in the apply-phase M-C1 disposition, both precisely characterized below.

---

## Completeness

| Metric | Value |
|--------|-------|
| Tasks total | 16 |
| Tasks complete | 12 (Phase 0–2, all gates re-verified green here) |
| Tasks incomplete | 4 (Phase 3: user-owned — checklist sign-off, merge decision C2-752, config re-pin, package v1.3) |

---

## Build & Tests Execution

**Build**: ✅ Passed — `sh ./mvnw -o clean test`

```text
[INFO] Tests run: 113, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**Classes**: 15 surefire report XMLs (`target/surefire-reports/TEST-*.xml`), incl. `TextosVistaTest` = 7 tests. Matches the FINAL-LOCK 113/15 claim exactly; the launch-note "16 classes" and proposal "~110–112" are reconciled by design.md as documented.

**Final re-gate after all mutations**: identical 113/0/0 / 15 classes; `git status` shows no tracked modifications (only the 3 pre-existing untracked `openspec/changes/*` dirs); `git diff` and `git diff --cached` both empty.

**Scoped pin run**: `sh ./mvnw -o surefire:test -Dtest=TextosVistaTest` → `Tests run: 7, Failures: 0`.

### Test-suite scope integrity (check 1)

- `git diff --name-only da55414 HEAD` = exactly: `README.md`, `src/unsa/sistemas/utils/TextosVista.java`, `src/unsa/sistemas/view/Sistema.fxml`, `src/unsa/sistemas/view/SistemaFXController.java`, `test/TextosVistaTest.java`.
- `git diff --name-only da55414 HEAD -- test/` = **only** `test/TextosVistaTest.java` (`--diff-filter=A` confirms it is the only ADDED file).
- Byte-untouched proof (blob-hash equality `da55414` vs `HEAD`, plus empty per-file diffs): `NormasTest.java` a4e6beca, `SORTest.java` 95cd17dd, `SistemaControllerTest.java` df2ac44c, `ParametrosTest.java` a3975448, `ValidadorParametrosTest.java` 26138e04, `RelajacionSimpleTest.java` 84685b57 — all SAME.

### C1 extraction integrity (check 2)

- `git show 619ae38 -- src/unsa/sistemas/view/Sistema.fxml` → **0 bytes** (no FXML touched at C1).
- `formatear`: da55414 body (lines 526–543) vs `TextosVista.formatear` (25–42): guards and `BigDecimal.valueOf(valor).stripTrailingZeros().toPlainString()` verbatim; only signature (`private String` → `public static String`) and indentation differ. Controller delegation at :568–571.
- BigDecimal: at da55414 the controller had exactly 2 hits — import `:14` and the sole consumer inside `formatear` (`:540`). At HEAD the controller has zero. `ValidadorParametros` keeps its own import (unaffected file). Sole-consumer proof ✅.
- `git diff -w da55414 619ae38 -- controller`: `mostrarSistema` builder removed, replaced by `areaSistema.setText(TextosVista.textoEcuaciones(matrizA, vectorB));`; the removed builder body matches `textoEcuaciones` token-for-token (`matrizA`→`a`, `vectorB`→`b` renames per design). The 9 original `formatear` callsites: 7 remain untouched in the controller (:543, :555, :709, :726, :778, :797, :806 — same sites as apply log, shifted by C2 insertions), 2 (:479/:481) moved into `TextosVista` with the builder, exactly as the wiring map prescribes.

---

## Byte-Pins Verification (check 3)

A Python comparator diffed the 7 locked literals **as raw bytes** across `design.md`, `tasks.md` and `test/TextosVistaTest.java`:

| Literal | design | tasks | test | Status |
|---------|--------|-------|------|--------|
| R2.1 canonical ecuaciones (`"4·x1 + 1·x2 + 1·x3 = 6\n…"` 69 chars) | ✅ | ✅ | ✅ | VERBATIM |
| R2.2 sign/zero (`"-2·x1 + 0·x2 + 1.5·x3 = 4\n"`, 26 chars) | ✅ | ✅ | ✅ | VERBATIM |
| R3.2 canonical augmented (`"[ 4 1 1 | 6 ]\n…"`, 3×13 chars, one `\|`/line) | ✅ | ✅ | ✅ | VERBATIM |
| R3.1 ragged (widths 1/12/3, left-pad) | ✅ | ✅ | ✅ | VERBATIM |
| R3.1b mixed magnitudes/negatives | ✅ | ✅ | ✅ | VERBATIM |
| R3.3 NaN cell (`"[ 1 NaN | 5 ]\n[ 3   4 | 6 ]\n"`) | ✅ | ✅ | ✅ | VERBATIM |
| R3.4 single column (`"[ 2 \| 1 ]\n[ 3 \| 6 ]\n"`) | ✅ | ✅ | ✅ | VERBATIM |

Additional byte evidence: resolved R2.1 semantic length = **69** (rows 22/22/22); `·` = U+00B7 present as `0xC2 0xB7` 13× in the test file (no lone `0xB7` — no mojibake); `grep -c 300000000000 test/TextosVistaTest.java` = 3 (≥2); divergence pin present: `formatear(1e-7)→"0.0000001"` **and** `ValidadorParametros.formatearNumero(1e-7)→"1E-7"` (T7 locks the two contracts apart); test imports only `TextosVista` + JUnit (zero `import javafx`); impl imports only `java.math.BigDecimal`. All 7 tests green scoped and in-suite.

## R3.1 First-Pin Significance (check 4)

Live spec `openspec/specs/procedure-trace/spec.md` R3 (:64–74) requires coefficients labeled `·x1 ·x2 ·x3` matching column order left-to-right (equation half) and solution list `x1, x2, x3` (solution half). The T1 pin fixture IS the canonical spec-style 3×3 (`A=[[4,1,1],[1,3,1],[1,1,3]], b=[6,5,5]` — the `cargarManual` example) and its locked string encodes column→label order exactly (`4·x1`, `1·x2`, `1·x3` per row 1). Sensitivity is not theoretical: **mutation M-A (coefficient order swapped) reddened exactly T1+T2**. The solution half is unchanged code (`agregarSolucion` labels `x(i+1)` by index; blob untouched since da55414) and its data path was already `SistemaControllerTest`-covered. **This test is the first-ever headless gate on R3.1's equation string; archive will rely on T1 as the automated R3.1 sentinel.** Live attestation also re-observed the rendered block in the UI matching the same 69-char content.

---

## Mutation Results (checks 5 & 6)

Every mutation was applied by me, run against the FULL suite, and (for C2 mutants) also executed live under Xephyr; all reverted with `git checkout -- <file>` (verified `git status --porcelain src/` empty after each; final re-gate 113/0/0).

| # | Mutation | Headless result | Live result | Disposition |
|---|----------|-----------------|-------------|-------------|
| M-A | `a[i][j]` → `a[i][len-1-j]` in `textoEcuaciones` | **CAUGHT**: `Tests run: 113, Failures: 2` (only `textoEcuaciones3x3EsIdenticoAlRenderActual` + `textoEcuacionesMuestraSignosYceros`, byte-pin messages shown), BUILD FAILURE | n/a (pure) | ✅ Red-test attribution matches apply-phase claim exactly (T1/T2) |
| M-B1 | bar `" \| "` → `"|| "` in `textoMatrizAumentada` | **CAUGHT**: 4 failures — T3, T4 (literal + one-bar-per-line loop assert), T5, T6, 113/F2 FAILURE | n/a | ✅ matches claim |
| M-B2 | pad side flipped (left→right, A cells and b) | **CAUGHT**: 2 failures — T3 (ragged), T5 (NaN col); T4/T6 correctly insensitive (all columns width 1 → zero pad) | n/a | ✅ 113/F2 FAILURE |
| M-C1-full | remove the whole D6 trio from `limpiarCampos` | **GAP RECONFIRMED**: 113/0/0 BUILD SUCCESS | **OBSERVABLE**: real-click Matricial → real-click Limpiar → "Matricial" stayed selected, MATRICIAL zone stayed visible, `vistaModo` stayed `MATRICIAL` → **checklist row (5) BITES** (+ row (2)/(6) expectations visibly violated) | ✅ caught live |
| M-C1-minimal | remove ONLY the explicit `aplicarVista(vistaModo);` call (the apply-phase M-C1) | **GAP RECONFIRMED**: 113/0/0 | **BEHAVIOR IDENTICAL**: Limpiar-from-MATRICIAL still resets correctly — `setSelected(true)` is a real selection change → listener fires → calls the surviving `aplicarVista`. Limpiar-from-ECUACIONES was already visible-correct | ⚠️ NO row can catch it, because it violates no observable requirement. See Finding W1 |
| M-C2 | listener mapping inverted (`botonVistaMatricial → ECUACIONES`, else `MATRICIAL`) | **GAP RECONFIRMED**: 113/0/0 BUILD SUCCESS | **OBSERVABLE ×2**: click "Matricial" → button highlights, zones DO NOT flip (ecuaciones stays) → **row (2) BITES**; click "Ecuaciones" → Ecuaciones highlighted but MATRICIAL zone displayed (`vistaModo=MATRICIAL`) → **row (3) BITES** (highlight/content mismatch + status parity broken); row (5) does **not** bite (the surviving explicit `aplicarVista(ECUACIONES)` in the trio fixes zones at Limpiar despite the broken listener) | ✅ caught live; screenshots `mcb-01/02` |

**M-C accepted-gaps final disposition (check 6c)**: the headless gap for ALL C2 UI code is real and remains (design chose manual-only mapping; no TestFX/Monocle in the toolset). Of the two accepted mutants: **inverted mapping is demonstrably caught by checklist rows (2) and (3)** (row (5) provably does not catch it — D6 redundancy masks it on the limpiar path); **the trio removal is caught by row (5)** for the observable variant (vistaModo/setSelected removed), while the minimal explicit-call-only removal is a behavior-preserving defensive line that **no test and no checklist row can catch, because it produces no violation** — the apply-phase line "covered by CHECKLIST row 5" is therefore accurate for observable mutations and *overstated* for that minimal variant (Finding W1).

---

## GUI Runtime Attestation (check 9)

Harness (new in verify phase): `vsm.Driver` — boots the **real** `Sistema.fxml`/`SistemaFXController` exactly like `MainApp` (`MainApp.class.getResource("Sistema.fxml")`), drives **real mouse/keyboard events** through the input robot on Xephyr `:97` (1280x800), reads live scene-graph state (fx:id fields via reflection: `vistaModo`, `isSelected`, `isVisible/isManaged`, TextArea contents), captures full-screen PNGs per row. Direct-launch boot (`java --module-path <openjfx 21.0.13 linux jars> -cp target/classes …`) because `mvn -o javafx:run` remains unresolvable offline (plugin deps never cached) — **same accepted infra deviation as apply**; the app itself booted from the pristine `target/classes` produced by the final clean-test re-gate.

| Check-9 row | Driver verdict | Evidence |
|---|---|---|
| boot → ECUACIONES default | ✅ PASS | `row1-default`: mode=ECUACIONES, Ecuaciones selected, Matricial unselected, zonaMatricial (visible=false, managed=false) — hidden **without gap**, visible in shot; areas empty; "Listo para cargar un sistema." — `baseline-00-default.png` |
| SOR params row (C3 interplay) | ✅ PASS | `row4b-sor-params-in-matricial`: real popup clicks selected Iterativo→SOR **while in MATRICIAL**; `filaParametros` (true,true), `celdaOmega` visible — `baseline-03-sor-matricial.png`; parity in ECUACIONES: `row4a` Gauss-Jordan hides row, SOR re-shows it — `baseline-05-paridad-fila-parametros.png` |
| switch Matricial → augmented box `[ 4 1 1 \| 6 ]` after manual canonical load | ✅ PASS | `row2-flip`: one click flips exactly the zones; content byte-equals `"[ 4 1 1 | 6 ]\n[ 1 3 1 | 5 ]\n[ 1 1 3 | 5 ]\n"`; status bar text unchanged — `baseline-02-matricial.png`; and `row3a` proves the augmented box was ALREADY current **while hidden at load time** (R4.2) — `baseline-01-cargada-ecuaciones.png` |
| hidden legacy boxes hold data (switch back, no blank) | ✅ PASS | `row3b-flip-back-hidden-current`: areaSistema byte-equals the 69-char canonical block without any re-render — `baseline-04-ecuaciones-params.png`; strongest variant: after the FILE load in MATRICIAL, flipping to ECUACIONES instantly shows the new system that had never been displayed while hidden (`fileonly-05-file-ecuaciones.png`) |
| Limpiar → Ecuaciones + all cleared + selector state | ✅ PASS | `row5-limpiar`: all five view areas + resultado empty, `filaParametros` hidden, Ecuaciones selected+shown; **re-toggle to Matricial proves hidden emptiness** (no stale) — `baseline-06-limpiar.png` |
| load file2 while in MATRICIAL → mode kept (no flip) | ✅ PASS | `row6b` (via `fileonly` scenario): loaded "prueba iterativo (converge).txt" (A=[[10,-1,2],[-1,11,-1],[2,-1,10]], b=[6,22,-10]) through the **native GTK FileChooser with real mouse events**; mode stayed MATRICIAL; aug box byte-equals `"[ 10 -1  2 |   6 ]\n[ -1 11 -1 |  22 ]\n[  2 -1 10 | -10 ]\n"` (per-column right alignment incl. width-3 b column — live proof of the R3 alignment contract on ragged real data, `fileonly-04-file-matricial.png`); hidden areaSistema holds the matching ecuaciones block; resultado empty + procedure pane disabled (R5.1 in MATRICIAL); labelArchivo shows the filename; "Sistema cargado correctamente: 3 incógnitas." |
| manual load while in MATRICIAL → mode kept | ✅ PASS | `row6a-manual-load-keeps-matricial`: dialog driven by real clicks + clipboard paste; mode stayed MATRICIAL, aug refreshed, panels cleared — `baseline-07-manual-matricial.png` |

Attestation honesty notes: (a) the file-load first attempts by keyboard (GTK location popup pre-filled with the last folder; keystrokes after the first one lost — focus flakiness under Xephyr **without a window manager**) did not land; the row was re-driven successfully with pointer-only events. The loaded file content is byte-identical (md5 `3d4c2ec6…`) to the repo's fixture, loaded via GTK Recientes — same bytes, honest substitution, logged in `fileonly` output. (b) All combo selections were **real popup clicks** (logged "real popup click"); the single "PROGRAMMATIC fallback" occurrence in the first baseline pass was superseded by the clean final pass. (c) All processes killed afterwards (app, driver, Xephyr — verified no strays). No row is UNVERIFIED.

---

## Render-Always Audit (check 7)

All `setText`/`clear` targets in the controller with nesting status:

| Site | Target | Nesting |
|---|---|---|
| :232 (`mostrarMatrizAumentada`) | `areaMatrizAumentada.setText` | method body is the single statement; no condition |
| :525 (`mostrarSistema`) | `areaSistema.setText` | single statement; no condition |
| :560/:561 (`mostrarFormaMatricial`) | `areaMatrizA`/`areaVectorB.setText` | builder loops are data loops; no visibility condition |
| :685/:692 (`mostrarResultado`) | `areaResultado`/`areaProcedimiento.setText` | :692 is inside `if (traza != null && !traza.estaVacia())` — a **data-existence** guard (pre-existing code, unchanged by this change), not a visibility gate |
| load sites :296–298 (file) and :395–397 (manual) | three adjacent unconditional `mostrar…();` calls | inside the parse-success path only; no `if`/visibility around them (sed context captured above) |
| :827–:832 (`limpiarCampos`) | clears all five view areas + resultado/procedimiento | unconditional |
| Label/field writes (:294/:305/:392/:404/:419/:602/:608/:616/:635/:697/:825/:843–845/:857/:876) | status/file labels, param defaults | not view-zone targets; pre-existing pattern |

Visibility writes exist ONLY at :206–210 (`aplicarVisibilidadParametros`) and :223–227 (`aplicarVista`) — neither method contains any `setText`/`clear`. `aplicarVista` is the sole zone-visibility writer. **Invariant holds in code and was confirmed behaviorally live** (hidden targets always current; switching never re-rendered — status text byte-stable across flips in `row2-flip`).

## fx:id Parity & FXML Copy (check 8)

Parity loop, each id exactly 1× `fx:id` in FXML and 1× `@FXML` field in controller: `zonaEcuaciones` ✅ `zonaMatricial` ✅ `areaMatrizAumentada` ✅ `grupoVista` ✅ `botonVistaEcuaciones` ✅ `botonVistaMatricial` ✅. ToggleButton texts exact: `text="Ecuaciones"` (:50, with `selected="true"` and nested `<ToggleGroup fx:id="grupoVista"/>`) / `text="Matricial"` (:55, `toggleGroup="$grupoVista"`). `zonaMatricial` FXML defaults `visible="false" managed="false"` (:64) — matches D5 (initial state must come from FXML, not the listener). `areaMatrizAumentada` has `wrapText="false"` (:78) per the alignment contract.

## Scope Hygiene / Honesty Sweep (check 10)

- `pom.xml`: **zero diff** (da55414→HEAD). `estilos.css`: **zero diff**. `openspec/specs/` (incl. procedure-trace): **zero diff** — R3.1 preserved with no delta, as claimed.
- No solver/model/controller-math changes: `git diff --name-only da55414 HEAD -- src/unsa/sistemas/model src/unsa/sistemas/controller` → empty. `ValidadorParametros` untouched (only *called* by the T7 divergence pin).
- README view section exists: new `## Vistas del sistema: ecuaciones y matriz` (+4 lines, Spanish user copy; describes selector, render-always, session lifetime, Limpiar reset, params-row independence).
- Branch: C1/C2 on `feature/tunable-parameters` only; `main` unchanged at `59f5777`; nothing pushed; no packages.
- Working tree after all verify mutations: clean (0 tracked diffs, 0 staged; untracked set identical to session start).

---

## TDD Compliance (strict mode)

| Check | Result | Details |
|-------|--------|---------|
| TDD Evidence reported | ✅ | Table present in apply-progress #128 |
| All tasks have tests | ✅ | T1–T7 exist in `test/TextosVistaTest.java` (7 `@Test`, count matches claim) |
| RED confirmed | ⚠️ (historical) | Compile-RED transcript (cannot find symbol `TextosVista`) is logged in #128 and consistent with the new-file-only test diff; historical RED cannot be re-executed — treated as reported, everything else was re-proved |
| GREEN confirmed | ✅ | All 7 pass in this run (scoped + full suite) |
| Triangulation adequate | ✅ | 7 distinct behaviors × varied expectations (2 ecuaciones fixtures, 2 augmented fixtures in T3, structural asserts in T4, NaN, single-col, 12-value edge table incl. cross-class divergence pin) |
| Safety Net for modified files | ✅ | All pre-existing 106 tests byte-untouched (hash proof) and green in mutant runs pre-restore |

**Test Layer Distribution**: Unit 7/1 file (pure, zero JavaFX imports — verified by import grep); Integration 0; E2E 0. Consistent with cached capabilities (`e2e: false`; no TestFX/Monocle) and with the spec's declared manual-row mapping for R1/R4/R5/R6.
**Changed-file coverage**: skipped — `testing.coverage.available: false` in config (not a failure).
**Quality metrics**: skipped — linter/type-checker `false` in config.
**Assertion Quality (Step 5f)**: ✅ All assertions verify real behavior — every `assertEquals` calls `TextosVista`/`ValidadorParametros` production code; no tautologies, no type-only asserts, no ghost loops (the T4 bar-count loop is guarded by preceding `assertEquals(3, lineas.length)` + length asserts, so an empty collection is impossible); no mocks at all; failure messages carry the locked-literal intent. Mutation evidence (M-A/M-B1/M-B2) proves the assertions bite.

---

## Spec Compliance Matrix

| Req | Scenario | Covering evidence | Result |
|-----|----------|-------------------|--------|
| R1 | R1.1 initial mode | live `row1-default` + FXML defaults (:50/:64) | ✅ COMPLIANT (live attestation; manual-mapped by spec) |
| R1 | R1.2 loads never change mode | live `row6a` (manual) + `row6b` (file) | ✅ COMPLIANT (live attestation) |
| R1 | R1.3 Limpiar resets mode | live `row5-limpiar`; M-C1-full proves the gate is real | ✅ COMPLIANT (live attestation) |
| R2 | R2.1 canonical byte-pin | `TextosVistaTest > textoEcuaciones3x3EsIdenticoAlRenderActual` | ✅ COMPLIANT |
| R2 | R2.2 sign/zero branches | `… > textoEcuacionesMuestraSignosYceros` | ✅ COMPLIANT |
| R3 | R3.1 ragged alignment | `… > textoMatrizAumentadaAlineaColumnasDesiguales` (2 fixtures) | ✅ COMPLIANT |
| R3 | R3.2 square 13-char lines, 1 bar | `… > textoMatrizAumentada3x3TieneLineasIgualesConBarra` | ✅ COMPLIANT |
| R3 | R3.3 NaN cell | `… > textoMatrizAumentadaCelulaNaNUsaNan` | ✅ COMPLIANT |
| R3 | R3.4 single column | `… > textoMatrizAumentadaUnaColumna` | ✅ COMPLIANT |
| R4 | R4.1 hidden targets current | live: byte-assert on hidden areas (`row3b`, `fileonly-05`); code audit (render-always) | ✅ COMPLIANT (live attestation) |
| R4 | R4.2 augmented fresh while hidden | live `row3a` (verified BEFORE any flip) + `row2-flip` no re-render | ✅ COMPLIANT (live attestation) |
| R5 | R5.1 Limpiar matrix | live `row5-limpiar` incl. re-toggle hidden emptiness | ✅ COMPLIANT (live attestation) |
| R6 | R6.1 six manual rows | rows (1)–(6) all attested by the live harness (equivalent driver rows all PASS, screenshots above); formal user sign-off of CHECKLIST-GUI.txt = task 3.1, still open | ⚠️ PARTIAL — attestation passed, signatures pending (by design, user-owned) |
| R7 | R7.1 R3.1 pin is first gate | suite 113/0 with T1 as TextosVistaTest's first test; M-A reddens suite on drift | ✅ COMPLIANT |
| R7 | R7.2 parameter-row parity | 16 ParametrosTest green untouched (hash) + live `row4a/row4b` both modes | ✅ COMPLIANT |
| R7 | R7.3 R5.1 holds in MATRICIAL | live `row6a/row6b` (panels cleared + hidden targets non-stale while MATRICIAL) | ✅ COMPLIANT (live attestation) |

**Compliance summary**: 15/16 scenarios compliant; 1 PARTIAL only in the sense that R6.1's *user signatures* on the manual checklist remain (task 3.1) — the checklist content itself passed 6/6 under the verify-phase headful attestation.

## Coherence (Design Decisions)

| Decision | Followed? | Notes |
|----------|-----------|-------|
| D1 private nested `VistaModo`, default ECUACIONES | ✅ | controller :110–:117 |
| D2 single-boolean `aplicarVista`, equality + structural negation | ✅ | :219–:228; sole visibility writer; listener decides nothing (:189–:192 matches design block) |
| D3 present-hidden legacy boxes + third renderer unconditional | ✅ | :230–:233; called at both load sites (:298/:397) |
| D4 line template / ASCII bar / right-align | ✅ | byte-pinned + live-observed on real ragged data |
| D5 nested ToggleGroup idiom, FXML-owned initial state | ✅ | Sistema.fxml :50–:55, :64 |
| D6 Limpiar trio (state + selected + explicit visibility) | ✅ | :848–:855 with Spanish javadoc pinning the silent-setSelected probe fact; mutation analysis shows both redundant paths (M-C1 rows above) |
| D7 no new CSS | ✅ | estilos.css zero diff; plain ToggleButtons |

## Issues Found

**CRITICAL**: None.

**WARNING**:
- **W1 — M-C1-minimal disposition overstated in apply evidence**: removing only the explicit `aplicarVista(vistaModo);` from the Limpiar trio is green headless **and** behaviorally identical live (the listener still fires via the real selection change). "covered by CHECKLIST row 5" is true for the observable trio mutations but **no** row (or any future black-box test) can catch the minimal variant — it is defense-in-depth, not behavior. Recommend the archive/user checklist wording not sell row (5) as guarding the redundant call itself.
- **W2 — zero automated coverage remains for the entire C2 UI surface** (R1/R4/R5/R6 map to manual rows by spec). This verify phase closed most of that gap with reproducible headful attestation (driver + screenshots + byte assertions on live state), but the attestation harness lives in `/tmp` and is not part of the repo/CI. Long-term: a Monocle/TestFX smoke in a future change would make rows (2)/(3)/(5) machine-enforceable (out of scope here; pom must stay untouched).
- **W3 — boot-gate infra deviation persists**: `sh ./mvnw -o javafx:run` still cannot resolve its plugin deps offline (verified this session). Attestation used the documented direct `java --module-path` launch (identical app/scene). Not a code issue; the config's verify command remains valid.
- **W4 — WM-less Xephyr keyboard focus is unreliable for the native GTK FileChooser**; file-load attestation succeeded via pointer-only driving of the Recientes list. Loaded bytes were md5-identical to the repo fixture (logged substitution). A future attestation should keep the pointer-driven chooser flow.

**SUGGESTION**:
- **S1**: Archive re-pin must update `openspec/config.yaml` expectations (stale 82/12 note; truth now **113/15**) — already tracked as task 3.3.
- **S2**: Keep `/tmp/opencode/verify-vsm/` until task 3.1 sign-off: the driver reproduces every checklist row mechanically; the user signing the rows can watch the same actions instead of trusting memory.
- **S3**: CHECKLIST-GUI.txt remains intentionally untracked until archive (tasks 0.2/2.5 govern) — archive should add it with the change folder.

## Verdict

**PASS WITH WARNINGS** — every spec-locked behavior was re-executed and observed at runtime: 113/0/0 across 15 classes with all byte-pins verbatim-locked and mutation-sensitive; C1 extraction proven byte-identical with FXML untouched; C2 selector, mode lifetime, render-always, full-reset and R5.1-in-MATRICIAL attestation passed live 6/6 checklist rows including a real file load; scope hygiene exact (pom/estilos/specs/solvers zero-diff). Warnings characterize the honest residual gap (no automated coverage for UI code; the minimal D6 defensive line is uncatchable by design) — none breaks a requirement.

**Next**: user task 3.1 (sign CHECKLIST rows — the verify attestation above supports each), then 3.2 merge decision, `sdd-archive` with the 113/15 re-pin.

---

### Evidence Artifacts

- Driver + logs + 26 PNGs: `/tmp/opencode/verify-vsm/` (`vsm/Driver.java`, `baseline-final.log`, `baseline-0*.png`, `fileonly-0*.png`, `mca-0*.png`, `mcb-0*.png`, `pincheck.py`)
- Engram: `sdd/vista-sistema-matriz/verify-report` (this file mirrored, topic-keyed upsert)
- Repro: boot `DISPLAY=:97 java --module-path <openjfx-21.0.13-linux jars from ~/.m2> --add-modules javafx.controls,javafx.fxml -Djava.awt.headless=false -cp target/classes:/tmp/opencode/verify-vsm/classes vsm.Driver baseline|fileonly|mca|mcb` after `sh ./mvnw -o clean test` (fresh `target/classes`) and Xephyr `:97` at 1280x800.
