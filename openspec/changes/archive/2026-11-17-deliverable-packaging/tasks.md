# Tasks: deliverable-packaging

- Change: `deliverable-packaging` · Store: `hybrid` · Baseline: `d7656fa`, 82 green
- Execution: 4 work-unit commits (verbatim Spanish messages from design commit_plan), each green + revertable.
- TDD note: `rules.apply.tdd=true` is N/A here — delta spec fixes the verification model: JUnit guards regression only (82 green per commit); ground truth = script hooks + archived evidence. Zero new JUnit tests.
- Gates re-verified at tasks time: `xwininfo`+`import` present, `DISPLAY=:1` live, cache `~/.cache/gentle-deliverable` 51 MB, **shellcheck ABSENT** → gate is `bash -n` only (note this in verify).

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | c1 ~40 · c2 ~120 · c3 ~230 (script stages 0/4/5 + package/ assets) · c4 ~250 (README + archived evidence); total ~640, each commit < 400 |
| 400-line budget risk | Low (per commit) |
| Chained PRs recommended | No |
| Suggested split | Not needed — 4 main-only work-unit commits per design |
| Delivery strategy | auto-chain (n/a; direct commits) |
| Chain strategy | pending (no PR chain) |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Low

**Approved-scope note (for verify):** apply DOES one-time network ONLY if cache fails task 0.1 recheck: pinned Temurin JRE + 4 jfx `-win` jars — user-approved option (a), immutable URL + hashes. With valid cache every run is offline (R5.2). Not a surprise.

## Phase 0: Pre-flight (no commit)

- [x] 0.1 **Cache re-check (offline)** — files: none. `sha256sum ~/.cache/gentle-deliverable/OpenJDK17U-jre_x64_windows_hotspot_17.0.21_9.zip` == design `fe4656bd…d117`; `for j in ~/.cache/gentle-deliverable/jfx-win/*-win.jar; do sha256sum "$j"; done` == design table (base `7a9a3dd9…`, graphics `42d59a74…`, controls `2fbf5667…`, fxml `67d4ad01…`). Gate: 5/5 match → stage 0 skips network. Any miss → run `bash package.sh --fetch` (approved scope), re-check before Phase 1. Rollback: n/a.
- [x] 0.2 **Q1 spec-delta correction (the ONLY allowed spec edit)** — file: `specs/deliverable-packaging/spec.md`, R3 body. Before: “the four `javafx-*-21.0.13-win.jar` present in `app/` each containing `.dll` natives;” → After: “the four `javafx-*-21.0.13-win.jar` present in `app/`, each matching its pinned SHA-256 in `package/pins.txt`, with `javafx-graphics-21.0.13-win.jar` containing ≥1 `.dll` native;”. Rationale (record inline): design D5 measurement E-jfx-win — only graphics bundles natives (53 dlls); base/controls/fxml have 0. Stage-4 hooks follow this text. R3 size bounds 40–120→**120–160 MB**: NO spec edit needed — R3 delegates bounds to the script (“MUST move only with a documented layout change”); design D2 + measured 136.5 MB folder IS that document; hook prints the bound + D2 pointer. Rollback: restore original line.
- [x] 0.3 **Q2 language rule** — shipped `CHECKLIST-WINDOWS.txt` and `LEEME.txt` in neutral Spanish (end-user handouts; documented product exception to English-artifacts contract; `Iniciar.bat` echo messages Spanish per design D6). Auditable rule: **the Spanish original is itself archived** — one file `package/CHECKLIST-WINDOWS.txt`, byte-identical copy in zip + change folder, same sha256 in the archive table. English audit trail = this tasks.md + design D8/Q2 resolution. No commit.

## Phase 1 → commit 1: Launcher rename + runnable jar (D7)

- [x] 1.1 `git mv src/unsa/sistemas/view/ConsoleView.java src/unsa/sistemas/view/Launcher.java`; rename class to `Launcher`; add the design §Launcher javadoc verbatim (Spanish, java-docs skill: describes the `MainApp.main` forward + launcher-guard rationale = E3/E4 only, no unverified claims).
- [x] 1.2 `pom.xml`: add cached `maven-jar-plugin` **3.4.1** block verbatim from design `<mainClass>unsa.sistemas.view.Launcher</mainClass>`.
- [x] 1.3 Gates: `grep -rn ConsoleView src test pom.xml` → **0 matches** (R4.1) AND `src/unsa/sistemas/view/Launcher.java` exists; `sh ./mvnw -o clean package -DskipTests` then `unzip -p target/resolucion-sistemas-lineales-1.0-SNAPSHOT.jar META-INF/MANIFEST.MF` → `Main-Class: unsa.sistemas.view.Launcher` (R4.2); `sh ./mvnw -o clean test` → `Tests run: 82, Failures: 0, Errors: 0` (R7.1).
- [x] 1.4 `git add src/unsa/sistemas/view/Launcher.java pom.xml && git commit -m "refactor: renombrar ConsoleView a Launcher y fijar Main-Class en el jar"`. Rollback: `git revert <c1>` → ConsoleView + no manifest; no consumers yet.

## Phase 2 → commit 2: package.sh — linux path (design: stages 1,2,3,6,7)

- [x] 2.1 `.gitignore`: append `dist/`.
- [x] 2.2 Create `package.sh` (`set -Eeuo pipefail`; `DELIVERABLE_CACHE` default `~/.cache/gentle-deliverable`, `DISPLAY` override; exit codes 0/1/2/3/4 per design contract). Stages 1 (build+manifest grep), 2 (jlink E5 flags→`dist/work/runtime-linux`), 3 (jpackage `--type app-image --name TrabajoIntegrador`, E7 options, static inspect), 6 (smoke), 7 (summary). Stages 0/4/5 = fail-fast stubs: default run exits **1** naming the missing windows input before any zip (R1.2 shape); `--linux-only` skips them and never writes the zip. Hooks print scenario ids (`[R2.1] PASS …`).
- [x] 2.3 `bash -n package.sh` → clean (shellcheck absent — noted). Probe: `DELIVERABLE_CACHE=$(mktemp -d) bash package.sh` → exit 1, `test ! -e dist/TrabajoIntegrador.zip` (R1.2 manual, design matrix).
- [x] 2.4 `bash package.sh --linux-only` → expect `[R2.1]` PASS (ELF `-x`, jar in `lib/app/`, `lib/runtime/bin/java --list-modules` shows `javafx.controls`,`javafx.fxml`) and `[R2.2]` PASS: `timeout 12 dist/…/bin/TrabajoIntegrador` exit **124** (fallback `xwininfo` frame); `import -window root` screenshot → `dist/evidence/`. Headless negative: `DISPLAY= bash package.sh --linux-only` → exit 4 `UNVERIFIED (no display)`, never fake PASS (R2.3). Evidence commit deferred to 4.2 (locked decision; overrides design's “evidence in c2”).
- [x] 2.5 `sh ./mvnw -o clean test` → 82 green. `git add package.sh .gitignore && git commit -m "build: agregar package.sh con runtime linux y app-image verificado"`. Rollback: `git revert <c2>` → script+gitignore gone; `dist/` regenerable, uncommitted.

## Phase 3 → commit 3: windows folder + pinned inputs (stages 0,4,5 + package/ assets)

- [x] 3.1 Create `package/pins.txt`: design Pinned-artifacts table verbatim (immutable Temurin URL `jdk-17.0.21%2B9`, sizes, sha256; jfx win jars sha1(central)+sha256; cache path; `--fetch` flag) → R5.3 grep-verifiable.
- [x] 3.2 Create `package/Iniciar.bat` = design block verbatim (ASCII, every `%~dp0` quoted, `java.exe`, `--module-path "%~dp0app"`, `-cp` app jar, main `Launcher`) + `package/LEEME.txt` + `package/CHECKLIST-WINDOWS.txt` (Spanish per 0.3; checklist: launch via bat, solve `pruebas txt/` fixture, load file, check trace/status panels, how to report PASS/FAIL; ~5 min).
- [x] 3.3 Implement stage 0 (valid cache → zero network, idempotent; `--fetch` curls pins; ALWAYS sha256-verify before use), stage 4 (R1.2 precondition gate → hash-verify → extract JRE 1:1 → `runtime/` → copy jar + 4 win jars + 3 package assets → bounds **120–160 MB** printing D2 rationale → bat `%~dp0`-quote grep), stage 5 (zip both folders, `unzip -l` tree assert, copy checklist into change folder).
- [x] 3.4 Probes: corrupt a cache copy → exit **2** before use (R5.1); no-flag run with valid cache → offline (R5.2).
- [x] 3.5 `bash package.sh` → exit 0; expect `[R1.1]` both folders + every R1 file, `[R3.1]` static gates green + log `static-verified / runtime-UNVERIFIED`, `[R3.2]` bat quoting, `[R3.3]` bounds (folder ≈136.5 MB), `[R5.1..3]`, `[R8.1]` checklist in zip + copy under `openspec/changes/deliverable-packaging/`, `[R2.2]` linux smoke still PASS, zip **~85–90 MB** (D3 measured; proposal's 50–65 est. superseded), `[R1.3]` `git ls-files dist/` empty.
- [x] 3.6 `sh ./mvnw -o clean test` → 82. `git add package.sh package/pins.txt package/Iniciar.bat package/LEEME.txt package/CHECKLIST-WINDOWS.txt && git commit -m "build: ensamblar la carpeta windows con runtime Temurin pinneado"`. Rollback: `git revert <c3>` → `--linux-only` still works (c2 shape). Assets land here (not c4) because stage-4 R3/LEEME hooks must be green at c3 — design commit_plan is authoritative.

## Phase 4 → commit 4: README truth + evidence archive + final run

- [x] 4.1 Rewrite `README.md` (Spanish, matching existing README voice): run per OS literally matching zip tree (linux: `TrabajoIntegrador-linux/bin/TrabajoIntegrador` double-click; windows: `TrabajoIntegrador-windows\Iniciar.bat`); **no Java/Maven on end-user machines**; build prereq JDK 17 (Temurin pin noted) + `sh ./mvnw -o clean test` + `bash package.sh`; measured zip ~85–90 MB → USB/Drive/campus hand-in; attestation matrix (Linux VERIFIED + evidence pointer; Windows UNVERIFIED until R8); D6 console-window note; corrected build-from-source section. Gate: `grep -n ConsoleView README.md` → 0 (R6; resolves known c1–c3 staleness); R6.1 — every cited path exists in `unzip -l dist/TrabajoIntegrador.zip`.
- [x] 4.2 Archive evidence: `mkdir -p openspec/changes/deliverable-packaging/evidence && cp dist/evidence/*.png …` — **DECISION: commit the screenshot PNG** (~200 KB; first binary in repo, accepted exception; sha256 recorded in the archive table so verify matches file↔report).
- [x] 4.3 Clean full run ×2: `rm -rf dist target && bash package.sh` then repeat; diff of sorted `unzip -l` name lists → empty. Determinism = names per R1, NOT bytes (zip entry mtimes differ; explicitly out of scope).
- [x] 4.4 `sh ./mvnw -o clean test` → 82. `git add README.md openspec/changes/deliverable-packaging/evidence/ && git commit -m "docs: reescribir README con instrucciones reales y empaquetar el zip final"`. Rollback: `git revert <c4>` → README only; artifacts untouched in git.

## Phase 5: Archive-readiness (no commit)

- [x] 5.1 Complete scenario→hook→evidence table (R1.1–R8.2) for verify; UNVERIFIED rows: R3 runtime + R8 flip (Windows machine pending — honest, never asserted PASS).
- [x] 5.2 Flake-watch: shared `DISPLAY=:1` — a 124-expectation failure is re-run once (occlusion vs death) before declaring `SMOKE FAILED`; record both attempts.
- [x] 5.3 Scope purity: `git diff --stat d7656fa HEAD` → only the 12 changed/created paths; `git diff d7656fa -- openspec/specs/ test/` → empty (R7.2); 4 commits present, suite never moved.

## Risks (ranked)

1. **Fetch/hash (external)** — mitigated by 0.1 pre-check; network only inside approved scope.
2. **Smoke flakiness on shared DISPLAY** (human may interact) — 5.2 re-run rule + `xwininfo` fallback.
3. Zip ~85–90 MB vs email — documented, USB/Drive hand-in in README (already measured; not a surprise).
4. Windows folder silently broken — static gates + UNVERIFIED label + R8 checklist until attestation returns.

## Out of Scope (confirmed)

CI; signing; macOS; installers (`deb`/`rpm`/`exe`); icons; `module-info`/jpackage module mode; Maven-phase plugin wiring; any solver/controller/view/test behavior; committing `dist/` or zip; any spec edit beyond 0.2.
