# Tasks: Trace Label Fixes

## Review Workload Forecast

~125 lines (C1≈35, C2≈90). Delivery: ask-always; main-only; every commit green.

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: stacked-to-main
400-line budget risk: Low

## Count Reconciliation (LOCKED sibling handoff)

Start 76/12 (k_s=9, c_s=0); 67+k_s+6 → final **82/12**. C1: re-pin is not a new test; +R4.1+R4.2 → gate 78. C2: +4 → gate 82. Design states no per-commit totals (73/77 assume k_s=0/4); derived gates match D4 (+6) and 82/12 — no discrepancy. Pre-fix C1 full run `Failures: 2` (re-pin + R4.1); literal `Failures: 1` gate = scoped re-pin run — record both.

## Phase 1: Commit 1 — Unify iteration count (RED→GREEN)

- [x] 1.1 RED: re-pin `test/RelajacionSimpleTest.java:45` to `totalPasos() == getIteraciones()`; :44 comment per D2. Jacobi/GS/SOR `+1` pins untouched.
- [x] 1.2 RED: add R4.1 `verificaIteracionesCuentanPasadaConvergente` — A={{2,1},{1,3}}, b={3,5}: `getIteraciones()==21`, converges.
- [x] 1.3 Add R4.2 characterization `verificaTopeDeIteracionesSinConvergencia` — A={{1,1},{1,1}}, b={1,0}: 1000/1001 iteraciones/pasos, convergio==false; green-on-arrival (exception in body).
- [x] 1.4 RED transcript (pre-fix): scoped `Failures: 1` ("expected 21, got 20"); full `Tests run: 78, Failures: 2`.
- [x] 1.5 GREEN: move `iteraciones = k;` (`RelajacionSimple.java:122`) after `errorFinal = norma;` (:103), before the stop check → invariant `totalPasos == iteraciones` (21/21).
- [x] 1.6 Gate: `sh ./mvnw -o clean test` → 78/0, 12 classes.
- [x] 1.7 Mutation: restore old order → re-pin + R4.1 redden; restore.
- [x] 1.8 Commit `fix: unificar el conteo de iteraciones en relajacion simple` — stage `RelajacionSimple.java` + `RelajacionSimpleTest.java` only; drift rationale in body; ask approval.

## Phase 2: Commit 2 — Suppress GaussJordan no-op normalization (RED→GREEN)

- [x] 2.1 RED: add `trazaSinNormalizacionesNoOp` (GaussJordanTest) — unit-pivot fixture: no "… / 1.0", 5 pasos (old: 6).
- [x] 2.2 Add 3 green-on-arrival tests (exceptions in body): pivot-2.0 normalization after swap still recorded (over-suppression guard); `verificaSwapPrimeraColumnaConsumeIndiceUno` (EliminacionGaussianaTest, R1.1); `verificaPasoInmuneAMutaciones` (R6.1: paso0 keeps A[0][0]=4, final 1).
- [x] 2.3 RED transcript (pre-fix): `Tests run: 82, Failures: 1` (2.1 finds "… / 1.0").
- [x] 2.4 GREEN: wrap `GaussJordan.java:58-63` (division loop + registrarPaso) in `if (Math.abs(pivote - 1.0) > EPS)`; `indice++` consumed only when recorded → monotonic, gapless.
- [x] 2.5 Gate: `sh ./mvnw -o clean test` → 82/0, 12 classes.
- [x] 2.6 Mutation: drop guard → 2.1 reddens; over-suppress → 2.2 guard reddens; restore.
- [x] 2.7 Update tracked `openspec/config.yaml` expected literals (:15, :26) to measured 82. Stage ONLY inside commit 2 (final count born here).
- [x] 2.8 Commit `fix: suprimir pasos de normalizacion redundantes en gauss-jordan` — stage `GaussJordan.java`, `GaussJordanTest.java`, `EliminacionGaussianaTest.java`, `openspec/config.yaml`; ask approval.

## Phase 3: R5 delta, GUI verification, rollback

- [x] 3.1 R5 owner: update untracked delta `openspec/changes/trace-label-fixes/specs/test-execution-baseline/spec.md` 12/67 → **12/82** (text, R5.1, provenance 67→76→82) before archive; lands in no commit.
- [x] 3.2 GUI (`sh ./mvnw -o javafx:run`), attest in verify-report: R3.1 label order (:424/:593/:673-676); R1 "Paso 1 / Intercambio", "Inicio" only at indice 0 (:631); R2.2 gapless, no "/ 1.0"; R5.1 stale clear (:180-183 file, :278-281 manual).
- [x] 3.3 Rollback: independent `git revert`, reverse order (2→1); C1 restores code+old pin atomically; C2 restores cosmetic steps+config.
