# Design: Trace Label Fixes

## Technical Approach

Two behavior fixes in already-landed code, proven test-first (strict TDD), plus contract pins. Formalizes `8b40d42`; no build/config changes. Maps to specs: R4 (iteration count), R2.2 (no-op suppression), R1.1/R6 (contract tests), R3/R5 (manual GUI), R5-baseline (count reconciliation).

## Measurements (probe, /tmp/opencode/probe, JDK 17, fixture A={{2,1},{1,3}}, b={3,5})

| Solver | iteraciones | totalPasos | relation |
|---|---|---|---|
| Jacobi / GS / SOR (untouched) | 20 / 11 / 27 | 21 / 12 / 28 | `totalPasos == iteraciones + 1` |
| RelajacionSimple OLD | 20 | 21 | `+1` (undercounts: break at :106-109 precedes `iteraciones = k` at :122) |
| RelajacionSimple FIXED | **21** | **21** | **`totalPasos == iteraciones`** |

Siblings count AND record pass k because their stop test measures the update just applied (assign at Jacobi:96, GS:96, SOR:150 precedes break). RelajacionSimple's convergence-proving pass measures the residual and executes no update → counted, not traced. Snapshot Paso(0) + executed updates (k−1) = k = iteraciones. Cap path (measured, both versions): `iteraciones=1000, totalPasos=1001, convergio=false`.

## Decision D1 — RelajacionSimple fix (minimal diff, one line moved)

Move `iteraciones = k;` (:122) to immediately after `errorFinal = norma;` (:103), before the stop check. `errorFinal` keeps the convergence-proving pass norm (<1e-7; measured 6.615e-08) — unchanged semantics. Cap/edge paths verified identical except convergence count. Alternative (extra cosmetic "converged" Paso to keep `+1` uniform) rejected: contradicts R2's no-cosmetic-steps principle and spec R4.3 ("snapshot + one step per executed update").

## Decision D2 — Re-pin list

| Location | Old literal | New literal | Justification (measured) |
|---|---|---|---|
| `RelajacionSimpleTest.java:45` | `traza.totalPasos() == g.getIteraciones() + 1` | `traza.totalPasos() == g.getIteraciones()` | 21 vs 21 (probe) |
| `RelajacionSimpleTest.java:44` comment | "1 paso extra por el residual inicial" | "snapshot + executed updates = unified count" | wording only |

Grep audit — no other count-dependent literals break: `JacobiTest:32`, `GaussSeidelTest:32`, `SORTest:51` (`+1`) stay valid (siblings untouched); `SistemaControllerTest:53,69,174` use `>0`/`<`; `MejoramientoIterativoTest` `>=0`/`>0`; `GaussJordanTest:53`, `EliminacionGaussianaTest:52` `>0`. New `RelajacionSimpleTest` R4.1 pins `getIteraciones() == 21` (deterministic IEEE fixture; re-measure if it ever reddens).

## Decision D3 — GaussJordan no-op suppression (≤6 lines, :58-63)

Wrap division+record in `if (Math.abs(pivote - 1.0) > EPS) { ... }` (EPS=1e-12 at :9, matches spec). Skips arithmetic AND record, mirroring the landed `≤ EPS` elimination idiom at :72; `indice++` consumed only when recorded → monotonic, gapless. Post-swap normalization with pivote ≠ 1 still records (probe: `Intercambio F1↔F2` idx 1, `F1 ← F1 / 2.0` idx 2). Suppressed pivots are 1±≤1e-12 → unit diagonal still holds (probe max deviation 0.0). Unit-pivot fixture: 6→5 pasos, zero "/ 1.0".

## Decision D4 — Test inventory (+6 @Test, 0 classes)

| Test | File | Spec | RED-able? |
|---|---|---|---|
| exact-k `verificaIteracionesCuentanPasadaConvergente` | RelajacionSimpleTest | R4.1 | Yes (old gives 20) |
| cap `verificaTopeDeIteracionesSinConvergencia` — fixture A={{1,1},{1,1}}, b={1,0} (bounded oscillation, measured finite: x=[500,−500]) | RelajacionSimpleTest | R4.2 | No — characterization pin (old already counts 1000); exception recorded in commit body |
| swap-first `verificaSwapPrimeraColumnaConsumeIndiceUno` (indice==1 "Intercambio", all ≥0) | EliminacionGaussianaTest | R1.1 | No — characterization (landed behavior, probe-confirmed) |
| no-op `trazaSinNormalizacionesNoOp` | GaussJordanTest | R2.2 | Yes (old emits "/ 1.0") |
| swap-normalize-recorded `verificaNormalizacionTrasSwapSeRegistra` | GaussJordanTest | R2 | No — regression guard vs over-suppression |
| snapshot-immunity `verificaPasoInmuneAMutaciones` (paso0 keeps A[0][0]=4 while final has 1) | GaussJordanTest | R6 | No — contractualization (Paso:30-33 already copies) |

Count formula: **M′ = 67 + k_s + 6**, classes **C′ = 12 + c_s**, where k_s/c_s = sibling `mejoramiento-iterativo` additions. Scenarios: k_s=0→73/12; k_s=4→77/12. Apply MUST lock actuals from the final `sh ./mvnw -o clean test` line; no placeholder ships to archive.

## Decision D5 — R5 baseline reconciliation + ordering

Single-line rule: **whichever change archives second pins the final R5 numbers in its delta; the earlier-archiving one defers.** Delta TEXT owner stays `trace-label-fixes` (collision-checked, 2026-11-17). Recommendation: sibling applies/archives FIRST (pure-model slice, lands the NaN-honesty guard our cap fixture depends on staying finite); `trace-label-fixes` second — its delta comment (12/67) is updated in the same apply to measured 12+c_s/67+k_s+6 before archive merge. Rationale: GUI-touching manual checklist makes ours the slower, later-verifiable slice.

## Decision D6 — Commit plan (main-only, green commits, work-unit commits, each <400 lines; est. ≤35 and ≤90)

1. `fix: unificar el conteo de iteraciones en relajacion simple` — RED: re-pin :45 + R4.1 test recorded failing (expected 21, got 20) against old code; GREEN: D1 line-move; plus R4.2 characterization pin. Mutation: restoring old line order reddens :45+R4.1; reverting commit restores code+pin atomically.
2. `fix: suprimir pasos de normalizacion redundantes en gauss-jordan` — RED: no-op test recorded failing ("/ 1.0" found); GREEN: D3 guard; rides along: swap-normalize guard, EG swap-first pin, R6 immunity pin (trace-contract unit). Mutation: removing guard reddens no-op test; reverting restores cosmetic steps cleanly.

Rollback: `git revert` each independently; neither touches build, config, or the other's tests.

## File Changes

| File | Action |
|---|---|
| `src/unsa/sistemas/model/iterative/RelajacionSimple.java` | Modify (1 line move + comment) |
| `src/unsa/sistemas/model/direct/GaussJordan.java` | Modify (block guard) |
| `test/RelajacionSimpleTest.java` | Modify (re-pin +2 tests) |
| `test/GaussJordanTest.java` | Modify (+3 tests) |
| `test/EliminacionGaussianaTest.java` | Modify (+1 test) |
| `openspec/changes/trace-label-fixes/specs/test-execution-baseline/spec.md` | Modify at apply (lock final counts) |

## Manual GUI Checklist (no TestFX in pom; `sh ./mvnw -o javafx:run`)

1. R3.1: load 3×3 file → equations render `·x1 ·x2 ·x3` left-to-right (:424); solve → results and trace list `x1,x2,x3` in order (:593, :673-676).
2. R1: load zero-first-pivot file (`0 2 1` / `1 0 3` / `2 1 0`), Gauss-Jordan → trace first numbered block is "Paso 1 / Intercambio F1 ↔ F2"; "Inicio" appears only for Paso 0 (:631).
3. R2.2: system with pivot 1 (e.g. `1 2 3` first row) → no "F1 ← F1 / 1.0" line; Paso numbers strictly increase without gaps.
4. R5.1: solve A (procedure pane expanded/disabled state visible) → load B via file → result+procedure cleared, pane collapsed+disabled (:180-183); repeat with manual-entry load (:278-281); re-solve → fresh trace only.

## Verification Gates

1. RED evidence (failure output) recorded for re-pin/R4.1 and R2.2 before each GREEN.
2. `sh ./mvnw -o clean test` green at each commit; final `Tests run: M′, Failures: 0, Errors: 0, Skipped: 0` across C′ classes; config `expected` comment and R5 delta updated to measured M′/C′.
3. Mutation checks per D6.
4. Manual GUI checklist rows 1-4 executed and attested in verify-report.

## Risks

- Re-pin reads as test-bending → mitigated: declared drift R4.3, RED evidence, rationale in commit body.
- Divergent literals (`+1` siblings vs bare `iteraciones` for RelajacionSimple) confuse maintainers → documented in test comment + this design.
- Cap fixture overflow ({{1,2},{2,1}} reaches ~1e301) → chosen bounded oscillating fixture instead.
- Sibling lands after us → R5 numbers re-checked at our verify regardless of order.

## Open Questions

- None blocking. k_s resolved at apply by measurement.

## Migration

No migration required.
