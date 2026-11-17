# Proposal: Mejoramiento Iterativo (retroactive formalization + honesty fix)

> **RETROACTIVE.** Code shipped in `e9582aa`, verified green: 63 tests at that commit, 67 at HEAD. The change is **not** done: it formalizes the spec **and** fixes known defects test-first.

## Intent

Give the shipped iterative-refinement solver a live contract and close its honesty gap. As implemented: `x0` via Gaussian elimination; residual `r = b − A·x`; correction solves `A·z = r` with `EliminacionGaussiana`, `x += z`; stops at `‖r‖∞ ≤ 1e-7` or 1000 iterations. Implements `ResolutorIterativo`; registered as `MEJORAMIENTO` via the controller's `instanceof` branch.

## Known defects (in scope — incomplete until fixed)

- **NaN convergence lie (WARNING).** `normaInfinito` skips NaN (comparisons with NaN are false), so a non-finite residual yields `convergio=true, errorFinal=0.0, iteraciones=0`. Verified probe: `A={{1,2},{3,5}}, b={1,NaN}` → `x=[NaN,NaN]` claiming convergence. The controller masks it as `NO_FINITO`, but the **solver API lies**. Fix test-first (RED): a non-finite residual MUST NOT set `convergio=true`. This corrects the convergence flag, not solution classification — no conflict with `honest-results` R7.
- **Weak tests.** `verificaSistema3x3ConTraza` asserts only `Ax≈b`, never ground truth `(47/23, −133/69, 95/69)`; keeps scratch comments; `assertTrue(iteraciones >= 0)` is trivially true. Missing: singular input, non-finite input, non-convergence at cap, controller integration.

## Scope

**In:** `mejoramiento-iterativo` delta spec; NaN-residual fix (RED→GREEN); test hardening; R5 supersession.
**Out:** UI changes, packaging, other algorithms, `Normas` refactors beyond the fix.

## Capabilities

**New:** `mejoramiento-iterativo` — solver behavior + convergence-honesty requirements.
**Modified:** `test-execution-baseline` R5 — pins 11 classes/52 tests; reality is 12/63 (`e9582aa`), 67 (HEAD). This change and sibling `trace-label-fixes` together supersede R5: one owner edits the R5 text, the other references it — no duplication.

## Approach

Fix site decided in design: propagate NaN in `normaInfinito`, **or** guard the stop test with `Double.isFinite(normaR)`. Either way the solver still returns `x` (possibly NaN); only the convergence flag becomes honest; classification stays in the controller.

## Affected Areas

| Area | Impact |
|---|---|
| `src/.../iterative/MejoramientoIterativo.java` | Modified |
| `src/.../matrix/Normas.java` | Possibly modified |
| `test/MejoramientoIterativoTest.java` | Hardened + new cases |
| `test/SistemaControllerTest.java` | Integration case |
| `openspec/specs/test-execution-baseline/spec.md` | R5 supersession |

## Risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| `normaInfinito` change ripples to 4 other solvers | Med | Prefer solver-side guard; else full suite + NaN cases |
| R5 supersession collides with `trace-label-fixes` | Med | Single R5 owner |

## Rollback Plan

`git revert` the fix commit; the spec delta is additive documentation, revertable independently. No build/infrastructure coupling.

## Success Criteria

- [ ] Suite grows 52→63+ tests, green via `sh ./mvnw -o clean test`
- [ ] NaN RED test recorded: fails without guard, passes with it
- [ ] Mutation: reverting the NaN guard turns the test red again
- [ ] R5 supersession merged without duplicating `trace-label-fixes`

## Review Workload Forecast

400-line budget risk: Low — Chained PRs recommended: No — Decision needed before apply: No
