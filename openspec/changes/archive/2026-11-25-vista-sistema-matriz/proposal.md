# Proposal: Vista Sistema ↔ Matricial (mode toggle)

- Change: `vista-sistema-matriz` · Artifact store: `hybrid` (this file + Engram `sdd/vista-sistema-matriz/proposal`)
- Baseline: `feature/tunable-parameters` @ `da55414` (106 tests / 14 classes green). Stacks on the same branch over C3 `parametros-ajustables` (user single-front decision).
- Change class: **UI presentation only** — not build/tooling, not algorithmic. Data path stays byte-identical.

## Intent

Users must alternate between the equation-system view and the matrix view; today both render simultaneously, stacked. This is a visibility concern only: `matrizA`/`vectorB` remain the single source of truth (verified read-only in both renderers), so no re-parse or data-model work is needed.

## User Locked Decisions (verbatim)

1. Selector = SEGMENTED ToggleButtons in ToggleGroup ("Ecuaciones" / "Matricial"); ComboBox rejected; ControlsFX/ToggleSwitch unavailable (no dep — pom untouched).
2. Matrix view = SINGLE box: augmented `[A|b]` in one TextArea, aligned columns (fixed-width per column, bar separator) — new pure helper `textoMatrizAumentada`; existing `areaMatrizA`/`areaVectorB` fx:ids REMAIN in FXML (controller parity, R5.1 stale-clear code paths keep their setText targets) but are hidden in Matricial mode (visibility-only, render-always preserved: hidden renderers keep running).
3. Mode persistence = per-session STATE preserved across file/manual loads (loading NEVER switches mode; only explicit "Limpiar" resets to Ecuaciones).
4. procedure-trace R3.1 byte-equivalence preserved by extraction (helper returns identical strings); new test #1 IS the headless pin (first ever for R3.1).

## Scope

### In Scope
- `unsa.sistemas.utils.TextosVista` (FX-free pure class): `formatear(double)`, `textoEcuaciones(A,b)`, `textoMatrizAumentada(A,b)`; controller's private `formatear:526` and `mostrarSistema:457` delegate to it (single source).
- FXML: selector `HBox` row (2 `ToggleButton`s in a `ToggleGroup`, "Ecuaciones" `selected=true`) above the zones; wrap `:46-48` → `zonaEcuaciones`, `:50-62` → `zonaMatricial` + new `areaMatrizAumentada` TextArea; legacy A/b areas stay present-hidden.
- Controller: mode state field (default `ECUACIONES`); listener applying `visible/managed` EXACTLY like `aplicarVisibilidadParametros:159-169`; load flows (`:232-233`, `:330-331`) call render-then-apply-visibility only — never change mode; `limpiarCampos:792` resets mode + selector selection (precedent: it already resets `filaParametros` at `:817`).
- Alignment algorithm (in helper): per column, pad to the max width of the FORMATTED cell strings (sign included), then one ` | ` separator before b — never CSS-dependent.
- Tests: 3–4 `TextosVistaTest` units — R3.1 byte-pin vs captured `mostrarSistema` output (first headless pin of spec `procedure-trace:64-74`), augmented alignment pins incl. ragged magnitudes/negatives, `formatear` edge set (0, NaN, Infinity, stripTrailingZeros); 1–2 controller-level per repo precedent (pure helpers + manual rows — no JavaFX harness). Suite 106 → ~110–112 / 15 classes.
- One new MANUAL GUI row appended to a `CHECKLIST-GUI.txt` in this change folder (C3 precedent): segmented visual toggle, both modes render the same fixtures, "Limpiar" reset, R5.1 verified while in Matricial mode.

### Out of Scope
- Data model / parser (`interpretarArchivo`), solver and `resolverSistema` numerics.
- R5.1 clear-logic content (only preserved; render-always rule).
- Manual dialog internals (`cargarManual:251-355`).
- `styles.css` beyond classes strictly needed — reusing existing patterns; new `.selector-*` is an optional design-phase decision.
- Packaging; the pending C2 merge decision for `parametros-ajustables`.

## Behavior Table

| Mode | Visible zone | Rendered always (every load) | Selector state |
|---|---|---|---|
| ECUACIONES (default) | "Sistema de ecuaciones" + `areaSistema` | `areaSistema`, `areaMatrizA`, `areaVectorB`, `areaMatrizAumentada` (setText runs even hidden) | "Ecuaciones" |
| MATRICIAL | "Forma matricial" + `areaMatrizAumentada` only (A/b present-hidden) | same four targets | "Matricial" |
| After load (file/manual) | unchanged by loading | all re-rendered, then R5.1 panel clear | unchanged |
| After "Limpiar" | ECUACIONES | all areas cleared, incl. hidden ones | reset to "Ecuaciones" |

## Capabilities

### New Capabilities
- `view-modes`: selector control, mode state lifetime (per-session; loads keep it, "Limpiar" resets), zone visibility rules, augmented `[A|b]` text format and alignment contract.

### Modified Capabilities
- None. `procedure-trace` is NOT modified — R3.1 preserved by construction (extraction returns byte-identical strings); live spec untouched.

## Approach

Reuse the mechanism C3 shipped on this branch: render always into all zones, flip `visible/managed` selectively. Extract presentation strings into a pure helper first (RED-first tests against captured current output), then add FXML + listener. Toggle changes visibility, never rendering.

## Affected Areas

| Area | Impact | Description |
|---|---|---|
| `src/unsa/sistemas/utils/TextosVista.java` | New | pure renderers + alignment |
| `src/unsa/sistemas/view/Sistema.fxml` | Modified | selector row, zone wrappers, `areaMatrizAumentada` |
| `src/unsa/sistemas/view/SistemaFXController.java` | Modified | mode field, listener, delegations, `limpiarCampos` |
| `test/TextosVistaTest.java` | New | byte-pins + alignment + format edges |
| `openspec/changes/vista-sistema-matriz/CHECKLIST-GUI.txt` | New | manual GUI rows |
| `pom.xml` | None | no new dependency |

## Risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| R3.1 text drift during extraction | Low | byte-pin test asserts captured current strings |
| fx:id parity break (hidden ≠ removed) → FXML load fails | Low | keep A/b areas + fields; parity grep check in apply |
| Alignment collapses under ragged magnitudes | Med | pad per column to max FORMATTED width (incl. sign); pinned by tests |
| "Limpiar" leaves stale hidden text | Low | clear all setText targets regardless of visibility |
| C3 interaction (selector vs `filaParametros`) | Low | selector row sits above combos; independent state, no shared flags |
| Vertical reflow on switch | Low | precedent: C3 row appears/disappears mid-column; do not touch `vgrow`/`prefRowCount` |

## Rollback Plan

Two independent work-unit commits. Revert commit 2 (`feat:` FXML+listener+checklist) → simultaneous rendering restored, helper extraction remains harmless. Revert commit 1 (`refactor:` helper+tests+delegation) if drift suspected. No fx:id removals, no data-path changes → no state migration needed.

## Forecast

~150–210 diff lines, 2 commits: (1) `TextosVista` + `TextosVistaTest` RED-first + delegation; (2) FXML + listener + limpiar reset + checklist. Per-commit < 400 ✅.

- Decision needed before apply: No
- Chained PRs recommended: No (same-branch stacking; not PR-based)
- 400-line budget risk: Low

## Dependencies

None new: base JavaFX `ToggleButton`/`ToggleGroup`. Prerequisite: C3 code present on the branch (satisfied at `da55414`).

## Success Criteria

- [ ] One click flips Ecuaciones ↔ Matricial; exactly one zone visible.
- [ ] Suite green at ~110–112 / 15 classes incl. R3.1 byte-pin.
- [ ] Augmented view column-aligned on ragged fixtures; loads keep mode; "Limpiar" resets to Ecuaciones.
- [ ] Manual checklist executed; `pom.xml` untouched; procedure-trace spec unmodified.
