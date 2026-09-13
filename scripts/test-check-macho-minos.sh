#!/usr/bin/env bash
#
# test-check-macho-minos.sh -- exercise scripts/check-macho-minos.py against real
# Mach-O binaries built on the spot.
#
# The checker guards the macOS deployment target of the .app launcher stub (#393:
# 3.6..3.6.5 shipped minos 26.0 and would not start on macOS 15 or earlier). Its
# own parser is the part that can go wrong silently, so build a universal binary
# with a known floor and assert both verdicts: pass at the ceiling, fail below it.
#
# Not wired into any CI job -- the workflows exercise the checker on the real
# stub. Run this locally after touching the parser. Skips cleanly (exit 0) where
# clang/lipo are unavailable, so it stays runnable on Linux.
#
# Usage: scripts/test-check-macho-minos.sh
#
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
checker="$here/check-macho-minos.py"
[ -f "$checker" ] || { echo "FAIL: $checker not found"; exit 1; }

for tool in clang lipo; do
  command -v "$tool" >/dev/null 2>&1 || {
    echo "SKIP: $tool unavailable -- this test needs a macOS toolchain"
    exit 0
  }
done

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

printf 'int main(void) { return 0; }\n' > "$work/stub.c"
clang -arch x86_64 -mmacosx-version-min=11.0 -o "$work/stub-x86_64" "$work/stub.c"
clang -arch arm64  -mmacosx-version-min=11.0 -o "$work/stub-arm64"  "$work/stub.c"
lipo -create "$work/stub-x86_64" "$work/stub-arm64" -output "$work/stub-universal"

failures=0
check() {  # $1 = human-readable case, $2 = expected exit status, rest = checker args
  local what="$1" expected="$2"; shift 2
  local status=0
  "$checker" "$@" >"$work/out" 2>"$work/err" || status=$?
  if [ "$status" -eq "$expected" ]; then
    echo "ok   $what (exit $status)"
  else
    echo "FAIL $what: expected exit $expected, got $status"
    sed 's/^/       /' "$work/out" "$work/err"
    failures=$((failures + 1))
  fi
}

check 'universal binary at its ceiling passes'    0 "$work/stub-universal" x86_64=11.0 arm64=11.0
check 'universal binary above a lower ceiling fails' 1 "$work/stub-universal" x86_64=10.15 arm64=11.0
check 'a higher ceiling still passes'            0 "$work/stub-universal" x86_64=26.0 arm64=26.0
check 'thin binary passes for its own arch'      0 "$work/stub-arm64" arm64=11.0
check 'an absent arch is an error'               1 "$work/stub-arm64" x86_64=11.0
check 'a non-Mach-O file is an error'            1 "$work/stub.c" arm64=11.0
check 'a missing file is an error'               1 "$work/does-not-exist" arm64=11.0
check 'a malformed ceiling is a usage error'     2 "$work/stub-universal" arm64=eleven

[ "$failures" -eq 0 ] || { echo "$failures case(s) failed"; exit 1; }
echo "all cases passed"
