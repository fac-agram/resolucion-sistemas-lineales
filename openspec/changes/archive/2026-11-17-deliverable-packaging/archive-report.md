# Archive Report: Deliverable Packaging

- **Change**: `deliverable-packaging` (roadmap #6 of `app-end-to-end`; runnable zip for machines without Java/Maven + README truth)
- **Capability**: `deliverable-packaging` (new, R1–R8; 21 scenarios) — no live spec modified by the delta or by this merge (R7.2)
- **Archived**: 2026-11-17
- **Archive path**: `openspec/changes/archive/2026-11-17-deliverable-packaging/` (evidence/ included, unmodified)
- **Repository state at archive**: `main` @ `e78a3c2`
- **Commits under this change**: `a12f14f`, `c91c9c8`, `dcc665c`, `e78a3c2` on base `d7656fa` (4 work-unit commits, main-only flow, zero PRs)
- **Verification verdict**: `pass_with_warnings` — zero CRITICAL; R1–R7 fully reproduced green by independent adversarial verify; R3-runtime + R8-flip remain honestly UNVERIFIED (no Windows host — by design). Warnings carried into the live spec provenance (§4).
- **Mode**: hybrid — openspec filesystem merge + move executed here; archive report persisted to Engram (`sdd/deliverable-packaging/archive-report`). Regression mode (strict TDD N/A for this change; the delta fixes the verification model: script hooks + archived evidence + R8 attestation).
- **Closure position**: sixth change closure of the project era — the fifth formal SDD cycle archived (establish-build-test-baseline, mejoramiento-iterativo + trace-label-fixes, honest-results before this; gui-launch-fix was a direct fix, never a formal cycle). Only `app-end-to-end` (exploration tracker) stays active.

---

## 1. What Was Delivered (per work unit, measured lines)

| Commit | Message (verbatim, Spanish) | Paths | Text lines | Forecast |
|---|---|---|---|---|
| `a12f14f` | `refactor: renombrar ConsoleView a Launcher y fijar Main-Class en el jar` | 3 — `Launcher.java` (+21, rename of `ConsoleView.java` −7), `pom.xml` (+10, `maven-jar-plugin` 3.4.1 `Main-Class`) | 31+/7− | ~40 ✓ |
| `c91c9c8` | `build: agregar package.sh con runtime linux y app-image verificado` | 2 — `package.sh` (+259, stages 1/2/3/6/7, stubs fail-fast), `.gitignore` (+3, `dist/`) | 262+/0− | ~120 (under 400, no flag) |
| `dcc665c` | `build: ensamblar la carpeta windows con runtime Temurin pinneado` | 5 — `package.sh` (stages 0/4/5, +337/−59 within file), `package/{pins.txt,Iniciar.bat,LEEME.txt,CHECKLIST-WINDOWS.txt}` | 443+/46− ≈ 489 changed | ~230 → **WARNING-1** |
| `e78a3c2` | `docs: reescribir README con instrucciones reales y empaquetar el zip final` | 2 — `README.md` (94+/37−, honest run instructions + attestation matrix), `evidence/linux-smoke-window.png` (binary +55,831 B — first binary in repo, accepted exception per task 4.2) | 94+/37− + 1 binary | ~250 ✓ |
| **Total vs base** | `git diff --stat d7656fa..e78a3c2` | **exactly 11 tracked paths**, all within the allowed set | **784+/44−** | ~640 |

## 2. Specs Synced (source of truth updated)

| Domain | Action | Details |
|---|---|---|
| `deliverable-packaging` | **Created** (new capability; delta was a full ADDED spec) | R1–R8 + all 21 scenarios (R1.1–R8.2) copied **verbatim, ids preserved — diff-verified byte-identical at merge time** (only `## ADDED Requirements` → `## Requirements` normalized per live-spec convention). Q1-corrected text (apply task 0.2, design D5/E-jfx-win) carried inside R3 including its inline rationale comment. Provenance header: origin change, commits `a12f14f..e78a3c2` on base `d7656fa`, sync 2026-11-17, verdict `pass_with_warnings`, carried WARNING-1/2 + UNVERIFIED rows as **non-normative notes — requirement text not weakened**. |
| `test-execution-baseline` · `result-honesty` · `mejoramiento-iterativo` · `procedure-trace` | **NOT modified — confirmed** | Delta had no MODIFIED/REMOVED sections (R7 purity). `git diff --stat -- openspec/specs/` at archive time: zero lines on the four pre-existing live specs. |

Live spec: `openspec/specs/deliverable-packaging/spec.md` (new). Post-archive note recorded in its provenance: R8.1's change-folder path is now durably at `openspec/changes/archive/2026-11-17-deliverable-packaging/`; `package.sh` recreates the live-path copy via `mkdir -p` (`package.sh:378`), so the R8.1 hook stays satisfiable as written — see §6 finding F3.

## 3. Gates and Zip Identity (verify numbers, carried as audit record)

- **Regression gate**: `sh ./mvnw -o clean test` → **82/0/0 (12 classes) at every commit** (apply: all 4) and on **every one of the 4 independent verify executions**, including after mutation probes and inside the clean worktree clone. The suite never moved across the whole chain.
- **Zip identity across the 6 verify builds** (apply-final + 5 verify builds incl. clean rebuild, determinism ×2, clone run, final restore): **507 entries in every build**; exactly two root folders `TrabajoIntegrador-linux` + `TrabajoIntegrador-windows`; sorted name lists diff-empty; byte size band **89,972,745–89,972,781 B (~85.8 MiB)** — the ±36 B spread is zip entry mtimes (determinism = names per R1, bytes explicitly out of scope); per-build sha256 differs accordingly (`06bd9c74…`, `d7896d9a…`, `871021b4…`, `45bf7eef…`, `c71f9782…`, `7d54cf73…`). Determinism run #3 matched apply's size byte-for-byte. README's "~86 MB" is true. Zip >25 MB → USB/Drive/campus hand-in (R6). No zip/`dist/` ever tracked (`git ls-files dist/` empty; `.gitignore` `dist/`).
- **Smoke (R2.2)**: exit 124 at 12 s, `xwininfo` `IsViewable`, clean kill, no strays — reproduced by the verifier independently of the script's stage 6. Headless → exit 4 `UNVERIFIED (no display)`, never fake PASS (R2.3). Fail-fast (R1.2) and corrupt-cache/pins-mutation (R5.1) probes exit 1/2 with no zip left behind — all reproduced.
- **Windows static (R3)**: 4 `-win` jar sha256 + Temurin sha256 recomputed **from within the zip** == `package/pins.txt`; in-zip bat 3/3 `%~dp0` quoted; folder 136 MB ∈ documented 120–160 bounds (design D2); log label `static-verified / runtime-UNVERIFIED`. Runtime behavior stays UNVERIFIED (R3 normative, §5 open loop).

## 4. Verify Outcome and Carried Warnings (into live spec provenance)

- **WARNING-1 (advisory)**: `dcc665c` ≈ 489 changed lines vs ~230 forecast (total 784+/44− vs ~640). Main-only flow — no PR existed to breach the 400-line review budget; compliance unaffected.
- **WARNING-2 (narrative hygiene)**: committed evidence PNG is 900×650 while current fresh builds render 1100×731 (~59 KB). The PNG itself is **authentic app-GUI evidence** (window title, Spanish labels, input/result panels — visually verified) and R2.2-compliant (a saved screenshot is required, not a byte-stable one). Standing rule carried: **never repeat byte-identity/deterministic-render claims for screenshots** — shared-DISPLAY bytes drift.
- **UNVERIFIED rows (normative, stay until flip)**: Windows runtime execution (R3 static-only + R8 attestation is the only flip path) and the `--fetch` codepath (never executed live; gating proven by source inspection + 5 offline builds).

## 5. Evidence sha256 (audit trail inside the archive)

| Artifact | sha256 | Notes |
|---|---|---|
| `evidence/linux-smoke-window.png` | `bbc9d887c31d3df73417b2f3778599626e30e10737b7b9d224f24476ee259d9b` (55,831 B, 900×650 RGB) | Committed at `e78a3c2`; hash claim verified by apply and verify. WARNING-2 applies: authentic GUI evidence, superseded by newer builds at 1100×731 — evidence of *a* real launch, not of byte-stable rendering. |
| `CHECKLIST-WINDOWS.txt` (archived copy) | `b201f72c3d2f25faedeeb0b5e0522867984ef20db1e0eb27d8c51dabc5d7936f` | **Byte-identical three ways** (verify R8.1): extracted-from-zip == this archived copy == committed `package/CHECKLIST-WINDOWS.txt`. Neutral Spanish end-user handout (documented product exception, tasks 0.3). |

## 6. Open Loops and Register (after this archive)

1. **R8 Windows attestation — THE open loop.** Owner: a Windows user (classmate). Steps: unzip the deliverable on a real Windows machine → run `TrabajoIntegrador-windows\Iniciar.bat` → solve a `pruebas txt/` fixture → load a file through the GUI → verify result, procedure-trace and status panels → fill `CHECKLIST-WINDOWS.txt` (~5 min, shipped inside the zip AND archived here) → **return it**. Receiver archives the returned checklist. **Only then** does Windows runtime flip UNVERIFIED → VERIFIED (R8.2); until that document physically returns, every artifact must keep saying UNVERIFIED — verify found zero PASS-faking anywhere.
2. **Roadmap register** (`app-end-to-end` six ordered changes): #1 `gui-launch-fix` — DONE as direct fix `3f2fd7e` (never a formal cycle) · #2 `honest-results` — archived (`185c09e`) · #3 `mejoramiento-iterativo` — archived (`96f0b7d`) · #4 `trace-and-label-fixes` — archived as `trace-label-fixes` (`96f0b7d`) · #5 `file-loading-fixtures` — **NOT STARTED; the only roadmap item remaining** (propose → … when the rubric needs fixture-driven acceptance) · #6 `deliverable-packaging` — **DONE (this archive)**.
3. **Forward findings (non-blocking)**: (F1) verify SUGGESTION-1 — `tasks.md` 5.3 says "12 changed/created paths"; the tracked diff is exactly **11** (the 12th, the archived checklist copy, was untracked like all openspec working files). Recorded here rather than editing the archive: **the archive is an audit trail — never modify it** (skill rule; consistent with honest-results precedent where R2.2's raw `11` stays as written and the correction lives in the live spec + report). (F2) README could note that `DISPLAY` is needed for stage 6 (headless build machines get exit 4 — correct per R2.3, but confusing mid-README). (F3) Future packaging touches should repoint `package.sh` `CHANGE_DIR` (line 56) to the archive path — currently any rebuild recreates `openspec/changes/deliverable-packaging/` (untracked, checklist copy only) via `mkdir -p`; harmless, but don't mistake it for an active change. (F4) `--fetch` should be exercised once on a cache-free machine (R5.2 offline path is what's proven here). (F5) `git show -M a12f14f` renders the rename as D+A (javadoc pushed similarity under the threshold); `git log --follow` traces it — cosmetic only.

## 7. Project State Snapshot (for the next session)

- **Live specs: 5 capabilities** (actual count of `openspec/specs/`): `test-execution-baseline`, `result-honesty`, `mejoramiento-iterativo`, `procedure-trace`, **`deliverable-packaging`** (new). Not 9 — only archived SDD cycles produce live specs and this was the 5th cycle.
- **Archived changes: 5** (now incl. `2026-11-17-deliverable-packaging`). **Active changes: 1** — `app-end-to-end` (explore-only tracker; nothing to archive).
- **Code/HEAD**: `main` @ `e78a3c2`; suite **82/0/0 across 12 classes** (pinned in `openspec/config.yaml`); `Launcher` (ex-`ConsoleView`) is the packaged main class; `package.sh` + `package/` assets committed; `dist/` gitignored.
- **Deliverable**: `dist/TrabajoIntegrador.zip` (~85.8 MiB, 507 entries) regenerable offline with valid `~/.cache/gentle-deliverable` cache (51 MB, hashes re-checked at tasks 0.1); Linux VERIFIED (smoke + screenshot), Windows static-verified/runtime UNVERIFIED until §6.1 returns; hand-in via USB/Drive/campus (email limit).
- **Honesty posture intact**: the R8 flip lives OUTSIDE the SDD lifecycle — a returned, archived `CHECKLIST-WINDOWS.txt` only updates status labels (README matrix + live spec provenance); no new change needed for it.

## 8. Rollback

Per-chain: `git revert` in reverse order **`e78a3c2` → `dcc665c` → `c91c9c8` → `a12f14f`** returns the tree to base `d7656fa`; each revert is independent and the suite stays green throughout (rename revert restores `ConsoleView` — zero consumers). The zip/`dist/` never entered git, so no history bloat to undo. This archive's own bookkeeping (live spec + move) commits separately and reverts cleanly with it (`git revert` on the archive commit — openspec docs only, zero code impact).

## 9. Git State Expected by the Orchestrator (NO git state changed by this phase)

Verified against `git ls-files`/`git status` after the merge+move:

- **The old change dir was NOT fully untracked**: exactly **one tracked file** — `openspec/changes/deliverable-packaging/evidence/linux-smoke-window.png` (committed at `e78a3c2`). It now shows ` D` (deleted at old path). All other artifacts (proposal.md, explore.md, design.md, tasks.md, verify-report.md, specs/, CHECKLIST-WINDOWS.txt) were and remain **untracked** — their move is invisible to git beyond being untracked at the new path.
- **After `git add -A openspec/`**, the orchestrator should see, and nothing else:
  - `R` — `openspec/changes/deliverable-packaging/evidence/linux-smoke-window.png` → `openspec/changes/archive/2026-11-17-deliverable-packaging/evidence/linux-smoke-window.png` (rename detected: bytes identical, both sides staged in one commit)
  - `A` — `openspec/specs/deliverable-packaging/spec.md` (new live spec)
  - `A` — `openspec/changes/archive/2026-11-17-deliverable-packaging/{archive-report.md, proposal.md, explore.md, design.md, tasks.md, verify-report.md, specs/deliverable-packaging/spec.md, CHECKLIST-WINDOWS.txt}` (all untracked-before → plain adds, **not** renames)
  - **`M`: none.** The four pre-existing live specs, all code, README, pom, package.sh/`package/`, config.yaml: zero diff from this phase.
- **Suggested commit (Spanish, repo conventional style, matching `96f0b7d`/`185c09e` pattern)**: `docs: archivar deliverable-packaging y publicar su spec viva`

## 10. Archive Contents (audit trail — never modify)

- `proposal.md` ✅ — locked user decisions, scope, deliverable layout, Q1 open question
- `explore.md` ✅ — E1–E10 offline probes (pre-existing evidence base)
- `specs/deliverable-packaging/spec.md` ✅ — originating delta (R1–R8, incl. inline Q1 correction; merged verbatim — diff-proved)
- `design.md` ✅ — D1–D9, pinned artifacts, 7-stage contract, exit codes 0–4
- `tasks.md` ✅ — 26/26 tasks complete, 5 phases
- `verify-report.md` ✅ — PASS WITH WARNINGS, 6-build zip identity, mutation probes, Windows honesty check
- `CHECKLIST-WINDOWS.txt` ✅ — R8.1 archived copy (`b201f72c…`)
- `evidence/linux-smoke-window.png` ✅ — committed R2.2 screenshot (`bbc9d887…`, see §5 WARNING-2 note)
- `archive-report.md` ✅ — this report

Active `openspec/changes/` no longer contains `deliverable-packaging`.

## 11. SDD Cycle Status

`deliverable-packaging` is fully proposed, specced (Q1-corrected), designed, tasked, applied (`a12f14f`→`e78a3c2`, 82 green ×4), verified (`pass_with_warnings`, zero CRITICAL), and archived. The live specs now reflect shipped reality: `deliverable-packaging` is the source of truth for the build-to-artifact contract. **The project's six-change roadmap is one item from complete: #5 `file-loading-fixtures` (not started).** The single open loop is manual and external: the returned Windows checklist (§6.1).

**Next recommended**: orchestrator commit of the merge + move (§9); then either close the R8 loop when the checklist returns, or launch `sdd-propose` for roadmap #5 `file-loading-fixtures` — the last remaining roadmap item.
