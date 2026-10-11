import os
import sys
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import nightly_drift as nd  # noqa: E402


class ParseTest(unittest.TestCase):
    def test_valid_flat_object(self):
        text = '{"java.Java 21": "21.0.11+10", "image.os": "ubuntu24"}'
        self.assertEqual(nd.parse(text), {"java.Java 21": "21.0.11+10", "image.os": "ubuntu24"})

    def test_invalid_json_gives_empty(self):
        self.assertEqual(nd.parse("not json"), {})

    def test_non_object_gives_empty(self):
        self.assertEqual(nd.parse("[1, 2, 3]"), {})
        self.assertEqual(nd.parse('"just a string"'), {})

    def test_non_string_entries_give_empty(self):
        self.assertEqual(nd.parse('{"a": 1, "b": "ok"}'), {})
        self.assertEqual(nd.parse('{"a": null, "b": "ok"}'), {})
        self.assertEqual(nd.parse('{"a": {"b": "c"}}'), {})


class DiffTest(unittest.TestCase):
    def test_added_removed_and_changed_keys(self):
        old = {"changed": "1", "removed": "2", "same": "x"}
        new = {"changed": "2", "added": "3", "same": "x"}
        self.assertEqual(
            nd.diff(old, new),
            {
                "added": [None, "3"],
                "changed": ["1", "2"],
                "removed": ["2", None],
            },
        )

    def test_result_is_sorted_by_key(self):
        old = {"z": "1", "a": "1"}
        new = {"m": "1", "a": "1"}
        self.assertEqual(list(nd.diff(old, new)), ["m", "z"])

    def test_unchanged_keys_are_omitted(self):
        self.assertEqual(nd.diff({"a": "1", "b": "2"}, {"a": "1", "b": "2"}), {})


class DriftCommentTest(unittest.TestCase):
    def test_empty_diff_gives_empty_comment(self):
        self.assertEqual(nd.drift_comment({}, "https://example.com/runs/1"), "")

    def test_comment_structure(self):
        comment = nd.drift_comment({"k": ["old", "new"]}, "https://example.com/runs/1")
        self.assertEqual(
            comment,
            "Environment changed in https://example.com/runs/1\n"
            "\n"
            "| Key | Old | New |\n"
            "| --- | --- | --- |\n"
            "| k | old | new |",
        )

    def test_missing_side_renders_as_empty_cell(self):
        comment = nd.drift_comment({"added": [None, "3"], "removed": ["2", None]}, "https://example.com/runs/1")
        self.assertIn("| added |  | 3 |", comment)
        self.assertIn("| removed | 2 |  |", comment)

    def test_pipe_in_values_is_escaped(self):
        comment = nd.drift_comment({"key": ["a|b", "c|d"]}, "https://example.com/runs/1")
        self.assertIn("| key | a\\|b | c\\|d |", comment)


if __name__ == "__main__":
    unittest.main()
