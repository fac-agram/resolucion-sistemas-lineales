# Verification Report: deliverable-packaging

- **Change**: `deliverable-packaging` · **Project**: `resolucion-sistemas-lineales`
- **Date**: 2026-11-17 · **Verifier**: sdd-verify executor (adversarial independent reproduction)
- **Base**: `d7656fa` · **Commits under verification**: `a12f14f`, `c91c9c8`, `dcc665c`, `e78a3c2` (HEAD)
- **Mode**: Standard (regression). `strict_tdd` NOT active for this change — the delta spec fixes the verification model: script hooks + archived evidence + R8 attestation are the test layer; JUnit (82 green) guards only the regression surface. `strict-tdd-verify.md` intentionally not loaded. Note: `openspec/config.yaml` carries `strict_tdd: true` globally (from `honest-results`); the orchestrator launch + this change's spec header declare regression mode — authoritative here and consistent with `tasks.md` ("rules.apply.tdd=true is N/A here").

## Verdict: **PASS WITH WARNINGS**

Everything material was independently re-executed and reproduces: full clean rebuild (exit 0), 507-entry zip with exactly the two folders, every static gate, my own smoke launch (exit 124 + IsViewable window), both fail-fast probes, the corrupt-cache and pins-mutation probes (both exit 2, hooks bite), README verbatim from a clean clone, and the unzipped app running with host `java` stripped from PATH. Two warnings: a workload-forecast overshoot (advisory, main-only) and an overstated "deterministic render" claim in the apply narrative over the committed evidence PNG (the PNG itself is genuine app-GUI evidence — verified visually).

## Skill Resolution

`paths-injected` — `sdd-verify/SKILL.md`, `_shared/openspec-convention.md`, `_shared/sdd-phase-common.md`, `references/report-format.md`. `strict-tdd-verify.md` skipped per regression mode (not applicable).

## Completeness

| Metric | Value |
|---|---|
| Tasks total | 26 |
| Tasks complete | 26 (all `[x]`; 0 unchecked — re-counted from `tasks.md`) |
| Tasks incomplete | 0 |

## Build & Tests Execution (independent runs, not read from apply logs)

**Regression suite**: ✅ 4/4 runs → `Tests run: 82, Failures: 0, Errors: 0, Skipped: 0` (surefire XMLs: 82 across 12 classes). Command: `sh ./mvnw -o clean test` ×2 at start, once after all probes, once verbatim inside the clean clone.

**Clean rebuild**: `rm -rf target dist && DISPLAY=:1 bash package.sh` → **exit 0**, 0 `FAIL` lines in log, every `[R*]` hook printed PASS or honest UNVERIFIED.

**Zip identity across rebuilds** (mandate: reproduce, don't trust):

| Run | Bytes | Entries | Roots | sha256 |
|---|---|---|---|---|
| apply (final) | 89,972,781 | 507 | 2 folders | `06bd9c74…` |
| verify clean-rebuild #1 | 89,972,769 | 507 | exactly `[TrabajoIntegrador-linux TrabajoIntegrador-windows]` | `d7896d9a…` |
| verify determinism #2 | 89,972,745 | 507 | same | `871021b4…` |
| verify determinism #3 | **89,972,781** | 507 | same | `45bf7eef…` |
| verify clone (worktree) | — | 507 | same | `c71f9782…` |
| verify final restore | 89,972,747 | 507 | same | `7d54cf73…` |

- **Entry count 507 reproduced on every run** (expected ~86 MB / 507: confirmed — no investigation triggered). Determinism run #3 size even equals apply's size byte-for-byte.
- **Sorted name lists identical ×2** (`diff` empty between runs #2/#3); byte sha differs by ±~36 B across runs — zip entry **mtimes**, explicitly out of R1 scope (determinism = names). Sizes ~85.8 MiB — README's "~86 MB" is true.
- `git ls-files dist/` → empty; `git ls-files | grep -i zip` → NONE; `.gitignore:12` = `dist/`.

**Zip-tree spot-check (15/15 PRESENT, via `unzip -l` directly)**: linux `bin/TrabajoIntegrador` + `lib/app/{jar,.cfg}` + `lib/libapplauncher.so` + `lib/runtime/bin/java`; windows `runtime/bin/java.exe` + `runtime/bin/server/jvm.dll` + `app/{jar + 4 javafx-*-21.0.13-win.jar}` + `Iniciar.bat` + `LEEME.txt` + `CHECKLIST-WINDOWS.txt`.

## Independent Smoke (R2.2, run by the verifier — separate from the script's own stage 6)

`DISPLAY=:1 timeout 12 dist/…/bin/TrabajoIntegrador` → **exit 124**, window `0x4600003` **Map State: IsViewable**, alive@8s YES, own capture `/tmp/opencode/verify-smoke-window.png` (59,259 B — byte-identical to the fresh build's own stage-6 capture, i.e. the current render is stable), clean kill, `pgrep` → **no stray processes** (shared-display courtesy honored). Log shows only the known benign FXML-API-26-on-runtime-21 warning (E8 profile). Headless probe re-run: `DISPLAY= bash package.sh --linux-only` → **exit 4 `UNVERIFIED (no display)`**, never fake PASS (R2.3 reproduced). R2.1 static independently confirmed: `ELF 64-bit LSB pie executable`, `-x`, `javafx.controls@21.0.13 javafx.fxml@21.0.13` via the bundled runtime's `--list-modules`.

## Fail-Fast & Mutation Probes (all reproduced)

| Probe | Expected | Reproduced |
|---|---|---|
| R1.2 empty cache (`DELIVERABLE_CACHE=$(mktemp -d)`) | exit 1, names 5 missing inputs, mentions `--fetch`, no zip | ✅ exit 1, all five named, no zip |
| R5.1 corrupt cache (isolated tmp cache, corrupted fxml) | exit 2 BEFORE use, no zip | ✅ exit 2, expected `67d4…` vs actual `0469…`, "No input was used and no zip was written"; real cache hashes untouched before/after |
| MUTATION probe (committed pins.txt graphics hash zeroed) | stage-0 rejects, exit 2, no zip | ✅ `[R5.1] FAIL: SHA-256 mismatch, REJECTING before use` (message unique to the exit-2 branch), no zip; **restored byte-identical** (`eaef274a…`), `git status` tracked-clean, suite re-green 82/0/0 |
| `--linux-only` | exit 0, **no zip written** | ✅ zip absent after successful run |
| curl gating | reachable only under `FETCH==1` | ✅ confirmed by source (single call site inside `if [ "$FETCH" -eq 1 ]`) and 5 offline builds exit 0 with `[R5.2] PASS` |

## Requirement Matrix (runtime-executed unless marked static)

| Req | Scenarios | Result | Independent evidence |
|---|---|---|---|
| R1 Reproducible build-to-artifact | R1.1, R1.2, R1.3 | **PASS** | 6 builds incl. clean rebuild + clone; 507 names identical; 15/15 entries; exit-1 probe; git purity |
| R2 Linux app-image | R2.1, R2.2, R2.3 | **PASS** | verifier-run smoke (124/IsViewable/screenshot/clean), headless exit 4, static ELF/modules checks re-run |
| R3 Windows folder (static) | R3.1, R3.2, R3.3 | **PASS static / runtime UNVERIFIED (honest)** | 4 jar sha256 + Temurin sha256 recomputed **from within the zip** == `package/pins.txt`; in-zip app-jar MANIFEST `Main-Class: unsa.sistemas.view.Launcher`; in-zip bat 3/3 `%~dp0` quoted (negative lookbehind grep empty); folder 136 MB ∈ 120–160 (D2); build log says `static-verified / runtime-UNVERIFIED` |
| R4 Launcher rename | R4.1, R4.2, R4.3 | **PASS** | `grep ConsoleView src test pom.xml README.md` → 0; `Launcher.java` exists; manifest from target jar AND shipped-in-zip jar; javadoc read against code: truthful (no-`Application`, `MainApp.main` forward, launcher-guard rationale = documented E3/E4 behavior, no unverified claims); `-Xdoclint:all` 0 warnings with proper modulepath; `git log --follow` traces the rename |
| R5 Pinned provenance | R5.1, R5.2, R5.3 | **PASS** | exit-2 probes ×2 (corrupt cache + pins mutation); offline rebuilds ×5; pins.txt parseable: 5 data lines, all exactly `sha256|size|url|dest`, immutable Temurin URL + sha + cache path + `--fetch` note verbatim |
| R6 README truth | R6.1, R6.2 | **PASS** | built verbatim in clean worktree clone (`sh ./mvnw -o clean test` + `bash package.sh` → exit 0, 507 entries); every run-cited path exists literally in unzipped clone-zip; **unzipped bin runs the real GUI with `java` stripped from PATH** (child cmdline = zip's own `lib/runtime/bin/java`) → "no Java needed" is functionally true; honesty table present (Linux VERIFICADO + evidence pointer; Windows static-only until checklist returns); zip > 25 MB hand-in stated |
| R7 Non-regression / purity | R7.1, R7.2 | **PASS** | 82/0/0 at all 4 of my test executions; `git diff --stat d7656fa..e78a3c2` = **exactly 11 paths**, all within allowed set; `git diff -- openspec/specs/ test/` → 0 lines; sole src change = the rename; no zip/dist tracked |
| R8 Windows attestation | R8.1, R8.2 | **PASS static; flip legitimately UNVERIFIED** | checklist byte-identical three ways: extracted-from-zip == archived copy == committed original (`b201f72c…`); archived copy exists under the change folder; every artifact that touches Windows runtime status says UNVERIFIED (README row, `[R8.2]` log line, LEEME note) — **zero PASS-faking found anywhere** |

**Compliance summary**: R1–R7 fully reproduced green; R3 runtime + R8 flip remain honestly UNVERIFIED (no Windows host — by design, not a gap in this verification).

## Design Coherence

D1 (JRE as-is) ✓ `runtime/` = 1:1 extraction, 127 MB + 85 dlls; D2 (bounds 120–160) ✓ printed with rationale; D3 (E5 flags, zip ~85–90) ✓ 85.8 MiB; D4 (module-path app dir + `-cp` jar + `Launcher`) ✓ exact in shipped bat; D5 (graphics-natives + hash checks) ✓ matches amended spec wording; D6 (`java.exe` + console note) ✓ bat + LEEME; D7 (jar plugin 3.4.1) ✓ pom; D8/Q2 (neutral Spanish handouts) ✓ LEEME/CHECKLIST; D9 (`package/` dir originals shipped) ✓ sha-identity proves no drift. Stage contract + exit codes 0/1/2/3/4 ✓ all reproduced. Q1 spec amendment carries inline rationale (allowed edit per task 0.2).

## Issues Found

**CRITICAL**: None.

**WARNING**
1. **Workload-forecast overshoot** (advisory — main-only flow, no PRs): commit `dcc665c` = 443+/46− ≈ 489 changed lines vs tasks-forecast ~230 for c3; total change = 784+/44− text lines vs forecast ~640. No 400-line budget breach at the PR level exists to breach (direct commits), and spec/design compliance is unaffected. Recorded per mandate as WARNING, not CRITICAL.
2. **Apply narrative overstates evidence determinism**: committed `evidence/linux-smoke-window.png` is 900×650 (`bbc9d887…`, 55,831 B — hash claim verified, file is genuine app GUI: window title, Spanish labels, input/result panels). But every fresh build here (incl. my own capture) renders the window at 1100×731 (~59 KB), and apply's own run-#2 note measured 59,262 B while claiming "byte-identical → deterministic render". The evidence itself is correct and spec-compliant (R2.2 requires a saved screenshot, not a byte-stable one); the *claim* about render determinism does not reproduce. Not a green hook over wrong evidence — a narrative exaggeration to avoid repeating.

**SUGGESTION**
1. `tasks.md` 5.3 says "12 changed/created paths"; the tracked diff is exactly **11** paths (the 12th — the archived CHECKLIST copy under the change dir — is untracked like all openspec working files). Align wording at archive time.
2. README's build steps don't mention `DISPLAY` is needed for stage 6 smoke; on a headless build machine `package.sh` exits 4 (correct per spec R2.3) but a reader following README verbatim over SSH gets a confusing end-of-run. One sentence would help.
3. `git show -M a12f14f` renders the rename as D+A (javadoc push similarity under the rename threshold); `git log --follow` traces it fine. Cosmetic for audit only.
4. The `--fetch` codepath has still never been executed end-to-end (cache always valid; verified only by gating inspection). A future clean-machine run should exercise it once.

## Windows Honesty Check

PASS. No artifact (README, build log, LEEME, CHECKLIST, spec/design, apply report) claims Windows runtime works. Status language: `static-verified / runtime-UNVERIFIED` + "flips only with a returned, archived checklist" (R8.2). The checklist states expected values normatively for the future tester (`RESULTADO ESPERADO`), never as verified results.

## Reproducibility / Repo State After Verification

No git state changed (read-only git + one worktree, removed and pruned). All mutations restored and proven: `package/pins.txt` sha256-identical post-restore, `git status` shows only the same pre-existing untracked openspec working files as at start, final suite 82/0/0, final full build exit 0 with restored `dist/TrabajoIntegrador.zip`. Real cache untouched (hashes verified before/after probes).

## Next Recommended

`sdd-archive` — implementation, evidence, and honesty posture are archive-ready. The R8 flip stays an open manual attestation OUTSIDE the SDD change lifecycle (returned `CHECKLIST-WINDOWS.txt` → archived → status update), per spec R8.2.

## Risks

1. Windows runtime untested until a physical Windows run returns the checklist — inherent, mitigated by static gates + honest labeling (R8 path defined).
2. Zip ≈ 85.8 MiB exceeds email limits; README mandates USB/Drive/campus hand-in.
3. Shared-desktop window geometry/screenshot bytes drift between runs (WARNING 2) — evidence authenticity is unaffected, but avoid byte-identity claims for screenshots.
4. `--fetch` unexercised in anger: a future machine without the cache depends on that path working first try.
