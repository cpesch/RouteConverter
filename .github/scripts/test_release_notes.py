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


NEW_FIX = "- Opens GPX files with a BOM again (#490).\n"


class AddedNextTest(CliTestCase):
    def added_next(self, base, head):
        return self.run_cli("added-next", self.write(base), self.write(head))

    def test_new_fixes_bullet_passes_and_is_printed(self):
        code, out, _ = self.added_next(EMPTY_NEXT + OLDER, EMPTY_NEXT + NEW_FIX + "\n" + OLDER)
        self.assertEqual(0, code)
        self.assertEqual("Opens GPX files with a BOM again (#490).\n", out)

    def test_identical_fails(self):
        code, out, err = self.added_next(CURATED_NEXT + OLDER, CURATED_NEXT + OLDER)
        self.assertEqual(1, code)
        self.assertEqual("", out)
        self.assertIn('no new bullet under "## Next release" in RELEASE_NOTES.md', err)

    def test_reworded_bullet_counts_as_new(self):
        head = CURATED_NEXT.replace("Stamp the release date", "Stamp the release date and time")
        code, out, _ = self.added_next(CURATED_NEXT + OLDER, head + OLDER)
        self.assertEqual(0, code)
        self.assertEqual("Stamp the release date and time (#427).\n", out)

    def test_existing_bullets_not_printed(self):
        code, out, _ = self.added_next(CURATED_NEXT + OLDER, CURATED_NEXT + NEW_FIX + "\n" + OLDER)
        self.assertEqual(0, code)
        self.assertEqual("Opens GPX files with a BOM again (#490).\n", out)

    def test_base_without_section_counts_as_empty(self):
        code, out, _ = self.added_next(NO_NEXT + OLDER, CURATED_NEXT + OLDER)
        self.assertEqual(0, code)
        self.assertEqual("Stamp the release date (#427).\n", out)

    def test_head_without_section_fails(self):
        code, _, err = self.added_next(CURATED_NEXT + OLDER, NO_NEXT + OLDER)
        self.assertEqual(1, code)
        self.assertIn('no new bullet under "## Next release"', err)

    def test_bullet_under_older_section_fails(self):
        code, _, err = self.added_next(EMPTY_NEXT + OLDER, EMPTY_NEXT + OLDER + NEW_FIX)
        self.assertEqual(1, code)
        self.assertIn('no new bullet under "## Next release"', err)


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
    "Most important fixes:\n"
    "\n"
    "- Starts again on macOS 15.\n"
    "- Opens GPX files with a BOM again.\n"
    "\n"
    "### Was ist neu (DE)\n"
    "\n"
    "RouteConverter 3.8 ist schneller.\n"
    "\n"
    "Die wichtigsten Fehlerbehebungen:\n"
    "\n"
    "- Startet wieder unter macOS 15.\n"
    "- Öffnet GPX-Dateien wieder mit BOM.\n"
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
    "### Upgrade notes\n"
    "\n"
    "- Java runtime requirement stays at 21 or later.\n"
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
    "RouteConverter 3.8 is faster.\n"
    "\n"
    "[b]Most important fixes:[/b]\n"
    "\n"
    "[list]\n"
    "[*]Starts again on macOS 15.\n"
    "[*]Opens GPX files with a BOM again.\n"
    "[/list]\n"
    "\n"
    "[b]New features[/b]\n"
    "\n"
    "[list]\n"
    "[*]Reads the phone's Timeline.json export from the Google Maps app without dropping a single point of the recorded day's history (#351)\n"
    "[*]Maintainer's favourite: a font-size setting\n"
    "[/list]\n"
    "\n"
    "[b]Fixes[/b]\n"
    "\n"
    "[list]\n"
    "[*]Starts again on macOS 15 (#393)\n"
    "[/list]\n"
    "\n"
    "[b]Upgrade notes[/b]\n"
    "\n"
    "[list]\n"
    "[*]Java runtime requirement stays at 21 or later.\n"
    "[/list]\n"
)

FORUM_DE = (
    "RouteConverter 3.8 ist erschienen\n"
    "\n"
    "RouteConverter 3.8 steht zum Download bereit: https://www.routeconverter.de/downloads/\n"
    "Versionshinweise: https://www.routeconverter.de/releases/3-8/\n"
    "\n"
    "RouteConverter 3.8 ist schneller.\n"
    "\n"
    "[b]Die wichtigsten Fehlerbehebungen:[/b]\n"
    "\n"
    "[list]\n"
    "[*]Startet wieder unter macOS 15.\n"
    "[*]Öffnet GPX-Dateien wieder mit BOM.\n"
    "[/list]\n"
    "\n"
    "TODO: Stichpunkte übersetzen\n"
    "\n"
    "[b]Neue Funktionen[/b]\n"
    "\n"
    "[list]\n"
    "[*]Reads the phone's Timeline.json export from the Google Maps app without dropping a single point of the recorded day's history (#351)\n"
    "[*]Maintainer's favourite: a font-size setting\n"
    "[/list]\n"
    "\n"
    "[b]Fehlerbehebungen[/b]\n"
    "\n"
    "[list]\n"
    "[*]Starts again on macOS 15 (#393)\n"
    "[/list]\n"
    "\n"
    "[b]Hinweise zum Update[/b]\n"
    "\n"
    "[list]\n"
    "[*]Java runtime requirement stays at 21 or later.\n"
    "[/list]\n"
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
    "description: 'What is new in RouteConverter 3.8 (1 Dec 2026): RouteConverter 3.8 is faster.'\n"
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
    "description: 'Neu in RouteConverter 3.8 (1. Dezember 2026): RouteConverter 3.8 ist schneller.'\n"
    "categories:\n"
    "- release\n"
    "---\n"
    "\n"
    "# Release 3.8 vom 01.12.2026\n"
    "\n"
    "<!-- TODO: übersetzen -->\n"
    "\n" + PAGE_BULLETS
)


def _description(content):
    """Return the decoded value of the single-quoted YAML ``description:``
    line in CONTENT (YAML escapes ``'`` inside single quotes as ``''``)."""
    for line in content.splitlines():
        if line.startswith("description: "):
            value = line[len("description: "):]
            if len(value) >= 2 and value[0] == value[-1] == "'":
                value = value[1:-1].replace("''", "'")
            return value
    raise AssertionError("no description line found")


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

    def test_forum_en_and_de_have_same_bullet_and_list_counts(self):
        en, de = self.draft("forum-3.8-en.txt"), self.draft("forum-3.8-de.txt")
        self.assertEqual(en.count("[*]"), de.count("[*]"))
        self.assertEqual(en.count("[list]"), de.count("[list]"))
        self.assertEqual(en.count("[list]"), en.count("[/list]"))

    def test_highlights_list_not_joined_into_paragraph(self):
        en = self.draft("forum-3.8-en.txt")
        self.assertIn("[b]Most important fixes:[/b]\n\n[list]\n[*]Starts again on macOS 15.", en)
        self.assertNotIn("Most important fixes: -", en)

    def test_upgrade_notes_present_known_issues_absent(self):
        en, de = self.draft("forum-3.8-en.txt"), self.draft("forum-3.8-de.txt")
        self.assertIn("[b]Upgrade notes[/b]", en)
        self.assertIn("[b]Hinweise zum Update[/b]", de)
        self.assertNotIn("Known issues", en)
        self.assertNotIn("Bekannte Probleme", de)

    def test_first_line_is_subject_second_line_blank(self):
        en_lines = self.draft("forum-3.8-en.txt").splitlines()
        de_lines = self.draft("forum-3.8-de.txt").splitlines()
        self.assertEqual("RouteConverter 3.8 released", en_lines[0])
        self.assertEqual("", en_lines[1])
        self.assertEqual("RouteConverter 3.8 ist erschienen", de_lines[0])
        self.assertEqual("", de_lines[1])

    def test_rc_content_en(self):
        self.assertEqual(PAGE_EN, self.draft("rc-content-en-3-8-index.md"))

    def test_rc_content_de(self):
        self.assertEqual(PAGE_DE, self.draft("rc-content-de-3-8-index.md"))

    def test_descriptions_fit_and_use_highlights(self):
        en = _description(self.draft("rc-content-en-3-8-index.md"))
        de = _description(self.draft("rc-content-de-3-8-index.md"))
        for description, prefix in ((en, "What is new in RouteConverter 3.8 (1 Dec 2026): "),
                                     (de, "Neu in RouteConverter 3.8 (1. Dezember 2026): ")):
            self.assertLessEqual(len(description), 160)
            self.assertTrue(description.startswith(prefix))
            self.assertTrue(description.endswith("."))
            self.assertNotIn("TODO", description)

    def test_missing_version_fails(self):
        path = self.write(DRAFTS_NOTES)
        code, _, err = self.run_cli("--file", path, "drafts", "3.9", "2026-12-01", "--out", self.out)
        self.assertEqual(1, code)
        self.assertIn('no "## 3.9 — …" section', err)


NO_HIGHLIGHTS_NOTES = (
    "# Release notes\n"
    "\n"
    "## 3.9 — 2026-12-15\n"
    "\n"
    "### New features\n"
    "\n"
    "- Adds a dark mode toggle for the map.\n"
    "\n"
    "### Fixes\n"
    "\n"
    "- Starts again on macOS 16.\n"
)


class RcContentFallbackTest(CliTestCase):
    def test_falls_back_to_first_feature_bullet_and_de_todo_without_highlights(self):
        out = tempfile.mkdtemp()
        self.addCleanup(shutil.rmtree, out)
        path = self.write(NO_HIGHLIGHTS_NOTES)
        code, _, _ = self.run_cli("--file", path, "drafts", "3.9", "2026-12-15", "--out", out)
        self.assertEqual(0, code)
        en = self.read(os.path.join(out, "rc-content-en-3-9-index.md"))
        de = self.read(os.path.join(out, "rc-content-de-3-9-index.md"))
        self.assertIn(
            "description: 'What is new in RouteConverter 3.9 (15 Dec 2026): Adds a dark mode toggle for the map.'",
            en)
        self.assertIn("description: 'Neu in RouteConverter 3.9 (15. Dezember 2026): TODO'", de)


class BbcodeTest(unittest.TestCase):
    def test_hard_wrapped_paragraph_joined_into_one_line(self):
        self.assertEqual(
            ["RouteConverter 3.8 is faster and looks fresh.", "", "[b]Most important fixes:[/b]", "",
             "[list]", "[*]Starts again.", "[/list]"],
            release_notes._bbcode(["RouteConverter 3.8 is faster", "and looks fresh.", "",
                                   "Most important fixes:", "", "- Starts again."]))

    def test_paragraph_runs_up_to_a_label_without_blank_line(self):
        self.assertEqual(
            ["Intro line one line two.", "[b]Fixes:[/b]", "[list]", "[*]A", "[*]B", "[/list]"],
            release_notes._bbcode(["Intro line one", "line two.", "Fixes:", "- A", "", "- B"]))

    def test_colon_line_without_bullets_stays_paragraph_text(self):
        self.assertEqual(["Note: nothing else changed. Enjoy:"],
                         release_notes._bbcode(["Note: nothing else changed.", "Enjoy:"]))

    def test_backticks_stripped(self):
        self.assertEqual(["Reads Timeline.json now."], release_notes._bbcode(["Reads `Timeline.json` now."]))


class FirstSentenceTest(unittest.TestCase):
    def test_period(self):
        self.assertEqual("It is faster.", release_notes._first_sentence("It is faster. And more."))

    def test_exclamation_and_question_marks_end_a_sentence(self):
        self.assertEqual("Big news!", release_notes._first_sentence("Big news! More follows."))
        self.assertEqual("Ready?", release_notes._first_sentence("Ready? Here it is."))

    def test_no_period_appended_after_exclamation_or_question_mark(self):
        self.assertEqual("Big news!", release_notes._first_sentence("Big news!"))
        self.assertEqual("Ready?", release_notes._first_sentence("Ready?"))
        self.assertEqual("P: Big news!", release_notes._highlight_description("P: ", "Big news!"))

    def test_text_without_end_gets_period(self):
        self.assertEqual("It is faster.", release_notes._first_sentence("It is faster"))


class HighlightDescriptionTest(unittest.TestCase):
    PREFIX = "What is new in RouteConverter 3.8 (15 Nov 2026): "

    def test_long_sentence_cut_at_first_clause(self):
        sentence = ("RouteConverter 3.8 gets a fresh look on Windows and Linux: a modern light or dark theme "
                    "that follows your system, with sharp text, icons and maps on high-resolution displays.")
        self.assertEqual(self.PREFIX + "RouteConverter 3.8 gets a fresh look on Windows and Linux.",
                         release_notes._highlight_description(self.PREFIX, sentence))

    def test_long_sentence_without_clause_truncated_at_word_boundary(self):
        sentence = "RouteConverter 3.8 " + "makes everything much faster " * 6 + "for your daily work."
        description = release_notes._highlight_description(self.PREFIX, sentence)
        self.assertLessEqual(len(description), 160)
        self.assertTrue(description.endswith("."))
        last_word = description.rstrip(".").split(" ")[-1].lower()
        self.assertNotIn(last_word, release_notes.DANGLING_WORDS)

    def test_possessive_never_ends_a_cut(self):
        self.assertEqual("light or dark theme that follows",
                         release_notes._truncate("light or dark theme that follows your system", 37))


class DescriptionApostropheTest(CliTestCase):
    def test_apostrophe_survives_yaml_quoting(self):
        out = tempfile.mkdtemp()
        self.addCleanup(shutil.rmtree, out)
        notes = DRAFTS_NOTES.replace("RouteConverter 3.8 is faster.", "RouteConverter 3.8 isn't slower.")
        path = self.write(notes)
        code, _, _ = self.run_cli("--file", path, "drafts", "3.8", "2026-12-01", "--out", out)
        self.assertEqual(0, code)
        content = self.read(os.path.join(out, "rc-content-en-3-8-index.md"))
        self.assertIn("isn''t slower.'", content)
        self.assertEqual("What is new in RouteConverter 3.8 (1 Dec 2026): RouteConverter 3.8 isn't slower.",
                         _description(content))


REAL_RELEASE_NOTES = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "RELEASE_NOTES.md")


class RealDraftsTest(CliTestCase):
    def test_3_8_drafts_match_the_posted_forum_threads(self):
        # Threads 4196 (EN) / 4197 (DE) as posted for 3.8: 44 list items in 5 lists each.
        if not os.path.exists(REAL_RELEASE_NOTES):
            self.skipTest("RELEASE_NOTES.md not present")
        out = tempfile.mkdtemp()
        self.addCleanup(shutil.rmtree, out)
        code, _, _ = self.run_cli("--file", REAL_RELEASE_NOTES, "drafts", "3.8", "2026-11-15", "--out", out)
        self.assertEqual(0, code)
        for name in ("forum-3.8-en.txt", "forum-3.8-de.txt"):
            forum = self.read(os.path.join(out, name))
            self.assertEqual(44, forum.count("[*]"), name)
            self.assertEqual(5, forum.count("[list]"), name)
            self.assertEqual(5, forum.count("[/list]"), name)


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
        path = REAL_RELEASE_NOTES
        if not os.path.exists(path):
            self.skipTest("RELEASE_NOTES.md not present")
        with open(path, encoding="utf-8") as f:
            sections = release_notes.parse(f.read())
        self.assertEqual("Next release", sections[0].title)


if __name__ == "__main__":
    unittest.main()
