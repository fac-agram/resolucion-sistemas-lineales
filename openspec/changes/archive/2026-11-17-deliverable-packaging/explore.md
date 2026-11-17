# Exploration: Deliverable Packaging

- Change: `deliverable-packaging` (roadmap item #6 of `openspec/changes/app-end-to-end/explore.md`)
- Project: `resolucion-sistemas-lineales`
- Artifact store: `hybrid` (this file + Engram `sdd/deliverable-packaging/explore`)
- Phase: explore (investigate and decide HOW to package; no implementation, no other artifacts)
- Date: 2026-11-17
- Baseline: HEAD `d7656fa`, `sh ./mvnw -o clean test` → **82 tests, 0 failures, 0 errors** (orchestrator-attested; suite untouched by this exploration — repo verified clean via `git status --short` after all probes).
- User constraint (explicit): packaging is its **own separate line of work/commits**, never mixed with prior changes; base = current HEAD. Goal, verbatim: the app must work on ANY computer ("dejar funcionando esta app" for delivery).

## Executive Summary

**The recommended packaging pipeline is fully proven end-to-end on this machine, offline, during this exploration.** `jlink` (custom runtime from the cached `-linux` modular JavaFX jars) + `jpackage --type app-image` (launcher main class = the non-`Application` `ConsoleView` trick) produces a double-clickable, self-contained app folder that starts the real GUI — verified twice, including from a path containing spaces. Total artifact: **87 MB raw, 29 MB zipped**. Zero new Maven plugins are needed; in fact **no Maven plugin available offline can invoke `jlink`/`jpackage`**, so the honest recommendation is a small `package-app.sh` script as the packaging entry point, with the only pom change being a `Main-Class` manifest entry via the already-cached `maven-jar-plugin`.

Hard boundary that no tooling can erase: `jpackage` **cannot cross-compile**. A Windows `.exe`/app-image can only be produced on Windows, and the `-win` JavaFX jars are not cached here. "Works on any computer" therefore decomposes into: *one verified artifact per OS*, built by running the same script on that OS. Linux x86_64 is VERIFIED here; Windows/macOS ship as UNVERIFIED-by-default with a concrete build-on-that-OS recipe (same honesty standard as R1.2/R7.1 in `establish-build-test-baseline`).

## Current State (verified this session)

### What exists today

- **Build:** `pom.xml` (87 lines) — compiler 3.14.1, surefire 3.5.4, `javafx-maven-plugin:0.0.8` (`mainClass=unsa.sistemas.view.MainApp`), resources from `src` (FXML/CSS land in `target/classes`). No `module-info.java` anywhere (`find src test -name 'module-info.java'` → empty).
- **Packaging today produces an inert jar:** `sh ./mvnw -o package -DskipTests` → BUILD SUCCESS, `target/resolucion-sistemas-lineales-1.0-SNAPSHOT.jar` = 58,314 B; `unzip -p … META-INF/MANIFEST.MF` → `Manifest-Version / Created-By: Maven JAR Plugin 3.4.1 / Build-Jdk-Spec: 17` — **no `Main-Class`, no `Class-Path`** (re-confirmed; the roadmap #6 claim still holds).
- **Entry classes:** `MainApp.java:17` now loads `getResource("Sistema.fxml")` (the `gui-launch-fix` landed). `ConsoleView.java` is a 7-line non-`Application` class whose `main` forwards to `MainApp.main(args)` — historically mislabeled a stub; it is exactly the launcher trick a classpath/`jpackage` launch requires.
- **Dev-time run path is still offline-broken:** `sh ./mvnw -o javafx:run` → BUILD FAILURE (re-run this session): the plugin jar is cached but `maven-plugin-api:3.6.0`, `plexus-java:0.9.11`, `plexus-archiver:3.6.0`, `commons-exec:1.3`, `maven-core:3.6.0` are not (`Cannot access central … in offline mode`).
- **Runtime image from the earlier phase still present:** `/tmp/opencode/rt` — 87 MB, contains `javafx.{base,controls,fxml,graphics}@21.0.13` + `java.base/datatransfer/desktop/prefs/scripting/xml` + `jdk.unsupported` (`--list-modules`).
- **README.md is stale** (claims no entry point, "javac only / no external dependencies", empty `Normas`/`AnalisisSensibilidad`) — its rewrite is explicitly inside roadmap #6 scope.
- **JDK tooling on host:** OpenJDK `17.0.20.1` with `jpackage`, `jlink`, `java` on PATH; `zip`/`unzip`/`dpkg-deb`/`fakeroot` present; **`rpmbuild` absent**; `DISPLAY=:1` live (X socket present; GUI probes confirmed).

### Measured evidence (every claim below was executed this session)

| # | Probe | Result |
|---|-------|--------|
| E1 | `mvnw -o package -DskipTests` | exit 0; 57 KB jar; manifest has no `Main-Class` |
| E2 | `mvnw -o javafx:run` | BUILD FAILURE — plugin transitive deps absent from `~/.m2` (offline) |
| E3 | `java -cp target/classes:<4 -linux javafx jars> unsa.sistemas.view.MainApp` (system JDK) | Dies at launcher guard: `Error: faltan los componentes de JavaFX runtime…` |
| E4 | Same classpath, entry = `unsa.sistemas.view.ConsoleView` | **Runs.** Only warnings: `Unsupported JavaFX configuration: classes were loaded from 'unnamed module'` + FXML namespace-26 note; GUI alive until 12 s timeout (exit 124) |
| E5 | `jlink --module-path <cached -linux jars> --add-modules javafx.controls,javafx.fxml --strip-debug --no-man-pages --no-header-files --compress=2` | exit 0, **fully offline**; output 55 MB (vs 87 MB previous image); app boots through it with `java -cp` (exit 124) |
| E6 | **`jlink` image resolves the linked JavaFX modules at `-cp` launch WITHOUT `--add-modules`** | exit 124 even without the flag — the image's link-time module set persists. `--add-modules javafx.controls,javafx.fxml` still recommended in scripts as defense-in-depth (costs nothing, survives image regeneration without the flag baked in) |
| E7 | `jpackage --type app-image --input <staged jar> --main-jar app.jar --main-class unsa.sistemas.view.ConsoleView --runtime-image <jlink image> --java-options "--add-modules javafx.controls,javafx.fxml"` | exit 0, **no external tools needed** (no fakeroot/dpkg for `app-image`). Layout: `bin/ResolucionSistemasLineales` (native launcher) + `lib/{runtime,app/app.jar+*.cfg,libapplauncher.so}`. `app.classpath=$APPDIR/app.jar`, `app.mainclass=…ConsoleView` recorded in `.cfg` |
| E8 | Launch `bin/ResolucionSistemasLineales` from `/tmp/opencode/jpkg-out/…` | exit 124 — GUI alive; only the namespace-26 warning (the E4 "unnamed module" warning disappears because JavaFX is in the named-module runtime) |
| E9 | Same, copied under `"/tmp/opencode/spaces test dir/app image/"` | exit 124 — **jpackage launcher handles spaces in the install path** (a hand-written `run.sh` must still quote `"$DIR"` — `pruebas txt/` proves this project punishes unquoted paths) |
| E10 | Zip sizes | app-image (87 MB raw, unstripped runtime) → **29 MB zip**. Optimized runtime alone (55 MB, `--compress=2`) → 36 MB zip. Flag interaction noted: an uncompressed jlink image deflates better in an outer zip; pick flags for the distribution medium at design time. Either way ≤ ~36 MB |
| E11 | Network probe | `curl` to `repo.maven.apache.org` → HTTP 200. **"Offline" here is project discipline (spec R7), not a physical constraint** — a one-time online fetch is possible with user approval |

### Local Maven repository inventory (the feasibility gate)

Cached under `~/.m2/repository/org/apache/maven/plugins/`: `antrun 3.1.0`, `assembly 3.7.1`, `clean`, `compiler 3.14.1`, `dependency 3.7.0`, `deploy`, `install`, `jar 3.4.1/3.5.0`, `resources`, `site`, `surefire 3.5.4`.

- **NOT cached:** `maven-shade-plugin`, `exec-maven-plugin` (`~/.m2/repository/org/codehaus/mojo/` does not exist).
- **Trap discovered:** `maven-antrun-plugin`'s jar is cached but its required dependency `org.apache.ant:ant:1.10.12` is **not** (`~/.m2/repository/org/apache/ant/` absent) → antrun would fail offline exactly like `javafx:run`. "Plugin present in `~/.m2`" ≠ "plugin usable offline"; transitive trees must be proven, never assumed.
- `javafx` artifacts cached for `21.0.13`: **`-linux` classifier only** (base/graphics/controls/fxml; graphics additionally has sources/javadoc). **No `-win`, no `-mac`, no `-mac-aarch64` jars.**
- Consequence: **any Maven-phase wiring of `jlink`/`jpackage` requires at least one new plugin (exec or working antrun) → a one-time online fetch.** The script route needs **zero** new artifacts (verified E5–E9).

### What "works on any computer" concretely means here

There is no single artifact that runs on Windows, Linux, and macOS: JavaFX natives are per-OS and `jpackage`/`jlink` produce per-OS outputs (native launcher binaries differ). The deliverable is therefore **"any computer of the OS(es) we hand over"**: a self-contained folder (bundled runtime; end user needs **no Java installed**) built per target OS by running one script **on that OS**. Course-facing reality check: nothing in the repo encodes the grader's submission rules (no rubric file; `pruebas txt/` is a manual-grading fixture set; `README.md` still speaks of a source-only `javac` flow). For an UNSA hand-in the safe universal format is **a folder on USB + its zip** (double-click launcher inside), with a source tree and build instructions alongside — an installer is optional garnish, not the accepted artifact. This assumption is listed as an open question because it cannot be verified from the repo.

## Affected Areas (packaging change, when implemented)

- `pom.xml` — `maven-jar-plugin` `<archive><manifest><mainClass>` entry (cached plugin, offline-safe); optional `<Classifier>` property tidy-up. No new plugins in the recommended shape.
- `package-app.sh` (new, repo root) — orchestrates: `mvnw -o package -DskipTests` → `jlink` (cached `-linux` jars as `--module-path`, strip flags) → stage jar → `jpackage --type app-image` → zip. All steps proven above.
- `src/unsa/sistemas/view/ConsoleView.java` — kept as the launcher main; rename to `Launcher` recommended (name lies today: it opens the GUI, and "ConsoleView" invites the reviewer to expect a CLI). Rename touches README + any docs only; zero test impact (no test references it — suite is math-level).
- `README.md` — mandatory rewrite: prerequisites (JDK 17 only on the BUILD machine; none on the RUN machine), `mvnw -o clean test`, `sh package-app.sh`, artifact layout, per-OS recipe, honest UNVERIFIED notes.
- `verify-package.sh` (new) or documented manual checklist — launch-attestation recipe (E8/E9 pattern: `timeout 12 "$BIN" ; [ $? -eq 124 ]` = window alive; then a manual solve of a `pruebas txt/` fixture).
- `.gitignore` — add `dist/` (build outputs), never commit the 87 MB image.
- `openspec/changes/deliverable-packaging/*` — this roadmap #6 closes `app-end-to-end`'s delivery line.

## Approaches

| Approach | Offline? | Cross-OS? | Effort | Verdict |
|----------|----------|-----------|--------|---------|
| **A. `package-app.sh`: jlink image + `jpackage --type app-image` + zip; `Main-Class` via cached jar-plugin** | **Yes, fully (E1,E5,E7)** | Recipe per OS; script is OS-portable (`.sh` here, `.cmd`/`.ps1` variant for Windows) | Low | **Recommended** |
| B. Portable folder done by hand: jlink runtime + jar + custom `run.sh`/`run.cmd` (no jpackage) | Yes | Yes | Low-Med | Fallback if a reviewer's JDK ever lacked `jpackage` (JDK ≥16 has it; E7 proves it here). Native launcher + `.cfg` + future `.deb`/`.exe` path all come free from jpackage — script route duplicates that work by hand and re-introduces space-quoting bugs |
| C. Shaded uber-jar + pure classpath (`java -jar`) | **No** — `maven-shade-plugin` absent from `~/.m2` | Single-OS natives baked in | Med | Rejected: needs network, per-OS jar anyway, and only runs with the "unnamed module" warning configuration (E4). It is strictly worse than A on every axis except `java -jar` muscle memory |
| D. Keep documenting `mvn javafx:run` as the run path | No (E2) | n/a | Trivial | Rejected: makes the recipient install JDK 17 + Maven + network and read build tooling — fails "dejar funcionando esta app" outright |
| E. OS installers (`jpackage --type deb/rpm/exe`) | deb: possible (dpkg-deb+fakeroot present); **rpm: no `rpmbuild`**; exe: needs Windows (+ WiX) | No | Med | Deferred/out-of-scope: installers want root/`dpkg -i` and complicate the USB hand-in; `app-image` is the folder a grader can double-click. Design keeps `--type` as a parameter so `deb` remains a one-flag option |
| Maven-phase integration (exec-plugin profile, or antrun `<exec>`) | **No — both blocked offline** (exec absent; antrun's `ant:1.10.12` absent) | — | Low once online | Optional later with the one-time online fetch + explicit user approval. Do NOT claim "single `mvn` command reproducibility" today; it would be a lie the first reviewer without network catches |

### Launcher decision (E3/E4/E6/E7 evidence)

- `jpackage --main-jar/--main-class` with **classpath mode** (no module-info) is the chosen shape; `--main-class = unsa.sistemas.view.ConsoleView` (or `Launcher` post-rename).
- The `ConsoleView` non-`Application` trick is **load-bearing, not cosmetic**: E3 (guard kills `MainApp` on classpath) vs E4 (identical classpath, launcher class, alive) is the A/B proof. Even inside a jlink runtime, keeping a launcher class means one single main story across dev/classpath/jpackage launches.
- jpackage supports module mode (`--module`) but that would require adding `module-info.java` — new surface, zero delivery benefit. Out of scope.
- Manifest `Main-Class` in the jar is still worth one pom block: jpackage then needs no `--main-class`, and the jar's intended role (component of the app-image, not a standalone `java -jar` entry) is documented in README.
- `--add-modules javafx.controls,javafx.fxml` as `--java-options`: kept although E6 shows the image resolves them implicitly — belt-and-suspenders, zero cost.

### Artifact layout proposal

```
dist/                                   # gitignored; produced by package-app.sh
├── ResolucionSistemasLineales/         # jpackage --type app-image output
│   ├── bin/ResolucionSistemasLineales  # native launcher (double-click; spaces-proof per E9)
│   └── lib/
│       ├── app/
│       │   ├── resolucion-sistemas-lineales-1.0-SNAPSHOT.jar   # 57 KB, Main-Class set
│       │   └── ResolucionSistemasLineales.cfg                  # app.classpath/mainclass/java-options
│       ├── runtime/                    # jlink image (55 MB stripped/compressed or 87 MB plain)
│       └── libapplauncher.so
├── ResolucionSistemasLineales-linux-x64.zip   # 29–36 MB (E10) — the email/Drive/USB copy
└── LEEME.txt (or README inside zip)    # "double-click bin/ResolucionSistemasLineales, no Java needed"
```

Hand-in folder for the course: the zip + source code + rewritten README. (Installer: only if the user asks; `deb` is technically available here, `exe` is not — see matrix.)

## Recommendation

**Shape: script-driven, offline-first, `jpackage app-image` on a custom `jlink` runtime, `ConsoleView`/`Launcher` as the main class, zip as the hand-in, per-OS builds honestly fenced by an attestation matrix.**

Implementation plan for the proposal/design/tasks phases — **3 green commits on top of `d7656fa`**, each independently verifiable, none touching the 82-test suite:

1. **Build wiring (offline):** `maven-jar-plugin` manifest `Main-Class` in `pom.xml`; `package-app.sh` implementing jlink → stage → jpackage `app-image` → zip with the exact proven flags (E5/E7); `dist/` gitignored; rename `ConsoleView` → `Launcher` (mechanical: class + its only reference is itself; README line 18 already says to run from `ConsoleView` — rewrite lands in commit 3). Verification: script exits 0 offline; jar manifest contains `Main-Class`; suite still 82 green.
2. **Packaging polish (offline):** `--type`/`--name`/version parameters, spaces-proofing, `LEEME.txt` generated into the zip, optional `--type deb` flag left OFF by default. Verification: E8/E9-style launch probes green.
3. **Verification + docs:** `verify-package.sh` (timeout-alive launcher test with recorded exit code, fixture-solve manual checklist), full README rewrite (build prereqs = JDK 17 on build machine only; run = double-click), per-OS recipe section, attestation matrix recorded in the change folder. Verification: `mvnw -o clean test` 82/82 + launch probe logs.

Why this over Maven-phase integration: it is the **only** shape that is fully reproducible offline today (inventory above), it keeps `pom.xml` honest (no plugin that fails the first cold `~/.m2`), and for an academic reviewer `sh package-app.sh` is a shorter truth than a profile they must discover. If the user approves a one-time online step later, an `exec-maven-plugin` profile can wrap the same script verbatim — the design decision is reversible, the script is the primitive.

## Per-OS Verification Matrix (honesty standard = R1.2/R7.1 precedent)

| Platform | Build offline here | Launch test (window opens, exit 124 alive) | Solve a `pruebas txt/` fixture | Status for verify phase |
|----------|--------------------|--------------------------------------------|--------------------------------|-------------------------|
| Linux x86_64 | **Proven** (E1/E5/E7) | **Proven** (E8, twice incl. spaces path E9) | Manual, `DISPLAY=:1` (same procedure archived for `trace-label-fixes`) | **VERIFIED locally** |
| Windows x64 | **No** — `-win` JavaFX jars absent (inventory), jpackage cannot cross-build (native launcher + `applauncher` are per-OS), `exe` type additionally needs WiX on Windows | N/A on this host | N/A | **UNVERIFIED by default.** Path to VERIFIED: user runs `package-app.cmd` (same logic) on any Windows JDK 17+ machine; needs one online fetch of the 4 `-win` jars (~11 MB). Attestation = user-reported launch |
| macOS (aarch64/x64) | No — no `-mac*` jars cached; needs a Mac host | N/A | N/A | **UNVERIFIED.** Same recipe on macOS hardware |
| Windows/macOS *running on the grader's machine* | — | — | — | Documented as UNVERIFIED with the exact repro command; never asserted as PASS (mirrors `test-execution-baseline` spec notes) |

## Risks

| ID | Risk | Evidence / why it matters | Mitigation |
|----|------|---------------------------|------------|
| P1 | Classifier trap: `~/.m2` has only `-linux` 21.0.13 jars | Inventory above; any pom edit adding `<classifier>win</classifier>` breaks the offline build for everyone | Script derives classifier from the **host** OS; foreign-OS jars fetched only with explicit user approval (one-time, ~11 MB) |
| P2 | jpackage cannot cross-compile; "any computer" ≠ one artifact | Native `bin/` launcher + `libapplauncher.so` are per-OS (E7 layout) | Per-OS script + build-on-that-OS docs; matrix states what is proven where |
| P3 | Cached-looking plugins fail offline (`antrun` without `ant:1.10.12`; `javafx-maven-plugin` without `maven-plugin-api:3.6.0`…) | E2 + inventory | Script route needs zero new plugins; any future plugin wiring ships with a proven cold-cache `mvn -o` run |
| P4 | Space-in-path breakage (`pruebas txt/` precedent) | E9 proves jpackage binary safe; **hand-written shell** wrappers are the actual risk surface | All script paths quoted (`"$SCRIPT_DIR"`); E9 spaces-probe codified into `verify-package.sh` |
| P5 | Zip size vs email limits: 29–36 MB > Gmail 25 MB | E10 | Recommend USB / Drive / campus platform; jlink flag choice tuned at design time (raw-folder vs zip tradeoff documented in E10) |
| P6 | Unsigned Windows exe → SmartScreen/AV noise | jpackage output is unsigned by default; academic LAN copies trip this | README warning ("More info → Run anyway"); signing out of scope (no cert exists to sign with) |
| P7 | Recipient confusion: two READMEs (repo vs in-zip) drift apart | Repo README is already stale today | Generate in-zip `LEEME.txt` from one source (or copy step in the script) so there is one truth |
| P8 | Headless verify host lacks `DISPLAY` | This host has `:1` (proven), CI would not | `verify-package.sh` skips with explicit `UNVERIFIED (no display)` rather than faking PASS — same pattern the archived verify reports used |

## Open Questions (for the user — one at a time, in order of consequence)

1. **Target OS(es) for the hand-in.** Recommended default: ship the VERIFIED Linux x86_64 zip now; add the Windows recipe (script + one-time online fetch of `-win` jars) **only if** classmates/graders are on Windows — confirm which.
2. **Zip folder vs installer.** Recommended default: zip (double-click folder, no root, USB-proof). `.deb` is technically buildable here but adds `dpkg` friction with zero academic upside.
3. **Network one-time fetch approval** — needed only if the user wants (a) Windows/Mac jars pre-cached for another machine's offline build, or (b) an `exec-maven-plugin` `mvn -Ppackage` convenience profile. The recommended default shape needs **zero** network.
4. **Rename `ConsoleView` → `Launcher`?** Recommended: yes (the current name misleads exactly as its old "stub" listing did); one file + README text.
5. **App name in the artifact** — `ResolucionSistemasLineales` (default, proven) vs something with a space (would then need the E9 spaces probe on every OS). No icon artwork exists in the repo; default jpackage icon unless the user supplies one.

## Ready for Proposal

**Yes.** Every decision-relevant question was answered by measurement, not speculation (E1–E11): offline feasibility is green end-to-end for the recommended shape; the exact `jlink`/`jpackage` command lines that work on this repo are recorded; the launcher choice is proven by an A/B test (E3 vs E4); size, zip, path-spaces, and cross-OS boundaries are quantified. The proposal should adopt the **3-commit script-driven app-image shape** above, declare the recommended defaults (Linux zip verified; Windows/Mac UNVERIFIED with build-on-that-OS recipe; no new Maven plugins), and carry questions 1–3 to the user before `sdd-tasks` fixes the acceptance list.

## Skill Resolution

| Skill | Read | Used for |
|---|---|---|
| `sdd-explore` | Yes (`~/.config/opencode/skills/sdd-explore/SKILL.md`) | Phase contract, executor boundary, return envelope |
| `_shared/sdd-phase-common.md` | Yes | Retrieval (B), hybrid persistence (C — file + Engram `sdd/deliverable-packaging/explore`, `capture_prompt: false`), envelope (D) |
| `_shared/openspec-convention.md` | Yes | Artifact path; project precedent `explore.md` (as in `app-end-to-end`) honored over generic `exploration.md` |

Registry fallback (`.atl/skill-registry.md` present) not needed: exact skill paths were in the launch contract; no TDD/refactoring skills apply to a read-only exploration.
