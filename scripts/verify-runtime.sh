#!/usr/bin/env bash
#
# verify-runtime.sh -- catch ClassNotFoundException / NoClassDefFoundError that
# only surface on the minimized JRE we ship, not on a full dev JDK.
#
# A full JDK has every module and every class, so unit tests pass while the
# shipped stripped JRE is missing a module a dependency needs at runtime (e.g.
# httpclient5 5.6 -> jdk.net). This script reproduces the stripped-runtime
# condition on any runner and fails the build before users see the dialog.
#
# Five checks (idea #1-#5 from the 2026-06 jdk.net post-mortem):
#   #1 jdeps module gate     -- modules the fat jar actually references
#   #2 module superset check -- those modules must be a subset of jre-modules.txt
#   #5 jdeps --missing-deps   -- classes referenced but absent (over-stripped libs)
#   #4 forced-init sweep      -- Class.forName(.,true,.) under a stripped JRE
#   #3 end-to-end smoke       -- run RouteConverterCmdLine on the stripped JRE
#
# The stripped verify-JRE is jlinked here from scripts/jre-modules.txt -- the
# same list the shipped Mac/Windows JREs use -- so a missing module fails
# identically regardless of runner OS/arch.
#
# Usage:
#   scripts/verify-runtime.sh <fat-jar> [cmdline-jar] [sample-gpx]
#
# Env:
#   MR_VERSION       multi-release version for jdeps (default 17)
#   INIT_PREFIXES    comma-separated binary-name prefixes for the #4 sweep
#                    (default: third-party library roots; app GUI classes are
#                    covered by the #3 smoke instead, to avoid opening windows)
#   STRICT_MISSING   1 = fail on #5 jdeps --missing-deps findings (default warn)
#
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
modules_file="$here/jre-modules.txt"

fat_jar="${1:?usage: verify-runtime.sh <fat-jar> [cmdline-jar] [sample-gpx]}"
cmdline_jar="${2:-}"
sample_gpx="${3:-}"
mr_version="${MR_VERSION:-17}"
init_prefixes="${INIT_PREFIXES:-org.apache.,org.sqlite.,com.sun.,jakarta.,net.java.dev.jna.,org.mozilla.,com.fasterxml.,com.google.}"
strict_missing="${STRICT_MISSING:-0}"

[[ -f "$fat_jar" ]]      || { echo "::error::fat jar not found: $fat_jar"; exit 2; }
[[ -f "$modules_file" ]] || { echo "::error::module list not found: $modules_file"; exit 2; }
command -v jdeps >/dev/null || { echo "::error::jdeps not on PATH (need a full JDK)"; exit 2; }
command -v jlink >/dev/null || { echo "::error::jlink not on PATH (need a full JDK)"; exit 2; }

allowed_csv="$(grep -vE '^[[:space:]]*#|^[[:space:]]*$' "$modules_file" | tr -d '[:blank:]' | paste -sd, -)"
echo "==> allowed modules (jre-modules.txt): $allowed_csv"

# dot-free template: RouteConverterCmdLine's removeExtension() strips from the
# last '.' in the whole path, so a dot in the temp dir name (mktemp's default
# tmp.XXXX) would mangle the #3 smoke target into a wrong/colliding file.
work="$(mktemp -d "${TMPDIR:-/tmp}/rcverifyXXXXXX")"
trap 'rm -rf "$work"' EXIT
fail=0

# ---- #1 + #2: jdeps module gate + superset check ----------------------------
echo
echo "==> [#1] jdeps required modules for $(basename "$fat_jar")"
required_csv="$(jdeps --multi-release "$mr_version" --print-module-deps --ignore-missing-deps "$fat_jar")"
echo "    required: $required_csv"

echo "==> [#2] modules referenced but not in jre-modules.txt (advisory)"
# jdeps over-approximates on a fat jar: it reports modules referenced by ANY
# bundled class, including dead/optional code never executed (intellij forms
# compiler, graphhopper DEM providers, ...). So this is advisory -- the true
# runtime gates are #4 (forced static-init) and #3 (smoke), which only fail on
# a module the app actually touches. If #4/#3 stay green, an "extra" here is a
# dead reference; if one fails citing a module, add it to jre-modules.txt.
extras=()
IFS=',' read -ra req_arr <<< "$required_csv"
for m in "${req_arr[@]}"; do
  [[ -z "$m" ]] && continue
  case ",$allowed_csv," in
    *",$m,"*) : ;;
    *) extras+=("$m") ;;
  esac
done
if ((${#extras[@]})); then
  echo "::warning::fat jar references module(s) not shipped: ${extras[*]} (dead refs unless #4/#3 fail)"
else
  echo "    OK: every referenced module is shipped"
fi

# ---- #5: jdeps --missing-deps (over-stripped libraries) ---------------------
echo
echo "==> [#5] jdeps --missing-deps (referenced classes that are absent)"
missing="$(jdeps --multi-release "$mr_version" --missing-deps "$fat_jar" 2>/dev/null || true)"
if [[ -n "$missing" ]]; then
  # head first (string heredoc has no upstream producer to SIGPIPE under pipefail)
  sed 's/^/    /' <<< "$(head -40 <<< "$missing")"
  if [[ "$strict_missing" == "1" ]]; then
    echo "::error::missing dependencies reported (STRICT_MISSING=1)"
    fail=1
  else
    echo "::warning::missing dependencies reported (often optional deps; set STRICT_MISSING=1 to gate)"
  fi
else
  echo "    OK: no missing class references"
fi

# ---- build the stripped verify-JRE from the shared module list --------------
echo
echo "==> jlink stripped verify-JRE from jre-modules.txt"
verify_jre="$work/verify-jre"
jlink --add-modules "$allowed_csv" --output "$verify_jre" \
      --strip-debug --no-header-files --no-man-pages
"$verify_jre/bin/java" --list-modules | sed 's/^/    /'

# ---- #4: forced-init sweep under the stripped JRE ---------------------------
echo
echo "==> [#4] forced static-init sweep on the stripped JRE"
javac -d "$work" "$here/ForceInit.java"
if "$verify_jre/bin/java" -Djava.awt.headless=true -cp "$work:$fat_jar" \
      ForceInit "$fat_jar" "$init_prefixes"; then
  echo "    OK: all swept classes initialized on the stripped JRE"
else
  echo "::error::a class failed static initialization on the stripped JRE (see above)"
  fail=1
fi

# ---- #3: end-to-end smoke on the stripped JRE -------------------------------
if [[ -n "$cmdline_jar" && -f "$cmdline_jar" ]]; then
  echo
  echo "==> [#3] CmdLine conversion smoke on the stripped JRE"
  gpx="${sample_gpx:-$here/../navigation-formats-samples/src/test/from.gpx}"
  if [[ ! -f "$gpx" ]]; then
    echo "::warning::sample gpx not found ($gpx); skipping #3 smoke"
  else
    out="$work/smoke"
    if "$verify_jre/bin/java" -Djava.awt.headless=true -jar "$cmdline_jar" \
          "$gpx" Kml22Format "$out" && [[ -s "$out.kml" ]]; then
      echo "    OK: $gpx -> $out.kml ($(wc -c < "$out.kml") bytes) on the stripped JRE"
    else
      echo "::error::CmdLine conversion failed on the stripped JRE"
      fail=1
    fi
  fi
else
  echo
  echo "==> [#3] no cmdline-jar argument; skipping end-to-end smoke"
fi

echo
if ((fail)); then
  echo "==> verify-runtime: FAILED"
  exit 1
fi
echo "==> verify-runtime: PASSED"
