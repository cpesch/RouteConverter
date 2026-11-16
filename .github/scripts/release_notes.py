#!/usr/bin/env python3
"""Maintain RELEASE_NOTES.md during release-prepare and PR checks.

Shape (see RELEASE_NOTES.md): line 1 ``# Release notes``, then version
sections ``## <title>``, each with ``### <subsection>`` blocks of ``- ``
bullets. The working section is titled exactly ``## Next release`` and sits
directly under ``# Release notes``.

Subcommands:
  check-next              fail unless "## Next release" exists and has bullets
  stamp VERSION DATE      rename "## Next release" to "## VERSION — DATE"
  open-next               insert an empty "## Next release" section on top
  extract VERSION         print the "## VERSION — DATE" section (or --out PATH)
  drafts VERSION DATE     write forum posts + rc-content pages into --out DIR
  added-next BASE HEAD    print "## Next release" bullets HEAD adds over BASE,
                          fail if none (ignores --file)

Python 3 stdlib only; other release tooling imports ``parse``.
"""

import argparse
import os
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


def _next_bullets(text):
    """Return the bullets of all subsections of Next release, [] without one."""
    for section in parse(text):
        if f"## {section.title}" == NEXT_HEADING:
            return [bullet for bullets in section.subsections.values() for bullet in bullets]
    return []


def added_next(base_text, head_text):
    """Return the Next release bullets of HEAD missing from BASE, in HEAD order.

    A reworded bullet counts as new; bullets of other sections never count.
    """
    known = set(_next_bullets(base_text))
    added = [bullet for bullet in _next_bullets(head_text) if bullet not in known]
    if not added:
        raise ReleaseNotesError(f'no new bullet under "{NEXT_HEADING}" in {DEFAULT_FILE}')
    return added


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


def extract(text, version, path=DEFAULT_FILE):
    """Return the whole "## VERSION — …" section, heading included, up to the
    next "## " heading, trailing blank lines trimmed, ending in one newline."""
    lines = text.splitlines()
    prefix = f"## {version} — "
    for start, line in enumerate(lines):
        if line.startswith(prefix):
            end = start + 1
            while end < len(lines) and not _is_section_heading(lines[end]):
                end += 1
            section = lines[start:end]
            if not any(line.startswith("- ") for line in section[1:]):
                raise ReleaseNotesError(f'section "{version}" has no bullets')
            while section and not section[-1].strip():
                section.pop()
            return "\n".join(section) + "\n"
    raise ReleaseNotesError(f'no "## {version} — …" section in {path}')


FORUM_SUBSECTIONS = (
    ("New features", "Neue Funktionen"),
    ("Changes", "Änderungen"),
    ("Fixes", "Fehlerbehebungen"),
    ("Upgrade notes", "Hinweise zum Update"),
)
DE_HIGHLIGHTS = "Was ist neu (DE)"
MONTHS_EN = ("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
MONTHS_DE = ("Januar", "Februar", "März", "April", "Mai", "Juni", "Juli", "August", "September",
             "Oktober", "November", "Dezember")
DESCRIPTION_LIMIT = 120
# rc-content's validate_frontmatter.py rejects descriptions above 160 chars.
DESCRIPTION_MAX = 160


def _subsection_lines(section_text, subsection):
    """Return the raw lines of a ``### subsection`` (trailing whitespace
    stripped), up to the next ``#`` heading, or None if the heading is
    absent."""
    lines = section_text.splitlines()
    heading = f"### {subsection}"
    for start, line in enumerate(lines):
        if line.rstrip() == heading:
            body = []
            for line in lines[start + 1:]:
                if line.startswith("#"):
                    break
                body.append(line.rstrip())
            return body
    return None


def _paragraph(section_text, subsection):
    """Return the non-bullet paragraph text of a ``### subsection`` (lines
    joined, ``- `` lines skipped), or None."""
    body = _subsection_lines(section_text, subsection)
    if body is None:
        return None
    parts = []
    for line in body:
        stripped = line.strip()
        if stripped.startswith("- "):
            continue
        parts.append(stripped)
    text = " ".join(part for part in parts if part)
    return text or None


def _block(section_text, subsection):
    """Return the raw lines of a ``### subsection``, trailing whitespace
    stripped and leading/trailing blank lines removed, or [] if absent."""
    body = _subsection_lines(section_text, subsection)
    if body is None:
        return []
    while body and not body[0].strip():
        body.pop(0)
    while body and not body[-1].strip():
        body.pop()
    return body


def _bbcode(lines):
    """Convert raw LINES to MyBB BBCode: a run of consecutive ``- `` lines
    (blank lines inside a run dropped) becomes ``[list]``/``[*]``/``[/list]``;
    a non-bullet line ending in ``:`` directly followed (blank lines allowed)
    by a bullet run becomes ``[b]...[/b]``; other lines pass through;
    backticks are stripped throughout."""
    out = []
    i, n = 0, len(lines)
    while i < n:
        line = lines[i]
        if line.startswith("- "):
            bullets = []
            while i < n:
                if lines[i].startswith("- "):
                    bullets.append(lines[i][2:].strip().replace("`", ""))
                    i += 1
                elif not lines[i].strip():
                    j = i
                    while j < n and not lines[j].strip():
                        j += 1
                    if j < n and lines[j].startswith("- "):
                        i = j
                    else:
                        break
                else:
                    break
            out.append("[list]")
            out.extend(f"[*]{bullet}" for bullet in bullets)
            out.append("[/list]")
            continue
        if line.rstrip().endswith(":"):
            j = i + 1
            while j < n and not lines[j].strip():
                j += 1
            if j < n and lines[j].startswith("- "):
                out.append(f"[b]{line.rstrip().replace('`', '')}[/b]")
                i += 1
                continue
        out.append(line.replace("`", ""))
        i += 1
    return out


def _first_sentence(text):
    """Return TEXT up to and including the end of its first sentence."""
    end = text.find(". ")
    if end != -1:
        return text[:end + 1]
    return text if text.endswith(".") else text + "."


def _highlight_description(prefix, highlight):
    """Build a <=DESCRIPTION_MAX description from PREFIX plus the first
    sentence of HIGHLIGHT, shortened with ``_truncate`` if it doesn't fit."""
    sentence = _first_sentence(highlight)
    if len(prefix) + len(sentence) <= DESCRIPTION_MAX:
        return f"{prefix}{sentence}"
    limit = DESCRIPTION_MAX - len(prefix) - 1
    return f"{prefix}{_truncate(sentence, limit)}."


DANGLING_WORDS = {"a", "an", "and", "as", "at", "by", "for", "from", "in", "of", "on", "or", "the",
                  "to", "with", "without"}


def _truncate(text, limit=DESCRIPTION_LIMIT):
    """Cut at a word boundary within limit; never end on an unclosed "(",
    dangling punctuation or a function word."""
    text = text.rstrip(".")
    if len(text) <= limit:
        return text
    words = text[:limit + 1].rsplit(" ", 1)[0].split(" ")
    while words:
        cut = " ".join(words)
        if cut.count("(") > cut.count(")"):
            cut = cut[:cut.rindex("(")]
        cut = cut.rstrip(" ,;:.—-/`(")
        if cut.split(" ")[-1].lower() not in DANGLING_WORDS:
            return cut
        words = cut.split(" ")[:-1]
    return ""


def _yaml_quote(value):
    return "'" + value.replace("'", "''") + "'"


def _forum(version, sections, en):
    slug = version.replace(".", "-")
    lines = []
    if en:
        lines += [f"RouteConverter {version} released", "",
                  f"RouteConverter {version} is available: https://www.routeconverter.com/downloads/",
                  f"Release notes: https://www.routeconverter.com/releases/{slug}/", ""]
        highlights = _block(sections["text"], "Highlights (EN)")
    else:
        lines += [f"RouteConverter {version} ist erschienen", "",
                  f"RouteConverter {version} steht zum Download bereit: https://www.routeconverter.de/downloads/",
                  f"Versionshinweise: https://www.routeconverter.de/releases/{slug}/", ""]
        highlights = _block(sections["text"], DE_HIGHLIGHTS)
    if highlights:
        lines += _bbcode(highlights)
        lines.append("")
    if not en:
        lines += ["TODO: Stichpunkte übersetzen", ""]
    for name_en, name_de in FORUM_SUBSECTIONS:
        bullets = sections["section"].subsections.get(name_en) or []
        if not bullets:
            continue
        name = name_en if en else name_de
        lines += [f"[b]{name}[/b]", "", "[list]"]
        lines += [f"[*]{bullet.replace('`', '')}" for bullet in bullets]
        lines += ["[/list]", ""]
    return "\n".join(lines).rstrip("\n") + "\n"


def _rc_content(version, date, sections, en):
    section = sections["section"]
    year, month, day = (int(part) for part in date.split("-"))
    dotted = f"{day:02d}.{month:02d}.{year}"
    slug = version.replace(".", "-")
    subsections = section.subsections
    bullets = list(subsections.get("New features") or []) + list(subsections.get("Fixes") or [])
    if en:
        long_date = f"{day} {MONTHS_EN[month - 1]} {year}"
        title = f"Release {version} from {dotted}"
        seo_title = f"RouteConverter {version} release notes ({long_date})"
        prefix = f"What is new in RouteConverter {version} ({long_date}): "
        highlight = _paragraph(sections["text"], "Highlights (EN)")
        if highlight:
            description = _highlight_description(prefix, highlight)
        else:
            features = subsections.get("New features") or []
            limit = min(DESCRIPTION_LIMIT, DESCRIPTION_MAX - len(prefix) - 1)
            summary = _truncate(features[0], limit) if features else "TODO"
            description = f"{prefix}{summary}."
    else:
        long_date = f"{day}. {MONTHS_DE[month - 1]} {year}"
        title = f"Release {version} vom {dotted}"
        seo_title = f"RouteConverter {version} Release Notes ({long_date})"
        prefix = f"Neu in RouteConverter {version} ({long_date}): "
        highlight = _paragraph(sections["text"], DE_HIGHLIGHTS)
        description = _highlight_description(prefix, highlight) if highlight else f"{prefix}TODO"
    lines = [
        "---",
        f"title: {title}",
        f"slug: releases/{slug}",
        "surface: site",
        "lang_status: translated",
        "provenance: hand-authored",
        f"last_modified: {_yaml_quote(date)}",
        f"seo_title: {_yaml_quote(seo_title)}",
        f"description: {_yaml_quote(description)}",
        "categories:",
        "- release",
        "---",
        "",
        f"# {title}",
        "",
    ]
    if not en:
        lines += ["<!-- TODO: übersetzen -->", ""]
    lines += ["- " + bullet for bullet in bullets]
    return "\n".join(lines) + "\n"


def drafts(text, version, date, path=DEFAULT_FILE):
    """Return {filename: content} for the four announcement drafts."""
    section_text = extract(text, version, path)
    section = parse(section_text)[0]
    sections = {"text": section_text, "section": section}
    slug = version.replace(".", "-")
    return {
        f"forum-{version}-en.txt": _forum(version, sections, en=True),
        f"forum-{version}-de.txt": _forum(version, sections, en=False),
        f"rc-content-en-{slug}-index.md": _rc_content(version, date, sections, en=True),
        f"rc-content-de-{slug}-index.md": _rc_content(version, date, sections, en=False),
    }


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
    extract_parser = commands.add_parser("extract", help='print the "## VERSION — DATE" section')
    extract_parser.add_argument("version")
    extract_parser.add_argument("--out", help="write the section to this file instead of stdout")
    drafts_parser = commands.add_parser("drafts", help="write announcement drafts for VERSION")
    drafts_parser.add_argument("version")
    drafts_parser.add_argument("date", help="release date YYYY-MM-DD")
    drafts_parser.add_argument("--out", required=True, help="directory for the draft files")
    added_parser = commands.add_parser("added-next", help='print "## Next release" bullets HEAD adds over BASE')
    added_parser.add_argument("base_file", help="release notes of the PR base")
    added_parser.add_argument("head_file", help="release notes of the PR head")
    args = parser.parse_args(argv)

    try:
        if args.command == "added-next":
            for bullet in added_next(_read(args.base_file), _read(args.head_file)):
                print(bullet)
            return 0
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
        elif args.command == "extract":
            section = extract(text, args.version, args.file)
            if args.out:
                _write(args.out, section)
            else:
                sys.stdout.write(section)
        elif args.command == "drafts":
            os.makedirs(args.out, exist_ok=True)
            for name, content in drafts(text, args.version, args.date, args.file).items():
                _write(os.path.join(args.out, name), content)
                print(os.path.join(args.out, name))
    except ReleaseNotesError as e:
        print(e, file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
