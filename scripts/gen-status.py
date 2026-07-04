#!/usr/bin/env python3
"""Aggregate spec frontmatter into ``specs/STATUS.md``.

Source of truth: each ``specs/000NN-*.md`` spec carries a YAML frontmatter
block at the top with these fields::

    ---
    name: <slug>
    status: proposed | planned | in-flight | shipped | live | retired
    phases_done: []
    phases_next: []
    last_touched: YYYY-MM-DD
    ---

Active specs (proposed/planned/in-flight) live in ``specs/``; done ones
(shipped/live/retired) in ``specs/done/``. When a spec's status crosses that
line, ``git mv`` it to match and rerun this script.

Output: ``specs/STATUS.md`` — one table per bucket (Active / Done). Zero
external deps — a tiny block-YAML reader handles the constrained frontmatter
schema (top-level scalars + flow-style lists).

Rerun with bare ``python3 scripts/gen-status.py`` — it writes ``specs/STATUS.md``
itself; do NOT redirect (``> specs/STATUS.md``) or you race its own write.
"""
from __future__ import annotations

import datetime as _dt
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SPECS = ROOT / "specs"
DONE_DIR = SPECS / "done"            # archived done specs (shipped/live/retired)
OUTPUT = SPECS / "STATUS.md"

# Any ``NNNNN-*.md`` (5-digit + hyphen). A dot deeper in the slug is fine
# (e.g. ``00018-release-automation-3.4.md``).
INITIATIVE_RE = re.compile(r"^0[0-9]{4}-.+\.md$")

VALID_STATUS = {"proposed", "planned", "in-flight", "shipped", "live", "retired"}

# Non-canonical synonyms normalized to the canonical value (rendered
# canonically), with a stderr nudge to fix the frontmatter. RouteConverter's
# older specs used prose words like "Implemented" / "In progress" / "Open".
STATUS_ALIASES = {
    "implemented": "shipped", "done": "shipped", "complete": "shipped",
    "in progress": "in-flight", "in-progress": "in-flight", "active": "in-flight",
    "open": "proposed", "draft": "proposed",
}

ACTIVE_STATUS = {"in-flight", "planned", "proposed"}
DONE_STATUS = VALID_STATUS - ACTIVE_STATUS   # shipped/live/retired -> specs/done/ + Done table
STALE_DAYS = 30


# ---------------------------------------------------------------------------
# Frontmatter parser

def _parse_flow_list(raw: str) -> list[str]:
    raw = raw.strip()
    if not (raw.startswith("[") and raw.endswith("]")):
        return []
    body = raw[1:-1].strip()
    if not body:
        return []
    return [part.strip().strip('"').strip("'") for part in body.split(",") if part.strip()]


def parse_frontmatter(text: str) -> dict[str, object] | None:
    if not text.startswith("---"):
        return None
    lines = text.splitlines()
    if len(lines) < 3:
        return None
    end = None
    for idx in range(1, len(lines)):
        if lines[idx].rstrip() == "---":
            end = idx
            break
    if end is None:
        return None
    out: dict[str, object] = {}
    for raw_line in lines[1:end]:
        stripped = raw_line.strip()
        if not stripped or stripped.startswith("#") or ":" not in stripped:
            continue
        key, _, value = stripped.partition(":")
        key = key.strip()
        value = value.strip()
        out[key] = _parse_flow_list(value) if value.startswith("[") else value.strip('"').strip("'")
    return out


# ---------------------------------------------------------------------------
# Discovery + rendering

def iter_initiatives() -> list[Path]:
    paths = [p for p in SPECS.iterdir() if INITIATIVE_RE.match(p.name)]
    if DONE_DIR.is_dir():
        paths += [p for p in DONE_DIR.iterdir() if INITIATIVE_RE.match(p.name)]
    return sorted(paths, key=lambda p: p.name)


def _phase_summary(items: list[str]) -> str:
    return ", ".join(items) if items else "—"


def render_row(path: Path, fm: dict[str, object]) -> str:
    name = str(fm.get("name", path.stem))
    status = str(fm.get("status", "?"))
    phases_done = fm.get("phases_done") or []
    phases_next = fm.get("phases_next") or []
    last_touched = str(fm.get("last_touched", "?"))
    if not isinstance(phases_done, list):
        phases_done = [str(phases_done)]
    if not isinstance(phases_next, list):
        phases_next = [str(phases_next)]
    sub = "done/" if path.parent.name == "done" else ""
    link = f"[{name}](./{sub}{path.name})"
    return (
        f"| {link} | `{status}` | {_phase_summary(list(phases_done))} | "
        f"{_phase_summary(list(phases_next))} | {last_touched} |"
    )


def render_table(rows: list[str]) -> str:
    header = (
        "| Spec | Status | Phases done | Phases next | Last touched |\n"
        "|------|--------|-------------|-------------|--------------|\n"
    )
    return header + "\n".join(rows) + "\n"


def _stale_days(last_touched: object) -> int | None:
    s = str(last_touched or "")[:10]
    try:
        d = _dt.date.fromisoformat(s)
    except ValueError:
        return None
    return (_dt.date.today() - d).days


def main(argv: list[str] | None = None) -> int:
    args = argv or sys.argv[1:]
    if "--help" in args or "-h" in args:
        print(__doc__)
        return 0

    active_rows: list[str] = []
    done_rows: list[str] = []
    missing: list[str] = []
    invalid: list[str] = []
    aliased: list[str] = []
    drifting: list[str] = []
    misplaced: list[str] = []

    for path in iter_initiatives():
        text = path.read_text(encoding="utf-8")
        fm = parse_frontmatter(text)
        if fm is None:
            missing.append(path.name)
            continue
        raw_status = str(fm.get("status", "")).lower()
        status = STATUS_ALIASES.get(raw_status, raw_status)
        if status != raw_status:
            aliased.append(f"{path.name}: {raw_status!r} -> {status!r}")
            fm["status"] = status
        if status not in VALID_STATUS:
            invalid.append(f"{path.name} (status={raw_status!r})")
            continue
        age = _stale_days(fm.get("last_touched"))
        if status in ACTIVE_STATUS and age is not None and age > STALE_DAYS:
            drifting.append(
                f"{path.name}: {status} but untouched {age}d "
                f"(last_touched={fm.get('last_touched')}) — recheck vs git"
            )
        in_done_dir = path.parent.name == "done"
        is_done = status in DONE_STATUS
        if is_done and not in_done_dir:
            misplaced.append(f"{path.name}: {status} -> git mv to specs/done/")
        elif not is_done and in_done_dir:
            misplaced.append(f"{path.name}: {status} -> git mv out of specs/done/ to specs/")
        (done_rows if is_done else active_rows).append(render_row(path, fm))

    if missing:
        print("ERROR: specs missing YAML frontmatter:\n  - " + "\n  - ".join(missing), file=sys.stderr)
        return 1
    if invalid:
        print("ERROR: specs with invalid status field:\n  - " + "\n  - ".join(invalid)
              + f"\n  valid values: {sorted(VALID_STATUS)}", file=sys.stderr)
        return 1
    if aliased:
        print("NOTE: normalized non-canonical status values (fix the frontmatter):\n  - "
              + "\n  - ".join(aliased), file=sys.stderr)
    if drifting:
        print(f"WARN: {len(drifting)} active spec(s) untouched >{STALE_DAYS}d — "
              "frontmatter may be stale vs landed work:\n  - " + "\n  - ".join(drifting), file=sys.stderr)
    if misplaced:
        print(f"WARN: {len(misplaced)} spec(s) in the wrong directory for their status:\n  - "
              + "\n  - ".join(misplaced), file=sys.stderr)

    today = _dt.date.today().isoformat()
    body = [
        "# Spec status (auto-generated)",
        "",
        f"Generated by `scripts/gen-status.py` on {today}. Do not edit by hand —",
        "edit the frontmatter block at the top of each `specs/000NN-*.md` and rerun.",
        "",
        "Active specs (`proposed`/`planned`/`in-flight`) live in `specs/`; done ones",
        "(`shipped`/`live`/`retired`) in `specs/done/`. When a spec's status crosses",
        "that line, `git mv` it to match and rerun this script.",
        "",
        f"## Active ({len(active_rows)}) — planned / proposed / in-flight",
        "",
        render_table(active_rows).rstrip() if active_rows else "_none_",
        "",
        f"## Done ({len(done_rows)}) — shipped / live / retired (`specs/done/`)",
        "",
        render_table(done_rows).rstrip() if done_rows else "_none_",
        "",
    ]
    OUTPUT.write_text("\n".join(body), encoding="utf-8")
    print(f"wrote {OUTPUT.relative_to(ROOT)} ({len(active_rows)} active + {len(done_rows)} done)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
