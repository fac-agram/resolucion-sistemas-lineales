# Verification Report — trace-label-fixes

**Change**: trace-label-fixes
**Commits under verification**: `c09f8ed` + `d83d63e` (base `70a0602`, main)
**Version**: delta specs 2026-11-17
**Mode**: Strict TDD (config `strict_tdd: true`, runner `sh ./mvnw -o clean test`)
**Verify date**: 2026-11-17
**Verifier stance**: adversarial independent re-verification — every apply claim was re-executed, not trusted.

## Status: PASS WITH WARNINGS

No CRITICAL finding. All apply claims reproduced or exceeded by independent execution, including a live in-process GUI verification harness that went beyond the "manual checklist" mapping. Warnings are: transient flake watch (unreproduced in 3 fresh full runs), one PARTIAL GUI row (R5.1 file-load path — static-identical, needs 30s human click-through), and cross-change count-staleness note (honest-results artifacts, out of scope here).

---

## Executive Summary

Implementation matches specs, design, and tasks. Full suite is green at 82/0/0 across 12 classes (three independent clean runs); the R4 invariant `totalPasos == iteracoes` (21/21) and cap path (1000/1001) were re-measured live; R2.2 suppression was proven by a reproduced mutation (`if (true)` → exactly the claimed RED transcript); the pre-fix C1 failure (`expected: <21> but was: <20>`) was reproduced by recombining the commit's tests with the base source. GUI rows R3.1, R1, R2.2 were verified programmatically against the REAL rendered TextArea content (FXML + controller driven on DISPLAY=:1, manual-load dialog automated); R5.1 verified live for the manual-load path, statically-identical for the file-load path.

---

## Completeness

| Metric | Value |
|--------|-------|
| Tasks total | 19 |
| Tasks complete | 19 (all `[x]` — verified against actual code, not just checkboxes) |
| Tasks incomplete | 0 |

## Diff Hygiene (check 1) — ✅

`git diff 70a0602..d83d63e --name-only` = **exactly** the 6 declared files:

- `src/unsa/sistemas/model/iterative/RelajacionSimple.java` (line-move, D1)
- `src/unsa/sistemas/model/direct/GaussJordan.java` (guard block, D3)
- `test/RelajacionSimpleTest.java`, `test/GaussJordanTest.java`, `test/EliminacionGaussianaTest.java`
- `openspec/config.yaml` (:15 and :26 both claim 82/12 — match measured reality)

`src/unsa/sistemas/model/trace/Paso.java`: **UNCHANGED** (empty diff — R6 was test-only as promised). JacobiTest / GaussSeidelTest / SORTest untouched (not in diff; `+1` pins still present at :32/:32/:51 and passing). Working tree clean after all verify mutations (`git status` shows only untracked `openspec/changes/*` dirs).

## Build & Tests Execution (check 2, 9)

**Build**: ✅ `sh ./mvnw -o clean test` compile phase passes offline in all runs.

**Tests**: ✅ 82 passed / 0 failed / 0 skipped / 0 errors — 12 classes, **three independent full runs** (flake watch):

| Run | Result | Notes |
|-----|--------|-------|
| 1 | 82/0/0, 12 classes, BUILD SUCCESS | clean full suite at HEAD |
| 2 | 82/0/0, 12 classes, BUILD SUCCESS | post-mutation-revert gate |
| 3 | 82/0/0, 12 classes, exit 0, BUILD SUCCESS | log `/tmp/opencode/verify-tlf/run3.log` |

Per-class: GaussJordanTest 10, RelajacionSimpleTest 6, EliminacionGaussianaTest 6, SistemaControllerTest 24, MejoramientoIterativoTest 9, NormasTest 6, SORTest 4, JacobiTest 3, GaussSeidelTest 3, CholeskyTest 4, CroutTest 3, AnalisisSensibilidadTest 4 → **82**. Surefire XMLs: 12 files summing `tests="82"`. `TrazaDemo`: no report, never run (R5.2 ✅). Static `@Test` grep also totals 82 across 12 `*Test.java` — config.yaml:15/:26 claims match reality.

**Coverage**: ➖ no coverage tool in cached capabilities — skipped (not a failure).
**Quality metrics**: ➖ linter/type-checker/formatter absent in capabilities — skipped.

---

## Requirement Matrix (R1–R6 + baseline)

| Req | Scenario | Covering test / evidence | Result |
|-----|----------|--------------------------|--------|
| R1 | R1.1 swap-first indice==1 | `EliminacionGaussianaTest.verificaSwapPrimeraColumnaConsumeIndiceUno` (asserts indice 1 + exact label `"Intercambio F1 ↔ F2"` + all ≥0 — STRONGER than spec's "contains Intercambio"; the dropped speculative assertion was a tautological op-label check, correctly removed per apply deviation #4); existing `trazaSinIndicesNegativos`/`trazaIndicesMonotonicos` green | ✅ COMPLIANT |
| R2 | R2.1 unit diagonal, no collisions | `GaussJordanTest.verificaDiagonalUnitaria` green; probe indices gapless 0..k | ✅ COMPLIANT |
| R2 | R2.2 no cosmetic "/ 1.0" | `trazaSinNormalizacionesNoOp` green (5 pasos, no "/ 1.0"); over-suppression guard `verificaNormalizacionTrasSwapSeRegistra` green ("/ 2.0" after swap still recorded at indice 2); **mutation reproduced** (see below); GUI probe: unit-pivot 3×3 → `/ 1.0` absent, only "/ 6.0" recorded, Paso [1..7] gapless | ✅ COMPLIANT |
| R3 | R3.1 label order | Spec maps to manual GUI + `SistemaControllerTest` data path. Programmatic probe on REAL UI text: equations render `0·x1 + 2·x2 + 1·x3` left-to-right in all 3 rows; solutions `x1,x2,x3` ordered in `areaResultado` AND trace's "Solución obtenida" | ✅ COMPLIANT (probe-verified) |
| R4 | R4.1 converging pass counted | `verificaIteracionesCuentanPasadaConvergente` green; live probe A={{2,1},{1,3}}, b={3,5}: `iteraciones=21`, `totalPasos=21`, `convergio=true`, `errorFinal=6.615268675168071E-8` (matches design's measured 6.615e-08 exactly) | ✅ COMPLIANT |
| R4 | R4.2 cap | `verificaTopeDeIteracionesSinConvergencia` green; live probe on {{1,1},{1,1}}, b={1,0}: **1000 iteraciones / 1001 pasos / convergio=false** — CURRENT code pins exactly what design D1 stated for the cap path (cap never breaks via tolerance, so every pass executes+records an update: snapshot + 1000 = 1001). Confirmed green-on-arrival vs OLD source too (RED reproduction: 6/2 with cap test passing) | ✅ COMPLIANT |
| R4 | R4.3 declared drift re-pin | `RelajacionSimpleTest:44-45` re-pinned to `totalPasos == iteraciones` with rationale comment; commit c09f8ed body carries drift rationale + verbatim RED transcripts (re-pin `expected <true> but was <false>`, R4.1 `expected: <21> but was: <20>`, full 78/2); siblings' `+1` pins untouched and still true | ✅ COMPLIANT |
| R5 | R5.1 stale trace cleared | GUI probe: after solving A, manual-load B → `areaResultado` empty, `areaProcedimiento` empty, `paneProcedimiento` collapsed + disabled (live, real handler chain via automated dialog). FILE path: clear block :180-183 statically identical; FileChooser is a native dialog, not automatable headlessly | ⚠️ PARTIAL (file sub-path awaits human click-through) |
| R6 | R6.1 snapshot immunity | `verificaPasoInmuneAMutaciones` green. Adversarial read: test is DISCRIMINATORY, not trivial — GaussJordan mutates one `G` in place through all steps; if `Paso` shared the reference, `pasos.get(0).matriz()[0][0]` would read 1.0 post-solve and the `4.0` assert (delta 0.0) would fail. `Paso.java:30-33` deep copy confirmed, file untouched by this change | ✅ COMPLIANT |
| baseline | R5.1' 12 classes / 82 discovered | 12 surefire XMLs sum 82 (measured); delta text 12/82 with provenance 67→76→82 present (:18); config :15/:26 = 82/12 | ✅ COMPLIANT |
| baseline | R5.2' TrazaDemo excluded | no `TrazaDemo` report or run entry in any of 3 runs | ✅ COMPLIANT |

**Compliance summary**: 11/12 scenarios COMPLIANT, 1 PARTIAL (R5.1 file-load GUI sub-path), 0 UNTESTED/FAILING at runtime level.

---

## Final Unified Invariant Table (honest, per design D1 — live re-measured on A={{2,1},{1,3}}, b={3,5})

| Solver | iteraciones | totalPasos | Relation | Why the relation differs |
|--------|-------------|------------|----------|--------------------------|
| Jacobi | 20 | 21 | `+1` | stop test measures residual of the update just applied → pass k is counted AND traced |
| Gauss-Seidel | 11 | 12 | `+1` | same idiom |
| SOR (ω=1.5) | 27 | 28 | `+1` | same idiom |
| **Relajación Simple (fixed)** | **21** | **21** | **==** | stop test measures residual BEFORE updating → convergence-proving pass counted, no update executed, nothing to trace that pass |
| Relajación Simple (cap path) | 1000 | 1001 | `+1` | no pass breaks via tolerance → all 1000 passes execute+record updates; snapshot + 1000 |

The parity that spec R4 actually demands is at `getIteraciones()`: **all four solvers now count the pass where the stop criterion first holds**. The `+1` vs `==` step relation is a recording consequence (updates executed vs measured), not a counting divergence — exactly the design D1/D2 story, documented in the test comment and commit bodies. The pre-fix RS (20/21) was the only genuine undercount; it is gone.

---

## Mutation Reproduction (check 6) — ✅ independently performed at verify

1. **GaussJordan guard removal** (`if (Math.abs(pivote - 1.0) > EPS)` → `if (true)`), scoped `mvnw -o test -Dtest=GaussJordanTest`:
   `Tests run: 10, Failures: 1` → `trazaSinNormalizacionesNoOp:200 ... encontrado: F1 ← F1 / 1.0 ==> expected: <false> but was: <true>` — **identical message to apply's RED transcript**. Reverted via `git checkout --`; status proven clean; full suite re-gated 82/0/0 (run 2).
2. **Pre-fix C1 recombination** (c09f8ed tests + `70a0602` `RelajacionSimple.java`): scoped `Tests run: 6, Failures: 2` — `verificaConvergencia` (re-pin) `expected <true> but was <false>` AND `verificaIteracionesCuentanPasadaConvergente` `expected: <21> but was: <20>`, while the cap test stayed green on old code — falsification attempt CONFIRMS all three of apply's RED claims (scoped 4/1, method `21 vs 20`, full 78/2) and D4's "green-on-arrival" exception. Restored, clean.

## Live Contract Probe (checks 3–4) — ✅

`/tmp/opencode/verify-tlf/VerifyProbe.java` against `target/classes`: RS 21/21 (invariant true), cap 1000/1001/false; siblings 20/21, 11/12, 27/28; GJ unit-pivot {{1,2},{3,4}}: 5 pasos `[0..4]` gapless monotonic, no "/ 1.0"; GJ swap fixture: `[1] Intercambio F1 ↔ F2`, `[2] F1 ← F1 / 2.0` recorded; EG swap fixture: indice 1 = "Intercambio F1 ↔ F2", gapless.

## GUI Rows Disposition (check 8)

The apply reported `javafx:run` offline-blocked (plugin jar absent) and left 4 rows PENDING-MANUAL. Verify re-attempted the known offline route successfully — NOT via maven plugin: JavaFX 21.0.13 linux jars in `~/.m2` are Java-17 bytecode, ran directly with `--module-path` on DISPLAY=:1. Built `/tmp/opencode/verify-tlf/GuiProbe.java`: a JavaFX harness that loads the **real `Sistema.fxml` + `SistemaFXController`**, automates the manual-load dialog (types coefficients, fires "Cargar sistema"), selects the method via the real combos, invokes `resolverSistema()`, and asserts on the **actual rendered TextArea strings** (not pixels — this verifies text content, order and pane state, which is precisely what the rows demand).

| Row | Disposition | Evidence |
|-----|-------------|----------|
| R3.1 label order | ✅ VERIFIED (programmatic) | equations `·x1 ·x2 ·x3` left-to-right ×3 rows; solutions ordered in result AND procedure panels |
| R1 numbering | ✅ VERIFIED (programmatic) | swap-first: "Paso 1" block = `Intercambio F1 ↔ F2`; "Inicio" appears exactly once (indice 0); Paso numbers [1..8] gapless; no negative rendered |
| R2.2 suppression | ✅ VERIFIED (programmatic) | unit-pivot system: zero "/ 1.0" lines (only "/ 6.0" recorded); Paso [1..7] gapless |
| R5.1 stale clear | ⚠️ PARTIAL | manual-load path VERIFIED live (both panels cleared, pane collapsed+disabled); file-load path code statically identical (:180-183) but FileChooser native dialog is not headlessly automatable — **30-second human click-through recommended: solve A → load file B → panels empty & pane collapsed** |

## TDD Compliance (Strict module, Step 5a)

| Check | Result | Details |
|-------|--------|---------|
| TDD Evidence reported | ⚠️ content-yes/format-no | apply-progress (#81) carries full RED transcripts, gates, mutation evidence in prose + commit bodies; NO literal per-task "TDD Cycle Evidence" table. Substance verified independently (mutations reproduced) → format deviation only |
| All tasks have tests | ✅ | 6 new @Test exactly per D4 inventory; 19/19 tasks landed |
| RED confirmed | ✅ | pre-fix transcripts reproduced at verify (see Mutation Reproduction §2) |
| GREEN confirmed | ✅ | all covering tests pass on verify execution (3 runs) |
| Triangulation adequate | ✅ | RS: relation-pin + exact-k 21 + cap 1000/1001 (different expected values); GJ: suppression + over-suppression guard (opposite-direction fixtures) |
| Safety Net | ✅ | full-suite gates (76→78→82) run before/after each modification; no modified file shipped as "N/A (new)" |

## Test Layer Distribution (informational)

| Layer | Tests | Files | Tools |
|-------|-------|-------|-------|
| Unit (+ model-integration) | 82 | 12 | JUnit 5 / surefire |
| GUI/E2E | 0 committed | 0 | TestFX NOT in pom (spec explicitly chose manual checklist) |

## Assertion Quality (Step 5f) — on all 6 new tests + 1 re-pin

No tautologies, no ghost loops (index loops are preceded by hard `pasos.get(0/1/2)` accesses), no mock abuse (zero mocks), no implementation-detail coupling. SUGGESTION-level note below.

| File | Line | Assertion | Issue | Severity |
|------|------|-----------|-------|----------|
| `test/GaussJordanTest.java` | ~200 | `contains("/ 1.0")` | Substring would over-match a hypothetical label `"/ 1.0000000001"` (pivot just outside EPS). Unreachable for current deterministic fixtures; note for future parametric tests | SUGGESTION |

**Assertion quality**: 0 CRITICAL, 0 WARNING, 1 SUGGESTION — all assertions verify real behavior.

## Correctness (Static) & Design Coherence

| Decision | Followed? | Notes |
|----------|-----------|-------|
| D1 line-move before stop check | ✅ | `:103→:108` position exact; `errorFinal` semantics preserved (6.615e-08 measured) |
| D2 re-pin list, siblings untouched | ✅ | :44 comment + :45 pin only; grep-confirmed no other count-dependent literal broke |
| D3 guard + indice consumed only when recorded | ✅ | idiom mirrors `≤ EPS` elimination skip at :79 |
| D4 +6 test inventory | ✅ | exact names/locations; 76→82 arithmetic verified (12 XMLs, static grep) |
| D5 delta ownership + counts locked at apply | ✅ | 12/82 with provenance 67→76→82 in delta text; lands untracked per task 3.1 |
| D6 two green work-unit commits, bodies w/ RED | ✅ | both commit bodies carry transcripts + exceptions; commit 2 measured 158 lines vs ~90 estimate (disclosed by apply; <400 budget) |
| R5 baseline reconcile | ⚠️ | honest-results' own artifacts still cite 11 classes/59 (its historical snapshot; different change's files — not edited here per scope discipline; actuality: 12 XML). Reconcile note stands for honest-results' archive step |
| Rollback (task 3.3) | ✅ | independent reverts possible: commits touch disjoint files |

## Issues Found

**CRITICAL**: None.

**WARNING**:
1. R5.1 file-load sub-path verified only statically (identical code block) — needs a 30s human confirmation (see GUI rows disposition).
2. Apply-reported transient flake did NOT reproduce in 3 fresh full runs here (nor in apply's own 3 re-runs). Keep the flake-watch habit at archive; if it ever reddens, capture `target/surefire-reports` BEFORE any `clean`.
3. apply-progress TDD evidence is prose+commit-body form, not the literal strict-module table (substance complete and independently reproduced).

**SUGGESTION**:
1. `contains("/ 1.0")` over-match edge (see assertion table).
2. `EliminacionGaussiana` records zero-factor elimination steps (e.g. `F2 ← F2 - (0.0) F1` in the R1.1 fixture) — same cosmetic smell R2 fixed in GJ, but spec R2 scopes Gauss-Jordan only; candidate for a future change.
3. Pre-existing FXML-load warning "API of version 26 by runtime 21.0.13" (Sistema.fxml untouched by this change).

## Verdict

**PASS WITH WARNINGS** — every apply claim survived adversarial reproduction; the only open item is a human 30-second click-through of the file-load clear (GUI R5.1), plus standing flake watch. Ready for archive once the file-path row is attested (or accepted as static-identical by the user).

---

### Evidence Artifacts (verify-side, /tmp only — repo untouched)

- `/tmp/opencode/verify-tlf/VerifyProbe.java` + output (model contract)
- `/tmp/opencode/verify-tlf/GuiProbe.java` + run transcript (real UI, DISPLAY=:1, 10/10 checks)
- `/tmp/opencode/verify-tlf/run3.log` (82/0/0, BUILD SUCCESS, exit 0)
- Repo state after verify: `git status` clean (only untracked `openspec/changes/*`), HEAD `d83d63e`

## GUI attestation (2026-11-17): R5.1 file-load

**Verdict: PASS — WARNING-1 resolved.** The PENDING file-load sub-path was closed by executing the real behavior end-to-end, not statically.

**Method.** Real app launched from `target/classes` on the JavaFX 21.0.13 jlink runtime (`/tmp/opencode/rt`, `--add-modules javafx.controls,javafx.fxml`) under DISPLAY=:1; window confirmed via `xwininfo`. AT-SPI exposed the JavaFX frame with zero scene children and no key-generation API (and no xdotool existed), so the dialog was driven via X11 XTEST synthetic input (python-xlib `Xlib.ext.xtest` — the mechanism xdotool uses), with `import` screenshots as the assertion medium. Sequence: seeded/solved a system (its result `x1=1,x2=1,x3=1` + expanded Gauss trace captured pre-load in `attest-2a-preload-app.png`) → XTEST-clicked "Cargar archivo .txt" → the **native GTK FileChooser** opened → Ctrl+L → typed the absolute fixture path `/home/russian/app-trabajointegrador/resolucion-sistemas-lineales/pruebas txt/prueba directo (Solu Uni, no sim).txt` → Enter accepted.

**Assertion (~0.7 s after accept, `attest-2d-postload-app-later.png` vs `attest-2a-preload-app.png`):** with a DIFFERENT system loaded from file (4·x1+1·x2+1·x3=6… → 2·x1+1·x2+1·x3=7…, confirmed rendered in `areaSistema`/`areaMatrizA`), `areaResultado` was empty (prompt restored — stale solution gone), `paneProcedimiento` collapsed (▶) AND disabled (greyed — stale trace gone), and `labelEstado` read exactly **"Sistema cargado correctamente: 3 incógnitas."** — i.e. the clear block `SistemaFXController.java:180-183` + status :185-188 executed live in the real handler chain. Combined with the already-verified manual sub-path (harness 10/10), **R5.1 is now fully COMPLIANT on both input paths**.

**Honesty note.** The desktop was shared with a human during the session; the mid-run solve that established the populated pre-state was partly user-performed. The R5.1-relevant transition itself (file-load → cleared panels) was 100% automation-driven and captured back-to-back. Evidence inventory + reproduction steps: `/tmp/opencode/attest/README.md` (screenshots `attest-0-initial.png` … `attest-3c-current.png`, `xdrive.py`, `run-app.sh`, `tree-initial.txt`/`tree-final.txt` AT-SPI dumps, `app.log`). Live-spec verification-status note for R5.1 updated accordingly; full suite re-gated 82/0/0 after the attestation.

