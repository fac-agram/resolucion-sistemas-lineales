# deliverable-packaging

<!--
Provenance
- Capability: deliverable-packaging (new capability; first sync — this merge created the live spec)
- Origin change: deliverable-packaging (roadmap #6 of app-end-to-end; 6th SDD closure of the project)
- Source: openspec/changes/archive/2026-11-17-deliverable-packaging/specs/deliverable-packaging/spec.md
- Synced: 2026-11-17
- Origin verdict: pass_with_warnings (zero CRITICAL; commits a12f14f, c91c9c8, dcc665c, e78a3c2 on base
  d7656fa; archived at HEAD e78a3c2; see archive-report.md and verify-report.md in the archived change)
- Requirements R1-R8 and their 21 scenarios (R1.1-R8.2) are preserved verbatim from the delta spec,
  ids unchanged, including the Q1 correction carried inline in R3 (apply task 0.2, design D5
  measurement E-jfx-win: only javafx-graphics-21.0.13-win.jar bundles .dll natives; base/controls/fxml
  have 0 — the hook text follows the corrected wording, hashes for all four).
- Verification model (honest split, from the delta header): this capability is gated by package.sh
  script ASSERTIONS + archived screenshot evidence + a manual Windows attestation (R8) — not by new
  JUnit tests. JUnit (82 green per commit) only guards the regression surface (R4/R7). R3 is
  static-only: Windows runtime behavior is UNVERIFIED until the checklist returns.
- Delta purity (R7.2): none of the four pre-existing live specs (test-execution-baseline,
  result-honesty, mejoramiento-iterativo, procedure-trace) was modified by this change or by this
  merge; the delta was a new-capability-only spec.
- Carried WARNING-1 (verify, advisory — non-normative): commit dcc665c delivered ~489 changed lines
  (443+/46-) vs the ~230 forecast for work unit c3 (change total 784+/44- vs ~640). Main-only
  delivery, no PR existed to breach the 400-line review budget; spec/design compliance unaffected.
- Carried WARNING-2 (verify, narrative hygiene — non-normative): the committed evidence PNG
  evidence/linux-smoke-window.png (900x650, sha256 bbc9d887...) is authentic app-GUI evidence and
  satisfies R2.2 (a saved screenshot is required, not a byte-stable one), but current fresh builds
  render 1100x731 (~59 KB). Do not repeat "byte-identical / deterministic render" claims for
  screenshots; render bytes drift on the shared DISPLAY.
- UNVERIFIED rows (normative per R3 and R8.2 — they stay until the flip condition is met, they do
  NOT weaken the requirement text): (1) Windows runtime execution — static gates green here only;
  the status flips to VERIFIED ONLY when a completed CHECKLIST-WINDOWS.txt, executed on a real
  Windows host, is returned and archived (R8.2). (2) The --fetch codepath (R5) has never been
  exercised live (cache always valid; confirmed by source inspection); a future clean-machine run
  should exercise it once.
- Post-archive path note: R8.1 cites "a copy under openspec/changes/deliverable-packaging/". The
  durable audit home after archiving is openspec/changes/archive/2026-11-17-deliverable-packaging/;
  package.sh (committed at dcc665c) recreates the live-path copy via mkdir -p on any future rebuild,
  so the R8.1 hook remains satisfiable as written.
-->

## Purpose

Contract for the runnable deliverable and its reproducible packaging pipeline: a single
`TrabajoIntegrador.zip` (via `bash package.sh`) containing a VERIFIED linux jpackage app-image and a
statically-assembled Windows folder whose runtime stays honestly UNVERIFIED until a manual
attestation returns. Covers the `Launcher` main-class contract, the runnable jar manifest, pinned
and hash-verified Windows inputs, README run-instruction truth, and the non-regression rule that
packaging changes never touch the solver, the tests, or the other live specs.

## Requirements

### Requirement: R1 Reproducible Build-to-Artifact

`bash package.sh` MUST produce exactly `dist/TrabajoIntegrador.zip` with the layout below, given a committed tree and a host with JDK 17 (`jlink`, `jpackage`) and the required inputs. Names MUST be deterministic (no timestamps). `dist/` MUST be gitignored and never committed. When a Windows input is neither cached nor fetched under the opt-in step 0 (R5), the script MUST fail fast — before writing any zip — with a message naming the missing input.

```
TrabajoIntegrador.zip
├── TrabajoIntegrador-linux/                       # jpackage app-image (VERIFIED here)
│   ├── bin/TrabajoIntegrador                      # native ELF launcher
│   └── lib/{app/<project jar>+TrabajoIntegrador.cfg, runtime/ (jlink image), libapplauncher.so}
└── TrabajoIntegrador-windows/                     # hand-assembled (UNVERIFIED until R8)
    ├── runtime/bin/{java.exe, JVM *.dll}
    ├── app/{<project jar>, javafx-{base,graphics,controls,fxml}-21.0.13-win.jar}
    ├── Iniciar.bat
    ├── LEEME.txt
    └── <Windows attestation checklist file>       # R8; exact name fixed by design
```

#### Scenario: R1.1 Zip tree matches layout

- GIVEN `dist/` is absent and all Windows inputs are cached
- WHEN `bash package.sh` runs to completion
- THEN it exits 0 AND `unzip -l dist/TrabajoIntegrador.zip` lists exactly the two folders and every required file above

#### Scenario: R1.2 Missing Windows input fails fast

- GIVEN the Windows runtime archive is not cached AND the fetch step is not enabled
- WHEN `bash package.sh` runs
- THEN it exits non-zero naming the missing input and the fetch opt-in AND no `TrabajoIntegrador.zip` is left behind

#### Scenario: R1.3 dist/ never enters git

- GIVEN a build has just succeeded
- WHEN `git ls-files dist/` and `git status --short` run
- THEN `git ls-files dist/` is empty AND `dist/` is covered by a `.gitignore` entry

### Requirement: R2 Linux App-Image Integrity

The linux folder MUST contain `bin/TrabajoIntegrador` as an ELF executable holding the app jar under `lib/app/` on a jlink runtime image including modules `javafx.controls` and `javafx.fxml`. A smoke-launch gate MUST run it under `DISPLAY`: PASS iff the process stays alive ≥12 s (`timeout` exit 124) or its window is detected via X11 (`xwininfo` frame); evidence screenshot MUST be archived in the change folder. Smoke failure MUST fail `package.sh`; a headless host MUST fail explicitly with `UNVERIFIED (no display)` and never fake PASS.

#### Scenario: R2.1 Static image integrity

- GIVEN a freshly built app-image
- WHEN the hook inspects it
- THEN `file` reports an ELF executable for `bin/TrabajoIntegrador` AND `-x` holds AND the project jar is under `lib/app/` AND `lib/runtime/bin/java --list-modules` lists `javafx.controls` and `javafx.fxml`

#### Scenario: R2.2 Smoke gate PASS with evidence

- GIVEN `DISPLAY` is available
- WHEN the hook runs `timeout 12 ./bin/TrabajoIntegrador`
- THEN the exit code is 124 AND a screenshot is saved into the change folder AND the process is killed cleanly

#### Scenario: R2.3 Smoke failure fails the build

- GIVEN the launched process exits before 12 s with a code other than 124, or no display exists
- WHEN `package.sh` reaches the smoke hook
- THEN it exits non-zero with reason `SMOKE FAILED` or `UNVERIFIED (no display)` AND the zip is not accepted

### Requirement: R3 Windows Folder Static Correctness

The Windows folder MUST pass static asserts only: `java.exe` and Windows JVM DLLs present under `runtime/bin/`; the four `javafx-*-21.0.13-win.jar` present in `app/`, each matching its pinned SHA-256 in `package/pins.txt`, with `javafx-graphics-21.0.13-win.jar` containing ≥1 `.dll` native; <!-- Q1 correction (apply task 0.2, design D5 measurement E-jfx-win): only javafx-graphics bundles natives (53 dlls); base/controls/fxml have 0. Stage-4 hooks follow this text. --> the project jar manifest `Main-Class` equal to `unsa.sistemas.view.Launcher`; `Iniciar.bat` quoting every `%~dp0` path and invoking `runtime\bin\java.exe` with `--module-path` at `app`, `--add-modules javafx.controls,javafx.fxml` and main class `Launcher`; `LEEME.txt` present and consistent; the folder's raw size inside the sanity bounds set by the script (40–120 MB for the trimmed runtime; bounds MUST move only with a documented layout change) — catching empty or truncated inputs. **Windows runtime behavior is UNVERIFIED on this host until the R8 attestation checklist is executed on a Windows machine; no scenario here may assert it.**

#### Scenario: R3.1 Static gates green, status honest

- GIVEN a freshly assembled Windows folder
- WHEN the Windows verification hooks run
- THEN every assert above passes AND the build log labels the folder `static-verified / runtime-UNVERIFIED`

#### Scenario: R3.2 Spaces-safe bat

- GIVEN `Iniciar.bat`
- WHEN the hook greps for `%~dp0` usages
- THEN each `%~dp0...` path occurrence appears inside double quotes

#### Scenario: R3.3 Size bounds catch truncation

- GIVEN a Windows runtime archive extracted partially or of the wrong artifact
- WHEN the hook measures the assembled folder
- THEN the size falls outside the bounds AND the build fails naming the component

### Requirement: R4 Launcher Rename Completeness

`src/unsa/sistemas/view/ConsoleView.java` MUST become `Launcher.java` (class `Launcher`); its Javadoc MUST truthfully describe what the code does — a non-`Application` entry point forwarding `main` to `MainApp`, required for classpath and packaged launches — claiming nothing unverified (java-docs principle). `Main-Class: unsa.sistemas.view.Launcher` MUST be set via cached `maven-jar-plugin` config. No reference to `ConsoleView` may remain in `src/`, `test/` or `pom.xml`; test code itself is unchanged and the suite MUST stay 82-green.

#### Scenario: R4.1 No dangling references

- GIVEN the rename commit
- WHEN `grep -rn ConsoleView src test pom.xml` runs
- THEN it returns zero matches AND `src/unsa/sistemas/view/Launcher.java` exists

#### Scenario: R4.2 Manifest points at Launcher

- GIVEN `sh ./mvnw -o clean package -DskipTests` succeeds
- WHEN `unzip -p target/*.jar META-INF/MANIFEST.MF` runs
- THEN it contains `Main-Class: unsa.sistemas.view.Launcher`

#### Scenario: R4.3 Javadoc truthfulness

- GIVEN `Launcher`'s Javadoc
- WHEN reviewed against the implementation and E3/E4 behavior
- THEN it describes the `MainApp.main(args)` forward and the launcher-guard rationale, with no unverified claims

### Requirement: R5 Pinned Fetch Provenance

The Windows runtime MUST be the pinned Temurin 17 x64 Windows zip (option (a)); its exact URL and expected SHA-256 MUST be documented in version-controlled script/manifest text. Fetch is a single gated, opt-in step 0 writing to a defined cache path. The archive MUST be hash-verified before use; a mismatch MUST reject it. A valid cache MUST make rebuilds fully offline, and cached inputs MUST be reused without re-download. The same pin-and-checksum rule applies to the four `-win` JavaFX jars.

#### Scenario: R5.1 Hash mismatch rejected

- GIVEN a cached archive whose SHA-256 differs from the pinned value
- WHEN step 0 or assembly runs
- THEN the script rejects the file and fails BEFORE using it

#### Scenario: R5.2 Offline rebuild from cache

- GIVEN valid cached Windows inputs AND the fetch flag absent
- WHEN `bash package.sh` runs
- THEN it exits 0 without any network access for Windows inputs

#### Scenario: R5.3 Provenance documented

- GIVEN the committed `package.sh` (or its fetch manifest)
- WHEN inspected
- THEN the pinned Temurin URL, SHA-256, cache path and opt-in flag are present verbatim

### Requirement: R6 README Truth

`README.md` MUST state run instructions matching the shipped zip: Linux — unzip, run `TrabajoIntegrador-linux/bin/TrabajoIntegrador` (double-click); Windows — unzip, run `TrabajoIntegrador-windows\Iniciar.bat`; end-user machines need NO Java or Maven; build prerequisites (JDK 17 + wrapper) apply to the build machine only; the zip (~50–65 MB) exceeds the 25 MB email limit, so hand-in goes via USB/Drive/campus platform. It MUST contain no claim contradicted by build outputs (no stale `javac`-only or `ConsoleView` flow).

#### Scenario: R6.1 Cited paths exist

- GIVEN the rewritten README and a fresh zip
- WHEN each artifact path cited in run instructions is checked against `unzip -l`
- THEN every cited path exists in the zip

#### Scenario: R6.2 Status labels match reality

- GIVEN the README attestation matrix
- WHEN compared with hook results
- THEN Linux is labeled VERIFIED (with evidence pointer) AND Windows runtime is labeled UNVERIFIED until R8 attestation

### Requirement: R7 Non-Regression and Delta Purity

This change MUST be a new-capability-only delta: none of the four live specs (test-execution-baseline, result-honesty, mejoramiento-iterativo, procedure-trace) may be modified. `sh ./mvnw -o clean test` MUST report 82 tests, 0 failures, 0 errors at every commit of the change. Source changes MUST be limited to the R4 rename; no solver/controller/view/test behavior may change; no zip or `dist/` output may be committed.

#### Scenario: R7.1 Suite green at every commit

- GIVEN any commit of this change
- WHEN `sh ./mvnw -o clean test` runs
- THEN `Tests run: 82, Failures: 0, Errors: 0`

#### Scenario: R7.2 Live specs and source diff untouched

- GIVEN the full change diff against base `d7656fa`
- WHEN `openspec/specs/` and `src/`/`test/` are inspected
- THEN no live spec file changed AND the only `src` change is the `Launcher` rename

### Requirement: R8 Windows Attestation Artifact

A Windows attestation checklist MUST ship inside the zip's Windows folder AND be archived in the change folder, so a Windows user can attest in ~5 minutes: launch via `Iniciar.bat`, solve a `pruebas txt/` fixture, load a file through the GUI, verify the result, procedure-trace and status panels; it MUST state how to report PASS/FAIL back. Windows runtime status flips from UNVERIFIED to VERIFIED ONLY with a completed, archived checklist.

#### Scenario: R8.1 Checklist shipped and archived

- GIVEN a completed build
- WHEN the zip and the change folder are inspected
- THEN the checklist file appears inside `TrabajoIntegrador-windows/` AND a copy exists under `openspec/changes/deliverable-packaging/`

#### Scenario: R8.2 No attestation, no flip

- GIVEN no returned checklist
- WHEN any artifact describes Windows runtime status
- THEN it says UNVERIFIED; a signed checklist archived is the only path to VERIFIED
