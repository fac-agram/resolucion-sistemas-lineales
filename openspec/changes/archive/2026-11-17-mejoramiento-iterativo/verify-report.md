# Verification Report: mejoramiento-iterativo

- **Change**: `mejoramiento-iterativo` (NaN honesty fix + test hardening)
- **Commits under verification**: `901bd84` (fix) + `70a0602` (test) on base `8b40d42`; verified at HEAD `d83d63e`
- **Mode**: openspec, Strict TDD active (`openspec/config.yaml testing.strict_tdd: true`)
- **Verifier stance**: adversarial — every apply claim independently reproduced, not trusted
- **Date**: 2026-11-17

## Verdict: PASS WITH WARNINGS

No CRITICAL findings. All 7 requirements verified by runtime evidence. Two WARNINGs are spec-wording/design-hygiene items for archive, not behavioral failures.

## Executive Summary

Independent re-verification confirms the apply's core claims exactly: the one-line `Math.max` fix in `Normas.normaInfinito(double[])` is the only production change; `MejoramientoIterativo.java` is untouched; the RED transcript (69/2, `expected: <NaN> but was: <4.0>` + `expected: <false> but was: <true>`) and mutation gate A (revert → the same 5 NaN-side tests red) were reproduced live at HEAD; the R7 firewall is bit-identical on all 13 non-NaN probe vectors; suite arithmetic 67→69→76 (→82 at HEAD incl. siblings) checks out commit-by-commit. Ground-truth values (47/23, −133/69, 95/69) and (1, 0.5) were re-derived independently via exact Cramer arithmetic.

## Completeness

Tasks 1.1–7.1: 22/22 marked `[x]` in `tasks.md`; all independently re-verified below. Nothing claimed was found false.

## Command Evidence

| Command | Result |
|---|---|
| `sh ./mvnw -o clean test` (HEAD, baseline) | `Tests run: 82, Failures: 0, Errors: 0, Skipped: 0` — 12 classes; MejoramientoIterativoTest **9**, NormasTest **6**, SistemaControllerTest **24** ✅ |
| `sh ./mvnw -o clean test` (mutation: fix reverted in `Normas.java`) | `Tests run: 82, Failures: 5` — exactly the 5 tests claimed in progress.md gate A, same line numbers (90/255/132/66/194), same messages (`expected: <NaN> but was: <4.0>`, `expected: <false> but was: <true>`); other 77 green ✅ |
| `git checkout -- src/.../Normas.java` + `git status --porcelain` | tracked tree clean (only pre-existing untracked `openspec/` dirs) ✅ |
| `sh ./mvnw -o clean test` (post-restore re-gate) | `Tests run: 82, Failures: 0, Errors: 0` — BUILD SUCCESS ✅ |
| `git diff 8b40d42..70a0602 --name-only` | exactly `Normas.java`, `MejoramientoIterativoTest.java`, `NormasTest.java`, `SistemaControllerTest.java` — **no `MejoramientoIterativo.java`** ✅ |
| `git ls-tree`/`grep -c @Test` per commit | `8b40d42`: 67 tests/12 classes → `901bd84`: 69 → `70a0602`: 76 → HEAD: 82. Per file: NormasTest 5→6, MejoramientoIterativoTest 3→9, SistemaControllerTest 22→24 ✅ |
| `git show --stat 901bd84 / 70a0602` | subjects verbatim per tasks 2.3/5.2; stats 52+/5− and 171+/11− match progress.md claims; C1 staged exactly the 3 claimed files, C2 exactly the 2 claimed test files ✅ |
| `/tmp/opencode/ProbeMI.java` vs `target/classes` | solver: `convergio=false iter=1000 errorFinal=NaN x=[NaN,NaN]`; controller: `NO_FINITO`, sol=null, `Convergencia=[false,1000,NaN]`, no éxito text ✅ |
| `grep "Double.is" src/unsa/sistemas/model/` | no matches (exit 1) ✅ — honest-results gate scope (`model/`) clean; pre-existing tokens live only in controller/view, untouched by this change |
| `grep "getIteraciones() >= 0"` / `grep "Wait:"` in `MejoramientoIterativoTest` | no matches ✅ |
| Cramer probe (exact fractions) | det₃=69, x=(47/23, −133/69, 95/69); det₂=−2, x=(1, 1/2) — ground truths independently confirmed ✅ |

## Requirement Matrix

| Req | Status | Evidence (independently executed) |
|---|---|---|
| **R1** Algorithm Contract | **PASS** | `verificaSolucionDirectaCanonica` (x≈(0.8,1.4)±1e-6, `assertEquals(0, iter)`, errFinal pinned 8.881784197001252e-16±1e-15), `verificaMejoramientoConX0InicialPerturbado` (iter>0, err≤1e-7), `verificaDobleEjecucionRepiteResultados` (state reset). All green at HEAD. Source read: TOLERANCIA=1e-7, cap=1000, per-run state reset at `resolver()` entry. See WARNING-1 for the R1.3 fixture. |
| **R2** Convergence-Flag Honesty | **PASS** | Live /tmp probe at HEAD: A={{1,2},{3,5}}, b={1,NaN} → `convergio=false, iter=1000, errorFinal=NaN`, x=[NaN,NaN] unsanitized — matches design's measured values exactly. R2.2 overflow test (`MAX_VALUE` x0 → false/1000/NaN) green; mechanism-neutral contract honored (fix landed in `Normas`, zero solver edits, no `Double.is*` in model/). |
| **R3** Controller Integration | **PASS** | Probe: `NO_FINITO`, sol null, `Convergencia[false,1000,NaN]` copied via `instanceof ResolutorIterativo` branch (SistemaController:121 read). Tests: `verificaMejoramientoIterativo` (R3.1, existing, asserts OK + copied flag), `verificaMejoramientoNoFinitoConBanderasHonestas` (R3.2+R3.4), `verificaMejoramientoSingularEstadoError` (R3.3, message names MEJORAMIENTO execution failure). All green. |
| **R4** Correctness Oracles | **PASS** | 3x3 ground truth re-derived independently (det=69, (47/23,−133/69,95/69)); asserted ±1e-6 in `verificaSistema3x3ConTraza`. R4.2 (1,0.5) re-derived (det=−2), asserted in `verificaRefinamientoConvergeDondeJacobiDiverge` with live inline Jacobi contrast (false/1000/finite). Green. |
| **R5** Edge Behaviors | **PASS** | Singular: `assertThrows(ArithmeticException)` + post-throw flag not true (R5.1), controller ERROR mapping (R3.3). Cap: NaN-cap witness asserts exactly `1000`, `!convergio`, last iterate returned (R5.2). Finite-capped-iterate clause: design proved unreachable for this algorithm (correction solves A·z=r exactly); contract covered controller-side by existing `verificaJacobiNoConvergeConAproximacionFinita` (SistemaControllerTest:157, confirmed present, green). |
| **R6** Test Quality | **PASS** | Mutation gate A reproduced live: reverting the fix reddens exactly 5 discriminating tests (unit norm, solver NaN/overflow/cap, controller flags). Gates B/C cross-checked statically against source: their claimed reds correspond to real assertions at the cited lines (TOLERANCIA pin at :30/:48/:88; `assertEquals(1000,…)` at :256/:133/:195). C2 deletions audited: removed only the trivial `>= 0` bound (replaced with `assertEquals(0,…)`), the "Wait:" scratch lines, and the `Ax≈b` proxy (replaced with ground truth) — **no assertion weakened**. No ghost loops (all collection loops preceded by length asserts), no tautologies, no mocks. |
| **R7** Regression Firewall | **PASS** | /tmp battery, 18 vectors old-formula vs new: all 13 non-NaN (incl. empty, −0.0, ±Inf, 1e308, subnormal) **bit-identical** (`doubleToLongBits`); exactly the 5 NaN-bearing vectors differ, in the directions the design predicted ({1,NaN}:1.0→NaN, {NaN}:0.0→NaN, {Inf,NaN}:Inf→NaN). `normaInfinito(Matriz)`/`AnalisisSensibilidad` unchanged (diff scope); full suite 82/0/0 at HEAD; Jacobi/GaussSeidel/SOR/RelajacionSimple production+test files zero-modified in `8b40d42..70a0602`. |

## TDD Compliance

| Check | Result | Details |
|---|---|---|
| TDD Evidence reported | ✅ | `output/progress.md`: RED-before-production transcript, GREEN gates, 3 mutation gates |
| RED confirmed | ✅ | Claims (69 run / 2 failures, exact expected/actual strings) match progress.md; semantically reproduced live via mutation A (identical messages and line numbers) |
| GREEN confirmed | ✅ | 69→76 per-commit counts verified from committed trees; 82/0/0 at HEAD |
| Triangulation adequate | ✅ | NaN contract: unit norm (5 cases) + solver + overflow + cap + controller — different fixtures, different expected values |
| Safety net for modified files | ✅ | Mutation A run shows the other 77 tests green with fix reverted — pre-existing suite intact |
| Never committed red | ✅ | `git show --stat`: each commit is a coherent unit; HEAD~1 and HEAD green by tree reconstruction counts |

**TDD Compliance**: 6/6 checks passed

## Test Layer Distribution

| Layer | Tests | Files | Tools |
|---|---|---|---|
| Unit (model) | 15 (+1 controller-component) | MejoramientoIterativoTest, NormasTest | JUnit 5 / surefire |
| Component (controller direct-call) | 2 new of 24 | SistemaControllerTest | JUnit 5 |
| E2E | 0 | — | not installed (config `e2e: false`) |

Coverage: skipped — no coverage tool (`config.yaml coverage.available: false`). Quality tools: skipped — none detected.

## Findings

### CRITICAL
None.

### WARNING
1. **W1 — R1.3 scenario premise is unsatisfiable on the public API; covered by an equivalent fixture.** Spec says "resolver() runs again on **changed inputs**", but `Resolutor` clones A/b in the constructor, so one instance cannot see changed inputs. Delivered test substitutes double-run result equality (accumulation-catch), documented in design's decisions table. Intent met; spec text should be reworded at archive time to match the reachable contract.
2. **W2 — canonical errFinal is pinned bit-exact (8.881784197001252e-16 ±1e-15) where spec only demands ≤1e-7.** This is deliberately stronger (discriminating, per R6) and currently correct, but couples the test to the exact float operation order of `EliminacionGaussiana`; a future legitimate pivoting change could break it without breaking the contract. Accepted tradeoff — flag for awareness at archive.

### SUGGESTION
1. Design claims NaN-cap wall time "3–8 ms"; first probe run measured ~10 ms (JIT warmup). Same O-cost, trivial magnitude — claim is slightly optimistic prose, not a behavioral issue.
2. GUI NO_FINITO rendering ("manual check" in design/Migration) remains unautomated and out of this change's scope; `app-end-to-end` change dir (untracked, sibling) appears to own that follow-up.

## Adversarial Re-verification Notes

- Every apply claim checked against live execution or committed-tree arithmetic; **zero discrepancies found** in scope. Sibling count shifts from `c09f8ed`/`d83d63e` (RelajacionSimple, GaussJordan) are legitimate out-of-scope commits and were excluded from this change's matrix per instruction.
- Mutation A, the full-suite gate, greps, R2/R3/R7 probes, ground-truth derivation, and commit-scope audits were all **re-executed by this verifier**, not read from apply output.
- Tree state after verification: identical to arrival (`git status --porcelain` clean for tracked paths; final re-gate 82/0/0 green).

## Next Recommended

`sdd-archive` — safe to proceed. Suggested during archive: reword R1.3 GIVEN per W1; consider a comment or relaxation on the bit-exact pin per W2.

## Risks

- Bit-exact errFinal pin (W2) couples tests to float-ordering of the direct solver — low likelihood, low cost to fix.
- NaN runs now execute 1000 corrections (~10 ms at n=2, O(1000·n³)) — documented and accepted in design; unchanged from apply's risk position.
