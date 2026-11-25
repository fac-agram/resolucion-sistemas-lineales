# Archive Report — `vista-sistema-matriz`

- **Archive executed**: 2026-11-25 · executor: sdd-archive · artifact store: **hybrid** (filesystem + Engram)
- **Verdict archived**: PASS WITH WARNINGS — 0 CRITICAL, 4 WARNING (W1–W4), 3 SUGGESTION (verify-report.md)
- **Branch reality**: `feature/tunable-parameters` @ HEAD `5c90456` (6 commits ahead of origin, **not merged**, `main` untouched at `59f5777`; nothing pushed by this archive)

## Date ruling (deviation from launch text — documented loudly)

The launch contract said "sync 2026-11-19" / "archive/2026-11-19-...". That date is **not this change's** — it
is the `parametros-ajustables` verify-session date (its verify-report.md line 5). Every `vista-sistema-matriz`
artifact is dated 2026-11-23 (proposal/spec/design/apply/verify, Engram timestamps #123–#129), and the sync
itself runs today. The skill rule mandates today's ISO date for archive folders. **Decision: `2026-11-25`**
for the folder name and the `Synced:` provenance line. Verified no breakage: `vista-solucion-matriz/proposal.md`
references the predecessor by change name and commits only — no dated path exists in any artifact.

## Commits delivered (audit chain)

| Commit | Subject | Scope |
|---|---|---|
| `619ae38` | `refactor: extraer los textos de vista a una clase pura` | C1: `TextosVista` (new) + `TextosVistaTest` (7 tests, RED-first) + controller delegation; FXML zero-diff; ~285 lines |
| `5c90456` | `feat: alternar entre vista de ecuaciones y matricial` | C2: FXML selector + zonas + `areaMatrizAumentada`, controller mode state/listener/`aplicarVista`, limpiar trio, README; ~140 lines |

Base: `da55414` (106/14). Suite chain on this branch: **82/12 (main) → 105/14 (parametros-ajustables) →
106/14 (da55414) → 113/15 (this change)**. Each commit < 400 review lines; no PR slices (branch stacking,
`ask-on-risk`, nothing to ask).

## Gates at archive time

- Verify re-executed everything adversarially: suite 113/0/0 across 15 classes; final re-gate after all
  mutations identical; all 7 byte-pins verbatim across design/tasks/test; mutation matrix M-A/M-B1/M-B2
  CAUGHT headless, M-C1-full/M-C2 CAUGHT live, M-C1-minimal honest non-behavioral gap (W1); R3.1 first-ever
  headless pin confirmed; fx:id parity 6/6; render-always audit clean; scope hygiene (pom/estilos/specs/solvers
  zero-diff).
- **Suite NOT re-run during this archive — deliberate**: the working tree carries a pre-existing dirty file,
  `test/MejoramientoIterativoTest.java` (package declaration removed → flat-style WIP, not authored by this
  archive; untouched). A clean run today would measure that WIP, not `5c90456`, and would wipe `target/classes`
  the verify harness repro depends on. The truth-of-record for the re-pin is the verify-proven HEAD reality 113/15.
- Task status: 12/16 complete at verify; Phase 3 items re-classified below — **3.3 executed now by this
  archive** (it was an archive-duty task); 3.1/3.2/3.4 remain user-owned.

## Sync actions (Step 2 of skill)

1. **`openspec/specs/view-modes/spec.md` — CREATED** (new capability; delta was ADDED-only, so the delta IS
   the full spec). Body verbatim: R1–R7 + all 16 scenarios byte-identical to the delta (diff-verified).
   Provenance header carries: origin change, commits `619ae38`/`5c90456` on `feature/tunable-parameters`
   base `da55414`, synced 2026-11-25, verdict pass_with_warnings.
   - **W2 note carried in provenance**: selector/pane-adjacent UI surface (R1/R4/R5/R6) has zero automated
     coverage by spec design; all 6 CHECKLIST-GUI rows mechanically attested headful under Xephyr via
     `/tmp/opencode/verify-vsm` (driver + 26 PNGs + byte assertions on live scene-graph state); user personal
     sign-off remains OPTIONAL backlog (task 3.1), not a gate.
   - W1 nuance also carried (D6 minimal-variant uncatchability — honest wording per verify's recommendation
     that row (5) must not be sold as guarding the redundant call itself).
2. **Other live specs: untouched** — procedure-trace R3.1 preserved with no delta (as the change claimed),
   result-honesty/mejoramiento-iterativo/deliverable-packaging/test-execution-baseline unchanged.

## `openspec/config.yaml` re-pin (task 3.3 duty — executed here, numbers only)

- Line 15: `expect Tests run: 82` → **`Tests run: 113`**
- Line 26: `# expected: 82 tests across 12 classes` → **`# expected: 113 tests across 15 classes`**
- `strict_tdd: true` untouched; no structural edits. Truth source per baseline R2: config is THE single
  source for the expected total; the delivered HEAD (5c90456) is the archive target, and the successor
  change's gates need the correct working number today (verify S1).

## R5 decision (the loud one) — baseline snapshot DEFERRED to merge-time

**One consistent rule, stated and applied:** *capability specs and the config.yaml single-source number
follow the delivered branch HEAD at archive time; the literal suite snapshots inside the live
test-execution-baseline (R5, R5.1, R2.2) follow merged-mainline reality and are re-pinned only at a
merge-time sync step.*

Facts verified at archive (correcting the launch note): **`main` is 82 tests / 12 classes**, not "82/14"
(`git ls-tree main -- test/` = 12 `*Test.java`; the live baseline R5 pin 12/82 is TRUE of main today).
The 14-class reality belongs to the branch at `da55414` (parametros added `ParametrosTest` +
`ValidadorParametrosTest`); this change's `TextosVistaTest` makes 15/113 at `5c90456`.

Why the split:
- Re-pinning live R5 to 15/113 today would make the **source-of-truth spec false for a `main` checkout**
  (main cannot reproduce 113/15; merge decision C2-752 is still user-owned). Every historical R5 re-pin in
  this repo was executed against merged code at its own HEAD — same semantics, so the rule matches precedent.
- config.yaml is by design the *working* gate number (baseline R2 deliberately un-pinned the literal so the
  number moves with active development); keeping it at 82 would misgate every apply/verify on the active
  branch — including the successor's 113/15-frozen gate.

**Supersession record (prevents a double-edit later):** `parametros-ajustables` is **NOT archived** (still
in `openspec/changes/`, verify-report exists, its T4.4 "archive supersession: R5/R5.1 → 105/14 + config
re-pin" was never executed). This archive's deferred merge-time duty — **re-pin live baseline R5/R5.1/R2.2
to the suite measured at merge** — SUPERSEDES that pending 105/14 duty: the chain is cumulative on one
branch (82/12 → 105/14 → 106/14 → 113/15), so 113/15-plus subsumes every intermediate claim. **Nobody
should ever re-pin to 105/14 or 106/14.** At merge time, re-measure (not copy 113): the successor
`vista-solucion-matriz` may add classes before the merge lands. When `parametros-ajustables` is eventually
archived, its T4.4 must be satisfied by pointing at this record + executing the tunable-parameters delta
sync, not by a second baseline edit.

Known, bounded inconsistency window: live baseline R5 says 12/82 while config says 113/15 — intentional per
the rule, resolves at the merge-time sync step. R2/R2.1 (the normative gate) reference config, not R5's
snapshot, so no phase reads a contradiction as a pass/fail input.

## Archive move (Step 3) + verification (Step 4)

- `openspec/changes/vista-sistema-matriz/` → **`openspec/changes/archive/2026-11-25-vista-sistema-matriz/`** (filesystem move; the whole dir was untracked, so git state is untouched).
- [x] Main spec published correctly (diff-verified verbatim body; 7 requirements / 16 scenarios)
- [x] Change folder moved; contains ALL artifacts: proposal.md, explore.md, design.md, tasks.md, verify-report.md, CHECKLIST-GUI.txt (moved with the folder per verify S3), specs/view-modes/spec.md
- [x] Active `openspec/changes/` no longer has this change
- [x] Archive treated as audit trail: nothing deleted, nothing edited retroactively (tasks.md still shows 12/16 + Phase-3 checkboxes as verified; 3.3 completion is recorded here and in config, not by rewriting the frozen tasks file)

## Obsolete-by-successor register (forward cross-reference — honest, not preemptive)

`vista-solucion-matriz` (active, proposed 2026-11-25) builds on this live spec — its own proposal states the
MODIFIED delta needs this base and flags **R1 (view-mode state) and R6 (selector UI) as obsolete-by-design**
(machinery deleted: selector row, `zonaMatricial`, `VistaModo`, listener, D6 trio), CHECKLIST rows 1–3 and
5–6 of this change die with it (row 4's params-parity behavior survives, re-framed single-mode), **R4**
survives as a content invariant with re-scoped targets (load-side hidden-but-current moves to the results
pane), **R5** is rewritten as the pane disable/collapse mirror, **R2/R3/R7 survive verbatim** (the 7
TextosVista byte-pins are a frozen invariant of the successor). This archive did NOT pre-apply any of that:
the published live spec is faithful to shipped HEAD `5c90456`; the successor's archive will sync the
deletions. Archive-order contract now satisfied for its spec phase (its proposal line 37 blocker is resolved).

## Risks carried forward

- **W1** (honest note): D6 explicit `aplicarVista` call is defense-in-depth; uncatchable by any row — recorded in live spec provenance so nobody cites row (5) as its guard.
- **W2**: zero automated UI coverage remains for the C2 surface; headful attestation harness lives in `/tmp/opencode/verify-vsm` (not repo/CI). Keep until 3.1 sign-off (S2). Long-term Monocle/TestFX smoke = future-change backlog (pom stays untouched).
- **W3**: `sh ./mvnw -o javafx:run` unresolvable offline (plugin deps never cached) — accepted infra deviation; attestation used the documented direct `java --module-path` launch. Config verify command remains valid.
- **W4**: WM-less Xephyr keyboard focus unreliable for the native GTK FileChooser — future attestations must drive the chooser pointer-only (logged md5-identical substitution).
- Baseline snapshot vs config divergence until merge-time sync (bounded, rule stated above).
- Branch remains unmerged/undelivered: main ships 82/12 until C2-752 resolves.

## Pending user decisions (not executor-side, not blockers for the successor's spec phase)

1. **3.1 CHECKLIST sign-off** — optional per archive contract: rows 6/6 are mechanically attested (harness + PNGs); personal sign-off is user preference, recorded as such in the live spec.
2. **3.2 Merge decision C2-752** — `feature/tunable-parameters` (now +2 commits: 6 total over main) → merge or not; at merge, execute the single baseline sync step (R5/R5.1/R2.2 → measured suite; supersedes parametros T4.4).
3. **3.4 Package v1.3** — after the merge decision (deliverable-packaging flow).
4. `parametros-ajustables` archive — still owed (tunable-parameters capability not yet in live specs); its baseline duty is discharged by the supersession record above.

## Git-status expectations for the orchestrator

- **No git state-changing commands were run** (no add/commit/checkout/mv-in-index/push; `main` untouched; branch commits untouched).
- Tracked change produced by this archive: **`openspec/config.yaml`** only (two numbers, lines 15 and 26) — shows as ` M openspec/config.yaml`.
- New untracked paths: `openspec/specs/view-modes/` (published capability) and the moved, still-untracked `openspec/changes/archive/2026-11-25-vista-sistema-matriz/` (was `openspec/changes/vista-sistema-matriz/`).
- Pre-existing, untouched: ` M test/MejoramientoIterativoTest.java` (WIP not from this phase), untracked `openspec/changes/{deliverable-packaging,parametros-ajustables,vista-solucion-matriz}/`, untracked `app-end-to-end` content.
- If the orchestrator wants the live spec + archive committed, that is a separate user-owned decision (prior archives committed openspec artifacts; C2-752 pending).

## Engram traceability (hybrid store)

| Artifact | Observation ID |
|---|---|
| explore | #123 |
| proposal | #124 |
| spec | #125 |
| design | #126 |
| apply | #128 |
| tasks | #127 |
| verify-report | #129 |
| archive-report | saved at close (topic_key `sdd/vista-sistema-matriz/archive-report`) — ID in return envelope |

**SDD cycle for `vista-sistema-matriz` is closed** (implementation `5c90456`, verification pass_with_warnings,
capability published, config re-pinned, audit trail archived). Ready for `vista-solucion-matriz` spec phase
on top of the live `view-modes` base.
