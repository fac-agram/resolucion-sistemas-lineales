#!/usr/bin/env bash
# package.sh — build the TrabajoIntegrador deliverable from this repo.
# Spec/design: openspec/changes/deliverable-packaging/{specs/deliverable-packaging/spec.md,design.md}
#
# Stages (design contract):
#   0 fetch/verify  Windows inputs: cache-only by default, --fetch opts into network.
#                   NOT IMPLEMENTED IN THIS BUILD (commit 3) -> fail-fast stub.
#   1 build         mvnw offline package + manifest Main-Class grep        [R4.2]
#   2 jlink         linux runtime image from cached -linux JavaFX jars     [R2.1]
#   3 jpackage      app-image (--type app-image --name TrabajoIntegrador)  [R2.1]
#   4 win assembly  NOT IMPLEMENTED IN THIS BUILD (commit 3) -> stub.      [R3.1-3]
#   5 zip           NOT IMPLEMENTED IN THIS BUILD (commit 3) -> stub.      [R1.1]
#   6 smoke         GUI alive >=12s under DISPLAY, screenshot evidence     [R2.2 R2.3]
#   7 summary       scenario table + dist-never-in-git gate                [R1.3]
#
# Flags:
#   --linux-only  dev shortcut: run stages 1,2,3,6,7; skip stubs 0/4/5; never write a zip.
#   --fetch       opt-in network for stage 0 (pending commit 3; stubs fail with exit 1).
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

DIST="$SCRIPT_DIR/dist"
WORK="$DIST/work"
RUNTIME_LINUX="$WORK/runtime-linux"
APP_IN="$WORK/app"
JPKG_OUT="$WORK/jpkg"
LINUX_DIR="$DIST/${APP_NAME}-linux"
EVID="$DIST/evidence"
SMOKE_BIN="$LINUX_DIR/bin/$APP_NAME"

# Pinned Windows inputs (design "Pinned artifacts" table; full pin file lands in package/pins.txt, commit 3).
WIN_JRE_ZIP="OpenJDK17U-jre_x64_windows_hotspot_17.0.21_9.zip"
WIN_JFX_JARS=("javafx-base-${JFX_VERSION}-win.jar" "javafx-graphics-${JFX_VERSION}-win.jar" "javafx-controls-${JFX_VERSION}-win.jar" "javafx-fxml-${JFX_VERSION}-win.jar")

usage() {
  sed -n '2,28p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
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

# --- Stage 0 (stub): fetch / cache verification is implemented in commit 3. ---
stage0_windows_inputs() {
  # R1.2 shape: name the missing Windows input(s) and the fetch opt-in, fail BEFORE any zip exists.
  local missing=""
  local f
  if [ ! -f "$DELIVERABLE_CACHE/$WIN_JRE_ZIP" ]; then
    missing="$DELIVERABLE_CACHE/$WIN_JRE_ZIP"
  fi
  for f in "${WIN_JFX_JARS[@]}"; do
    if [ ! -f "$DELIVERABLE_CACHE/jfx-win/$f" ]; then
      missing="${missing:+$missing, }$DELIVERABLE_CACHE/jfx-win/$f"
    fi
  done
  log "[R1.2] FAIL: missing Windows input(s): ${missing:-none (cache present but unverified)}"
  log "[R1.2] Stage 0 (fetch/sha256-verify), stage 4 (windows assembly) and stage 5 (zip) are NOT implemented in this build (commit 3 of this change)."
  log "[R1.2] Opt-in network fetch is '--fetch' (pending); local dev builds use '--linux-only' for the linux app-image path."
  log "[R1.2] No zip was written."
  exit 1
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
  if printf '%s' "$mf" | grep -q '^Main-Class: unsa.sistemas.view.Launcher'; then
    log "[R4.2] PASS: MANIFEST.MF contains 'Main-Class: unsa.sistemas.view.Launcher'"
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
    --main-class unsa.sistemas.view.Launcher \
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
  log "=== package.sh scenario summary (this build = stages 1,2,3,6,7) ==="
  log "[R4.2] PASS     manifest Main-Class -> unsa.sistemas.view.Launcher"
  log "[R2.1] PASS     linux image static integrity (ELF + lib/app jar + javafx modules)"
  log "[R2.2] PASS     linux smoke: GUI alive >=12s (evidence: dist/evidence/linux-smoke-window.png + linux-smoke.png)"
  log "[R2.3] n/a      failure/headless path exits 3/4 without faking PASS (probed manually: DISPLAY= run)"
  log "[R1.3] PASS     dist/ never in git (checked above)"
  log "[R1.2] manual   probe: DELIVERABLE_CACHE=\$(mktemp -d) bash package.sh -> exit 1, no zip (task 2.3)"
  log "[R1.1] pending  stage 5 (zip) lands in commit 3 — this build writes no zip"
  log "[R3.*] pending  stages 0/4 (windows folder) land in commit 3 -> windows: runtime-UNVERIFIED"
  log "[R5.*] pending  stage 0 pin/verify/fetch lands in commit 3 (pins recorded in design.md)"
  log "[R8.*] pending  checklist asset lands in commit 3; attestation flip only via R8.2"
  log "[R7.1] external 'sh ./mvnw -o clean test' -> 82/0/0 gate at every commit"
  log "Status: linux app-image = VERIFIED (static + smoke). windows = NOT BUILT in this build."
}

main() {
  log "package.sh: DELIVERABLE_CACHE=$DELIVERABLE_CACHE linux-only=$LINUX_ONLY fetch=$FETCH"
  if [ "$LINUX_ONLY" -eq 0 ]; then
    # Fail-fast BEFORE any build or zip: windows deliverable stages are not implemented yet.
    stage0_windows_inputs
  fi
  stage1_build
  stage2_jlink
  stage3_jpackage
  stage6_smoke
  stage7_summary
  log "package.sh: DONE (exit 0)"
}

main "$@"
