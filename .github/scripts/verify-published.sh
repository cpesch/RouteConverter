#!/usr/bin/env bash
# Verify a published release on every automated surface; read-only.
#
#   verify-published.sh VERSION     run all checks, exit 1 if any failed
#   verify-published.sh --self-test assert the archive -> latest/ name mapping
#
# Checks (each prints "OK  : <check>" or "FAIL: <check> — <detail>"):
#   - artefact parity: every previous-releases/VERSION/*.sha256 matches the
#     bytes served under latest/
#   - javadoc title names VERSION
#   - PAD catalog XMLs (EN+DE) carry VERSION
#   - latest/VERSION (the update check's source) equals VERSION; the
#     update-check API itself is not called: every call writes a Program row
#   - GitHub Release body starts with the curated "## VERSION — " section
# Needs GH_TOKEN for the GitHub Release check; writes nothing anywhere.

set -uo pipefail

RELEASES=https://releases.routeconverter.com
STATIC=https://static.routeconverter.com
# Top-level release listings redirect to / without a releases Referer.
REFERER=https://releases.routeconverter.com/

# previous-releases/ keeps the *.app.zip names, latest/ the *-app.zip names
# the build produces; everything else is named identically.
latest_name() {
  case "$1" in
    *-x64.app.zip) echo "${1%-x64.app.zip}-x64-app.zip" ;;
    *-aarch64.app.zip) echo "${1%-aarch64.app.zip}-aarch64-app.zip" ;;
    *) echo "$1" ;;
  esac
}

self_test() {
  local failed=0 input expected actual
  while read -r input expected; do
    actual=$(latest_name "$input")
    if [[ "$actual" == "$expected" ]]; then
      echo "OK  : latest_name $input"
    else
      echo "FAIL: latest_name $input — got $actual, expected $expected"
      failed=1
    fi
  done <<'EOF'
RouteConverterMac-x64.app.zip RouteConverterMac-x64-app.zip
RouteConverterMac-aarch64.app.zip RouteConverterMac-aarch64-app.zip
TimeAlbumProMac-x64.app.zip TimeAlbumProMac-x64-app.zip
TimeAlbumProMac-aarch64.app.zip TimeAlbumProMac-aarch64-app.zip
RouteConverterWindows.exe RouteConverterWindows.exe
EOF
  return "$failed"
}

if [[ "${1:-}" == "--self-test" ]]; then
  self_test
  exit $?
fi

VERSION=${1:?usage: verify-published.sh VERSION | --self-test}
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT
FAILED=0

ok() { echo "OK  : $1"; }
fail() { echo "FAIL: $1 — $2"; FAILED=1; }

# fetch URL FILE — 3 attempts, 10 s apart (CDN/Apache cache may lag).
fetch() {
  local attempt
  for attempt in 1 2 3; do
    if curl -fsSL -e "$REFERER" -o "$2" "$1"; then
      return 0
    fi
    [[ $attempt -lt 3 ]] && sleep 10
  done
  return 1
}

check_artefacts() {
  local index="$WORK/index.html" listed=0 sums name hash latest file
  if ! fetch "$RELEASES/previous-releases/$VERSION/" "$index"; then
    fail "artefact parity" "cannot list $RELEASES/previous-releases/$VERSION/"
    return
  fi
  sums=$(grep -o 'href="[^"]*\.sha256"' "$index" | sed -E 's/^href="(.*)"$/\1/' | sort -u)
  for sums_file in $sums; do
    listed=1
    name=${sums_file%.sha256}
    latest=$(latest_name "$name")
    if [[ "$name" == "SnapshotCatalog.jar" ]] &&
       ! curl -fsI -e "$REFERER" "$RELEASES/latest/$latest" >/dev/null; then
      ok "artefact parity $name (absent from latest/, skipped)"
      continue
    fi
    if ! fetch "$RELEASES/previous-releases/$VERSION/$sums_file" "$WORK/$sums_file"; then
      fail "artefact parity $name" "cannot fetch $sums_file"
      continue
    fi
    hash=$(awk '{print $1; exit}' "$WORK/$sums_file")
    file="$WORK/$latest"
    if ! fetch "$RELEASES/latest/$latest" "$file"; then
      fail "artefact parity $name" "cannot fetch latest/$latest"
      continue
    fi
    if [[ "$(sha256sum "$file" | awk '{print $1}')" == "$hash" ]]; then
      ok "artefact parity $name = latest/$latest"
    else
      fail "artefact parity $name" "latest/$latest does not match $sums_file"
    fi
    rm -f "$file"
  done
  [[ $listed -eq 1 ]] || fail "artefact parity" "no *.sha256 listed in previous-releases/$VERSION/"
}

check_contains() {
  local check=$1 url=$2 needle=$3 file="$WORK/page"
  if ! fetch "$url" "$file"; then
    fail "$check" "cannot fetch $url"
  elif grep -qF "$needle" "$file"; then
    ok "$check"
  else
    fail "$check" "$url does not contain \"$needle\""
  fi
}

check_version_file() {
  local file="$WORK/VERSION" actual
  if ! fetch "$RELEASES/latest/VERSION" "$file"; then
    fail "update check source" "cannot fetch $RELEASES/latest/VERSION"
    return
  fi
  actual=$(tr -d '[:space:]' < "$file")
  if [[ "$actual" == "$VERSION" ]]; then
    ok "update check source latest/VERSION = $VERSION"
  else
    fail "update check source" "latest/VERSION is \"$actual\""
  fi
}

check_release_body() {
  local body
  if ! body=$(gh release view "$VERSION" --repo cpesch/RouteConverter --json body --jq .body); then
    fail "GitHub Release body" "gh release view $VERSION failed"
  elif [[ "$body" == "## $VERSION — "* ]]; then
    ok "GitHub Release body"
  else
    fail "GitHub Release body" "does not start with \"## $VERSION — \""
  fi
}

check_artefacts
check_contains "javadoc" "$STATIC/javadoc/" "$VERSION API"
check_contains "PAD catalog EN" "$STATIC/downloads/RouteConverter.xml" "<Program_Version>$VERSION</Program_Version>"
check_contains "PAD catalog DE" "$STATIC/downloads/RouteConverter_de.xml" "<Program_Version>$VERSION</Program_Version>"
check_version_file
check_release_body

exit "$FAILED"
