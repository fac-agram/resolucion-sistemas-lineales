# Archive Report: Mejoramiento Iterativo

- **Change**: `mejoramiento-iterativo` (retroactive formalization of `e9582aa` + NaN convergence-honesty fix + test hardening)
- **Capability**: `mejoramiento-iterativo` (new)
- **Archived**: 2026-11-17
- **Archive path**: `openspec/changes/archive/2026-11-17-mejoramiento-iterativo/`
- **Repository state at archive**: `main` @ `d83d63e`
- **Commits under this change**: `901bd84` (fix, 52+/5−) + `70a0602` (test, 171+/11−) on base `8b40d42`
- **Verification verdict**: `pass_with_warnings` — zero CRITICAL; WARNING-1 and WARNING-2 carried into the live spec at merge (details below)
- **Mode**: openspec, Strict TDD active (`openspec/config.yaml testing.strict_tdd: true`)
- **Ordering**: this change applied AND archived FIRST per locked ordering; sibling `trace-label-fixes` archives second (it is next).

---

## 1. What Was Delivered

1. **Convergence lie fixed** — `Normas.normaInfinito(double[])` silently skipped NaN (`NaN > max` is false), so a non-finite residual yielded `convergio=true, errorFinal=0.0, iteraciones=0`. One-line root fix: `Math.max` accumulation propagates NaN naturally; `NaN ≤ 1e-7` is false, so the loop runs to cap and the flag stays false. **Zero production edits in `MejoramientoIterativo.java`**; no `Double.is*` tokens added in `model/` (honest-results gate scope clean).
2. **Test hardening (R6)** — `verificaSistema3x3ConTraza` now asserts ground truth (47/23, −133/69, 95/69) ±1e-6 instead of the `Ax≈b` proxy; trivial `assertTrue(iteraciones >= 0)` replaced with `assertEquals(0, …)`; scratch comments removed.
3. **New coverage** — NaN input, intermediate overflow, cap exhaustion (1000), singular `assertThrows`, double-run state reset, controller integration (NO_FINITO with honest copied `Convergencia`, singular → ERROR, no hardcoded éxito text).

## 2. Gates and Suite Arithmetic

| Gate | Command | Observed |
|---|---|---|
| RED (before production change) | `sh ./mvnw -o clean test` | `Tests run: 69, Failures: 2` — `expected: <false> but was: <true>` + norm NaN mismatch (transcript in `output/progress.md`) |
| GREEN C1 (`901bd84`) | `sh ./mvnw -o clean test` | **69/0/0** |
| GREEN C2 (`70a0602`) | `sh ./mvnw -o clean test` | **76/0/0** — MejoramientoIterativoTest 9, NormasTest 6, SistemaControllerTest 24, 12 classes |
| Mutation A (fix reverted) | full suite | exactly 5 NaN-side tests red, 77 green; restored and re-gated |
| HEAD at verify/archive | `sh ./mvnw -o clean test` | 82/0/0 (includes sibling commits `c09f8ed`, `d83d63e`) |

Commit-count chain: `8b40d42` 67 → `901bd84` 69 → `70a0602` 76 → HEAD 82. The locked gates for this change are **69/76**; 82 is the shared HEAD with the sibling.

## 3. Specs Synced

| Domain | Action | Details |
|---|---|---|
| `mejoramiento-iterativo` | **Created** (new capability; delta was full spec) | 7 requirements (R1–R7), 17 scenarios, all ids preserved verbatim |

Live spec: `openspec/specs/mejoramiento-iterativo/spec.md` — provenance header records origin change, archived source path, sync date 2026-11-17, verify verdict `pass_with_warnings`.

**Verify findings applied at merge:**
- **WARNING-1 → R1.3 reworded.** The delta said `resolver()` runs again "on changed inputs", but `Resolutor` clones A/b in the constructor, so that premise is unsatisfiable on the public API. The live scenario now states the honest, observable contract: double-run equality on one instance (identical iteration count/error/flag; no cross-run accumulation) — exactly what the delivered `verificaDobleEjecucionRepiteResultados` proves. Intent preserved.
- **WARNING-2 → R1.2 annotated.** The delivered test pins `getErrorFinal()` bit-exact at 8.881784197001252e-16 ±1e-15 — deliberately stronger than the normative `≤ 1e-7` (discriminating per R6) but coupling the test to the float-operation order of `EliminacionGaussiana`. Non-normative annotation added: a future legitimate pivoting change may relax the pin without touching the contract.
- **SUGGESTION (wall time)**: design prose claimed NaN-cap runs cost "3–8 ms"; measured ~10 ms (JIT warmup). Same trivial O(1000·n³) cost — recorded here only; archived `design.md` is audit trail and stays unmodified.

**NOT touched:** `openspec/specs/test-execution-baseline/spec.md`. This change deliberately wrote no R5 delta (single-owner rule); its live R5 still pins 11/52 until `trace-label-fixes` merges its supersession.

## 4. Archive Contents (audit trail — never modify)

- `proposal.md` ✅ — intent, known defects, R5 ownership delegation
- `specs/mejoramiento-iterativo/spec.md` ✅ — originating delta (R1.3 keeps its original wording here; the reword lives in the live spec)
- `design.md` ✅ — `Math.max` decision table, measured behavior, commit plan
- `tasks.md` ✅ — 22/22 tasks `[x]` (Phases 1–7 all complete)
- `verify-report.md` ✅ — adversarial re-verification, PASS WITH WARNINGS, TDD compliance 6/6
- `output/progress.md` ✅ — RED/GREEN transcripts + mutation gates A/B/C
- `archive-report.md` ✅ — this report

Active `openspec/changes/` no longer contains `mejoramiento-iterativo`. Directory was untracked (`openspec/` is not committed); moved with plain `mv`, no git state-changing commands used.

## 5. Backlog — Explicitly Untouched by This Change

- **UI "MEJORAMIENTO" polish was never promised** in proposal scope ("Out: UI changes, packaging") — still out. GUI NO_FINITO rendering remains a manual check; the `app-end-to-end` tracker change owns that follow-up.
- **NaN-cost accepted**: non-finite runs now execute the full 1000-correction cap (~10 ms at n=2, O(1000·n³)) — documented and accepted in design/verify; no behavioral follow-up opened.
- **`getErrorFinal()` exposes NaN** on non-finite runs (was the `0.0` lie) — accepted; spec R2 forbids sanitization, controller masks the UI, `Convergencia` is display-only.

## 6. NEXT-CHANGE HANDOFF — `trace-label-fixes` (archives second)

- **Live suite at HEAD is 82 tests / 12 classes** (`sh ./mvnw -o clean test` → 82/0/0, confirmed at verify and again at this archive's baseline).
- **Its R5 delta already pins 82** — `openspec/changes/trace-label-fixes/specs/test-execution-baseline/spec.md` (MODIFIED R5: 12 classes / 82 methods, reconciled at apply after this change's +9 landed). No re-pin needed; merge it as-is into `openspec/specs/test-execution-baseline/spec.md` (replace R5, preserve R1–R4/R6–R8).
- **It must NOT merge `result-honesty`** — that capability belongs to the separate, still-pending change `honest-results` (its own specs/, its own archive later). trace-label-fixes' delta tree contains only `procedure-trace` + `test-execution-baseline`; keep it that way.
- **`openspec/config.yaml` already says 82** (context line 15 and runner comment line 26, updated inside `d83d63e`) — **no edit needed**.
- Its verify-report exists; it goes straight to its own archive after its spec merge, with the same dated-prefix convention.

## 7. Pending Changes Register (after this archive, 2026-11-17)

| Change | State | Remaining phases |
|---|---|---|
| `trace-label-fixes` | applied + verified (verify-report.md present) | **archive next** — owns the R5 supersession text; also merges `procedure-trace` (new capability) |
| `honest-results` | applied (tasks/design/proposal/specs present; no verify-report yet) | **verify + archive** — owns the `result-honesty` capability merge |
| `app-end-to-end` | exploration only (`explore.md`) | tracker for GUI end-to-end follow-up — **no archive yet**, no spec/apply/verify done |

## 8. SDD Cycle Status

`mejoramiento-iterativo` is fully proposed, specced, designed, tasked, applied (`901bd84` + `70a0602`), verified (pass_with_warnings), and archived. Its capability contract now lives in `openspec/specs/mejoramiento-iterativo/spec.md`.

**Next recommended**: `sdd-archive` for `trace-label-fixes` (locked ordering, second), then `sdd-verify` for `honest-results`.
