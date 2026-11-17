# Verification Report

**Change**: `establish-build-test-baseline`
**Version**: N/A (delta spec `test-execution-baseline`, R1–R8)
**Mode**: Standard (`strict_tdd: false`)
**Date**: 2026-11-17
**Repository**: `resolucion-sistemas-lineales` @ `db0c95d` on `main`, working tree clean
**Base commit**: `e7bb7be`

**Verdict**: **PASS WITH WARNINGS** — every locally reproducible requirement passed with real command output; two scenarios (R1.2, R7.1) are not reproducible in this environment and are reported as UNVERIFIED, not PASS.

---

## Executive Summary

All 8 requirements were re-executed independently from the artifacts and repo state (the Engram `apply-progress` claims were treated as claims only):

- `sh ./mvnw -o clean compile` → exit 0, `BUILD SUCCESS`, `Copying 2 resources from src`, 23 sources → 26 classes, 0 `.class` under `src/`.
- `sh ./mvnw -o clean test` → exit 0, `Tests run: 52, Failures: 0, Errors: 0, Skipped: 0`, provider `JUnitPlatformProvider`, exactly 11 `TEST-*.xml`, 0 `*.dump*`, no `TrazaDemo`.
- Anti-false-green was proven twice on a COMMITTED test file (assertion-literal mutation and `assertThrows` expected-type swap): both produced `BUILD FAILURE`, exit 1, `Failures: 1`, and a `<failure>` naming the method; both were reverted with `git checkout --` and the suite returned to 52 green with a clean tree.
- Scope diff `e7bb7be..HEAD` is exactly the 21 expected paths (`pom.xml`, `mvnw` mode-only, 11 `test/*.java`, 8 `openspec/**`); zero changes to `src/**`, `README.md`, `pruebas txt/**`, `src/**/*.fxml`, or `test/TrazaDemo.java`.
- `sh ./mvnw -o clean package` → exit 0, `BUILD SUCCESS`, jar `.class` count 26 == `target/classes` `.class` count 26.

No CRITICAL findings. The two warnings are environmental limits (no JDK 21 JVM installed; network cannot be truly disabled here), not implementation defects. No flake occurred during this verification run.

---

## Completeness

| Metric | Value |
|--------|-------|
| Tasks total | 24 |
| Tasks complete | 21 |
| Tasks incomplete | 3 (T22–T24: rollback procedures, intentionally not executed by design) |
| Implementation scope | 100% of T1–T21 complete |

T22–T24 are documented rollback procedures (revert PR3 / revert PR2 / revert whole change), not deliverables. All three are written and marked unchecked in `tasks.md`; not executing them is the intended state, not a gap.

---

## Requirement Matrix

| Req | Status | Evidence (command → observed) |
|-----|--------|-------------------------------|
| R1 Build reproducibility | **PASS** (R1.1) / **UNVERIFIED** (R1.2) | `sh ./mvnw -o clean compile` → exit 0, `BUILD SUCCESS`, `[INFO] Copying 2 resources from src to target/classes`, `Compiling 23 source files with javac [debug release 17]`, `find src -name '*.class'` → 0, `find target/classes -name '*.class'` → 26. No `-D` property override. R1.2: only JDK 17 is installed (`ls /usr/lib/jvm/` → `java-17-openjdk-amd64`; `update-alternatives --list java` → single entry). |
| R2 Test execution | **PASS** | `sh ./mvnw -o clean test` → exit 0, `[INFO] Tests run: 52, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`, `Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider`, 11 `TEST-*.xml`, 0 `*.dump*`, `grep -c "JUnit3Provider"` → 0, `grep -c 'tests="0"'` → 0. |
| R3 Failure propagation | **PASS** | Two independent red proofs on committed `test/NormasTest.java`: literal mutation → exit 1 / `Failures: 1` / `BUILD FAILURE` / `<failure message="‖(3,-4)‖₁ ==> expected: <8.0> but was: <7.0>">`; `assertThrows` type swap → exit 1 / `Failures: 1` / `<failure message="norma1 vector null ==> Unexpected exception type thrown, expected: <java.lang.IllegalStateException> but was: <java.lang.IllegalArgumentException>">` naming `verificaValidaciones`. Both reverted; rerun `Tests run: 52, Failures: 0, Errors: 0` and `git status --porcelain` empty. |
| R4 No process-killing / print signals | **PASS** | `grep -rn "System.exit\|TEST_OK\|TEST_FALLO" test/` → no matches (exit 1); `grep -rn "static void main" test/` → only `test/TrazaDemo.java:19`. Harness helpers absent: `grep -rn "compara(\|chequea(\|esperarIae(\|static boolean ok" test/` → no matches; `System.out` only in untouched `TrazaDemo.java`. |
| R5 Test discovery hygiene | **PASS** | 11 `[INFO] Running <Class>` lines: AnalisisSensibilidad, Cholesky, Crout, EliminacionGaussiana, GaussJordan, GaussSeidel, Jacobi, Normas, RelajacionSimple, SistemaController, SOR. `grep -rl "TrazaDemo" target/surefire-reports/` → 0. `git hash-object test/TrazaDemo.java` = `git rev-parse e7bb7be:test/TrazaDemo.java` = `b2543944f7c3280f1789910eba67eaf508d64d3e`; `git diff e7bb7be..HEAD -- test/TrazaDemo.java` → 0 lines. |
| R6 Wrappers and tooling | **PASS** | `git ls-files -s mvnw` → `100755 bd8896bf... mvnw`; `git diff --summary e7bb7be..HEAD -- mvnw` → `mode change 100644 => 100755 mvnw` (no content change); `./mvnw -v` (no `sh`) → exit 0, `Apache Maven 3.9.11`, Java `17.0.20.1`. |
| R7 Offline capability | **PASS** (`-o` resolution) / **UNVERIFIED** (R7.1) | Every gate in this report ran with `-o` and succeeded: `clean compile`, `clean test`, two `test` red probes, post-revert `test`, `clean package`. Truly network-disabled execution is not reproducible in this environment. |
| R8 Scope exclusion guard | **PASS** | `git diff --name-status e7bb7be..HEAD` → 21 paths: `M mvnw`, `M pom.xml`, 11 `M test/*Test.java`, 8 `A openspec/**` (committed in `db0c95d`). Negative diff `git diff --name-only e7bb7be..HEAD -- 'src/**' README.md 'pruebas txt' test/TrazaDemo.java` → empty. `find src -name '*.class'` → 0; `git ls-files 'src/**/*.class'` → 0. `sh ./mvnw -o clean package` → exit 0, `BUILD SUCCESS`, jar contains 26 `.class` == 26 in `target/classes`. |

---

## Build & Tests Execution

**Build — R1** ✅
```text
$ sh ./mvnw -o clean compile            # exit 0
[INFO] --- resources:3.3.1:resources (default-resources) ---
[INFO] Copying 2 resources from src to target/classes
[INFO] Compiling 23 source files with javac [debug release 17] to target/classes
[INFO] BUILD SUCCESS

$ find src -name '*.class' | wc -l      # 0
$ find target/classes -name '*.class' | wc -l   # 26
$ find target/classes -type f ! -name '*.class'
unsa/sistemas/view/estilos.css
unsa/sistemas/view/Sistema.fxml
```

**Tests — R2** ✅ 52 passed / 0 failed / 0 skipped
```text
$ sh ./mvnw -o clean test               # exit 0
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] Running GaussJordanTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.202 s -- in GaussJordanTest
... (11 classes, 52 methods) ...
[INFO] Tests run: 52, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS

$ ls target/surefire-reports/TEST-*.xml | wc -l   # 11
$ find target/surefire-reports -name '*.dump*' | wc -l   # 0
$ grep -rl "TrazaDemo" target/surefire-reports/ | wc -l   # 0
```

**Package — R8** ✅
```text
$ sh ./mvnw -o clean package            # exit 0
[INFO] Copying 2 resources from src to target/classes
[INFO] Tests run: 52, Failures: 0, Errors: 0, Skipped: 0
[INFO] Building jar: target/resolucion-sistemas-lineales-1.0-SNAPSHOT.jar
[INFO] BUILD SUCCESS

$ unzip -l target/*.jar | grep -c '\.class'   # 26
$ find target/classes -name '*.class' | wc -l # 26
```

**Coverage**: ➖ Not available (`testing.coverage.available: false`, threshold 0).

---

## Red Proof Evidence (R3)

All probes were run on the working tree of a COMMITTED file and reverted with `git checkout --`. Nothing red was committed; no branch/stash/revert-history operation was used.

### R3.1 — assertion literal must turn the build red

```text
$ sed -i 's/assertEquals(7.0, Normas.norma1(v)/assertEquals(8.0, Normas.norma1(v)/' test/NormasTest.java
$ git diff --stat test/NormasTest.java
 test/NormasTest.java | 2 +-

$ sh ./mvnw -o test                     # exit 1
[ERROR] Tests run: 5, Failures: 1, Errors: 0, Skipped: 0 ... in NormasTest
[ERROR] NormasTest.verificaNormasVectoriales ... <<< FAILURE!
[ERROR]   NormasTest.verificaNormasVectoriales:29 ‖(3,-4)‖₁ ==> expected: <8.0> but was: <7.0>
[ERROR] Tests run: 52, Failures: 1, Errors: 0, Skipped: 0
[INFO] BUILD FAILURE

$ grep -c '<failure' target/surefire-reports/TEST-NormasTest.xml          # 1
$ grep '<testsuite ' target/surefire-reports/TEST-NormasTest.xml
<testsuite ... name="NormasTest" time="0.142" tests="5" errors="0" skipped="0" failures="1">
$ grep '<failure ' target/surefire-reports/TEST-NormasTest.xml
<failure message="‖(3,-4)‖₁ ==&gt; expected: &lt;8.0&gt; but was: &lt;7.0&gt;" type="org.opentest4j.AssertionFailedError">
```

### R3.2 — `assertThrows` expected-type mismatch must turn the build red

```text
$ sed -i '0,/assertThrows(IllegalArgumentException.class,/s//assertThrows(IllegalStateException.class,/' test/NormasTest.java
$ sh ./mvnw -o test                     # exit 1
[ERROR] Tests run: 5, Failures: 1, Errors: 0, Skipped: 0 ... in NormasTest
[ERROR] NormasTest.verificaValidaciones ... <<< FAILURE!
[ERROR] Tests run: 52, Failures: 1, Errors: 0, Skipped: 0
[INFO] BUILD FAILURE

$ grep '<failure ' target/surefire-reports/TEST-NormasTest.xml
<failure message="norma1 vector null ==&gt; Unexpected exception type thrown, expected: &lt;java.lang.IllegalStateException&gt; but was: &lt;java.lang.IllegalArgumentException&gt;" ... >
```

### Restoration

```text
$ git checkout -- test/NormasTest.java
$ git status --porcelain                 # (empty)
$ diff -q test/NormasTest.java /tmp/opencode/sdd-verify/NormasTest.java.orig
MATCHES-SAVED-ORIG
$ sh ./mvnw -o test                      # exit 0
[INFO] Tests run: 52, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

No red state survived. Final `git status --porcelain` (after all gates, including `clean package`) is empty at `db0c95d`. Writing this report is the only subsequent working-tree change (`?? openspec/changes/establish-build-test-baseline/verify-report.md`); no source, pom, wrapper, or test file was touched.

---

## Static Correctness Checks

| Check | Command | Result |
|-------|---------|--------|
| `@Test` distribution | `grep -c '@Test' test/*Test.java` | 4/4/3/4/4/3/3/5/4/14/4 per class → total **52**, exactly the design multiset (4/3/4/4/14/5/3/3/4/4/4). Surefire `tests="N"` attrs also sum to 52. |
| `AnalisisSensibilidadTest` tolerance | `grep -n 1e-9 / 1e-6 test/AnalisisSensibilidadTest.java` | `1e-9` at lines 14 and 26; `1e-6` → NONE. Not normalized. `1e-8` present only as test-matrix input (`double e = 1e-8`). `1e-9` appears nowhere else in `test/`. |
| No exact-equality `assertEquals(double,double,String)` | `grep -rn "assertEquals(" test/ \| grep -v "1e-6\|1e-9\|1e-8"` | no matches — every numeric assert passes a delta (4-arg). |
| No `package` declarations | `grep -rn "^package " test/` | none. |
| Controller array asserts | `grep -c assertArrayEquals test/SistemaControllerTest.java` | 5, all 4-arg `(expected, actual, 1e-6, msg)`. Negative paths asserted through `Resultado` flags/messages (`assertTrue(!r.isExitoso(), ...)`), so failures still propagate. |
| `assertThrows` presence | per-file counts | Normas 6, AnalisisSensibilidad 4, SOR 3, Cholesky 2, Crout/EliminacionGaussiana/GaussJordan/GaussSeidel/Jacobi/RelajacionSimple 1 each. Exact exception types preserved (no widening to `RuntimeException`). |
| SOR ω=1 oracle kept verbatim | read `test/SORTest.java:29-37` | `assertEquals(xGs[i], xSor[i], 1e-6, msg)` — SOR vs GaussSeidel comparison, not a substituted literal. |
| `TrazaDemo.java` untouched | hash + diff | identical blob hash to base; 0-line diff. |
| `openspec/config.yaml` truthfulness | read file + run its commands | `testing.runner.command` / `rules.apply.test_command` / `rules.verify.test_command` = `sh ./mvnw -o clean test` → verified exit 0 with 52 tests; expected count 52 stated in `context` and `runner` comment; `strict_tdd: false`; `verify.build_command` = `sh ./mvnw -o clean compile` → verified exit 0. Claims match reality. |

---

## Coherence with Design

| Design decision | Followed? | Notes |
|-----------------|-----------|-------|
| `maven.compiler.release=17` | ✅ Yes | `pom.xml:15`; compile log shows `release 17`. |
| `junit-jupiter` `${junit.version}=5.11.4`, scope `test` | ✅ Yes | `pom.xml:17,36-41`. |
| `maven-surefire-plugin` `3.5.4`, no `<configuration>` | ✅ Yes | `pom.xml:69-73`; provider auto-detected as `JUnitPlatformProvider`. |
| Keep compiler `3.14.1`, javafx plugin `0.0.8` | ✅ Yes | `pom.xml:64-65,77-79`; javafx `21.0.13`. |
| `<resources>` excludes `**/*.java` + `**/*.class` | ✅ Yes | `pom.xml:50-58`; `Copying 2 resources`, no stale class copy. |
| Conversion contract (`compara` → 4-arg `assertEquals`, `chequea` → `assertTrue`, `esperarIae`/try-catch → `assertThrows`, `comparaVector` → 4-arg `assertArrayEquals`) | ✅ Yes | Verified in all 11 files; no harness helpers remain. |
| Tolerance 1e-6 everywhere except `AnalisisSensibilidadTest` 1e-9 | ✅ Yes | Static check above. |
| No `package` line added | ✅ Yes | None in `test/`. |
| `mvnw` mode-only change | ✅ Yes | `git diff --summary` shows mode change only. |
| Commit style `type: descripcion` | ✅ Yes | 14 commits: 1 `build:`, 1 `chore:`, 11 `test:`, 1 `docs:`. |
| TrazaDemo not converted / not discovered | ✅ Yes | Unmodified hash; no report entry. |

---

## Findings

### CRITICAL
None. No false-green, no out-of-scope change, no surviving red state.

### WARNING

**W1 — R1 / Scenario R1.2 (JDK 21 build) not reproducible locally**
- Evidence: `ls /usr/lib/jvm/` → `java-1.17.0-openjdk-amd64`, `java-17-openjdk-amd64`, `openjdk-17`; `update-alternatives --list java` → only `/usr/lib/jvm/java-17-openjdk-amd64/bin/java`. `mvnw -v` reports `Java version: 17.0.20.1`.
- Why it matters: `release=17` is claimed to let a JDK 21 JVM build the project, but this checkout cannot prove it. The compiler is already constrained by `release 17` (visible in the compile log), so the risk is low, but the scenario stays unproven until a CI job or a JDK 21 host runs it.

**W2 — R7 / Scenario R7.1 (truly network-disabled run) not reproducible locally**
- Evidence: all gates used `-o` and resolved from the warm `~/.m2` cache (compile, test, two red probes, post-revert test, package — all exit 0). Network isolation cannot be enforced in this environment.
- Why it matters: `-o` proves Maven never reached out, but a host-level network cut is the only way to rule out a silent local proxy/mirror fallback; the requirement remains unproven in that stricter form.

### SUGGESTION

**S1 — `openspec/config.yaml` command form differs from `tasks.md` T20**
- Evidence: T20 says to set `sh ./mvnw -o test`; the committed config uses `sh ./mvnw -o clean test` (verified working). The deviation is documented in `apply-progress`.
- Why it matters: only documentation drift; both commands are truthful, and the `clean` form is what the design's verification gates actually run.

**S2 — Rollback tasks T22–T24 remain unchecked**
- Evidence: `tasks.md` marks T22, T23, T24 as `[ ]`; all other tasks are `[x]`.
- Why it matters: they are procedures, not deliverables; leaving them unchecked correctly records that no rollback was executed.

**S3 — Preflight commit-count expectation does not match the repo**
- Evidence: `git rev-list --count e7bb7be..HEAD` → **14**, not 17. Composition: 1 `build:` (pom), 1 `chore:` (mvnw mode), 11 `test:` conversions, 1 `docs:` (openspec). HEAD `db0c95d` matches the expected tip.
- Why it matters: the history is internally coherent and matches the 3-slice plan; the 17 figure in the orchestration brief is a miscount, not a missing-commit signal.

---

## Deviations Found

1. **Config command form** (`clean test` vs `test`) — S1, benign and documented.
2. **R3 red-probe run size**: the spec's R3.1 probe was executed on the final committed tree, so the red run reported `Tests run: 52, Failures: 1` (the apply phase had recorded an intermediate `Tests run: 37, Failures: 1`). The assertion under test is slice-independent; the failure stayed confined to `TEST-NormasTest.xml`.
3. **Rollback tasks not executed** — S2, by design.

No deviations from the design's pom wiring, conversion mapping, tolerance policy, discovery strategy, or scope allowlist.

---

## Unverified Items

| Item | Reason |
|------|--------|
| R1.2 — build on a JDK 21 machine | Only JDK 17 is installed on this host; no JDK 21 toolchain or `JAVA_HOME` alternative is available. Recommend a CI matrix job with `actions/setup-java` 21 to close it. |
| R7.1 — offline suite with networking physically disabled | Network cannot be dropped in this session; `-o` runs are the strongest available proxy and all passed. |

---

## Reproducibility Notes (known flake)

The intermittent `testCompile` `NoSuchFileException` on `clean test` did **not** reproduce during this verification. Every gate ran once and produced its expected outcome on the first attempt (green green, red red); no rerun was needed, so the "rerun once before diagnosing" rule was not exercised.

---

## Artifacts

- File: `openspec/changes/establish-build-test-baseline/verify-report.md`
- Engram: topic key `sdd/establish-build-test-baseline/verify-report` (type `architecture`, project `resolucion-sistemas-lineales`)

**Next recommended**: `sdd-archive` — implementation and verification are complete; acknowledge W1/W2 (environmental, no code change) before archiving. Do not start archive from this phase (interactive mode).
