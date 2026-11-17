# Design: deliverable-packaging

- Change: `deliverable-packaging` · Project: `resolucion-sistemas-lineales` · Store: `hybrid`
- Baseline: HEAD `d7656fa`, 82 green. All sizes below **measured this phase** (cache `~/.cache/gentle-deliverable/`, 51 MB, hash-verified).
- No numerical method or convergence change (config `rules.design` first bullet: N/A — build/docs only).

## Technical Approach

`package.sh` (repo root) orchestrates 7 stages producing `dist/TrabajoIntegrador.zip` with `TrabajoIntegrador-linux/` (jlink→jpackage app-image, E5/E7 flags) and `TrabajoIntegrador-windows/` (hand-assembled from pinned, hash-verified Temurin JRE + 4 JavaFX `-win` jars + `Iniciar.bat`). Every hook prints its spec scenario id (`[R2.1] PASS …`). Windows side: static asserts only; runtime **UNVERIFIED** until R8 checklist returns.

## Architecture Decisions

| # | Decision | Options | Choice | Rationale |
|---|----------|---------|--------|-----------|
| D1 | Windows runtime source | Temurin JDK zip 191 MB + trim / **Temurin JRE zip 44 MB as-is** / user-supplied / build-on-Windows | **JRE as-is** | Cross-jlink impossible (jlink is host-native — no `--target-platform`); measured: JDK trim saves nothing meaningful, JRE has no `jmods`/`demo`/`fonts`. 80 MB of `lib/modules` is the untrimmable jimage |
| D2 | R3 size bounds | keep 40–120 MB / **document layout change → 120–160 MB (full folder, raw)** | **120–160 MB** | Measured folder ≈ 136.5 MB (127.0 runtime + 9.4 jfx + 0.06 app + docs). R3 allows moving bounds with a documented layout change — this is that document. Truncation still caught (< 120 MB) |
| D3 | Zip flag combo | E5 stripped runtime | **E5 flags** (`--strip-debug --no-man-pages --no-header-files --compress=2`) | Proven offline E5/E6. Expect ~36 MB linux + ~53 MB windows zip parts → **total zip ~85–90 MB** (corrects proposal's 50–65 est.); README states measured number + USB/Drive hand-in |
| D4 | Win launch mode | module-path everything / **jfx dirs on `--module-path`, app jar on `-cp`, main `Launcher`** | latter | Exact mirror of E4 (A/B-proven) and the linux `.cfg` (E7); app is non-modular so it cannot go on the module path |
| D5 | Windows jar check | "each jar contains .dll" per R3 wording | **graphics jar contains ≥1 `.dll`; all 4 checked by pinned hash** | **Measured deviation: only `javafx-graphics-win.jar` bundles natives (53 dlls incl. `prism_*.dll`, `javafx_font.dll`); base/controls/fxml contain 0** — Open Question Q1 |
| D6 | `Iniciar.bat` java vs javaw | `javaw.exe` silent / **`java.exe` visible console** | `java.exe` | Spec R3 mandates it; during attestation the console surfaces errors for the report. LEEME: "una ventana de consola acompaña a la app" |
| D7 | jar plugin version | 3.5.0 / **3.4.1 explicit** | 3.4.1 | Already used offline by current build (E1 `Created-By: Maven JAR Plugin 3.4.1`); transitive tree proven; no new deps |
| D8 | Doc language | all-English / **LEEME+bat Spanish, checklist English** | see Testing | LEEME is an end-user handout (UNSA Spanish users) — documented product exception; code javadoc Spanish = existing convention (`MainApp` comments). Flagged: Q2 |
| D9 | Static assets in git | heredocs in script / **`package/` dir with `pins.txt`, `Iniciar.bat`, `LEEME.txt`, `CHECKLIST-WINDOWS.txt`** | `package/` | Reviewable, grep-verifiable (R3.2/R5.3 read committed files directly); script copies + verifies, no drift (P7) |

## Pinned artifacts (R5 — committed to `package/pins.txt`)

| Artifact | SHA-256 (SHA-1 for jars) | URL |
|---|---|---|
| `OpenJDK17U-jre_x64_windows_hotspot_17.0.21_9.zip` (43,860,024 B) | `fe4656bd78fb09812dc38c49da777f79cc33e7ee435a254be82f9e7f40d9d117` | `https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.21%2B9/OpenJDK17U-jre_x64_windows_hotspot_17.0.21_9.zip` |
| `javafx-base-21.0.13-win.jar` (753,226 B) | `7a9a3dd9e7ab445ae751f50d9feddd550056ef5c267faf145512686a58f61651` | `repo.maven.apache.org/maven2/org/openjfx/javafx-base/21.0.13/…-win.jar` |
| `javafx-graphics-21.0.13-win.jar` (5,911,208 B) | `42d59a74a1d0fcfae03ecd440db185e76f08b7a65dfc870a0a7eb5bad6d8f2d7` | `…/javafx-graphics/21.0.13/…-win.jar` |
| `javafx-controls-21.0.13-win.jar` (2,582,586 B) | `2fbf566741cba2de8dd886209cd8ba497995b51d2371441fc4d17e0a22ba6e03` | `…/javafx-controls/21.0.13/…-win.jar` |
| `javafx-fxml-21.0.13-win.jar` (130,030 B) | `67d4ad011c60e770d23369796669d5ab28075a8cdc91f792c28d97ebb96b25ef` | `…/javafx-fxml/21.0.13/…-win.jar` |

All sha1s matched Central-published values on download; sha256s computed locally. API `latest/17` resolved 2026-11-17 to `jdk-17.0.21+9`; the pin uses the **immutable** per-release URL. Cache validation = sha256 recheck (skip stage 0 when valid).

## Measured Windows layout

`runtime/` = JRE zip extracted 1:1 (`jdk-17.0.21+9-jre/` → `runtime/`): 127 MB — `bin/` 44 MB (java.exe, javaw.exe, 85 dlls, `server/jvm.dll` 12.7 MB), `lib/` 82 MB (`modules` 80 MB), `conf/`, `legal/`, `release`, `NOTICE`. Verified present: `bin/java.exe` ✓, `bin/server/jvm.dll` ✓, graphics-jar natives ✓, `module-info.class` in jars ✓.

## package.sh — stages, contract

`set -Eeuo pipefail`; env overrides `DELIVERABLE_CACHE` (default `~/.cache/gentle-deliverable`) and `DISPLAY`. Flags: `--fetch` (opt-in network), default = cache-only. Exit codes: 0 ok · 1 missing input (fail-fast **before** stage 5) · 2 hash mismatch (reject before use) · 3 `SMOKE FAILED` · 4 `UNVERIFIED (no display)`.

| Stage | Action | Scenario ids printed |
|---|---|---|
| 0 fetch | `--fetch`: curl pins → cache; always sha256-verify; valid cache ⇒ zero network (idempotent skip) | R5.1 R5.2 R5.3 |
| 1 build | `sh ./mvnw -o clean package -DskipTests`; grep `Main-Class` in manifest | R4.2 R7.1 |
| 2 jlink | E5 flags, cached `-linux` jars → `dist/work/runtime-linux` (55 MB) | R2.1 (modules list) |
| 3 jpackage | `--type app-image --name TrabajoIntegrador` + E7 options; static inspect (`file` ELF, `-x`, jar in `lib/app/`) | R2.1 |
| 4 win assembly | precondition gate (R1.2) → hash-verify → extract JRE → copy app jar + 4 jars + `package/{Iniciar.bat,LEEME.txt,CHECKLIST-WINDOWS.txt}`; size bounds 120–160 MB; bat grep | R3.1 R3.2 R3.3 |
| 5 zip | `dist/TrabajoIntegrador.zip` from the two folders; `unzip -l` tree assert; copy checklist into change folder | R1.1 R8.1 |
| 6 smoke | `timeout 12 dist/…/bin/TrabajoIntegrador`; PASS iff exit 124 (or `xwininfo` frame); `import -window root` screenshot; copy evidence to `dist/evidence/` **and** `openspec/changes/deliverable-packaging/evidence/`; headless → exit 4 | R2.2 R2.3 |
| 7 summary | table scenario→PASS/FAIL/UNVERIFIED; `git ls-files dist/` empty + `.gitignore` grep; log line `static-verified / runtime-UNVERIFIED` | R1.3 R3.1 |

## Iniciar.bat (committed `package/Iniciar.bat`)

```bat
@echo off
rem TrabajoIntegrador - no necesita Java instalado. Ver LEEME.txt
"%~dp0runtime\bin\java.exe" --module-path "%~dp0app" --add-modules javafx.controls,javafx.fxml -cp "%~dp0app\resolucion-sistemas-lineales-1.0-SNAPSHOT.jar" unsa.sistemas.view.Launcher
if errorlevel 1 (
  echo.
  echo La aplicacion no pudo iniciar. Reporte este mensaje con CHECKLIST-WINDOWS.txt
  pause
)
```

Every `%~dp0` is quoted (R3.2); no accented chars (codepage-safe); bat stays ASCII. **Ordering fallback** (if dir-form module-path misbehaves on some Windows): explicit 4-jar `--module-path` with `;` separators — documented in design, not shipped by default.

## pom.xml manifest block (D7)

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-jar-plugin</artifactId>
    <version>3.4.1</version>
    <configuration><archive><manifest>
        <mainClass>unsa.sistemas.view.Launcher</mainClass>
    </manifest></archive></configuration>
</plugin>
```

## Launcher rename (R4)

`git mv src/unsa/sistemas/view/ConsoleView.java …/Launcher.java`; class + javadoc only. Re-greped this phase: **exactly 1 match** in `src test pom.xml` (the declaration). Javadoc follows java-docs skill + project's Spanish-comment convention; claims only what E3/E4 prove:

```java
/**
 * Punto de entrada empaquetado de la aplicación.
 * No extiende {@link javafx.application.Application}: reenvía {@code main}
 * a {@link MainApp#main(String[])}, lo que permite lanzar la GUI desde el
 * classpath (jar con {@code Main-Class}, app-image jpackage), donde el
 * lanzador del JDK rechaza a {@code MainApp} si JavaFX no está en los
 * módulos de arranque.
 */
```

README `ConsoleView` mentions (lines 14, 18, 71) are fixed by the commit-4 rewrite — never stale in any intermediate commit? No: they go stale **only** inside commit 1→3 window on a renamed class; acceptable (README rewrite is the same change, commit 4; noted in risks).

## File Changes

| File | Action |
|---|---|
| `src/unsa/sistemas/view/{ConsoleView→Launcher}.java` | rename + javadoc |
| `pom.xml` | jar-plugin block above |
| `package.sh` · `package/{pins.txt,Iniciar.bat,LEEME.txt,CHECKLIST-WINDOWS.txt}` | create |
| `.gitignore` | + `dist/` |
| `README.md` | rewrite run instructions (commit 4) |
| `openspec/changes/deliverable-packaging/evidence/` | screenshot/logs/checklist copy |

## Verification Matrix

| Scenario | Hook/manual | Where |
|---|---|---|
| R1.1 / R1.3 | `unzip -l` tree assert / git gate | stage 5 / 7 |
| R1.2 | manual apply probe: `DELIVERABLE_CACHE=/tmp/empty bash package.sh` → exit 1, no zip | apply |
| R2.1–R2.3 | stage 3/6 hooks (tools verified present: `xwininfo`, `import`; `timeout 12` pattern proven E8/E9) | automated |
| R3.1–R3.3 | stage 4 hooks; D5 replaces R3's "each jar has .dll" with hash-check + graphics-dll-check (**Q1**) | automated + verify |
| R4.1–R4.3 | grep gate / stage 1 / manual review vs E3-E4 | commit 1 + verify |
| R5.1–R5.3 | corrupt-cache probe (exit 2) / `--fetch` absent run / read `pins.txt` | apply + verify |
| R6.1 / R6.2 | cross-check README paths vs `unzip -l`; labels vs log | verify manual |
| R7.1 / R7.2 | `mvnw -o clean test` 82 green per commit; `git diff d7656fa` scope check | every commit + verify |
| R8.1 / R8.2 | stage 5 listing + archived copy; status flip = returned signed checklist **only** | automated + policy |

## Commit Plan (work units; repo style: lowercase Spanish conventional commits; each < 400 lines, green)

| # | Commit | Contents | Rollback |
|---|--------|----------|----------|
| 1 | `refactor: renombrar ConsoleView a Launcher y fijar Main-Class en el jar` | rename + javadoc + pom block; gates: grep 0, manifest, 82 green | revert → `ConsoleView` + no manifest; no runtime consumers yet |
| 2 | `build: agregar package.sh con runtime linux y app-image verificado` | script stages 1-3,6-7 (linux path only, `--linux-only` guard for stages 4-5 stubs failing cleanly), `.gitignore dist/`; smoke evidence committed | revert script+gitignore; `dist/` regenerable, never committed |
| 3 | `build: ensamblar la carpeta windows con runtime Temurin pinneado` | stages 0,4,5, `package/` assets (pins, bat, LEEME, checklist); R3/R5 hooks green | revert → `--linux-only` still works (commit 2 shape) |
| 4 | `docs: reescribir README con instrucciones reales y empaquetar el zip final` | full README (measured zip size, hand-in, attestation matrix, D6 note), evidence + checklist copies archived; R6 checks | revert README only; artifacts untouched in git |

## Testing Strategy

JUnit adds nothing (suite guards regression only — 82 green at each commit). Ground truth = script hooks + archived screenshot + attestation. Linux E2E: stage 6 smoke (window alive 12 s, exit 124). Windows: **UNVERIFIED by design** — static gates here; flip path = `CHECKLIST-WINDOWS.txt` (English, per orchestrated contract; ships in zip + archived) executed on a real Windows box, per R8.2 no signed checklist ⇒ forever UNVERIFIED.

## Risks

| Risk | Mitigation |
|---|---|
| Temurin drift (`latest` pointer) | immutable per-release URL + sha256 pin; update = deliberate pin-commit |
| 17.0.21 JRE + JavaFX 21.0.13 natives on Windows | identical pairing VERIFIED on linux (E5-E9); windows analogy only → R8 attests |
| Zip ~85-90 MB > proposal's 50-65 | measured components above; README hand-in instructions; Gmail limit irrelevant |
| D5 spec-wording gap (jars "each" with dlls) | **Q1 — resolve before verify** |
| bat quoting/semicolons | R3.2 grep hook; fallback form documented |
| Cache 51 MB eviction between phases | stage 0 `--fetch` reproducible; pins committed |
| README stale `ConsoleView` between commits 1-3 | accepted (docs commit 4, same change; final tree consistent) |

## Open Questions

- [ ] **Q1 (spec vs reality):** R3 says all four `-win` jars contain `.dll` natives — measured: only `graphics` does. Accept D5 hook substitution (verify records it), or amend the delta-spec wording?
- [ ] **Q2 (language):** checklist ships English per contract, but its Windows executor is likely a Spanish-speaking classmate — flip to Spanish (same end-user exception as LEEME)?
