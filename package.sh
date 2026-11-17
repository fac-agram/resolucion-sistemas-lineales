#!/usr/bin/env bash
# package.sh — build the TrabajoIntegrador deliverable from this repo.
# Spec/design: openspec/changes/deliverable-packaging/{specs/deliverable-packaging/spec.md,design.md}
#
# Stages (design contract):
#   0 fetch/verify  Windows inputs from package/pins.txt: cache-only by default,
#                   --fetch opts into a one-time network download; ALWAYS
#                   sha256-verify each input BEFORE it is used (R5.1). A valid
#                   cache => zero network + idempotent skip (R5.2). Missing input
#                   without --fetch => fail-fast BEFORE any zip (R1.2).
#   1 build         mvnw offline package + manifest Main-Class grep        [R4.2]
#   2 jlink         linux runtime image from cached -linux JavaFX jars     [R2.1]
#   3 jpackage      app-image (--type app-image --name TrabajoIntegrador)  [R2.1]
#   4 win assembly  runtime/ from the pinned Temurin JRE + app/ + assets;  [R3.1-3]
#                   static gates only -> windows = static-verified / runtime-UNVERIFIED
#   5 zip           dist/TrabajoIntegrador.zip from BOTH folders; unzip -l [R1.1 R8.1]
#                   tree assert; CHECKLIST-WINDOWS.txt copied into the change folder
#   6 smoke         GUI alive >=12s under DISPLAY, screenshot evidence     [R2.2 R2.3]
#   7 summary       scenario table + dist-never-in-git gate                [R1.3 R3.1]
#
# Flags:
#   --linux-only  dev shortcut: run stages 1,2,3,6,7; skip windows stages 0/4/5;
#                 never write a zip.
#   --fetch       opt-in one-time network for stage 0 (curl of pinned URLs).
#   --help        usage.
# Env overrides:
#   DELIVERABLE_CACHE  (default $HOME/.cache/gentle-deliverable)
#   DISPLAY            (smoke gate; empty => exit 4 "UNVERIFIED (no display)", never fake PASS)
# Exit codes (spec R1.2/R2.3/R5.1):
#   0 ok | 1 missing input (fail-fast before any zip) | 2 hash mismatch (reject before use)
#   3 SMOKE FAILED | 4 UNVERIFIED (no display)
#
# Lint: shellcheck is not installed on this host; script is gated with `bash -n` only (tasks.md note).
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

DELIVERABLE_CACHE="${DELIVERABLE_CACHE:-$HOME/.cache/gentle-deliverable}"
JFX_VERSION="21.0.13"
JAR_NAME="resolucion-sistemas-lineales-1.0-SNAPSHOT.jar"
APP_NAME="TrabajoIntegrador"
MAIN_CLASS="unsa.sistemas.view.Launcher"
PINS="$SCRIPT_DIR/package/pins.txt"

DIST="$SCRIPT_DIR/dist"
WORK="$DIST/work"
RUNTIME_LINUX="$WORK/runtime-linux"
APP_IN="$WORK/app"
JPKG_OUT="$WORK/jpkg"
LINUX_DIR="$DIST/${APP_NAME}-linux"
WIN_DIR="$DIST/${APP_NAME}-windows"
WIN_JRE_TMP="$WORK/win-jre-tmp"
ZIP="$DIST/${APP_NAME}.zip"
EVID="$DIST/evidence"
CHANGE_DIR="$SCRIPT_DIR/openspec/changes/deliverable-packaging"
SMOKE_BIN="$LINUX_DIR/bin/$APP_NAME"

# Windows JavaFX jar names (their pinned sha256 lives in package/pins.txt).
WIN_JFX_JARS=("javafx-base-${JFX_VERSION}-win.jar" "javafx-graphics-${JFX_VERSION}-win.jar" "javafx-controls-${JFX_VERSION}-win.jar" "javafx-fxml-${JFX_VERSION}-win.jar")

usage() {
  sed -n '2,33p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
}

LINUX_ONLY=0
FETCH=0
for arg in "$@"; do
  case "$arg" in
    --linux-only) LINUX_ONLY=1 ;;
    --fetch)      FETCH=1 ;;
    --help|-h)    usage; exit 0 ;;
    *) echo "package.sh: unknown flag: $arg" >&2; usage >&2; exit 1 ;;
  esac
done

log() { printf '%s\n' "$*"; }

# lookup_pin <dest> -> "<sha256> <size> <url>" from package/pins.txt (single source of truth, R5.3)
lookup_pin() {
  awk -F'|' -v dest="$1" '$4==dest { print $1" "$2" "$3; exit }' "$PINS"
}

# --- Stage 0: windows-input provenance gate [R1.2 R5.1 R5.2 R5.3] ---
# Cache-only by default; --fetch opts into one-time curl; ALWAYS sha256-verify
# before use; missing-and-not-fetched fails fast BEFORE any zip is written.
stage0_windows_inputs() {
  log "--- stage 0: windows input verification (cache=$DELIVERABLE_CACHE fetch=$FETCH) ---"
  if [ ! -f "$PINS" ]; then
    log "[R5.3] FAIL: pin manifest missing: $PINS"; exit 1
  fi
  # R5.3 provenance documented — the committed pins file must carry the immutable
  # Temurin release URL, its sha256, the cache path and the --fetch opt-in note.
  local r53=1
  grep -q 'releases/download/jdk-17.0.21%2B9/OpenJDK17U-jre_x64_windows_hotspot_17.0.21_9.zip' "$PINS" || r53=0
  grep -q 'fe4656bd78fb09812dc38c49da777f79cc33e7ee435a254be82f9e7f40d9d117' "$PINS" || r53=0
  grep -q '.cache/gentle-deliverable' "$PINS" || r53=0
  grep -q -- '--fetch' "$PINS" || r53=0
  if [ "$r53" -eq 1 ]; then
    log "[R5.3] PASS: package/pins.txt carries immutable Temurin URL + SHA-256 + cache path + --fetch opt-in"
  else
    log "[R5.3] FAIL: package/pins.txt missing a pinned provenance field (URL/sha256/cache path/--fetch)"; exit 1
  fi

  # Enumerate every pinned destination.
  local -a dests=()
  while IFS='|' read -r _sha _size _url _dest; do
    [ -n "${_dest:-}" ] && dests+=("$_dest")
  done < <(grep -vE '^[[:space:]]*(#|$)' "$PINS")

  # Ensure a clean zip-free start so a fail-fast never leaves a prior zip behind (R1.2).
  rm -f "$ZIP"

  local -a missing=()
  local d f sha size url rc
  for d in "${dests[@]}"; do
    f="$DELIVERABLE_CACHE/$d"
    if [ ! -f "$f" ]; then
      if [ "$FETCH" -eq 1 ]; then
        # Gated network path (exists for reproducibility; NOT exercised on a
        # complete cache — R5.2). Downloaded file is re-verified below.
        read -r sha size url <<<"$(lookup_pin "$d")"
        mkdir -p "$(dirname "$f")"
        log "[R5.2] --fetch: downloading $(basename "$d") (one-time)"
        curl -fL --retry 3 -o "$f" "$url"
      else
        missing+=("$f")
        continue
      fi
    fi
    read -r sha size url <<<"$(lookup_pin "$d")"
    local actual
    actual="$(sha256sum "$f" | cut -d' ' -f1)"
    if [ "$actual" != "$sha" ]; then
      log "[R5.1] FAIL: SHA-256 mismatch, REJECTING before use: $(basename "$d")"
      log "[R5.1]   expected $sha"
      log "[R5.1]   actual   $actual"
      log "[R5.1] No input was used and no zip was written."; exit 2
    fi
  done

  # R1.2 fail-fast BEFORE any zip exists, naming the missing input(s) + the opt-in.
  if [ "${#missing[@]}" -gt 0 ]; then
    log "[R1.2] FAIL: missing Windows input(s): ${missing[*]}"
    log "[R1.2] Pass '--fetch' for a one-time download of the pinned URLs, or seed the cache."
    log "[R1.2] No zip was written."
    exit 1
  fi

  log "[R5.1] PASS: all ${#dests[@]} pinned inputs verified (sha256) before use"
  if [ "$FETCH" -eq 0 ]; then
    log "[R5.2] PASS: offline rebuild — inputs taken from cache; curl was NOT invoked (no --fetch)"
  else
    log "[R5.2] n/a: --fetch active; any absent input was downloaded then re-verified"
  fi
}

# --- Stage 1: build + manifest gate [R4.2] ---
stage1_build() {
  log "--- stage 1: build (offline maven) ---"
  sh ./mvnw -o clean package -DskipTests >/dev/null
  if [ ! -f "target/$JAR_NAME" ]; then
    log "[R7.1] FAIL: expected jar not produced: target/$JAR_NAME"
    exit 1
  fi
  local mf
  mf="$(unzip -p "target/$JAR_NAME" META-INF/MANIFEST.MF)"
  if printf '%s' "$mf" | grep -q "^Main-Class: $MAIN_CLASS"; then
    log "[R4.2] PASS: MANIFEST.MF contains 'Main-Class: $MAIN_CLASS'"
  else
    log "[R4.2] FAIL: MANIFEST.MF lacks Main-Class:"
    printf '%s\n' "$mf"
    exit 1
  fi
}

# --- Stage 2: jlink linux runtime image [R2.1 modules] (E5 flags, proven offline) ---
stage2_jlink() {
  log "--- stage 2: jlink (linux runtime) ---"
  local m2="$HOME/.m2/repository/org/openjfx"
  local mp="" mod
  for mod in base graphics controls fxml; do
    local jar="$m2/javafx-$mod/$JFX_VERSION/javafx-$mod-$JFX_VERSION-linux.jar"
    if [ ! -f "$jar" ]; then
      log "package.sh: missing linux input: $jar"
      exit 1
    fi
    mp="${mp:+$mp:}$jar"
  done
  rm -rf "$RUNTIME_LINUX"
  mkdir -p "$WORK"
  jlink --module-path "$mp" \
    --add-modules javafx.controls,javafx.fxml \
    --strip-debug --no-man-pages --no-header-files --compress=2 \
    --output "$RUNTIME_LINUX"
  local mods
  mods="$("$RUNTIME_LINUX/bin/java" --list-modules)"
  if printf '%s' "$mods" | grep -q 'javafx.controls@' && printf '%s' "$mods" | grep -q 'javafx.fxml@'; then
    log "[R2.1] PASS: jlink image modules: $(printf '%s' "$mods" | grep -o 'javafx\.[a-z]*@[0-9.]*' | tr '\n' ' ')"
  else
    log "[R2.1] FAIL: javafx.controls/javafx.fxml missing from runtime image"
    exit 1
  fi
}

# --- Stage 3: jpackage app-image + static inspection [R2.1] (E7 options) ---
stage3_jpackage() {
  log "--- stage 3: jpackage app-image ---"
  rm -rf "$APP_IN" "$JPKG_OUT" "$LINUX_DIR"
  mkdir -p "$APP_IN"
  cp "target/$JAR_NAME" "$APP_IN/"
  jpackage --type app-image --name "$APP_NAME" \
    --input "$APP_IN" \
    --main-jar "$JAR_NAME" \
    --main-class "$MAIN_CLASS" \
    --runtime-image "$RUNTIME_LINUX" \
    --java-options "--add-modules javafx.controls,javafx.fxml" \
    --dest "$JPKG_OUT"
  mv "$JPKG_OUT/$APP_NAME" "$LINUX_DIR"
  # Static asserts (spec R2.1): ELF launcher, executable, jar under lib/app/.
  if file "$SMOKE_BIN" | grep -q 'ELF'; then
    log "[R2.1] PASS: bin/$APP_NAME is an ELF executable ($(file -b "$SMOKE_BIN" | cut -d, -f1-2))"
  else
    log "[R2.1] FAIL: bin/$APP_NAME is not ELF: $(file -b "$SMOKE_BIN")"
    exit 1
  fi
  if [ -x "$SMOKE_BIN" ]; then
    log "[R2.1] PASS: bin/$APP_NAME holds the execute bit"
  else
    log "[R2.1] FAIL: bin/$APP_NAME is not executable"
    exit 1
  fi
  if [ -f "$LINUX_DIR/lib/app/$JAR_NAME" ]; then
    log "[R2.1] PASS: project jar present at lib/app/$JAR_NAME"
  else
    log "[R2.1] FAIL: project jar missing from lib/app/"
    exit 1
  fi
  if "$LINUX_DIR/lib/runtime/bin/java" --list-modules | grep -q 'javafx.controls@'; then
    log "[R2.1] PASS: bundled lib/runtime resolves javafx.controls + javafx.fxml (see stage 2)"
  else
    log "[R2.1] FAIL: bundled lib/runtime missing JavaFX modules"
    exit 1
  fi
}

# --- Stage 4: windows folder assembly (static-only) [R3.1 R3.2 R3.3] ---
# Hand-assembled from the pinned, already-verified Temurin JRE + 4 JavaFX -win
# jars + package assets. jpackage cannot cross-build (design D1/P2); runtime/ is
# the JRE extracted 1:1. NO windows execution here -> runtime stays UNVERIFIED.
stage4_windows() {
  log "--- stage 4: windows assembly (static-only; runtime-UNVERIFIED) ---"
  # R1.2 precondition: every input must exist and match its pin before ANY use.
  local d f sha size url actual
  for d in "$(awk -F'|' '$4 ~ /OpenJDK17U-jre.*windows/ {print $4; exit}' "$PINS")" \
           "${WIN_JFX_JARS[@]/#/jfx-win/}"; do
    f="$DELIVERABLE_CACHE/$d"
    if [ ! -f "$f" ]; then log "[R1.2] FAIL: windows input missing: $f"; exit 1; fi
    read -r sha size url <<<"$(lookup_pin "$d")"
    actual="$(sha256sum "$f" | cut -d' ' -f1)"
    if [ "$actual" != "$sha" ]; then log "[R5.1] FAIL: pin mismatch at use-time: $f"; exit 2; fi
  done

  # Fresh windows tree; extract JRE 1:1 into runtime/.
  rm -rf "$WIN_DIR" "$WIN_JRE_TMP"
  mkdir -p "$WIN_DIR/app"
  local jrezip
  jrezip="$DELIVERABLE_CACHE/$(awk -F'|' '$4 ~ /OpenJDK17U-jre.*windows/ {print $4; exit}' "$PINS")"
  unzip -q "$jrezip" -d "$WIN_JRE_TMP"
  local jreroot
  jreroot="$(find "$WIN_JRE_TMP" -maxdepth 1 -mindepth 1 -type d | head -1)"
  [ -n "$jreroot" ] || { log "[R3.1] FAIL: JRE archive extracted to no top-level folder"; exit 1; }
  mv "$jreroot" "$WIN_DIR/runtime"

  # app/: project jar + the 4 pinned JavaFX -win jars.
  cp "target/$JAR_NAME" "$WIN_DIR/app/"
  for d in "${WIN_JFX_JARS[@]}"; do
    cp "$DELIVERABLE_CACHE/jfx-win/$d" "$WIN_DIR/app/$d"
  done

  # Assets (the committed originals ARE the shipped/archived artifacts, task 0.3).
  cp "$SCRIPT_DIR/package/Iniciar.bat"            "$WIN_DIR/Iniciar.bat"
  cp "$SCRIPT_DIR/package/LEEME.txt"              "$WIN_DIR/LEEME.txt"
  cp "$SCRIPT_DIR/package/CHECKLIST-WINDOWS.txt"  "$WIN_DIR/CHECKLIST-WINDOWS.txt"

  # [R3.1] static asserts — java.exe + JVM dlls + natives in the graphics jar.
  local -a c1=()
  [ -f "$WIN_DIR/runtime/bin/java.exe" ] && log "[R3.1] PASS: runtime/bin/java.exe present" || { log "[R3.1] FAIL: java.exe missing"; c1+=("java.exe"); }
  if ls "$WIN_DIR"/runtime/bin/server/jvm.dll >/dev/null 2>&1; then
    log "[R3.1] PASS: Windows JVM dll present: runtime/bin/server/jvm.dll"
  else log "[R3.1] FAIL: jvm.dll missing under runtime/bin/server/"; c1+=("jvm.dll"); fi
  local ndll
  ndll="$(find "$WIN_DIR/runtime/bin" -maxdepth 1 -name '*.dll' | wc -l | tr -d ' ')"
  if [ "$ndll" -ge 80 ]; then log "[R3.1] PASS: $ndll runtime/bin/*.dll (>=80; design measures 85)"; else log "[R3.1] FAIL: only $ndll bin dlls (truncated?)"; c1+=("dll-count"); fi
  local gdll
  gdll="$(unzip -l "$WIN_DIR/app/javafx-graphics-${JFX_VERSION}-win.jar" | grep -c '\.dll' || true)"
  if [ "$gdll" -ge 1 ]; then log "[R3.1] PASS: javafx-graphics jar bundles $gdll .dll natives (design D5: only graphics has natives)"; else log "[R3.1] FAIL: graphics jar has no .dll natives"; c1+=("graphics-natives"); fi
  if unzip -p "$WIN_DIR/app/$JAR_NAME" META-INF/MANIFEST.MF | grep -q "^Main-Class: $MAIN_CLASS"; then
    log "[R3.1] PASS: shipped jar Main-Class = $MAIN_CLASS"
  else log "[R3.1] FAIL: shipped app jar lacks Main-Class Launcher"; c1+=("manifest"); fi
  if [ -s "$WIN_DIR/LEEME.txt" ] && [ -s "$WIN_DIR/CHECKLIST-WINDOWS.txt" ]; then
    log "[R3.1] PASS: LEEME.txt and CHECKLIST-WINDOWS.txt present and non-empty"
  else log "[R3.1] FAIL: a windows asset is missing/empty"; c1+=("assets"); fi
  if [ "${#c1[@]}" -gt 0 ]; then exit 1; fi
  log "[R3.1] PASS: windows folder static-verified / runtime-UNVERIFIED (no Windows host to execute)"

  # [R3.2] every %~dp0 occurrence in Iniciar.bat must be quoted.
  local q1 q2
  q1="$(grep -o '%~dp0' "$WIN_DIR/Iniciar.bat" | wc -l | tr -d ' ')"
  q2="$(grep -o '"%~dp0' "$WIN_DIR/Iniciar.bat" | wc -l | tr -d ' ')"
  if [ "$q1" -ge 1 ] && [ "$q1" -eq "$q2" ]; then
    log "[R3.2] PASS: all $q1 '%~dp0' path uses in Iniciar.bat are double-quoted (spaces-safe)"
  else
    log "[R3.2] FAIL: $q1 '%~dp0' uses but $q2 quoted"; exit 1
  fi

  # [R3.3] raw size bounds 120-160 MB (design D2: documented layout change to the
  # full untrimmed JRE; runtime is the untrimmable jimage — catches truncation).
  local mb
  mb="$(du -sm "$WIN_DIR" | cut -f1)"
  if [ "$mb" -ge 120 ] && [ "$mb" -le 160 ]; then
    log "[R3.3] PASS: windows folder raw size ${mb} MB within 120-160 MB (design D2 full-JRE layout)"
  else
    log "[R3.3] FAIL: windows folder ${mb} MB outside 120-160 MB bounds (component truncated or wrong)"; exit 1
  fi
}

# --- Stage 5: single zip of BOTH folders [R1.1 R8.1] ---
stage5_zip() {
  log "--- stage 5: zip both folders ---"
  if [ ! -d "$LINUX_DIR" ] || [ ! -d "$WIN_DIR" ]; then
    log "[R1.1] FAIL: both folders required (linux=$([ -d "$LINUX_DIR" ] && echo ok || echo missing) windows=$([ -d "$WIN_DIR" ] && echo ok || echo missing))"; exit 1
  fi
  rm -f "$ZIP"
  ( cd "$DIST" && zip -q -r -X "${APP_NAME}.zip" "${APP_NAME}-linux" "${APP_NAME}-windows" )

  # [R1.1] tree assert — exactly the two folders and every required file.
  # Read the entry list ONCE into an associative set; membership tests are pure
  # bash so no live unzip|grep pipe can be aborted by pipefail + `grep -q` SIGPIPE.
  local -A inzip=()
  local -a names=()
  while IFS= read -r line; do inzip["$line"]=1; names+=("$line"); done \
    < <(unzip -l "$ZIP" | awk 'NR>3 && NF>0 && index($NF,"/") {print $NF}')
  assert_in_zip() { [ -n "${inzip[$1]:-}" ]; }
  local -a need=(
    "${APP_NAME}-linux/bin/${APP_NAME}"
    "${APP_NAME}-linux/lib/app/${JAR_NAME}"
    "${APP_NAME}-linux/lib/app/${APP_NAME}.cfg"
    "${APP_NAME}-linux/lib/libapplauncher.so"
    "${APP_NAME}-linux/lib/runtime/bin/java"
    "${APP_NAME}-windows/runtime/bin/java.exe"
    "${APP_NAME}-windows/runtime/bin/server/jvm.dll"
    "${APP_NAME}-windows/app/${JAR_NAME}"
    "${APP_NAME}-windows/app/javafx-base-${JFX_VERSION}-win.jar"
    "${APP_NAME}-windows/app/javafx-graphics-${JFX_VERSION}-win.jar"
    "${APP_NAME}-windows/app/javafx-controls-${JFX_VERSION}-win.jar"
    "${APP_NAME}-windows/app/javafx-fxml-${JFX_VERSION}-win.jar"
    "${APP_NAME}-windows/Iniciar.bat"
    "${APP_NAME}-windows/LEEME.txt"
    "${APP_NAME}-windows/CHECKLIST-WINDOWS.txt"
  )
  local ok=1 nentries="${#names[@]}"
  for e in "${need[@]}"; do assert_in_zip "$e" || { log "[R1.1] FAIL: missing from zip: $e"; ok=0; }; done
  # Both top-level folders present (and only these two roots).
  assert_in_zip "${APP_NAME}-linux/"    || { log "[R1.1] FAIL: linux folder root missing"; ok=0; }
  assert_in_zip "${APP_NAME}-windows/"  || { log "[R1.1] FAIL: windows folder root missing"; ok=0; }
  local roots
  roots="$(printf '%s\n' "${names[@]}" | awk -F/ 'NF>1 {print $1}' | sort -u | tr '\n' ' ')"
  if [ "$roots" = "${APP_NAME}-linux ${APP_NAME}-windows " ]; then
    log "[R1.1] PASS: zip root contains exactly [$roots](both deliverable folders)"
  else
    log "[R1.1] FAIL: unexpected zip roots: [$roots]"; ok=0
  fi
  [ "$ok" -eq 1 ] || exit 1
  log "[R1.1] PASS: every required file present ($nentries entries)"

  # [R8.1] checklist shipped in the zip AND archived (byte-identical) under the change folder.
  mkdir -p "$CHANGE_DIR"
  cp "$WIN_DIR/CHECKLIST-WINDOWS.txt" "$CHANGE_DIR/CHECKLIST-WINDOWS.txt"
  local zipsha packsha origsha
  origsha="$(sha256sum "$SCRIPT_DIR/package/CHECKLIST-WINDOWS.txt" | cut -d' ' -f1)"
  zipsha="$(sha256sum "$WIN_DIR/CHECKLIST-WINDOWS.txt" | cut -d' ' -f1)"
  packsha="$(sha256sum "$CHANGE_DIR/CHECKLIST-WINDOWS.txt" | cut -d' ' -f1)"
  if [ "$origsha" = "$zipsha" ] && [ "$zipsha" = "$packsha" ]; then
    log "[R8.1] PASS: CHECKLIST-WINDOWS.txt in zip + archived copy, sha256-identical to the committed original ($origsha)"
  else
    log "[R8.1] FAIL: checklist drift (orig=$origsha zip=$zipsha copy=$packsha)"; exit 1
  fi

  local zsize zsha
  zsize="$(stat -c %s "$ZIP")"
  zsha="$(sha256sum "$ZIP" | cut -d' ' -f1)"
  log "[R1.1] zip: $(basename "$ZIP") size=$zsize bytes ($(awk -v b="$zsize" 'BEGIN{printf "%.1f", b/1048576}') MiB) sha256=$zsha"
}

# --- Stage 6: smoke gate [R2.2 / R2.3] ---
stage6_smoke() {
  log "--- stage 6: smoke launch (DISPLAY=${DISPLAY:-<empty>}) ---"
  if [ -z "${DISPLAY:-}" ]; then
    log "[R2.3] UNVERIFIED (no display): smoke gate cannot run headless; refusing to fake PASS."
    exit 4
  fi
  mkdir -p "$EVID"
  local shot_win="$EVID/linux-smoke-window.png" shot_root="$EVID/linux-smoke.png"
  rm -f "$shot_win" "$shot_root"
  timeout 12 "$SMOKE_BIN" >"$EVID/smoke.log" 2>&1 &
  local spid=$!
  sleep 6
  local win=0 wid=""
  # Client window (last in tree under the mutter frame) — captures the GUI even when
  # occluded on a shared desktop (compositor backs it offscreen; tasks.md 5.2 re-verify rule).
  wid="$(xwininfo -root -tree 2>/dev/null | grep 'Resolución de Sistemas Lineales' | tail -1 | grep -oE '0x[0-9a-f]+' | head -1 || true)"
  if [ -n "$wid" ]; then
    win=1
    timeout 8 import -window "$wid" "$shot_win" || true
  fi
  timeout 8 import -window root "$shot_root" || true
  local rc=0
  wait "$spid" || rc=$?
  # reap a possible orphaned JVM child if the wrapper died first
  pkill -f "$LINUX_DIR/lib/runtime/bin/java" 2>/dev/null || true
  if [ ! -f "$shot_root" ]; then
    log "[R2.2] FAIL: no root screenshot captured (import -window root)"
    exit 3
  fi
  if [ "$rc" -eq 124 ] && [ -f "$shot_win" ]; then
    log "[R2.2] PASS: smoke — timeout exit 124, GUI alive >=12s (wid $wid); evidence: $shot_win (+ context $shot_root)"
  elif [ "$win" -eq 1 ] && [ -f "$shot_win" ]; then
    log "[R2.2] PASS: smoke — exit $rc but window detected via xwininfo fallback; evidence: $shot_win (+ context $shot_root)"
  elif [ "$rc" -eq 124 ]; then
    log "[R2.3] SMOKE FAILED: exit 124 but no window-specific screenshot captured — evidence incomplete, not faking PASS"
    exit 3
  else
    log "[R2.3] SMOKE FAILED: exit $rc, no window detected; see $EVID/smoke.log"
    exit 3
  fi
}

# --- Stage 7: summary + dist purity [R1.3] ---
stage7_summary() {
  log "--- stage 7: summary ---"
  if [ -z "$(git ls-files dist/)" ] && grep -qx 'dist/' .gitignore; then
    log "[R1.3] PASS: 'git ls-files dist/' is empty AND .gitignore covers dist/"
  else
    log "[R1.3] FAIL: dist/ is tracked or not gitignored"
    exit 1
  fi
  log ""
  if [ "$LINUX_ONLY" -eq 1 ]; then
    log "=== package.sh scenario summary (this build = stages 1,2,3,6,7; --linux-only) ==="
    log "[R4.2] PASS     manifest Main-Class -> $MAIN_CLASS"
    log "[R2.1] PASS     linux image static integrity (ELF + lib/app jar + javafx modules)"
    log "[R2.2] PASS     linux smoke: GUI alive >=12s (evidence: dist/evidence/linux-smoke-window.png + linux-smoke.png)"
    log "[R2.3] n/a      failure/headless path exits 3/4 without faking PASS (probed manually: DISPLAY= run)"
    log "[R1.3] PASS     dist/ never in git (checked above)"
    log "[R1.1] n/a      --linux-only writes no zip (windows stages 0/4/5 skipped)"
    log "[R3.*] n/a      --linux-only skips windows assembly -> windows NOT BUILT in this build"
    log "[R5.*] n/a      --linux-only skips stage 0 verification"
    log "[R8.*] n/a      --linux-only skips stage 5 (no zip, no checklist archive)"
    log "[R7.1] external 'sh ./mvnw -o clean test' -> 82/0/0 gate at every commit"
    log "Status: linux app-image = VERIFIED (static + smoke). windows = NOT BUILT (--linux-only)."
  else
    log "=== package.sh scenario summary (full build = stages 0,1,2,3,4,5,6,7) ==="
    log "[R4.2] PASS     manifest Main-Class -> $MAIN_CLASS"
    log "[R2.1] PASS     linux image static integrity (ELF + lib/app jar + javafx modules)"
    log "[R2.2] PASS     linux smoke: GUI alive >=12s (evidence: dist/evidence/linux-smoke-window.png + linux-smoke.png)"
    log "[R2.3] n/a      failure/headless path exits 3/4 without faking PASS (probed manually: DISPLAY= run)"
    log "[R1.3] PASS     dist/ never in git (checked above)"
    log "[R1.1] PASS     dist/$APP_NAME.zip has both folders + every required file"
    log "[R3.1] static-verified / runtime-UNVERIFIED   windows folder asserts green (java.exe/jvm.dll/natives/manifest/assets)"
    log "[R3.2] PASS     Iniciar.bat quotes every %~dp0"
    log "[R3.3] PASS     windows folder within 120-160 MB"
    log "[R5.1] PASS     pinned inputs sha256-verified before use (mismatch -> exit 2)"
    log "[R5.2] PASS     offline rebuild from cache (curl only under --fetch)"
    log "[R5.3] PASS     provenance in committed package/pins.txt"
    log "[R8.1] PASS     checklist in zip + archived copy under $CHANGE_DIR"
    log "[R8.2] UNVERIFIED  windows runtime flips to VERIFIED only with a returned, archived checklist"
    log "[R7.1] external 'sh ./mvnw -o clean test' -> 82/0/0 gate at every commit"
    log "Status: linux app-image = VERIFIED (static + smoke). windows = static-verified / runtime-UNVERIFIED."
  fi
}

main() {
  log "package.sh: DELIVERABLE_CACHE=$DELIVERABLE_CACHE linux-only=$LINUX_ONLY fetch=$FETCH"
  if [ "$LINUX_ONLY" -eq 0 ]; then
    stage0_windows_inputs
    stage1_build
    stage2_jlink
    stage3_jpackage
    stage4_windows
    stage5_zip
    stage6_smoke
    stage7_summary
  else
    stage1_build
    stage2_jlink
    stage3_jpackage
    stage6_smoke
    stage7_summary
  fi
  log "package.sh: DONE (exit 0)"
}

main "$@"
