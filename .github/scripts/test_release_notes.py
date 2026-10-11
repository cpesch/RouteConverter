import contextlib
import io
import os
import shutil
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import release_notes  # noqa: E402

OLDER = (
    "## 3.7 — 2026-10-10\n"
    "\n"
    "**GitHub Release:** https://github.com/cpesch/RouteConverter/releases/tag/3.7\n"
    "\n"
    "### Highlights (EN)\n"
    "\n"
    "RouteConverter 3.7 reads Google Maps Timeline exports — a paragraph.\n"
    "\n"
    "### Fixes\n"
    "\n"
    "- Starts again on macOS 15.\n"
)

EMPTY_NEXT = (
    "# Release notes\n"
    "\n"
    "## Next release\n"
    "\n"
    "### New features\n"
    "\n"
    "### Changes\n"
    "\n"
    "### Fixes\n"
    "\n"
)

CURATED_NEXT = (
    "# Release notes\n"
    "\n"
    "## Next release\n"
    "\n"
    "### New features\n"
    "\n"
    "- Stamp the release date (#427).\n"
    "\n"
    "### Changes\n"
    "\n"
    "### Fixes\n"
    "\n"
)

NO_NEXT = "# Release notes\n\n"


class CliTestCase(unittest.TestCase):
    def write(self, text):
        fd, path = tempfile.mkstemp(suffix=".md")
        with os.fdopen(fd, "w", encoding="utf-8", newline="") as f:
            f.write(text)
        self.addCleanup(os.remove, path)
        return path

    def read(self, path):
        with open(path, encoding="utf-8", newline="") as f:
            return f.read()

    def run_cli(self, *args):
        out, err = io.StringIO(), io.StringIO()
        with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
            code = release_notes.main(list(args))
        return code, out.getvalue(), err.getvalue()


class CheckNextTest(CliTestCase):
    def test_section_with_bullets_passes(self):
        path = self.write(CURATED_NEXT + OLDER)
        code, _, _ = self.run_cli("--file", path, "check-next")
        self.assertEqual(0, code)

    def test_section_with_only_headings_fails(self):
        path = self.write(EMPTY_NEXT + OLDER)
        code, _, err = self.run_cli("--file", path, "check-next")
        self.assertEqual(1, code)
        self.assertIn('"## Next release" has no bullets — curate it before cutting a release', err)

    def test_bullets_of_older_sections_do_not_count(self):
        path = self.write(EMPTY_NEXT + OLDER)
        code, _, _ = self.run_cli("--file", path, "check-next")
        self.assertEqual(1, code)

    def test_missing_section_fails(self):
        path = self.write(NO_NEXT + OLDER)
        code, _, err = self.run_cli("--file", path, "check-next")
        self.assertEqual(1, code)
        self.assertIn('has no "## Next release" section', err)


class StampTest(CliTestCase):
    def test_stamps_heading_and_inserts_github_release(self):
        path = self.write(CURATED_NEXT + OLDER)
        code, _, _ = self.run_cli("--file", path, "stamp", "3.8", "2026-12-01")
        self.assertEqual(0, code)
        self.assertEqual(
            "# Release notes\n"
            "\n"
            "## 3.8 — 2026-12-01\n"
            "\n"
            "**GitHub Release:** https://github.com/cpesch/RouteConverter/releases/tag/3.8\n"
            "\n"
            "### New features\n"
            "\n"
            "- Stamp the release date (#427).\n"
            "\n"
            "### Changes\n"
            "\n"
            "### Fixes\n"
            "\n" + OLDER,
            self.read(path))

    def test_later_sections_byte_identical(self):
        path = self.write(CURATED_NEXT + OLDER)
        self.run_cli("--file", path, "stamp", "3.8", "2026-12-01")
        self.assertTrue(self.read(path).endswith("### Fixes\n\n" + OLDER))

    def test_does_not_duplicate_github_release_line(self):
        text = CURATED_NEXT.replace(
            "## Next release\n",
            "## Next release\n\n**GitHub Release:** https://github.com/cpesch/RouteConverter/releases/tag/3.8\n")
        path = self.write(text + OLDER)
        self.run_cli("--file", path, "stamp", "3.8", "2026-12-01")
        self.assertEqual(1, self.read(path).count("releases/tag/3.8"))

    def test_refuses_existing_version(self):
        text = CURATED_NEXT + OLDER.replace("3.7", "3.8")
        path = self.write(text)
        code, _, err = self.run_cli("--file", path, "stamp", "3.8", "2026-12-01")
        self.assertEqual(1, code)
        self.assertIn('"## 3.8 " section already exists', err)
        self.assertEqual(text, self.read(path))

    def test_refuses_empty_next_release(self):
        path = self.write(EMPTY_NEXT + OLDER)
        code, _, err = self.run_cli("--file", path, "stamp", "3.8", "2026-12-01")
        self.assertEqual(1, code)
        self.assertIn("has no bullets", err)
        self.assertEqual(EMPTY_NEXT + OLDER, self.read(path))


class OpenNextTest(CliTestCase):
    def test_inserts_block_under_top_heading(self):
        path = self.write(NO_NEXT + OLDER)
        code, _, _ = self.run_cli("--file", path, "open-next")
        self.assertEqual(0, code)
        self.assertEqual(EMPTY_NEXT + OLDER, self.read(path))

    def test_idempotent(self):
        path = self.write(NO_NEXT + OLDER)
        self.run_cli("--file", path, "open-next")
        code, out, _ = self.run_cli("--file", path, "open-next")
        self.assertEqual(0, code)
        self.assertIn("already open", out)
        self.assertEqual(EMPTY_NEXT + OLDER, self.read(path))

    def test_stamp_then_open_round_trip(self):
        path = self.write(CURATED_NEXT + OLDER)
        self.run_cli("--file", path, "stamp", "3.8", "2026-12-01")
        self.run_cli("--file", path, "open-next")
        sections = release_notes.parse(self.read(path))
        self.assertEqual(["Next release", "3.8 — 2026-12-01", "3.7 — 2026-10-10"],
                         [s.title for s in sections])


MIDDLE = (
    "## 3.6 — 2026-08-01\n"
    "\n"
    "### Changes\n"
    "\n"
    "- Faster startup.\n"
    "\n"
    "\n"
)

OLDEST = (
    "## 3.5 — 2026-06-01\n"
    "\n"
    "### Fixes\n"
    "\n"
    "- Older fix.\n"
)

NO_BULLETS = (
    "## 3.6 — 2026-08-01\n"
    "\n"
    "Only a paragraph.\n"
    "\n"
)


class ExtractTest(CliTestCase):
    def test_middle_section_stops_before_next_heading(self):
        path = self.write(NO_NEXT + OLDER + "\n" + MIDDLE + OLDEST)
        code, out, _ = self.run_cli("--file", path, "extract", "3.6")
        self.assertEqual(0, code)
        self.assertEqual("## 3.6 — 2026-08-01\n\n### Changes\n\n- Faster startup.\n", out)

    def test_newest_section(self):
        path = self.write(NO_NEXT + OLDER + "\n" + MIDDLE + OLDEST)
        code, out, _ = self.run_cli("--file", path, "extract", "3.7")
        self.assertEqual(0, code)
        self.assertEqual(OLDER, out)

    def test_missing_version_fails(self):
        path = self.write(NO_NEXT + OLDER)
        code, out, err = self.run_cli("--file", path, "extract", "3.8")
        self.assertEqual(1, code)
        self.assertEqual("", out)
        self.assertIn(f'no "## 3.8 — …" section in {path}', err)

    def test_version_prefix_does_not_match_longer_version(self):
        path = self.write(NO_NEXT + OLDER.replace("3.7", "3.7.1"))
        code, _, err = self.run_cli("--file", path, "extract", "3.7")
        self.assertEqual(1, code)
        self.assertIn('no "## 3.7 — …" section', err)

    def test_section_without_bullets_fails(self):
        path = self.write(NO_NEXT + NO_BULLETS + OLDEST)
        code, _, err = self.run_cli("--file", path, "extract", "3.6")
        self.assertEqual(1, code)
        self.assertIn('section "3.6" has no bullets', err)

    def test_out_writes_same_bytes_as_stdout(self):
        path = self.write(NO_NEXT + OLDER + "\n" + MIDDLE + OLDEST)
        _, out, _ = self.run_cli("--file", path, "extract", "3.6")
        fd, target = tempfile.mkstemp(suffix=".md")
        os.close(fd)
        self.addCleanup(os.remove, target)
        code, stdout, _ = self.run_cli("--file", path, "extract", "3.6", "--out", target)
        self.assertEqual(0, code)
        self.assertEqual("", stdout)
        self.assertEqual(out, self.read(target))


DRAFTS_NOTES = (
    "# Release notes\n"
    "\n"
    "## 3.8 — 2026-12-01\n"
    "\n"
    "**GitHub Release:** https://github.com/cpesch/RouteConverter/releases/tag/3.8\n"
    "\n"
    "### Highlights (EN)\n"
    "\n"
    "RouteConverter 3.8 is faster.\n"
    "\n"
    "### Was ist neu (DE)\n"
    "\n"
    "RouteConverter 3.8 ist schneller\n"
    "und kann mehr.\n"
    "\n"
    "### New features\n"
    "\n"
    "- Reads the phone's `Timeline.json` export from the Google Maps app without dropping a single point of the recorded day's history (#351)\n"
    "- Maintainer's favourite: a font-size setting\n"
    "\n"
    "### Changes\n"
    "\n"
    "### Fixes\n"
    "\n"
    "- Starts again on macOS 15 (#393)\n"
    "\n"
    "### Known issues\n"
    "\n"
    "- Blurry text with display scaling (#343)\n"
    "\n"
    "## 3.7 — 2026-10-10\n"
    "\n"
    "### Fixes\n"
    "\n"
    "- Older fix.\n"
)

FORUM_EN = (
    "RouteConverter 3.8 released\n"
    "\n"
    "RouteConverter 3.8 is available: https://www.routeconverter.com/downloads/\n"
    "Release notes: https://www.routeconverter.com/releases/3-8/\n"
    "\n"
    "New features\n"
    "\n"
    "- Reads the phone's Timeline.json export from the Google Maps app without dropping a single point of the recorded day's history (#351)\n"
    "- Maintainer's favourite: a font-size setting\n"
    "\n"
    "Fixes\n"
    "\n"
    "- Starts again on macOS 15 (#393)\n"
    "\n"
    "Known issues\n"
    "\n"
    "- Blurry text with display scaling (#343)\n"
)

FORUM_DE = (
    "RouteConverter 3.8 ist erschienen\n"
    "\n"
    "RouteConverter 3.8 steht zum Download bereit: https://www.routeconverter.de/downloads/\n"
    "Versionshinweise: https://www.routeconverter.de/releases/3-8/\n"
    "\n"
    "RouteConverter 3.8 ist schneller und kann mehr.\n"
    "\n"
    "TODO: Stichpunkte übersetzen\n"
    "\n"
    "Neue Funktionen\n"
    "\n"
    "- Reads the phone's Timeline.json export from the Google Maps app without dropping a single point of the recorded day's history (#351)\n"
    "- Maintainer's favourite: a font-size setting\n"
    "\n"
    "Fehlerbehebungen\n"
    "\n"
    "- Starts again on macOS 15 (#393)\n"
    "\n"
    "Bekannte Probleme\n"
    "\n"
    "- Blurry text with display scaling (#343)\n"
)

PAGE_BULLETS = (
    "- Reads the phone's `Timeline.json` export from the Google Maps app without dropping a single point of the recorded day's history (#351)\n"
    "- Maintainer's favourite: a font-size setting\n"
    "- Starts again on macOS 15 (#393)\n"
)

PAGE_EN = (
    "---\n"
    "title: Release 3.8 from 01.12.2026\n"
    "slug: releases/3-8\n"
    "surface: site\n"
    "lang_status: translated\n"
    "provenance: hand-authored\n"
    "last_modified: '2026-12-01'\n"
    "seo_title: 'RouteConverter 3.8 release notes (1 Dec 2026)'\n"
    "description: 'What is new in RouteConverter 3.8 (1 Dec 2026): Reads the phone''s `Timeline.json` export from the Google Maps app without dropping a single point.'\n"
    "categories:\n"
    "- release\n"
    "---\n"
    "\n"
    "# Release 3.8 from 01.12.2026\n"
    "\n" + PAGE_BULLETS
)

PAGE_DE = (
    "---\n"
    "title: Release 3.8 vom 01.12.2026\n"
    "slug: releases/3-8\n"
    "surface: site\n"
    "lang_status: translated\n"
    "provenance: hand-authored\n"
    "last_modified: '2026-12-01'\n"
    "seo_title: 'RouteConverter 3.8 Release Notes (1. Dezember 2026)'\n"
    "description: 'Neu in RouteConverter 3.8 (1. Dezember 2026): TODO'\n"
    "categories:\n"
    "- release\n"
    "---\n"
    "\n"
    "# Release 3.8 vom 01.12.2026\n"
    "\n"
    "<!-- TODO: übersetzen -->\n"
    "\n" + PAGE_BULLETS
)


class TruncateTest(unittest.TestCase):
    def test_short_text_kept_without_final_period(self):
        self.assertEqual("Faster startup", release_notes._truncate("Faster startup.", 120))

    def test_drops_unclosed_parenthesis(self):
        self.assertEqual("Reads exports from the app",
                         release_notes._truncate("Reads exports from the app (`a.json` / `b.json`) fast", 40))

    def test_drops_trailing_function_words(self):
        self.assertEqual("Keeps every point", release_notes._truncate("Keeps every point of the day", 22))


class DraftsTest(CliTestCase):
    def setUp(self):
        self.out = tempfile.mkdtemp()
        self.addCleanup(shutil.rmtree, self.out)
        path = self.write(DRAFTS_NOTES)
        code, _, _ = self.run_cli("--file", path, "drafts", "3.8", "2026-12-01", "--out", self.out)
        self.assertEqual(0, code)

    def draft(self, name):
        return self.read(os.path.join(self.out, name))

    def test_writes_exactly_four_files(self):
        self.assertEqual(
            sorted(["forum-3.8-en.txt", "forum-3.8-de.txt",
                    "rc-content-en-3-8-index.md", "rc-content-de-3-8-index.md"]),
            sorted(os.listdir(self.out)))

    def test_forum_en(self):
        self.assertEqual(FORUM_EN, self.draft("forum-3.8-en.txt"))

    def test_forum_de(self):
        self.assertEqual(FORUM_DE, self.draft("forum-3.8-de.txt"))

    def test_rc_content_en(self):
        self.assertEqual(PAGE_EN, self.draft("rc-content-en-3-8-index.md"))

    def test_rc_content_de(self):
        self.assertEqual(PAGE_DE, self.draft("rc-content-de-3-8-index.md"))

    def test_missing_version_fails(self):
        path = self.write(DRAFTS_NOTES)
        code, _, err = self.run_cli("--file", path, "drafts", "3.9", "2026-12-01", "--out", self.out)
        self.assertEqual(1, code)
        self.assertIn('no "## 3.9 — …" section', err)


class ParseTest(unittest.TestCase):
    def test_multi_line_bullet_joined(self):
        sections = release_notes.parse(
            "# Release notes\n\n## 3.4\n\n### Upgrade notes\n\n"
            "- Java runtime requirement stays at 17 or later (bundled installer\n"
            "  ships a JRE).\n"
            "- Settings carry over.\n")
        self.assertEqual(
            ["Java runtime requirement stays at 17 or later (bundled installer ships a JRE).",
             "Settings carry over."],
            sections[0].subsections["Upgrade notes"])

    def test_subsection_order_kept(self):
        sections = release_notes.parse(CURATED_NEXT + OLDER)
        self.assertEqual(["New features", "Changes", "Fixes"], list(sections[0].subsections))
        self.assertEqual(["Stamp the release date (#427)."], sections[0].subsections["New features"])
        self.assertEqual([], sections[0].subsections["Changes"])

    def test_paragraph_text_ignored(self):
        sections = release_notes.parse(OLDER)
        self.assertEqual("3.7 — 2026-10-10", sections[0].title)
        self.assertEqual([], sections[0].subsections["Highlights (EN)"])
        self.assertEqual(["Starts again on macOS 15."], sections[0].subsections["Fixes"])

    def test_parses_real_release_notes(self):
        path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "RELEASE_NOTES.md")
        if not os.path.exists(path):
            self.skipTest("RELEASE_NOTES.md not present")
        with open(path, encoding="utf-8") as f:
            sections = release_notes.parse(f.read())
        self.assertEqual("Next release", sections[0].title)


if __name__ == "__main__":
    unittest.main()
