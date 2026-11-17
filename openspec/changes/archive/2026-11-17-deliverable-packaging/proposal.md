# Proposal: deliverable-packaging

- Change: `deliverable-packaging` (roadmap #6 of `app-end-to-end`)
- Project: `resolucion-sistemas-lineales`
- Artifact store: `hybrid` (this file + Engram `sdd/deliverable-packaging/proposal`)
- Phase: propose
- Date: 2026-11-17
- Baseline: HEAD `d7656fa`, `sh ./mvnw -o clean test` → **82 tests, 0 failures, 0 errors**
- Classification: **build/tooling + docs**. No algorithm work, no behavior change, test suite untouched.

## Intent

The app must run on Linux and Windows machines with **no Java and no Maven installed**. The current artifact cannot: `mvnw -o package` emits a 57 KB jar with **no `Main-Class`** (E1) — inert on double-click; `javafx:run` is offline-broken (E2); `java -cp … MainApp` dies at the JDK launcher guard (E3). This change closes roadmap #6: ship a runnable deliverable and make the README's run instructions true.

## Locked user decisions (not reopened here)

1. **One zip, two folders**: `TrabajoIntegrador-linux/` (jpackage app-image, native ELF launcher — VERIFIED buildable and launchable offline here) and `TrabajoIntegrador-windows/` (java.exe + Windows runtime DLLs + `Iniciar.bat`).
2. `ConsoleView` **renamed to `Launcher`**, with javadoc stating its role (non-`Application` launcher for classpath/packaged launch; the E3-vs-E4 A/B proof shows `MainApp` direct dies the JDK guard, `Launcher` survives).
3. Windows side **built here, NOT executed here**: UNVERIFIED until attestation on a Windows machine (R1.2/R7.1 honesty precedent); the attestation checklist ships with the deliverable.
4. **One-time network fetch** approved for the win JavaFX jars (`javafx-{base,graphics,controls,fxml}-21.0.13:win`) — the only network step; everything else stays offline. ⚠️ Sourcing gap for Windows `java.exe`/DLLs: see Open questions.
5. **No installers, no code signing, no icons.**

## Scope

### In Scope

- Rename `ConsoleView` → `Launcher` (`src/unsa/sistemas/view/`, one file + javadoc).
- Runnable jar: `maven-jar-plugin` (already cached) `<archive><manifest><mainClass>unsa.sistemas.view.Launcher` in `pom.xml` — offline-safe.
- jlink runtime builder producing **both** runtime images: linux from the cached `-linux` jars (proven E5: 55 MB with `--strip-debug --no-man-pages --no-header-files --compress=2`) and windows from the fetched `-win` jars + Windows JDK binaries (see Q1; jlink cross-link probed in apply, fallback: ship the win JRE layout as-is).
- `jpackage --type app-image` for linux: `--main-class unsa.sistemas.view.Launcher --java-options "--add-modules javafx.controls,javafx.fxml"` (proven E7/E8/E9).
- Windows folder assembly: `runtime/` (java.exe + DLLs), `app/` (project jar + 4 win JavaFX jars), generated `Iniciar.bat` using `%~dp0` with every path quoted (spaces-safety, P4).
- `package.sh` at repo root orchestrating: **gated step 0 (fetch, explicit pinned commands, opt-in flag)** → build → linux image → windows assembly → zip → verification hooks (manifest grep, `java.exe` presence, bat quoting, launch smoke).
- Linux smoke-launch script: start the app-image under `DISPLAY=:1`, assert the window is alive (timeout exit-124 pattern), capture screenshot evidence, kill — same mechanism as the archived R5.1 attestation.
- `README.md` rewrite of run instructions — now TRUE (unzip + double-click / `Iniciar.bat`), build prerequisites (JDK 17 + Maven on the **build** machine only), artifact layout, per-OS attestation matrix.
- Archive-report-style **Windows attestation checklist** (what to run on a Windows box to flip UNVERIFIED → VERIFIED).
- `.gitignore`: add `dist/`.

### Out of Scope

- CI, code signing, macOS, installer types (`deb`/`rpm`/`exe`), icons, multi-version (17/21) matrix.
- jpackage Windows cross-build — **impossible from Linux** (native launcher + `libapplauncher` are per-OS, P2); the windows folder is hand-assembled instead.
- Maven-plugin integration of jlink/jpackage — the antrun trap (plugin cached, `org.apache.ant:ant` absent) and missing exec-plugin make any Maven-phase wiring network-dependent (P3). Scripts are the primitive; reversible later.
- Any solver/controller/view/test change; any spec'd behavior change.

## Capabilities

### New Capabilities

- `deliverable-packaging`: the packaging pipeline and deliverable contract — `Launcher` main-class contract, runnable jar manifest, linux jlink/jpackage app-image, windows folder layout + `Iniciar.bat`, single-zip hand-in, smoke-verification hooks, per-OS attestation status rules.

### Modified Capabilities

- None. `test-execution-baseline` pins (82 tests / 12 classes) stay valid — the rename touches zero test code; `result-honesty`, `procedure-trace`, `mejoramiento-iterativo` untouched.

## Approach

Script-driven per the explore recommendation: the only offline-reproducible shape is `package.sh` + one pom block (zero new plugins — inventory-proven). Flow: `mvnw -o clean package -DskipTests` → jar with `Main-Class` → **linux**: `jlink` (cached `-linux` jars) → `jpackage --type app-image` → **windows**: gated step-0 fetch → assemble `runtime/` + `app/` + `Iniciar.bat` → zip both folders → verification hooks incl. the DISPLAY=:1 smoke launch. The jar's role is documented: component of the app-image, not a standalone `java -jar` entry.

## Deliverable layout

```
TrabajoIntegrador.zip                          # the single hand-in
├── TrabajoIntegrador-linux/                   # jpackage app-image (VERIFIED here)
│   ├── bin/TrabajoIntegrador                  # native ELF launcher, double-click, spaces-proof (E9)
│   └── lib/{app/<jar>+TrabajoIntegrador.cfg, runtime/ (jlink image), libapplauncher.so}
└── TrabajoIntegrador-windows/                 # hand-assembled (UNVERIFIED until attested)
    ├── runtime/bin/{java.exe, *.dll}          # Windows JDK binaries — source per Q1
    ├── app/{project jar, javafx-{base,graphics,controls,fxml}-21.0.13-win.jar}
    ├── Iniciar.bat                            # @"%~dp0runtime\bin\java.exe" --module-path "%~dp0app"
    │                                          #   --add-modules javafx.controls,javafx.fxml
    │                                          #   -cp "%~dp0app\<jar>" unsa.sistemas.view.Launcher
    └── LEEME.txt                              # "run Iniciar.bat — no Java needed" + SmartScreen note
```

Sizes (E10 measured / estimated):

| Component | Raw | Zipped | Status |
|---|---|---|---|
| linux app-image | 87 MB (55 MB stripped runtime variant) | 29 MB | **measured** |
| windows folder | ~60–85 MB | ~25–35 MB | estimated, unbuilt |
| **single zip, both folders** | ~150–170 MB | **~50–65 MB (est.)** | **exceeds Gmail 25 MB** → hand in via USB / Google Drive / campus platform (README states it) |

## Compatibility impact

82 tests MUST stay green at every commit. `ConsoleView` reference inventory (grep-verified, 21 hits):

| Location | Refs | Disposition |
|---|---|---|
| `src/unsa/sistemas/view/ConsoleView.java:3` | 1 | rename target (class + file → `Launcher.java`, add javadoc) |
| `test/**` | **0** | no test code or string references the class |
| `pom.xml` | **0** | `javafx-maven-plugin` uses `MainApp` (dev-time, untouched); new manifest entry names `Launcher` |
| `README.md:14,18,71` | 3 | absorbed by the README rewrite (in scope) |
| `openspec/changes/app-end-to-end/explore.md:16,110,124,137` | 4 | historical record — untouched |
| `openspec/changes/deliverable-packaging/explore.md` (10 lines) | 10 | historical record — untouched |
| `archive/…/archive-report.md:124`, `honest-results/proposal.md:39`, `honest-results/tasks.md:246` | 3 | archived audit trail — never modified |

No behavior change. Spec impact: one new capability; zero modified; `test-execution-baseline` and `result-honesty` untouched.

## Rollback plan

Four independent commits (below) — `git revert` any or all; final state = `d7656fa`, suite never moves. **Decision: the zip and `dist/` outputs are NOT committed to git** — a 50–65 MB binary build artifact permanently bloats history and regenerates from committed scripts (`bash package.sh`, step-0 fetch documented). `.gitignore` gains `dist/` so stray builds keep `git status` clean.

## Success criteria

- [ ] `bash package.sh` produces `TrabajoIntegrador-linux/`, `TrabajoIntegrador-windows/`, and the single zip.
- [ ] Jar manifest contains `Main-Class: unsa.sistemas.view.Launcher` (grep evidence).
- [ ] Linux app-image launches the real GUI under `DISPLAY=:1`: window-alive probe (exit 124) + **screenshot evidence archived** (R5.1 attestation mechanism), then killed cleanly.
- [ ] Windows side statically verified here: `java.exe` + runtime DLLs present, 4 pinned `-win` jars present, `Iniciar.bat` quotes all paths. Functional status stays **UNVERIFIED** until the shipped attestation checklist is executed on a Windows machine.
- [ ] `sh ./mvnw -o clean test` → 82/0/0 before and after every commit.
- [ ] README's linux run instructions, followed **literally from a clean clone**, work.
- [ ] Attestation matrix + Windows checklist archived in the change folder.

## Review workload forecast

~4 commits: (1) rename + jar manifest (~40 lines), (2) linux jlink/jpackage script + smoke launch (~120), (3) windows gated fetch + assembly + `Iniciar.bat` (~120), (4) README rewrite + zip orchestration + attestation docs (~200). All scripts/docs/config; each well under 400.

```
Decision needed before apply: No
Chained PRs recommended: No (main-only flow)
400-line budget risk: Low
```

Flips to `Yes` only if the step-0 fetch fails — documented fallback: ship build-on-Windows instructions and produce `TrabajoIntegrador-windows/` on the user's Windows machine (recipe already proven portable by E5–E7 logic).

## Risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| Fetch failure or version drift | Med | Pin exact coordinates (`21.0.13` win jars + one Windows JDK 17 archive); fetch is one explicit, opt-in, idempotent step; fallback: build-on-Windows recipe |
| Windows folder silently broken (untestable here) | Med-High | Static verification gates; honest UNVERIFIED status; attestation checklist ships with the zip |
| Zip (~50–65 MB est.) vs Gmail 25 MB | Near-certain | USB / Drive / campus platform instruction in README |
| SmartScreen/AV flags unsigned artifacts | Med | `Iniciar.bat` avoids `.exe` suspicion entirely; README note; signing out of scope (no cert exists) |
| Spaces in paths break hand-written scripts | Med | every path quoted (`"%~dp0"`, `"$SCRIPT_DIR"`); `pruebas txt/` precedent; spaces-path probe codified in the smoke script (E9) |
| jlink cross-link rejects host-linked flags for win | Med | apply-time probe; fallback ships the win JRE as-is (+~15–25 MB raw) — decision documented |
| `/tmp/opencode` probe dirs cleaned before evidence is copied | Low | evidence (logs, screenshot) lands in the change folder, not `/tmp` |
| README rewrite scope creep | Low | success criterion is literal-workability of run instructions only; status sections updated surgically |

## Dependencies

- Host tooling (verified): JDK 17.0.20.1 with `jlink`/`jpackage`, `zip`/`unzip`/`curl`, `DISPLAY=:1`.
- Cached Maven artifacts (verified): `maven-jar-plugin`, `-linux` JavaFX 21.0.13 jars.
- One-time network (user-approved): the 4 `-win` JavaFX jars + Windows JDK binaries (source per Q1).

## Open questions

1. **Source of the Windows `java.exe`/JVM DLLs.** The approved fetch covers the four JavaFX `-win` jars; those jars contain JavaFX natives, not a JVM, and no Windows JDK exists on this host (searched). The locked decision "built here" needs one of: **(a) recommended** — extend the *same single gated step-0 moment* to one pinned Windows JDK 17 x64 archive (jmods preferred → trimmed ~55 MB runtime; full JRE acceptable); (b) user supplies a Windows JDK zip locally (zero extra network); (c) fallback — `TrabajoIntegrador-windows/` assembled on a Windows machine. Blocks only the windows commit; answer before apply.
