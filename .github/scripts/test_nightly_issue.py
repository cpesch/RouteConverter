import os
import sys
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import nightly_issue as ni  # noqa: E402
from nightly_model import Finding, TestFailure  # noqa: E402


def make_finding(
    klass="deterministic",
    sig="sig1",
    jobs=("Java 21",),
    tests=(),
    module=None,
    goal=None,
    error_line=None,
    excerpt="",
):
    return Finding(
        klass=klass,
        signature=sig,
        jobs=jobs,
        tests=tests,
        module=module,
        goal=goal,
        error_line=error_line,
        excerpt=excerpt,
    )


def make_ctx(**overrides):
    ctx = {
        "run_url": "https://example.com/runs/1",
        "last_green": {"sha": "aaa", "url": "https://example.com/commits/aaa"},
        "first_red": {"sha": "bbb", "url": "https://example.com/commits/bbb"},
        "commits": ["bbb fix thing", "ccc another"],
        "env_diff": {"JAVA_VERSION": ["21", "25"]},
        "repro": "mvn -pl common test",
        "seed": None,
        "state": {"sig": "sig1"},
    }
    ctx.update(overrides)
    return ctx


class StateTest(unittest.TestCase):
    def test_parse_write_round_trip(self):
        state = {"b": 2, "a": 1}
        body = ni.write_state("", state)
        self.assertEqual(ni.parse_state(body), state)

    def test_write_state_appends_on_own_line_when_absent(self):
        body = ni.write_state("hello world", {"x": 1})
        self.assertTrue(body.startswith("hello world\n"))
        self.assertEqual(ni.parse_state(body), {"x": 1})

    def test_write_state_replaces_existing_marker(self):
        body = ni.write_state('before\n<!-- nightly-state {"old": 1} -->\nafter', {"new": 2})
        self.assertIn("before", body)
        self.assertIn("after", body)
        self.assertNotIn('"old"', body)
        self.assertIn('"new":2', body)
        self.assertEqual(ni.parse_state(body), {"new": 2})

    def test_parse_write_round_trip_nested_state(self):
        state = {"a": {"b": 1}, "c": [1, {"d": 2}]}
        body = ni.write_state("", state)
        self.assertEqual(ni.parse_state(body), state)

    def test_write_state_replaces_nested_marker_without_dangling_brace(self):
        body = ni.write_state(
            'before\n<!-- nightly-state {"old": {"x": 1}} -->\nafter',
            {"new": {"y": 2}},
        )
        self.assertIn("before\n", body)
        self.assertIn("\nafter", body)
        self.assertNotIn('"old"', body)
        self.assertEqual(ni.parse_state(body), {"new": {"y": 2}})

    def test_invalid_json_gives_none(self):
        self.assertIsNone(ni.parse_state("<!-- nightly-state {not valid json} -->"))
        self.assertIsNone(ni.parse_state("no marker at all"))


class ResolveFilesTest(unittest.TestCase):
    def test_maps_test_under_test_and_frame_in_order_dropping_unknown(self):
        ls_files = [
            "src/test/java/slash/nav/AlphaTest.java",
            "src/test/java/slash/nav/BetaTests.java",
            "src/main/java/slash/nav/Alpha.java",
            "src/main/java/slash/nav/Beta.java",
            "src/main/java/slash/nav/Helper.java",
        ]
        finding = make_finding(
            tests=(
                TestFailure(
                    test_class="slash.nav.AlphaTest",
                    method="m",
                    exception="E",
                    frames=("slash.nav.Helper", "slash.nav.Dangling"),
                ),
                TestFailure(test_class="slash.nav.BetaTests", method="m", exception="E", frames=()),
                TestFailure(test_class="slash.nav.GammaTest", method="m", exception="E", frames=()),
            ),
        )
        self.assertEqual(
            ni.resolve_files(finding, ls_files),
            [
                "src/test/java/slash/nav/AlphaTest.java",
                "src/test/java/slash/nav/BetaTests.java",
                "src/main/java/slash/nav/Alpha.java",
                "src/main/java/slash/nav/Beta.java",
                "src/main/java/slash/nav/Helper.java",
            ],
        )

    def test_caps_at_max_files(self):
        ls_files = ["src/main/java/slash/nav/Frame%02d.java" % i for i in range(12)]
        frames = tuple("slash.nav.Frame%02d" % i for i in range(12))
        finding = make_finding(
            tests=(TestFailure(test_class="slash.nav.MissingTest", method="m", exception="E", frames=frames),),
        )
        result = ni.resolve_files(finding, ls_files)
        self.assertEqual(len(result), 10)
        self.assertEqual(result, ["src/main/java/slash/nav/Frame%02d.java" % i for i in range(10)])

    def test_compile_error_with_windows_style_path(self):
        finding = make_finding(
            tests=(),
            module="common",
            goal="compile",
            excerpt=(
                r"[ERROR] D:\a\RouteConverter\RouteConverter\common\src\main\java\slash\nav\Foo.java:[12,34] "
                r"error: cannot find symbol"
            ),
        )
        ls_files = ["common/src/main/java/slash/nav/Foo.java"]
        self.assertEqual(ni.resolve_files(finding, ls_files), ["common/src/main/java/slash/nav/Foo.java"])

    def test_empty_result_is_allowed(self):
        finding = make_finding(tests=(TestFailure("slash.nav.UnknownTest", "m", "E", ()),))
        self.assertEqual(ni.resolve_files(finding, []), [])


class RenderTitleTest(unittest.TestCase):
    def test_title_for_test_finding(self):
        finding = make_finding(
            sig="abc123def456",
            tests=(TestFailure("slash.nav.converter.ConverterTest", "m", "E", ()),),
        )
        self.assertEqual(ni.render_title(finding), "nightly: ConverterTest [abc123def456]")

    def test_title_for_build_finding(self):
        finding = make_finding(tests=(), module="common", goal="compile", sig="feedface")
        self.assertEqual(ni.render_title(finding), "nightly: common:compile [feedface]")

    def test_title_without_module_and_goal_never_prints_none(self):
        finding = make_finding(tests=(), module=None, goal=None, jobs=("Drift snapshot",), sig="18bb70b72028")
        title = ni.render_title(finding)
        self.assertNotIn("None", title)
        self.assertEqual(title, "nightly: build failure in Drift snapshot [18bb70b72028]")

    def test_title_with_module_only(self):
        finding = make_finding(tests=(), module="rest", goal=None, sig="aa")
        self.assertEqual(ni.render_title(finding), "nightly: rest [aa]")

    def test_title_capped_at_120_characters(self):
        finding = make_finding(
            sig="s",
            tests=(TestFailure("slash.nav." + "X" * 140 + "Test", "m", "E", ()),),
        )
        self.assertEqual(len(ni.render_title(finding)), 120)


class RenderBodyTest(unittest.TestCase):
    def test_files_section_appears_exactly_once_with_marker(self):
        finding = make_finding(tests=(TestFailure("slash.nav.FooTest", "m", "E", ()),))
        body = ni.render_body(finding, [], make_ctx(commits=[], env_diff={}, seed=None))
        self.assertEqual(body.count("### Files"), 1)
        self.assertIn("\n### Files\n", body)
        self.assertIn(ni.FILER_MARKER, body)
        self.assertIn("**No commits in between — environment drift.**", body)
        self.assertIn("_none resolved — not eligible for auto-approval_", body)

    def test_files_bullet_list(self):
        finding = make_finding(tests=(TestFailure("slash.nav.FooTest", "m", "E", ()),))
        body = ni.render_body(finding, ["src/test/java/slash/nav/FooTest.java"], make_ctx())
        self.assertIn("- `src/test/java/slash/nav/FooTest.java`", body)
        self.assertNotIn("_none resolved — not eligible for auto-approval_", body)

    def test_excerpt_is_fenced_and_backticks_are_sanitised(self):
        finding = make_finding(excerpt="ERROR: ```boom```")
        body = ni.render_body(finding, [], make_ctx())
        self.assertIn("```text\nERROR: '''boom'''\n```", body)
        self.assertNotIn("```boom```", body)

    def test_env_diff_table_and_seed(self):
        finding = make_finding(tests=(TestFailure("slash.nav.FooTest", "m", "E", ()),))
        body = ni.render_body(
            finding,
            [],
            make_ctx(env_diff={"A": ["1", "2"]}, seed="1234", commits=[]),
        )
        self.assertIn("| A | 1 | 2 |", body)
        self.assertIn("Seed: 1234", body)


class CommentHelpersTest(unittest.TestCase):
    def test_resolved_comment_moot_pr_wording(self):
        self.assertEqual(
            ni.resolved_comment("https://example.com/runs/1", 42),
            "Resolved by https://example.com/runs/1; PR #42 may be moot.",
        )

    def test_resolved_comment_without_pr(self):
        self.assertEqual(ni.resolved_comment("https://example.com/runs/1"), "Resolved by https://example.com/runs/1.")

    def test_still_failing_comment(self):
        self.assertEqual(ni.still_failing_comment(3, "https://example.com/runs/3"), "Still failing on night 3. [Run](https://example.com/runs/3)")

    def test_ledger_body_carries_state(self):
        body = ni.ledger_body("flaky", {"a": 1})
        self.assertTrue(body.startswith("Tracks flaky-test occurrences"))
        self.assertEqual(ni.parse_state(body), {"a": 1})


if __name__ == "__main__":
    unittest.main()
