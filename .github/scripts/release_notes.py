#!/usr/bin/env python3
"""Maintain RELEASE_NOTES.md during release-prepare.

Shape (see RELEASE_NOTES.md): line 1 ``# Release notes``, then version
sections ``## <title>``, each with ``### <subsection>`` blocks of ``- ``
bullets. The working section is titled exactly ``## Next release`` and sits
directly under ``# Release notes``.

Subcommands:
  check-next              fail unless "## Next release" exists and has bullets
  stamp VERSION DATE      rename "## Next release" to "## VERSION — DATE"
  open-next               insert an empty "## Next release" section on top

Python 3 stdlib only; other release tooling imports ``parse``.
"""

import argparse
import sys
from dataclasses import dataclass, field

DEFAULT_FILE = "RELEASE_NOTES.md"
TOP_HEADING = "# Release notes"
NEXT_HEADING = "## Next release"
GITHUB_RELEASE = "**GitHub Release:** https://github.com/cpesch/RouteConverter/releases/tag/{version}"
NEXT_BLOCK = (
    "## Next release\n"
    "\n"
    "### New features\n"
    "\n"
    "### Changes\n"
    "\n"
    "### Fixes\n"
    "\n"
)


class ReleaseNotesError(Exception):
    pass


@dataclass
class Section:
    title: str
    subsections: dict = field(default_factory=dict)


def _is_section_heading(line):
    return line.startswith("## ")


def parse(text):
    """Split RELEASE_NOTES.md into sections with bullet lists per subsection.

    Bullets have their leading ``- `` stripped; indented continuation lines
    are joined with a single space. Non-bullet text (paragraphs, tables,
    the GitHub Release line) is ignored.
    """
    sections = []
    section = None
    bullets = None
    current = None
    for raw in text.splitlines():
        line = raw.rstrip()
        if _is_section_heading(line):
            section = Section(title=line[3:].strip())
            sections.append(section)
            bullets = None
            current = None
        elif line.startswith("### ") and section is not None:
            bullets = section.subsections.setdefault(line[4:].strip(), [])
            current = None
        elif bullets is None:
            continue
        elif line.startswith("- "):
            bullets.append(line[2:].strip())
            current = len(bullets) - 1
        elif current is not None and line and raw[:1].isspace():
            bullets[current] = bullets[current] + " " + line.strip()
        else:
            current = None
    return sections


def _next_bounds(lines):
    """Return (start, end) line indices of the Next release section, or None."""
    for start, line in enumerate(lines):
        if line.rstrip("\r\n") == NEXT_HEADING:
            end = start + 1
            while end < len(lines) and not _is_section_heading(lines[end]):
                end += 1
            return start, end
    return None


def check_next(text, path=DEFAULT_FILE):
    lines = text.splitlines(keepends=True)
    bounds = _next_bounds(lines)
    if bounds is None:
        raise ReleaseNotesError(f'{path}: has no "{NEXT_HEADING}" section')
    start, end = bounds
    if not any(line.startswith("- ") for line in lines[start + 1:end]):
        raise ReleaseNotesError(
            f'{path}: "{NEXT_HEADING}" has no bullets — curate it before cutting a release')
    return lines, start


def stamp(text, version, date, path=DEFAULT_FILE):
    lines, start = check_next(text, path)
    if any(line.rstrip("\r\n") == f"## {version}" or line.startswith(f"## {version} ") for line in lines):
        raise ReleaseNotesError(f'{path}: a "## {version} " section already exists')
    newline = lines[start][len(NEXT_HEADING):] or "\n"
    lines[start] = f"## {version} — {date}{newline}"
    release_line = GITHUB_RELEASE.format(version=version)
    following = [line.rstrip("\r\n") for line in lines[start + 1:start + 3]]
    if release_line not in following:
        lines[start + 1:start + 1] = [newline, release_line + newline]
    return "".join(lines)


def open_next(text, path=DEFAULT_FILE):
    lines = text.splitlines(keepends=True)
    if _next_bounds(lines) is not None:
        return None
    if not lines or lines[0].rstrip("\r\n") != TOP_HEADING:
        raise ReleaseNotesError(f'{path}: line 1 is not "{TOP_HEADING}"')
    insert = 1
    if insert < len(lines) and not lines[insert].strip():
        insert += 1
    else:
        lines.insert(insert, "\n")
        insert += 1
    lines.insert(insert, NEXT_BLOCK)
    return "".join(lines)


def _read(path):
    with open(path, encoding="utf-8", newline="") as f:
        return f.read()


def _write(path, text):
    with open(path, "w", encoding="utf-8", newline="") as f:
        f.write(text)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--file", default=DEFAULT_FILE, help=f"release notes file (default {DEFAULT_FILE})")
    commands = parser.add_subparsers(dest="command", required=True)
    commands.add_parser("check-next", help='fail unless "## Next release" has bullets')
    stamp_parser = commands.add_parser("stamp", help='rename "## Next release" to "## VERSION — DATE"')
    stamp_parser.add_argument("version")
    stamp_parser.add_argument("date")
    commands.add_parser("open-next", help='insert an empty "## Next release" section')
    args = parser.parse_args(argv)

    try:
        text = _read(args.file)
        if args.command == "check-next":
            check_next(text, args.file)
            print(f'{args.file}: "{NEXT_HEADING}" is curated')
        elif args.command == "stamp":
            _write(args.file, stamp(text, args.version, args.date, args.file))
            print(f"{args.file}: stamped {args.version} — {args.date}")
        elif args.command == "open-next":
            result = open_next(text, args.file)
            if result is None:
                print("already open")
            else:
                _write(args.file, result)
                print(f'{args.file}: opened "{NEXT_HEADING}"')
    except ReleaseNotesError as e:
        print(e, file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
