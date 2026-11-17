# Archive Report: Honest Results

- **Change**: `honest-results` (outcome contract: `Estado`/`Resultado`, classification precedence, state-derived messages, state-driven rendering)
- **Capabilities**: `result-honesty` (new, R1–R7), `test-execution-baseline` (R2 MODIFIED supersession)
- **Archived**: 2026-11-17
- **Archive path**: `openspec/changes/archive/2026-11-17-honest-results/`
- **Repository state at archive**: `main` @ `96f0b7d`
- **Commits under this change**: `8e0f854` (controller + tests, squashed) + `6ab9fb9` (view + CSS + config) on base `3f2fd7e`
- **Verification verdict**: `pass_with_warnings` — zero CRITICAL; 14/14 scenarios COMPLIANT (3 with the documented view-side caveat); warnings carried into the live specs and §4/§6 below
- **Mode**: openspec, Strict TDD active (`openspec/config.yaml testing.strict_tdd: true`, flipped by this change)
- **Ordering**: THIRD SDD closure of the project cycle — after `mejoramiento-iterativo` and `trace-label-fixes` (both already archived; `establish-build-test-baseline` archived before that pair). This archive executes the reconciliation both siblings deferred to it: `honest-results` owns the `result-honesty` merge AND its own R2 supersession (collision-checked: its baseline delta modifies R2 only; the siblings' R5 re-pin stands).

---

## 1. What Was Delivered

1. **Outcome contract (R1–R5)** — `SistemaController.Estado {OK, NO_CONVERGIO, NO_FINITO, ERROR}`; `Resultado.estado` as single source of truth; `Resultado.clasificar` with structural `ERROR` → non-finite `NO_FINITO` (solution nulled) → `NO_CONVERGIO` (last iterate kept) → `OK` precedence; messages derived from state (success text only for `OK`); `isExitoso()`/`isAdvertencia()` retained as derivations, no flags.
2. **State-driven rendering (R6)** — pure JavaFX-free mapper `SistemaController.textoDe(Estado)` → `TextoEstado(titulo, barraEstado, claseCss)` with spec-exact Spanish copy; `SistemaFXController.mostrarResultado` refactored to a single exhaustive `switch (estado)` with null-guard before formatting; three CSS state classes appended to the existing `estilos.css` (2-asset rule intact).
3. **Honest classification in the controller only (R7)** — solvers keep returning raw `NaN`/`±Infinity`; zero `src/unsa/sistemas/model` diff.
4. **The lie rewritten** — `verificaJacobiSinDominanciaDiagonal` (which asserted `isExitoso()` for a diverging Jacobi) now demands `NO_CONVERGIO`; 7 new test methods (14 → 21 in `SistemaControllerTest`).

## 2. Gates and Suite Arithmetic

| Gate | Command | Observed |
|---|---|---|
| Planned gate at own tip | `sh ./mvnw -o clean test` on rebuilt `6ab9fb9` (/tmp) | **59/0/0**, 11 `TEST-*.xml` — exactly the design pin (52 + 7) |
| Re-measured at HEAD `96f0b7d` (siblings included) | `sh ./mvnw -o clean test` ×3 independent runs | **82/0/0**, 12 reports, BUILD SUCCESS |
| Drift attribution | class-by-class rebuild | +23 tests / +1 class fully sibling-attributed (`mejoramiento-iterativo`, `trace-label-fixes`); contract unaffected |
| Mutations (verify, re-run at HEAD) | classifier / mapper copy / finiteness detector | 3 red, 1 red, 5 red respectively; each restored, re-gated 82/0/0 |
| Behavior probes | `HonestProbe.java` vs `target/classes` | 12/12 pass |

`openspec/config.yaml` :15/:26 already pin 82/12 (updated in the sibling chain) — **no config edit at archive**; this is consistent with the superseded R2 formulation (counts live in `config.yaml`, not in the spec).

## 3. Specs Synced (the exact reconciliation prescribed by verify-report.md)

| Domain | Action | Details |
|---|---|---|
| `result-honesty` | **Created** (new capability; delta was a full ADDED spec) | R1–R7 + all 14 scenarios (R1.1–R7.1) verbatim, ids preserved; no count pins in any scenario (grep-verified, per reconcile note 2); provenance: origin honest-results, commits `8e0f854` + `6ab9fb9` (squashed, base `3f2fd7e`), sync 2026-11-17, verdict `pass_with_warnings`; WARNING notes carried into the header |
| `test-execution-baseline` | **Updated** (1 requirement MODIFIED — R2 supersession) | R2 body: literal-52 pinning → unpinned single-source formulation (expected count maintained in `openspec/config.yaml`, currently 82 across 12 classes; SHALL NOT pin a literal) + `(Previously: pinned the literal Tests run: 52.)` + the why-note; R2.1: `Tests run: <expected>` + AND-line binding to `config.yaml`; R2.2 ids/scenarios preserved |

**Reconciliation registered (per reconcile note 1, verify WARNING-3):** scenario **R2.2 literal `11` → `12` `TEST-*.xml`** — the delta text was written at this change's own era (11 classes at `6ab9fb9`); HEAD measures 12 because sibling `mejoramiento-iterativo` added the 12th class. Merging the stale `11` verbatim would have installed a false count into the source of truth, so the merge applied the count-fix with a previously-note preserved inside the scenario. This also **closes the cross-change reconcile item** flagged in the live `procedure-trace` provenance ("honest-results' Scenario R2.2 cites 11 — owned by that change's own archive"): it is now executed here. R5 (already re-pinned 12/82 by `trace-label-fixes`) and R1, R3–R8 untouched; historical Delivery Mapping untouched (non-normative).

Live specs: `openspec/specs/result-honesty/spec.md` (new), `openspec/specs/test-execution-baseline/spec.md` (R2 block + provenance header).

## 4. Verify Outcome and Carried Warnings

Verdict **pass_with_warnings** (`verify-report.md`, first verify for this change — apply was interrupted before verification). Highlights:

- **RED evidence — reconstructed, not imported**: the apply-time RED transcripts were lost when history was squashed (`1700f45` + `738ef7b` → `8e0f854`). Pre-squash objects survive via the **`backup/pre-squash-honest` tag**, and verify independently re-executed both phases in /tmp against real pre-change code: **RED-a** = `Tests run: 19, Failures: 6` with the exact six planned methods (`expected: <false> but was: <true>` — the lie made red before any production line existed); **RED-b** = compile failure `cannot find symbol` for `Estado` / `getEstado()` / `textoDe`. Stated plainly in the report; nothing hidden.
- **TDD evidence table**: no `apply-progress` artifact ever existed (process gap) — compensated by the reconstruction above plus 3 fresh mutations. No spec scenario left untested.
- **WARNING-2 (carried, unresolved)** — the 8-row manual GUI checklist (tasks 3.7) was **never signed off**; no automated UI oracle exists (TestFX/Monocle absent by design). R6 view wiring rests on mapper test + mutation + source-inspection evidence. Attested into the live `result-honesty` provenance; sign-off row remains in §6.
- **WARNING (carried)** — **no TestFX oracle for the R6 view-application half** ("the view MUST apply it"): verified statically (single exhaustive switch, null-guard-before-formatting, zero success literals in view source). Carried into the live `result-honesty` provenance; scripted GUI pass is a backlog item (§6).
- WARNING-3 (delta R2.2 "11") — **resolved by this archive** (§3). WARNING-4 (stale 59/11-era config pins) — superseded facts; `config.yaml` already 82/12, no action.

## 5. Deviations and Git/Filing Notes

- **Commit shape**: planned 3 commits (RED/GREEN/render) delivered as 2 squashed commits touching the same 5 files; content identical (`git archive 6ab9fb9` rebuild proves green tree). Per-slice commit granularity and its RED transcripts lost — reconstructed at verify (§4).
- **Not touched at archive**: `src/` — the dead javadoc `{@link #exitoConAdvertencia}` (`SistemaController.Resultado` :191, factory deleted by D8) was **not** fixed here; recorded as a one-line backlog fix-forward in §6 per archive instruction (and per verify WARNING-5's same-class nits on two stale test-file javadoc blocks).
- **Git state**: NO state-changing commands executed. `openspec/changes/honest-results/` was untracked → moved with plain `mv`. After the merge, `openspec/specs/test-execution-baseline/spec.md` shows **tracked-modified (uncommitted)** and `openspec/specs/result-honesty/spec.md` **untracked-new**; both sit in the working tree for the orchestrator to commit. `git status` otherwise unchanged (only untracked `openspec/changes/` folders).

## 6. Backlog / Remaining Attestation (open after archive)

| Item | Action | Owner |
|---|---|---|
| Javadoc dead link | Remove/rewrite `{@link #exitoConAdvertencia}` in `SistemaController.Resultado` (:191) — one-line fix in the next controller touch (also two stale test javadoc blocks, verify WARNING-5) | next change touching controller/tests |
| GUI sign-off | tasks 3.7 8-row manual checklist never signed off — run the GUI pass once (fixtures + per-state headings/status/colour), then attest | user, ~10 min, non-blocking |
| Scripted GUI pass | When TestFX + offline cache are available: automate R6 view-application (real FXML + controller) to replace the static-only evidence | future tooling change |

## 7. Archive Contents (audit trail — never modify)

- `proposal.md` ✅ — intent, behavior table, strict-TDD adoption rationale
- `specs/result-honesty/spec.md` ✅ · `specs/test-execution-baseline/spec.md` ✅ — originating deltas (R2.2's raw `11` stays here as written; the merge count-fix lives in the live spec + §3)
- `design.md` ✅ — D1–D9 decisions, measured fixtures, mapper spec, view integration
- `tasks.md` ✅ — 26/27 tasks with evidence; 3.7 (manual GUI) never completed — 0 checkbox markers (slices squashed); per verify Completeness
- `verify-report.md` ✅ — PASS WITH WARNINGS, mutation reproductions, RED reconstruction, exact archive-reconciliation notes
- `archive-report.md` ✅ — this report

Active `openspec/changes/` no longer contains `honest-results`.

## 8. Pending Changes Register (after this archive, 2026-11-17)

| Change | State | Remaining phases |
|---|---|---|
| `app-end-to-end` | exploration only (`explore.md`) | **still open** — tracker for GUI end-to-end follow-ups (incl. its item that honest-results closes contract-side but not click-side: manual sign-off §6); no archive yet |
| Roadmap #5 `file-loading-fixtures` | not started | propose → … — **honest-results supersedes nothing in roadmap #5's favor: parsing `pruebas txt/` fixtures is still needed** (fixtures were only used as hand-fed matrices in probes/tests here) |
| Roadmap #6 `deliverable-packaging` | not started | propose → … |

No other active changes; all four SDD cycles launched in this project (establish, mejoramiento-iterativo, trace-label-fixes, honest-results) are now closed.

## 9. SDD Cycle Status

`honest-results` is fully proposed, specced, designed, tasked, applied (`8e0f854` + `6ab9fb9`), verified (`pass_with_warnings`), and archived. The live specs now reflect shipped reality: `result-honesty` is the source of truth for the outcome contract; `test-execution-baseline` R2 no longer pins counts (single source: `config.yaml` = 82/12) and R2.2 reports the measured 12.

**Next recommended**: orchestrator commit of the spec edits + archive move; then user decision between the `app-end-to-end` tracker follow-ups (manual GUI sign-off, ~10 min) and roadmap #5 `file-loading-fixtures` as the next full SDD cycle.
