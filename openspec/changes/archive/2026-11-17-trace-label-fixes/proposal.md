# Proposal: Trace and label fixes

> **RETROACTIVE formalization.** Roadmap #4 (`app-end-to-end/explore.md`) landed without SDD artifacts in `8b40d42` — "fix: normalizar pivotes en Gauss-Jordan y corregir traza y etiquetas (paso 4)" (squashed; old 234b478). Algorithm-related; no build impact.

## Intent

Honest, monotonic pedagogical output. Original defects: swap `Paso(-1, …)` rendered "Inicio"; step-number collisions; no pivot normalization; reversed `x3…x1` labels; stale trace on reload.

## Scope

### In Scope

Landed (185+/71−, verified):
- `GaussJordan`: pivot rows normalized to 1; `pivoteo_simple` consumes the step index (monotonic); `≤ EPS` no-op eliminations skipped.
- `EliminacionGaussiana`: swap records the active index; `-1` exception-sentinel only. `SistemaFXController`: labels `j + 1` (`x1…xn`); stale panels cleared on load.
- Tests additive only (`trazaSinIndicesNegativos` ×2, `trazaIndicesMonotonicos`, `verificaDiagonalUnitaria`); audit: no pre-existing literals changed. Config 63→67.

Remaining (apply work):
1. `RelajacionSimple` iteration count: `break` before `iteraciones = k` leaves the convergence-proving pass uncounted (siblings assign before stopping, `Jacobi.java:96`). Fix aligns and re-pins `test/RelajacionSimpleTest.java:45` (`traza.totalPasos() == iteraciones + 1`) — declared intentional spec drift: RED first, same commit, rationale recorded.
2. Skip cosmetic `"F1 ← F1 / 1.0"` steps when `|pivote − 1| ≤ EPS` (audit suggestion).
3. Pure formatter extraction: **out** — `Paso`/`Traza` are model-testable; no user-visible value.

### Out of Scope

Solver math beyond the landed fixes; `MejoramientoIterativo` (sibling `mejoramiento-iterativo` — coordinate, don't duplicate); packaging; README; fixtures.

## Capabilities

New: `procedure-trace` — step-index conventions (paso 0, then strictly monotonic; swaps consume active index; no cosmetic no-ops), unified iteration count, label order. sdd-spec writes `specs/procedure-trace/spec.md` only.
Modified: `test-execution-baseline` — R5 pins 11/52 vs reality 12/67; supersession owned **jointly with the sibling change**; delta written once.

## Approach

Two RED→GREEN commits (re-pin; no-op suppression). Verify: `sh ./mvnw -o clean test` + manual GUI checklist.

## Affected Areas

`RelajacionSimple.java`, `GaussJordan.java`; `RelajacionSimpleTest.java` (re-pin), `GaussJordanTest.java`; `openspec/config.yaml`.

## Risks

| Risk | L | Mitigation |
|---|---|---|
| Re-pin reads as test-bending | Med | Declared drift; RED evidence; rationale in commit |
| R5 delta orphaned | Med | Named in both proposals; archive order checked |
| No UI tests | Low | Model-level tests + manual GUI |

## Rollback Plan

`git revert` each remaining commit; RelajacionSimple revert restores old convention and pin together. Reverting `8b40d42` is last-resort (reintroduces defects, breaks 67 baseline).

## Dependencies

Sibling change (joint R5 delta); `strict_tdd: true`.

## Success Criteria

- [ ] RED recorded for the `RelajacionSimpleTest.java:45` re-pin
- [ ] Suite green: ≥67 tests, 12 classes; config updated
- [ ] No `… / 1.0` or negative-index steps; monotonic indices
- [ ] GUI: `x1…xn` labels; swaps never "Inicio"; no stale trace

## Review Workload Forecast

Landed 256 lines; remaining ≈ 60–100.
Decision needed before apply: No
Chained PRs recommended: No
400-line budget risk: Low
