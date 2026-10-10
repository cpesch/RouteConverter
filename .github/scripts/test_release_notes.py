import contextlib
import io
import os
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
