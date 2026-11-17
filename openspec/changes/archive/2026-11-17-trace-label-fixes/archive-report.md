# Archive Report: Trace Label Fixes

- **Change**: `trace-label-fixes` (retroactive formalization of `8b40d42` + remaining R4 iteration-count fix and R2.2 no-op suppression)
- **Capabilities**: `procedure-trace` (new), `test-execution-baseline` (R5 MODIFIED supersession)
- **Archived**: 2026-11-17
- **Archive path**: `openspec/changes/archive/2026-11-17-trace-label-fixes/`
- **Repository state at archive**: `main` @ `d83d63e`
- **Commits under this change**: `c09f8ed` (fix: unificar el conteo de iteraciones en relajacion simple) + `d83d63e` (fix: suprimir pasos de normalizacion redundantes en gauss-jordan) on base `70a0602`
- **Verification verdict**: `pass_with_warnings` — zero CRITICAL; WARNING-1 carried into the live `procedure-trace` spec as a PARTIAL attestation note; WARNING-2/3 and suggestions carried as notes (details in §4)
- **Mode**: openspec, Strict TDD active (`openspec/config.yaml testing.strict_tdd: true`)
- **Ordering**: SECOND in the locked ordering — sibling `mejoramiento-iterativo` archived first (pure-model slice); this change executes the joint R5 supersession whose delta TEXT it owns per design D5.

---

## 1. What Was Delivered

1. **Unified iteration count (R4)** — `RelajacionSimple` broke on convergence BEFORE assigning `iteraciones = k`, undercounting the convergence-proving pass (20 vs true 21). One-line move (`:122` → after `:103`, before stop check); `errorFinal` semantics preserved (6.615e-08 measured). Declared spec drift: `RelajacionSimpleTest.java:45` re-pinned `totalPasos == iteraciones` (RED first, rationale in commit body); siblings' `+1` pins untouched and still true.
2. **Cosmetic no-op suppression (R2.2)** — Gauss-Jordan division+record wrapped in `if (|pivote − 1| > EPS)` guard; `indice++` consumed only when recorded → monotonic, gapless. Over-suppression guard test proves pivot-2.0 normalization after swap still records.
3. **New coverage** — 6 `@Test` (0 classes): exact-k 21 pin, cap 1000/1001 characterization, swap-first indice-1 pin (EG), no-op suppression, swap-normalize guard, `Paso` snapshot-immunity (R6 contractualization, `Paso.java` untouched).

## 2. Gates and Suite Arithmetic

| Gate | Command | Observed |
|---|---|---|
| RED C1 (scoped re-pin run) | `sh ./mvnw -o test -Dtest=RelajacionSimpleTest` | `Failures: 1` — re-pin `expected <true> but was <false>`; full pre-fix run `78, Failures: 2` (re-pin + R4.1 `21 vs 20`); cap test green-on-arrival (exception recorded) |
| GREEN C1 (`c09f8ed`) | `sh ./mvnw -o clean test` | **78/0/0**, 12 classes |
| RED C2 | scoped run pre-GREEN | `Tests run: 82, Failures: 1` — `trazaSinNormalizacionesNoOp` finds "F1 ← F1 / 1.0" |
| GREEN C2 (`d83d63e`) | `sh ./mvnw -o clean test` | **82/0/0**, 12 classes |
| Verify reproduction | 3 independent full runs | 82/0/0 ×3; mutations re-executed (`if (true)` guard → exact apply RED transcript; pre-fix recombination → `21 vs 20` confirmed) |

Locked gates for this change: **78 → 82**. Chain: 67 (`8b40d42`) → 76 (sibling `70a0602`) → 78 (`c09f8ed`) → 82 (`d83d63e` = HEAD). `openspec/config.yaml` :15/:26 already pin 82/12 (updated inside `d83d63e`) — **no edit at archive**.

## 3. Specs Synced

| Domain | Action | Details |
|---|---|---|
| `procedure-trace` | **Created** (new capability; delta was full ADDED spec) | 6 requirements (R1–R6), 8 scenarios, ids preserved verbatim; R5.1 PARTIAL verification note added at merge |
| `test-execution-baseline` | **Updated** (1 requirement MODIFIED — R5 supersession) | R5 re-pinned 11 classes/52 methods → **12 classes / 82 methods**; previously-note chain preserved (11/52 → 12/67 delta-time → 12/76 sibling → 12/82); scenario R5.1 renamed "All twelve classes discovered" with the `Tests run: 82` AND-line; R1–R4, R6–R8 and historical Delivery Mapping untouched |

Live specs: `openspec/specs/procedure-trace/spec.md`, `openspec/specs/test-execution-baseline/spec.md`. Both provenance headers record origin change, archived source path, sync date 2026-11-17, verdict `pass_with_warnings`.

**NOT merged here (ownership discipline):** `honest-results`' deltas — the `result-honesty` capability AND its own R2 supersession of `test-execution-baseline` (R2 still cites the historical 52 methods / 11 `TEST-*.xml`; reality 82/12 — reconcile belongs to that change's archive, collision-checked: its baseline delta modifies R2 only).

## 4. Verify Outcome and Carried Warnings

Verdict **pass_with_warnings**; 11/12 scenarios COMPLIANT, 1 PARTIAL, 0 failing. Beyond the planned "manual checklist", verify built a **JavaFX harness** (`GuiProbe.java`, real `Sistema.fxml` + `SistemaFXController` on DISPLAY=:1, JavaFX 21.0.13 jars run directly via `--module-path` after offline `javafx:run` proved plugin-blocked) that asserts on **actual rendered TextArea text** — upgrading 3 GUI rows from manual to programmatic: **R3.1 label order ✅, R1 numbering ("Paso 1 / Intercambio", "Inicio" only at indice 0) ✅, R2.2 suppression (no "/ 1.0", Paso [1..7] gapless) ✅** — all verified at archive as COMPLIANT without human action.

- **WARNING-1 → R5.1 file-load sub-path PARTIAL.** Manual-load path verified live through the real handler chain; the file-load clear block (`SistemaFXController.java:180-183`) is statically identical but FileChooser is a native dialog, not headlessly automatable. **Human attestation pending: ~30s click-through — solve A → load file B → both panels empty, procedure pane collapsed+disabled.** Recorded in the live `procedure-trace` spec (verification-status note) and as the remaining attestation row in §6. Accepted at archive as static-identical per the verify disposition.
- **WARNING-2 → flake watch carried.** Apply's transient flake did NOT reproduce in 3 fresh full runs (nor apply's own re-runs). Standing habit: if it ever reddens, capture `target/surefire-reports` BEFORE any `clean`.
- **WARNING-3 → TDD evidence format.** Prose + commit-body transcripts instead of the literal strict-module table; substance independently reproduced at verify. No spec impact.
- **SUGGESTIONS (no action at archive):** `contains("/ 1.0")` substring over-match edge (unreachable with deterministic fixtures; note for future parametric tests); EG's zero-factor cosmetic steps (`F2 ← F2 - (0.0) F1`) — same smell R2 fixed in GJ but out of R2's Gauss-Jordan scope, candidate future change; FXML "API of version 26 by runtime 21.0.13" warning pre-existing, `Sistema.fxml` untouched.

## 5. Deviations and Git/Filing Notes

- **Commit 2 size**: 158 lines vs design D6 estimate ≈90 — disclosed by apply, within the 400-line review budget; chained PRs not warranted.
- **1 speculative assertion dropped** (apply deviation #4, upheld by verify): tautological op-label check; the delivered R1.1 pin is strictly stronger than spec.
- **Tracking anomaly at move**: `openspec/` is untracked EXCEPT the earlier-merged live specs and `config.yaml`. `openspec/specs/test-execution-baseline/spec.md` therefore shows as **tracked-modified (uncommitted)** after the R5 merge, and `openspec/specs/procedure-trace/spec.md` as untracked-new. Per the no-git-state-change instruction, NOTHING was committed at archive — the commit (or discard) of these spec edits is left to the user's normal flow. The change folder itself was untracked → moved with plain `mv`, no git commands.

## 6. Remaining Attestation Row (open after archive)

| Row | Action | Est. | Owner |
|---|---|---|---|
| procedure-trace R5.1 file-load | GUI click-through: solve A → load file B → result/procedure panels empty, pane collapsed+disabled | ~30s | user |
| Flake watch | if any run reddens, save `target/surefire-reports` before `clean` | — | whoever runs the suite |

## 7. Archive Contents (audit trail — never modify)

- `proposal.md` ✅ — retroactive-formalization framing, joint-R5 ownership
- `specs/procedure-trace/spec.md` ✅ · `specs/test-execution-baseline/spec.md` ✅ — originating deltas (R5.1 in the baseline delta keeps its raw text; merge notes live in the live specs)
- `design.md` ✅ — probe measurements, D1–D6 decisions, GUI checklist
- `tasks.md` ✅ — 19/19 tasks `[x]` (all three phases complete)
- `verify-report.md` ✅ — adversarial re-verification, PASS WITH WARNINGS, mutation reproductions, harness upgrade
- `archive-report.md` ✅ — this report

Active `openspec/changes/` no longer contains `trace-label-fixes`. `git status` at archive: HEAD `d83d63e`, no tracked changes beyond the §5 spec files; working tree otherwise clean.

## 8. Pending Changes Register (after this archive, 2026-11-17)

| Change | State | Remaining phases |
|---|---|---|
| `honest-results` | applied (proposal/design/tasks/specs incl. its `result-honesty` + R2 baseline deltas; no verify-report yet) | **verify + archive next** — owns `result-honesty` merge AND its own R2 supersession (reconcile its stale 11/59 counts against reality 12/82 at its archive) |
| `app-end-to-end` | exploration only (`explore.md`) | tracker for GUI end-to-end follow-ups — no archive yet |
| Roadmap #5 `file-loading-fixtures` | not started | propose → … (parses `pruebas txt/`, deferred since establish-build-test-baseline R8) |
| Roadmap #6 `deliverable-packaging` | not started | propose → … |

## 9. SDD Cycle Status

`trace-label-fixes` is fully proposed, specced, designed, tasked, applied (`c09f8ed` + `d83d63e`), verified (pass_with_warnings), and archived. Second in the locked ordering — the joint R5 supersession is now executed; both live specs reflect shipped reality (82 tests / 12 classes).

**Next recommended**: `sdd-verify` for `honest-results`, then its archive; user click-through of the R5.1 file-load row whenever convenient (~30s, non-blocking).
