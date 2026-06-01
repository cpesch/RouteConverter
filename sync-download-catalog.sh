#!/usr/bin/env zsh
#
# Drives the RouteConverter download catalog workflow.
# Reads scan + mirror config from each datasource <source> element.
# Replaces the legacy catalog-sources.zsh / catalog-mirror-jobs.zsh arrays.

set -eu
set -o pipefail

SCRIPT_DIR=${0:A:h}
PROJECT_DIR=$SCRIPT_DIR
DOWNLOAD_TOOLS_DIR="$PROJECT_DIR/download-tools"
MVNW="$PROJECT_DIR/mvnw"
SNAPSHOT_JAR="$DOWNLOAD_TOOLS_DIR/target/SnapshotCatalog.jar"
SCAN_JAR="$DOWNLOAD_TOOLS_DIR/target/ScanWebsite.jar"
UPDATE_JAR="$DOWNLOAD_TOOLS_DIR/target/UpdateCatalog.jar"
MIRROR_JAR="$DOWNLOAD_TOOLS_DIR/target/MirrorCatalog.jar"

SERVER=https://api.routeconverter.com/
CATALOG_USERNAME=routeconverter
MIRROR=/Volumes/5TB\ Mirror/Mirrors/
PASSWORD_ENV_VAR=${PASSWORD_ENV_VAR:-PASSWORD}
PHASE=all
BUILD=0
DRY_RUN=0
KEEP_GOING=0
WITH_MIRROR=0
LAST_COMMAND_SUCCESS=1

typeset -a FILTER_IDS FAILURE_LABELS

usage() {
  cat <<EOF
Usage: ${0:t} [options]

Drives the RouteConverter download catalog workflow.
Scan + mirror config comes from each datasource <source> element in the snapshot.

Options:
  --phase snapshot|scan|mirror|update|all
                                     Which phase to run (default: all)
  --id <catalog-id>                  Limit scan/mirror/update to one ID (repeatable)
  --server <url>                     Data sources server (default: $SERVER)
  --username <name>                  Server user name (default: $CATALOG_USERNAME)
  --mirror <path>                    Local mirror root (default: $MIRROR)
  --password-env-var <name>          Environment variable containing the password (default: $PASSWORD_ENV_VAR)
  --with-mirror                      Run mirror phase before update/all
  --build                            Build download-tools before running
  --dry-run                          Print commands without executing them
  --keep-going                       Continue after failed jobs and report failures at the end
  --help                             Show this help

Environment:
  \$$PASSWORD_ENV_VAR                  Password used by ScanWebsite / UpdateCatalog / MirrorCatalog

Examples:
  ${0:t} --dry-run --phase all
  ${0:t} --phase all --with-mirror
  ${0:t} --phase scan --id openandromaps
  ${0:t} --phase mirror
EOF
}

die() {
  print -u2 -- "ERROR: $*"
  exit 1
}

warn() {
  print -u2 -- "WARN: $*"
}

info() {
  print -- "==> $*"
}

format_command() {
  local rendered=""
  local argument
  for argument in "$@"; do
    rendered+=" ${(q)argument}"
  done
  print -r -- "${rendered# }"
}

run_command() {
  local label="$1"
  shift
  local -a command=("$@")

  info "$label"
  LAST_COMMAND_SUCCESS=1
  if (( DRY_RUN )); then
    format_command "${command[@]}"
    return 0
  fi

  if "${command[@]}"; then
    return 0
  fi

  LAST_COMMAND_SUCCESS=0
  FAILURE_LABELS+=("$label")
  if (( KEEP_GOING )); then
    warn "Command failed: $label"
    return 0
  fi

  exit 1
}

snapshot_directory() {
  local host=${SERVER#http://}
  host=${host#https://}
  host=${host%/}
  host=${host//:/}
  print -r -- "$HOME/.routeconverter/snapshot-$host"
}

datasources_directory() {
  print -r -- "$(snapshot_directory)/datasources"
}

ids_with_source() {
  local dir
  dir=$(datasources_directory)
  [[ -d "$dir" ]] || return 0
  local file id
  for file in "$dir"/*.xml(N); do
    # presence of <source ...> element on a datasource
    if grep -q '<source\b' "$file"; then
      id=${file:t:r}
      print -r -- "$id"
    fi
  done
}

is_selected_id() {
  local id="$1"
  if (( ${#FILTER_IDS[@]} == 0 )); then
    return 0
  fi
  local selected
  for selected in "${FILTER_IDS[@]}"; do
    [[ "$selected" == "$id" ]] && return 0
  done
  return 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || die "Required command not found: $1"
}

require_file() {
  [[ -f "$1" ]] || die "Required file not found: $1"
}

ensure_directory() {
  local directory="$1"
  [[ -d "$directory" ]] && return 0
  (( DRY_RUN )) && return 0
  mkdir -p "$directory"
}

ensure_password_available() {
  local password_value="${(P)PASSWORD_ENV_VAR-}"
  [[ -n "$password_value" ]] || die "Set $PASSWORD_ENV_VAR before running scan/update phases"
}

build_download_tools() {
  run_command "Build download-tools" "$MVNW" -pl download-tools -am package -DskipTests
}

run_snapshot() {
  local label="$1"
  run_command "$label" java -jar "$SNAPSHOT_JAR" --server "$SERVER"
}

run_scan_jobs() {
  local matched=0 id
  for id in $(ids_with_source); do
    is_selected_id "$id" || continue
    matched=1
    run_command "Scan $id" java -jar "$SCAN_JAR" --id "$id" --server "$SERVER" \
      --username "$CATALOG_USERNAME"
  done
  (( matched > 0 )) || die "No datasources with <source> matched the selected IDs"
}

run_update_jobs() {
  local matched=0 id
  for id in $(ids_with_source); do
    is_selected_id "$id" || continue
    matched=1
    run_command "Update $id" java -jar "$UPDATE_JAR" --id "$id" --server "$SERVER" \
      --username "$CATALOG_USERNAME" --mirror "$MIRROR"
  done
  (( matched > 0 )) || die "No datasources with <source> matched the selected IDs"
}

run_mirror_jobs() {
  local -a command=(java -jar "$MIRROR_JAR" --snapshot "$(datasources_directory)" --mirror "$MIRROR")
  (( DRY_RUN )) && command+=(--dry-run)
  local id
  for id in "${FILTER_IDS[@]}"; do
    command+=(--id "$id")
  done
  run_command "Mirror catalog" "${command[@]}"
}

preflight() {
  require_command java
  require_file "$MVNW"

  if ! (( BUILD )); then
    case "$PHASE" in
      snapshot) require_file "$SNAPSHOT_JAR" ;;
      scan)     require_file "$SNAPSHOT_JAR"; require_file "$SCAN_JAR" ;;
      mirror)   require_file "$MIRROR_JAR" ;;
      update)   require_file "$SNAPSHOT_JAR"; require_file "$UPDATE_JAR" ;;
      all)      require_file "$SNAPSHOT_JAR"; require_file "$SCAN_JAR"; require_file "$UPDATE_JAR"; require_file "$MIRROR_JAR" ;;
    esac
  fi

  if [[ "$PHASE" == scan || "$PHASE" == update || "$PHASE" == all ]]; then
    ensure_password_available
    export "$PASSWORD_ENV_VAR=${(P)PASSWORD_ENV_VAR}"
  fi

  if [[ "$PHASE" == mirror || "$WITH_MIRROR" == 1 ]]; then
    require_command wget
  fi

  if [[ "$PHASE" == mirror || "$PHASE" == update || "$PHASE" == all || "$WITH_MIRROR" == 1 ]]; then
    ensure_directory "$MIRROR"
  fi
}

parse_arguments() {
  while (( $# > 0 )); do
    case "$1" in
      --phase)             (( $# >= 2 )) || die "Missing value for --phase"; PHASE="$2"; shift 2 ;;
      --id)                (( $# >= 2 )) || die "Missing value for --id"; FILTER_IDS+=("$2"); shift 2 ;;
      --server)            (( $# >= 2 )) || die "Missing value for --server"; SERVER="$2"; shift 2 ;;
      --username)          (( $# >= 2 )) || die "Missing value for --username"; CATALOG_USERNAME="$2"; shift 2 ;;
      --mirror)            (( $# >= 2 )) || die "Missing value for --mirror"; MIRROR="$2"; shift 2 ;;
      --password-env-var)  (( $# >= 2 )) || die "Missing value for --password-env-var"; PASSWORD_ENV_VAR="$2"; shift 2 ;;
      --with-mirror)       WITH_MIRROR=1; shift ;;
      --build)             BUILD=1; shift ;;
      --dry-run)           DRY_RUN=1; shift ;;
      --keep-going)        KEEP_GOING=1; shift ;;
      --help|-h)           usage; exit 0 ;;
      *)                   die "Unknown option: $1" ;;
    esac
  done

  case "$PHASE" in
    snapshot|scan|mirror|update|all) ;;
    *) die "Invalid phase: $PHASE" ;;
  esac
}

print_summary() {
  if (( ${#FAILURE_LABELS[@]} == 0 )); then
    info "Workflow completed successfully"
    return 0
  fi
  warn "Workflow completed with ${#FAILURE_LABELS[@]} failure(s):"
  local label
  for label in "${FAILURE_LABELS[@]}"; do
    warn "  - $label"
  done
  exit 1
}

main() {
  parse_arguments "$@"
  preflight

  (( BUILD )) && build_download_tools

  case "$PHASE" in
    snapshot)
      run_snapshot "Snapshot catalog"
      ;;
    scan)
      run_snapshot "Snapshot catalog before scan"
      run_scan_jobs
      run_snapshot "Refresh snapshot after scan"
      ;;
    mirror)
      run_mirror_jobs
      ;;
    update)
      run_snapshot "Snapshot catalog before update"
      (( WITH_MIRROR )) && run_mirror_jobs
      run_update_jobs
      ;;
    all)
      run_snapshot "Snapshot catalog before scan"
      run_scan_jobs
      run_snapshot "Refresh snapshot after scan"
      (( WITH_MIRROR )) && run_mirror_jobs
      run_update_jobs
      ;;
  esac

  print_summary
}

main "$@"
