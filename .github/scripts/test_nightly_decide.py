import os
import sys
import unittest
from datetime import date

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import nightly_decide as nd  # noqa: E402
from nightly_model import Finding, TestFailure  # noqa: E402

TODAY = date(2026, 10, 11)


def make_finding(klass="deterministic", sig="sig1", jobs=("Java 21",), tests=()):
    return Finding(
        klass=klass,
        signature=sig,
        jobs=jobs,
        tests=tests,
        module=None,
        goal=None,
        error_line=None,
        excerpt="",
    )


def make_issue(number=1, labels=(), state=None, open_pr=None, attempt_pr_state=None):
    return nd.Issue(
        number=number,
        labels=frozenset(labels),
        state=dict(state or {}),
        open_pr=open_pr,
        attempt_pr_state=attempt_pr_state,
    )


def make_settings(**overrides):
    defaults = {"autoapprove": True}
    defaults.update(overrides)
    return nd.Settings(**defaults)


def make_ctx(**overrides):
    defaults = {
        "today": TODAY,
        "green_jobs": frozenset(),
        "files_by_sig": {},
        "trunk_overlap_sigs": frozenset(),
        "flaky_ledger": {},
        "infra_streak": 0,
    }
    defaults.update(overrides)
    return nd.Context(**defaults)


def by_kind(actions, kind):
    return [a for a in actions if a.kind == kind]


class NewDeterministicTest(unittest.TestCase):
    def test_new_finding_is_approved(self):
        finding = make_finding(sig="sig1")
        ctx = make_ctx(files_by_sig={"sig1": ["a.java"]})
        actions = nd.plan([finding], [], ctx, make_settings())
        self.assertEqual(len(actions), 1)
        self.assertEqual(actions[0].kind, "open_issue")
        self.assertIn("agent:approved", actions[0].data["labels"])
        self.assertEqual(actions[0].data["state"]["attempts"], [{"date": "2026-10-11", "result": "pending"}])

    def test_no_files_means_needs_triage(self):
        finding = make_finding(sig="sig1")
        ctx = make_ctx(files_by_sig={})
        actions = nd.plan([finding], [], ctx, make_settings())
        self.assertEqual(len(actions), 1)
        self.assertNotIn("agent:approved", actions[0].data["labels"])
        self.assertIn("needs-triage", actions[0].data["labels"])

    def test_autoapprove_disabled_means_needs_triage(self):
        finding = make_finding(sig="sig1")
        ctx = make_ctx(files_by_sig={"sig1": ["a.java"]})
        actions = nd.plan([finding], [], ctx, make_settings(autoapprove=False))
        self.assertEqual(len(actions), 1)
        self.assertNotIn("agent:approved", actions[0].data["labels"])

    def test_trunk_overlap_means_needs_triage_and_comment(self):
        finding = make_finding(sig="sig1")
        ctx = make_ctx(files_by_sig={"sig1": ["a.java"]}, trunk_overlap_sigs=frozenset({"sig1"}))
        actions = nd.plan([finding], [], ctx, make_settings())
        self.assertEqual(len(actions), 2)
        self.assertEqual(actions[0].kind, "open_issue")
        self.assertNotIn("agent:approved", actions[0].data["labels"])
        self.assertEqual(actions[1].kind, "comment")
        self.assertEqual(actions[1].data["reason"], "trunk-overlap")

    def test_open_approved_cap(self):
        existing = make_issue(
            number=1,
            labels={"agent:approved", "nightly:deterministic"},
            state={"sig": "other", "jobs": ["Java 21"], "tests": [], "nights": ["2026-10-01"], "escalated": False},
        )
        findings = [make_finding(sig=s) for s in ("sig2", "sig3", "sig4")]
        ctx = make_ctx(files_by_sig={"sig2": ["a"], "sig3": ["b"], "sig4": ["c"]})
        actions = nd.plan(findings, [existing], ctx, make_settings(max_open_approved=3))
        opens = by_kind(actions, "open_issue")
        by_sig = {a.data["finding"].signature: a for a in opens}
        self.assertIn("agent:approved", by_sig["sig2"].data["labels"])
        self.assertIn("agent:approved", by_sig["sig3"].data["labels"])
        self.assertNotIn("agent:approved", by_sig["sig4"].data["labels"])


class MatchDedupeTest(unittest.TestCase):
    def test_same_signature_night_two_is_a_comment_not_an_issue(self):
        issue = make_issue(
            number=7,
            labels={"needs-triage"},
            state={
                "sig": "sig1",
                "jobs": ["Java 21"],
                "tests": [],
                "nights": ["2026-10-09", "2026-10-10"],
                "escalated": True,
                "attempts": [{"date": "2026-10-09", "result": "failed"}, {"date": "2026-10-10", "result": "failed"}],
            },
        )
        finding = make_finding(sig="sig1")
        ctx = make_ctx()
        actions = nd.plan([finding], [issue], ctx, make_settings())
        self.assertEqual({a.kind for a in actions}, {"comment", "update_state"})
        self.assertFalse(by_kind(actions, "open_issue"))
        comment = by_kind(actions, "comment")[0]
        self.assertEqual(comment.data["night"], 3)

    def test_two_findings_matching_same_issue_fold_into_one_update(self):
        issue = make_issue(
            number=9,
            labels={"needs-triage"},
            state={"sig": "oldsig", "jobs": ["Java 21"], "tests": ["pkg.FooTest"], "nights": ["2026-10-10"], "escalated": False},
        )
        finding_a = make_finding(
            sig="newsig1",
            jobs=("Java 21",),
            tests=(TestFailure(test_class="pkg.FooTest", method="testX", exception="E", frames=()),),
        )
        finding_b = make_finding(
            sig="newsig2",
            jobs=("Java 25",),
            tests=(TestFailure(test_class="pkg.FooTest", method="testY", exception="E", frames=()),),
        )
        ctx = make_ctx()
        actions = nd.plan([finding_a, finding_b], [issue], ctx, make_settings())

        comments = by_kind(actions, "comment")
        state_updates = by_kind(actions, "update_state")
        self.assertEqual(len(comments), 1)
        self.assertEqual(comments[0].data["night"], 2)
        self.assertEqual(len(state_updates), 1)
        self.assertEqual(set(state_updates[0].data["jobs"]), {"Java 21", "Java 25"})
        self.assertEqual(state_updates[0].data["nights"], ["2026-10-10", "2026-10-11"])
        self.assertFalse(by_kind(actions, "open_issue"))

    def test_test_class_overlap_with_different_signature_is_a_comment(self):
        issue = make_issue(
            number=9,
            labels={"needs-triage"},
            state={"sig": "oldsig", "jobs": ["Java 21"], "tests": ["pkg.FooTest"], "nights": ["2026-10-10"], "escalated": False},
        )
        finding = make_finding(
            sig="newsig",
            tests=(TestFailure(test_class="pkg.FooTest", method="testX", exception="E", frames=()),),
        )
        ctx = make_ctx()
        actions = nd.plan([finding], [issue], ctx, make_settings())
        self.assertEqual({a.kind for a in actions}, {"comment", "update_state"})
        self.assertFalse(by_kind(actions, "open_issue"))


class FlakyTest(unittest.TestCase):
    def test_threshold_reached_within_window_opens_issue(self):
        finding = make_finding(klass="flaky", sig="flakysig")
        ctx = make_ctx(flaky_ledger={"flakysig": ["2026-10-05"]})
        actions = nd.plan([finding], [], ctx, make_settings())
        self.assertTrue(by_kind(actions, "open_issue"))
        self.assertTrue(by_kind(actions, "update_ledger"))

    def test_spread_over_window_does_not_open_issue(self):
        finding = make_finding(klass="flaky", sig="flakysig")
        ctx = make_ctx(flaky_ledger={"flakysig": ["2026-10-04"]})
        actions = nd.plan([finding], [], ctx, make_settings())
        self.assertFalse(by_kind(actions, "open_issue"))
        self.assertTrue(by_kind(actions, "update_ledger"))


class InfraTest(unittest.TestCase):
    def test_streak_reaches_threshold_opens_issue(self):
        finding = make_finding(klass="infra", sig="infrasig")
        ctx = make_ctx(infra_streak=2)
        actions = nd.plan([finding], [], ctx, make_settings())
        self.assertTrue(by_kind(actions, "open_issue"))
        self.assertTrue(by_kind(actions, "ntfy"))

    def test_streak_below_threshold_does_not_open_issue(self):
        finding = make_finding(klass="infra", sig="infrasig")
        ctx = make_ctx(infra_streak=1)
        actions = nd.plan([finding], [], ctx, make_settings())
        self.assertFalse(by_kind(actions, "open_issue"))
        self.assertTrue(by_kind(actions, "ntfy"))


class ForwardCompatTest(unittest.TestCase):
    def test_new_forward_compat_is_never_approved(self):
        finding = make_finding(klass="forward_compat", sig="fcsig")
        ctx = make_ctx(files_by_sig={"fcsig": ["a.java"]})
        actions = nd.plan([finding], [], ctx, make_settings())
        self.assertEqual(len(actions), 1)
        self.assertNotIn("agent:approved", actions[0].data["labels"])
        self.assertIn("needs-triage", actions[0].data["labels"])
        self.assertIn("nightly:forward-compat", actions[0].data["labels"])


class EscalationLifecycleTest(unittest.TestCase):
    def test_builder_abort_swap_counts_as_failed_and_refires(self):
        issue = make_issue(
            number=3,
            labels={"needs-triage"},
            state={
                "sig": "detsig",
                "jobs": ["Java 21"],
                "tests": [],
                "nights": ["2026-10-09"],
                "escalated": False,
                "attempts": [{"date": "2026-10-09", "result": "pending"}],
            },
        )
        finding = make_finding(sig="detsig")
        ctx = make_ctx(today=date(2026, 10, 11), files_by_sig={"detsig": ["a.java"]})
        actions = nd.plan([finding], [issue], ctx, make_settings())

        attempt_updates = [a for a in by_kind(actions, "update_state") if "attempts" in a.data]
        self.assertEqual(attempt_updates[0].data["attempts"][-1]["result"], "failed")
        self.assertEqual(attempt_updates[-1].data["attempts"][-1], {"date": "2026-10-11", "result": "pending"})
        self.assertTrue(by_kind(actions, "add_label"))
        self.assertFalse(by_kind(actions, "ntfy"))

    def test_second_failure_escalates_exactly_once(self):
        issue = make_issue(
            number=3,
            labels={"agent:approved"},
            state={
                "sig": "detsig",
                "jobs": ["Java 21"],
                "tests": [],
                "nights": ["2026-10-09", "2026-10-11"],
                "escalated": False,
                "attempts": [
                    {"date": "2026-10-09", "result": "failed"},
                    {"date": "2026-10-11", "result": "pending"},
                ],
            },
            attempt_pr_state="closed",
        )
        finding = make_finding(sig="detsig")
        ctx = make_ctx(today=date(2026, 10, 13), files_by_sig={"detsig": ["a.java"]})
        actions = nd.plan([finding], [issue], ctx, make_settings())

        self.assertEqual(len(by_kind(actions, "ntfy")), 1)
        remove_actions = by_kind(actions, "remove_label")
        add_actions = by_kind(actions, "add_label")
        self.assertEqual([a.data["label"] for a in remove_actions], ["agent:approved"])
        self.assertEqual([a.data["label"] for a in add_actions], ["needs-triage"])
        escalated_updates = [a for a in by_kind(actions, "update_state") if a.data.get("escalated") is True]
        self.assertEqual(len(escalated_updates), 1)

    def test_third_night_after_escalation_has_no_label_changes(self):
        issue = make_issue(
            number=3,
            labels={"needs-triage"},
            state={
                "sig": "detsig",
                "jobs": ["Java 21"],
                "tests": [],
                "nights": ["2026-10-09", "2026-10-11", "2026-10-13"],
                "escalated": True,
                "attempts": [
                    {"date": "2026-10-09", "result": "failed"},
                    {"date": "2026-10-11", "result": "failed"},
                ],
            },
        )
        finding = make_finding(sig="detsig")
        ctx = make_ctx(today=date(2026, 10, 14), files_by_sig={"detsig": ["a.java"]})
        actions = nd.plan([finding], [issue], ctx, make_settings())

        self.assertFalse(by_kind(actions, "add_label"))
        self.assertFalse(by_kind(actions, "remove_label"))
        self.assertFalse(by_kind(actions, "ntfy"))
        self.assertEqual({a.kind for a in actions}, {"comment", "update_state"})


class RefireGuardsTest(unittest.TestCase):
    def test_no_second_refire_on_the_same_day(self):
        issue = make_issue(
            number=4,
            labels={"agent:approved"},
            state={
                "sig": "detsig",
                "jobs": ["Java 21"],
                "tests": [],
                "nights": ["2026-10-11"],
                "escalated": False,
                "attempts": [{"date": "2026-10-11", "result": "pending"}],
            },
        )
        finding = make_finding(sig="detsig")
        ctx = make_ctx(today=date(2026, 10, 11), files_by_sig={"detsig": ["a.java"]})
        actions = nd.plan([finding], [issue], ctx, make_settings())
        self.assertFalse(by_kind(actions, "add_label"))
        self.assertFalse(by_kind(actions, "remove_label"))

    def test_no_refire_while_pr_is_open(self):
        issue = make_issue(
            number=4,
            labels={"agent:approved"},
            state={
                "sig": "detsig",
                "jobs": ["Java 21"],
                "tests": [],
                "nights": ["2026-10-09"],
                "escalated": False,
                "attempts": [{"date": "2026-10-09", "result": "pending"}],
            },
            open_pr=42,
        )
        finding = make_finding(sig="detsig")
        ctx = make_ctx(today=date(2026, 10, 11), files_by_sig={"detsig": ["a.java"]})
        actions = nd.plan([finding], [issue], ctx, make_settings())
        self.assertFalse(by_kind(actions, "add_label"))
        self.assertFalse(by_kind(actions, "remove_label"))


class MergedPrTest(unittest.TestCase):
    def test_merged_pr_with_still_red_nightly_is_a_failed_attempt(self):
        issue = make_issue(
            number=6,
            labels={"agent:approved"},
            state={
                "sig": "detsig",
                "jobs": ["Java 21"],
                "tests": [],
                "nights": ["2026-10-09"],
                "escalated": False,
                "attempts": [{"date": "2026-10-09", "result": "pending"}],
            },
            attempt_pr_state="merged",
        )
        finding = make_finding(sig="detsig")
        ctx = make_ctx(today=date(2026, 10, 11), files_by_sig={})
        actions = nd.plan([finding], [issue], ctx, make_settings())
        attempt_updates = [a for a in by_kind(actions, "update_state") if "attempts" in a.data]
        self.assertEqual(len(attempt_updates), 1)
        self.assertEqual(attempt_updates[0].data["attempts"], [{"date": "2026-10-09", "result": "failed"}])
        self.assertFalse(by_kind(actions, "add_label"))


class CloseTest(unittest.TestCase):
    def test_close_when_all_jobs_green(self):
        issue = make_issue(
            number=8,
            labels={"nightly:deterministic"},
            state={"sig": "detsig", "jobs": ["Java 21"], "tests": [], "nights": ["2026-10-09"], "escalated": False},
        )
        ctx = make_ctx(green_jobs=frozenset({"Java 21"}))
        actions = nd.plan([], [issue], ctx, make_settings())
        self.assertEqual(len(actions), 1)
        self.assertEqual(actions[0].kind, "close")

    def test_comment_instead_of_close_when_pr_open(self):
        issue = make_issue(
            number=8,
            labels={"nightly:deterministic"},
            state={"sig": "detsig", "jobs": ["Java 21"], "tests": [], "nights": ["2026-10-09"], "escalated": False},
            open_pr=11,
        )
        ctx = make_ctx(green_jobs=frozenset({"Java 21"}))
        actions = nd.plan([], [issue], ctx, make_settings())
        self.assertEqual(len(actions), 1)
        self.assertEqual(actions[0].kind, "comment")
        self.assertEqual(actions[0].data["reason"], "resolved-pr-open")
        self.assertEqual(actions[0].data["pr"], 11)

    def test_no_close_when_a_job_is_not_green(self):
        issue = make_issue(
            number=8,
            labels={"nightly:deterministic"},
            state={
                "sig": "detsig",
                "jobs": ["Java 21", "Java 21 on Windows"],
                "tests": [],
                "nights": ["2026-10-09"],
                "escalated": False,
            },
        )
        ctx = make_ctx(green_jobs=frozenset({"Java 21"}))
        actions = nd.plan([], [issue], ctx, make_settings())
        self.assertEqual(actions, [])


class OrderingTest(unittest.TestCase):
    def test_output_ordering_is_independent_of_input_order(self):
        issue_low = make_issue(
            number=2,
            labels={"needs-triage"},
            state={"sig": "sig_low", "jobs": ["Java 21"], "tests": [], "nights": ["2026-10-09"], "escalated": False},
        )
        issue_high = make_issue(
            number=5,
            labels={"needs-triage"},
            state={"sig": "sig_high", "jobs": ["Java 21"], "tests": [], "nights": ["2026-10-09"], "escalated": False},
        )
        finding_low = make_finding(sig="sig_low")
        finding_high = make_finding(sig="sig_high")
        finding_new_a = make_finding(sig="aaa_new")
        finding_new_z = make_finding(sig="zzz_new")
        findings = [finding_high, finding_new_z, finding_low, finding_new_a]
        issues = [issue_high, issue_low]
        ctx = make_ctx()
        settings = make_settings()

        forward = nd.plan(findings, issues, ctx, settings)
        backward = nd.plan(list(reversed(findings)), list(reversed(issues)), ctx, settings)

        self.assertEqual(forward, backward)

        def owner(action):
            if action.issue is not None:
                return (0, action.issue)
            return (1, action.data["finding"].signature)

        owners = [owner(a) for a in forward if a.kind in ("comment", "update_state", "open_issue")]
        self.assertEqual(owners, sorted(owners))


if __name__ == "__main__":
    unittest.main()
