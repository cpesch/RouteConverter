import contextlib
import io
import json
import os
import sys
import tempfile
import unittest
import urllib.error
import urllib.request
import zipfile
from unittest import mock

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import nightly_issue  # noqa: E402
import nightly_triage  # noqa: E402

REPO = "owner/name"
RUN_ID = 123
RUN_PATH = f"/repos/{REPO}/actions/runs/{RUN_ID}"
ISSUES_PATH = f"/repos/{REPO}/issues?labels=from:nightly&state=open"
PULLS_PATH = f"/repos/{REPO}/pulls?state=all&sort=updated&direction=desc&per_page=100"
RUNS_PATH = f"/repos/{REPO}/actions/workflows/77/runs?per_page=100"
REPO_PATH = f"/repos/{REPO}"

LOG = b"[ERROR] Tests run: 1, Failures: 1"


def run(**overrides):
    data = {
        "id": RUN_ID,
        "run_attempt": 1,
        "event": "schedule",
        "path": ".github/workflows/build.yml",
        "workflow_id": 77,
        "head_branch": "master",
        "head_sha": "bbb",
        "html_url": f"https://github.com/{REPO}/actions/runs/{RUN_ID}",
        "conclusion": "failure",
        "status": "completed",
        "run_started_at": "2026-10-11T03:00:00Z",
        "created_at": "2026-10-11T03:00:00Z",
    }
    data.update(overrides)
    return data


def job(name, conclusion, job_id, failed=False, failed_step="Build with Maven"):
    if failed:
        steps = [
            {"name": "Set up JDK", "conclusion": "success"},
            {"name": failed_step, "conclusion": "failure"},
        ]
    elif conclusion == "success":
        steps = [{"name": failed_step, "conclusion": "success"}]
    else:
        steps = []
    return {"id": job_id, "name": name, "conclusion": conclusion, "steps": steps}


def jobs_response(*jobs):
    return {"total_count": len(jobs), "jobs": list(jobs)}


def issue(number, title, state, labels):
    return {
        "number": number,
        "title": title,
        "body": nightly_issue.write_state("", state),
        "labels": [{"name": label} for label in labels],
    }


def surefire_xml(classname, method):
    return (
        '<?xml version="1.0" encoding="UTF-8"?>\n'
        f'<testsuite><testcase classname="{classname}" name="{method}" time="0.1">'
        f'<failure message="boom" type="java.lang.AssertionError"></failure>'
        "</testcase></testsuite>"
    )


def surefire_zip(classname, method):
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, "w") as zf:
        zf.writestr(f"TEST-{classname}.xml", surefire_xml(classname, method))
    return buf.getvalue()


def drift_zip(mapping, member="drift.json"):
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, "w") as zf:
        zf.writestr(member, json.dumps(mapping))
    return buf.getvalue()


class FakeApi:
    """Records calls and serves canned responses; ``queues`` is a per-path FIFO."""

    def __init__(self, routes, queues=None):
        self.routes = routes
        self.queues = queues or {}
        self.calls = []

    def request(self, method, path, body=None, raw=False):
        self.calls.append((method, path, body))
        key = (method, path)
        if key in self.queues:
            queue = self.queues[key]
            if not queue:
                raise AssertionError(f"queue exhausted for {method} {path}")
            return queue.pop(0)
        if key not in self.routes:
            raise AssertionError(f"unexpected API call: {method} {path}")
        return self.routes[key]

    def paginate(self, path):
        self.calls.append(("GET", path, None))
        key = ("GET", path)
        if key not in self.routes:
            raise AssertionError(f"unexpected paginate call: {path}")
        return list(self.routes[key])


def run_triage(api, repo=REPO, run_id=RUN_ID, dry_run=False, settings=None, env=None, ls_files=()):
    out, err = io.StringIO(), io.StringIO()
    with mock.patch.object(nightly_triage, "_git_ls_files", return_value=list(ls_files)):
        with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
            code = nightly_triage.triage(api, repo, run_id, dry_run=dry_run, settings=settings, env=env)
    return code, out.getvalue(), err.getvalue()


def create_issue_call(api, path=f"/repos/{REPO}/issues"):
    return next(c for c in api.calls if c[0] == "POST" and c[1] == path)


class KillSwitchTest(unittest.TestCase):
    def test_disabled_exits_before_any_call(self):
        api = FakeApi({})
        code, out, _ = run_triage(api, env={"NIGHTLY_TRIAGE": "0"})
        self.assertEqual(code, 0)
        self.assertEqual(api.calls, [])
        self.assertIn("nightly-triage disabled", out)


class RunFetchTest(unittest.TestCase):
    def test_manual_rerun_gives_no_action(self):
        api = FakeApi({("GET", RUN_PATH): run(run_attempt=2)})
        code, _, _ = run_triage(api)
        self.assertEqual(code, 0)
        self.assertEqual([(m, p) for m, p, _ in api.calls], [("GET", RUN_PATH)])


class GreenCloseTest(unittest.TestCase):
    def test_green_run_closes_open_deterministic_issue(self):
        state = {"sig": "abc123def456", "jobs": ["Java 21"], "tests": [], "nights": ["2026-10-09"], "escalated": False}
        api = FakeApi(
            {
                ("GET", RUN_PATH): run(conclusion="success"),
                ("GET", f"{RUN_PATH}/attempts/1/jobs"): jobs_response(job("Java 21", "success", 1)),
                ("GET", f"{RUN_PATH}/artifacts"): {"artifacts": []},
                ("GET", ISSUES_PATH): [
                    issue(5, "nightly: FooTest [abc123def456]", state, ["from:nightly", "nightly:deterministic"])
                ],
                ("GET", PULLS_PATH): [],
                ("GET", RUNS_PATH): {"workflow_runs": []},
                ("GET", REPO_PATH): {"default_branch": "master"},
                ("POST", f"/repos/{REPO}/issues/5/comments"): {},
                ("PATCH", f"/repos/{REPO}/issues/5"): {},
            }
        )
        code, _, _ = run_triage(api)
        self.assertEqual(code, 0)
        methods = [(m, p) for m, p, _ in api.calls]
        self.assertIn(("POST", f"/repos/{REPO}/issues/5/comments"), methods)
        self.assertIn(("PATCH", f"/repos/{REPO}/issues/5"), methods)


class FlakyRerunTest(unittest.TestCase):
    def test_fail_then_green_updates_ledger_without_opening_issue(self):
        api = FakeApi(
            {
                ("GET", f"{RUN_PATH}/attempts/1/jobs"): jobs_response(job("Java 21", "failure", 1, failed=True)),
                ("GET", f"/repos/{REPO}/actions/jobs/1/logs"): LOG,
                ("GET", f"{RUN_PATH}/artifacts"): {"artifacts": []},
                ("POST", f"{RUN_PATH}/rerun-failed-jobs"): {},
                ("GET", f"{RUN_PATH}/attempts/2/jobs"): jobs_response(job("Java 21", "success", 1)),
                ("GET", ISSUES_PATH): [],
                ("GET", PULLS_PATH): [],
                ("GET", RUNS_PATH): {"workflow_runs": []},
                ("GET", REPO_PATH): {"default_branch": "master"},
                ("POST", f"/repos/{REPO}/issues"): {"number": 99},
            },
            queues={("GET", RUN_PATH): [run(), run(run_attempt=2)]},
        )
        code, _, _ = run_triage(api)
        self.assertEqual(code, 0)
        creates = [c for c in api.calls if c[0] == "POST" and c[1] == f"/repos/{REPO}/issues"]
        self.assertEqual(len(creates), 1)
        self.assertEqual(creates[0][2]["title"], "nightly: flaky ledger")


class DeterministicOpenTest(unittest.TestCase):
    def test_fail_then_fail_opens_issue_and_adds_approval_separately(self):
        zipped = surefire_zip("slash.nav.FooTest", "testFoo")
        api = FakeApi(
            {
                ("GET", f"{RUN_PATH}/attempts/1/jobs"): jobs_response(job("Java 21", "failure", 1, failed=True)),
                ("GET", f"/repos/{REPO}/actions/jobs/1/logs"): LOG,
                ("GET", f"{RUN_PATH}/artifacts"): {"artifacts": [{"id": 9, "name": "surefire-Java-21"}]},
                ("GET", f"/repos/{REPO}/actions/artifacts/9/zip"): zipped,
                ("POST", f"{RUN_PATH}/rerun-failed-jobs"): {},
                ("GET", f"{RUN_PATH}/attempts/2/jobs"): jobs_response(job("Java 21", "failure", 1, failed=True)),
                ("GET", ISSUES_PATH): [],
                ("GET", PULLS_PATH): [],
                ("GET", RUNS_PATH): {"workflow_runs": []},
                ("GET", REPO_PATH): {"default_branch": "master"},
                ("POST", f"/repos/{REPO}/issues"): {"number": 42},
                ("POST", f"/repos/{REPO}/issues/42/labels"): [],
            },
            queues={("GET", RUN_PATH): [run(), run(run_attempt=2)]},
        )
        code, _, _ = run_triage(api, ls_files=["src/test/java/slash/nav/FooTest.java"])
        self.assertEqual(code, 0)
        create = create_issue_call(api)
        self.assertIn("### Files", create[2]["body"])
        self.assertIn("src/test/java/slash/nav/FooTest.java", create[2]["body"])
        self.assertNotIn("agent:approved", create[2]["labels"])
        create_idx = next(i for i, c in enumerate(api.calls) if c[0] == "POST" and c[1] == f"/repos/{REPO}/issues")
        label_idx = next(
            i for i, c in enumerate(api.calls) if c[0] == "POST" and c[1] == f"/repos/{REPO}/issues/42/labels"
        )
        self.assertGreater(label_idx, create_idx)
        self.assertEqual(api.calls[label_idx][2], {"labels": ["agent:approved"]})


class TimeoutInfraTest(unittest.TestCase):
    def test_rerun_timeout_is_infra_and_ntfy_skipped(self):
        api = FakeApi(
            {
                ("GET", f"{RUN_PATH}/attempts/1/jobs"): jobs_response(job("Java 21", "failure", 1, failed=True)),
                ("GET", f"/repos/{REPO}/actions/jobs/1/logs"): LOG,
                ("GET", f"{RUN_PATH}/artifacts"): {"artifacts": []},
                ("POST", f"{RUN_PATH}/rerun-failed-jobs"): {},
                ("GET", ISSUES_PATH): [],
                ("GET", PULLS_PATH): [],
                ("GET", RUNS_PATH): {"workflow_runs": []},
                ("GET", REPO_PATH): {"default_branch": "master"},
                ("POST", f"/repos/{REPO}/issues"): {"number": 99},
            },
            queues={("GET", RUN_PATH): [run(), run()]},
        )
        code, _, err = run_triage(api, env={"NIGHTLY_POLL_TIMEOUT_S": "0"})
        self.assertEqual(code, 0)
        self.assertIn("ntfy skipped", err)
        create = create_issue_call(api)
        self.assertEqual(create[2]["title"], "nightly: infra streak")


class PrereleaseInfraTest(unittest.TestCase):
    def test_pin_nsis_failure_is_infra_no_issue_streak_increments_under_prerelease(self):
        prerelease_runs_path = f"/repos/{REPO}/actions/workflows/88/runs?per_page=100"
        api = FakeApi(
            {
                ("GET", f"{RUN_PATH}/attempts/1/jobs"): jobs_response(
                    job("build-windows / build", "failure", 1, failed=True, failed_step="Pin NSIS 3.11")
                ),
                ("GET", f"/repos/{REPO}/actions/jobs/1/logs"): b"Unable to connect to source",
                ("GET", f"{RUN_PATH}/artifacts"): {"artifacts": []},
                ("POST", f"{RUN_PATH}/rerun-failed-jobs"): {},
                ("GET", f"{RUN_PATH}/attempts/2/jobs"): jobs_response(
                    job("build-windows / build", "failure", 1, failed=True, failed_step="Pin NSIS 3.11")
                ),
                ("GET", ISSUES_PATH): [],
                ("GET", PULLS_PATH): [],
                ("GET", prerelease_runs_path): {"workflow_runs": []},
                ("GET", REPO_PATH): {"default_branch": "master"},
                ("POST", f"/repos/{REPO}/issues"): {"number": 77},
            },
            queues={
                ("GET", RUN_PATH): [
                    run(path=".github/workflows/prerelease.yml", workflow_id=88),
                    run(path=".github/workflows/prerelease.yml", workflow_id=88, run_attempt=2),
                ]
            },
        )
        fd, summary_path = tempfile.mkstemp()
        os.close(fd)
        self.addCleanup(os.remove, summary_path)
        code, _, _ = run_triage(api, env={"GITHUB_STEP_SUMMARY": summary_path}, ls_files=[])
        self.assertEqual(code, 0)
        self.assertEqual([c for c in api.calls if c[0] == "POST" and c[1] == f"/repos/{REPO}/issues"][0][2]["title"], "nightly: infra streak")
        self.assertEqual([c for c in api.calls if c[0] == "POST" and c[1] == f"/repos/{REPO}/issues"][0][2]["labels"], ["from:nightly"])
        created_body = [c for c in api.calls if c[0] == "POST" and c[1] == f"/repos/{REPO}/issues"][0][2]["body"]
        self.assertEqual(nightly_issue.parse_state(created_body), {"streaks": {"prerelease": 1}})
        with open(summary_path, encoding="utf-8") as f:
            content = f.read()
        self.assertIn("Workflow: prerelease", content)


class DryRunTest(unittest.TestCase):
    def test_dry_run_makes_no_write_calls_and_prints_lines(self):
        zipped = surefire_zip("slash.nav.FooTest", "testFoo")
        api = FakeApi(
            {
                ("GET", RUN_PATH): run(),
                ("GET", f"{RUN_PATH}/attempts/1/jobs"): jobs_response(job("Java 21", "failure", 1, failed=True)),
                ("GET", f"/repos/{REPO}/actions/jobs/1/logs"): LOG,
                ("GET", f"{RUN_PATH}/artifacts"): {"artifacts": [{"id": 9, "name": "surefire-Java-21"}]},
                ("GET", f"/repos/{REPO}/actions/artifacts/9/zip"): zipped,
                ("GET", ISSUES_PATH): [],
                ("GET", PULLS_PATH): [],
                ("GET", RUNS_PATH): {"workflow_runs": []},
                ("GET", REPO_PATH): {"default_branch": "master"},
            }
        )
        code, out, _ = run_triage(api, dry_run=True, ls_files=["src/test/java/slash/nav/FooTest.java"])
        self.assertEqual(code, 0)
        self.assertEqual([m for m, _, _ in api.calls if m in ("POST", "PATCH", "DELETE")], [])
        self.assertIn("DRY-RUN", out)


class TrunkOverlapTest(unittest.TestCase):
    def test_trunk_pr_without_signature_blocks_approval(self):
        zipped = surefire_zip("slash.nav.FooTest", "testFoo")
        trunk_pr = {
            "number": 7,
            "state": "open",
            "merged_at": None,
            "head": {"ref": "agent/patcher/trunk-x"},
            "title": "Fix trunk",
            "body": "no signature here",
        }
        api = FakeApi(
            {
                ("GET", f"{RUN_PATH}/attempts/1/jobs"): jobs_response(job("Java 21", "failure", 1, failed=True)),
                ("GET", f"/repos/{REPO}/actions/jobs/1/logs"): LOG,
                ("GET", f"{RUN_PATH}/artifacts"): {"artifacts": [{"id": 9, "name": "surefire-Java-21"}]},
                ("GET", f"/repos/{REPO}/actions/artifacts/9/zip"): zipped,
                ("POST", f"{RUN_PATH}/rerun-failed-jobs"): {},
                ("GET", f"{RUN_PATH}/attempts/2/jobs"): jobs_response(job("Java 21", "failure", 1, failed=True)),
                ("GET", ISSUES_PATH): [],
                ("GET", PULLS_PATH): [trunk_pr],
                ("GET", RUNS_PATH): {"workflow_runs": []},
                ("GET", REPO_PATH): {"default_branch": "master"},
                ("POST", f"/repos/{REPO}/issues"): {"number": 42},
                ("POST", f"/repos/{REPO}/issues/42/comments"): {},
            },
            queues={("GET", RUN_PATH): [run(), run(run_attempt=2)]},
        )
        code, _, _ = run_triage(api, ls_files=["src/test/java/slash/nav/FooTest.java"])
        self.assertEqual(code, 0)
        create = create_issue_call(api)
        self.assertNotIn("agent:approved", create[2]["labels"])
        self.assertIn("needs-triage", create[2]["labels"])
        comment = next(c for c in api.calls if c[0] == "POST" and c[1] == f"/repos/{REPO}/issues/42/comments")
        self.assertIn("trunk", comment[2]["body"])

    def test_no_trunk_overlap_with_autoapprove_gets_approved(self):
        scratch_push_run = {
            "id": 555,
            "event": "push",
            "head_branch": "scratch/x",
            "conclusion": "failure",
            "run_started_at": "2026-10-12T03:00:00Z",
            "created_at": "2026-10-12T03:00:00Z",
            "head_sha": "ccc",
            "html_url": f"https://github.com/{REPO}/actions/runs/555",
        }
        zipped = surefire_zip("slash.nav.FooTest", "testFoo")
        api = FakeApi(
            {
                ("GET", f"{RUN_PATH}/attempts/1/jobs"): jobs_response(job("Java 21", "failure", 1, failed=True)),
                ("GET", f"/repos/{REPO}/actions/jobs/1/logs"): LOG,
                ("GET", f"{RUN_PATH}/artifacts"): {"artifacts": [{"id": 9, "name": "surefire-Java-21"}]},
                ("GET", f"/repos/{REPO}/actions/artifacts/9/zip"): zipped,
                ("POST", f"{RUN_PATH}/rerun-failed-jobs"): {},
                ("GET", f"{RUN_PATH}/attempts/2/jobs"): jobs_response(job("Java 21", "failure", 1, failed=True)),
                ("GET", ISSUES_PATH): [],
                ("GET", PULLS_PATH): [],
                ("GET", RUNS_PATH): {"workflow_runs": [scratch_push_run]},
                ("GET", REPO_PATH): {"default_branch": "master"},
                ("GET", f"/repos/{REPO}/actions/runs/555/attempts/1/jobs"): jobs_response(
                    job("Java 21", "failure", 1, failed=True)
                ),
                ("GET", f"/repos/{REPO}/actions/runs/555/artifacts"): {"artifacts": []},
                ("POST", f"/repos/{REPO}/issues"): {"number": 42},
                ("POST", f"/repos/{REPO}/issues/42/labels"): [],
            },
            queues={("GET", RUN_PATH): [run(head_branch="scratch/x"), run(head_branch="scratch/x", run_attempt=2)]},
        )
        code, _, _ = run_triage(api, env={"NIGHTLY_AUTOAPPROVE": "1"}, ls_files=["src/test/java/slash/nav/FooTest.java"])
        self.assertEqual(code, 0)
        create = create_issue_call(api)
        self.assertNotIn("agent:approved", create[2]["labels"])
        label_calls = [c for c in api.calls if c[0] == "POST" and c[1] == f"/repos/{REPO}/issues/42/labels"]
        self.assertEqual(len(label_calls), 1)
        self.assertEqual(label_calls[0][2], {"labels": ["agent:approved"]})

    def test_trunk_overlap_when_master_push_run_shares_signature(self):
        master_push_run = {
            "id": 555,
            "event": "push",
            "head_branch": "master",
            "conclusion": "failure",
            "run_started_at": "2026-10-12T03:00:00Z",
            "created_at": "2026-10-12T03:00:00Z",
            "head_sha": "ccc",
            "html_url": f"https://github.com/{REPO}/actions/runs/555",
        }
        zipped = surefire_zip("slash.nav.FooTest", "testFoo")
        api = FakeApi(
            {
                ("GET", f"{RUN_PATH}/attempts/1/jobs"): jobs_response(job("Java 21", "failure", 1, failed=True)),
                ("GET", f"/repos/{REPO}/actions/jobs/1/logs"): LOG,
                ("GET", f"{RUN_PATH}/artifacts"): {"artifacts": [{"id": 9, "name": "surefire-Java-21"}]},
                ("GET", f"/repos/{REPO}/actions/artifacts/9/zip"): zipped,
                ("POST", f"{RUN_PATH}/rerun-failed-jobs"): {},
                ("GET", f"{RUN_PATH}/attempts/2/jobs"): jobs_response(job("Java 21", "failure", 1, failed=True)),
                ("GET", ISSUES_PATH): [],
                ("GET", PULLS_PATH): [],
                ("GET", RUNS_PATH): {"workflow_runs": [master_push_run]},
                ("GET", REPO_PATH): {"default_branch": "master"},
                ("GET", f"/repos/{REPO}/actions/runs/555/attempts/1/jobs"): jobs_response(
                    job("Java 21", "failure", 1, failed=True)
                ),
                ("GET", f"/repos/{REPO}/actions/runs/555/artifacts"): {
                    "artifacts": [{"id": 10, "name": "surefire-Java-21"}]
                },
                ("GET", f"/repos/{REPO}/actions/artifacts/10/zip"): zipped,
                ("POST", f"/repos/{REPO}/issues"): {"number": 42},
                ("POST", f"/repos/{REPO}/issues/42/comments"): {},
            },
            queues={("GET", RUN_PATH): [run(head_branch="scratch/x"), run(head_branch="scratch/x", run_attempt=2)]},
        )
        code, _, _ = run_triage(api, ls_files=["src/test/java/slash/nav/FooTest.java"])
        self.assertEqual(code, 0)
        create = create_issue_call(api)
        self.assertNotIn("agent:approved", create[2]["labels"])
        self.assertIn("needs-triage", create[2]["labels"])
        comment = next(c for c in api.calls if c[0] == "POST" and c[1] == f"/repos/{REPO}/issues/42/comments")
        self.assertIn("trunk", comment[2]["body"])

    def test_nightly_run_on_master_still_overlaps_with_master_push_run(self):
        master_push_run = {
            "id": 555,
            "event": "push",
            "head_branch": "master",
            "conclusion": "failure",
            "run_started_at": "2026-10-12T03:00:00Z",
            "created_at": "2026-10-12T03:00:00Z",
            "head_sha": "ccc",
            "html_url": f"https://github.com/{REPO}/actions/runs/555",
        }
        zipped = surefire_zip("slash.nav.FooTest", "testFoo")
        api = FakeApi(
            {
                ("GET", f"{RUN_PATH}/attempts/1/jobs"): jobs_response(job("Java 21", "failure", 1, failed=True)),
                ("GET", f"/repos/{REPO}/actions/jobs/1/logs"): LOG,
                ("GET", f"{RUN_PATH}/artifacts"): {"artifacts": [{"id": 9, "name": "surefire-Java-21"}]},
                ("GET", f"/repos/{REPO}/actions/artifacts/9/zip"): zipped,
                ("POST", f"{RUN_PATH}/rerun-failed-jobs"): {},
                ("GET", f"{RUN_PATH}/attempts/2/jobs"): jobs_response(job("Java 21", "failure", 1, failed=True)),
                ("GET", ISSUES_PATH): [],
                ("GET", PULLS_PATH): [],
                ("GET", RUNS_PATH): {"workflow_runs": [master_push_run]},
                ("GET", REPO_PATH): {"default_branch": "master"},
                ("GET", f"/repos/{REPO}/actions/runs/555/attempts/1/jobs"): jobs_response(
                    job("Java 21", "failure", 1, failed=True)
                ),
                ("GET", f"/repos/{REPO}/actions/runs/555/artifacts"): {
                    "artifacts": [{"id": 10, "name": "surefire-Java-21"}]
                },
                ("GET", f"/repos/{REPO}/actions/artifacts/10/zip"): zipped,
                ("POST", f"/repos/{REPO}/issues"): {"number": 42},
                ("POST", f"/repos/{REPO}/issues/42/comments"): {},
            },
            queues={("GET", RUN_PATH): [run(), run(run_attempt=2)]},
        )
        code, _, _ = run_triage(api, ls_files=["src/test/java/slash/nav/FooTest.java"])
        self.assertEqual(code, 0)
        create = create_issue_call(api)
        self.assertNotIn("agent:approved", create[2]["labels"])
        self.assertIn("needs-triage", create[2]["labels"])
        comment = next(c for c in api.calls if c[0] == "POST" and c[1] == f"/repos/{REPO}/issues/42/comments")
        self.assertIn("trunk", comment[2]["body"])


class DefaultBranchTest(unittest.TestCase):
    def test_falls_back_to_master_when_field_missing(self):
        api = FakeApi({("GET", REPO_PATH): {}})
        self.assertEqual(nightly_triage._default_branch(api, REPO), "master")

    def test_falls_back_to_master_when_field_empty(self):
        api = FakeApi({("GET", REPO_PATH): {"default_branch": ""}})
        self.assertEqual(nightly_triage._default_branch(api, REPO), "master")

    def test_uses_returned_default_branch(self):
        api = FakeApi({("GET", REPO_PATH): {"default_branch": "main"}})
        self.assertEqual(nightly_triage._default_branch(api, REPO), "main")


class StepSummaryTest(unittest.TestCase):
    def test_step_summary_written_to_file(self):
        zipped = surefire_zip("slash.nav.FooTest", "testFoo")
        api = FakeApi(
            {
                ("GET", f"{RUN_PATH}/attempts/1/jobs"): jobs_response(job("Java 21", "failure", 1, failed=True)),
                ("GET", f"/repos/{REPO}/actions/jobs/1/logs"): LOG,
                ("GET", f"{RUN_PATH}/artifacts"): {"artifacts": [{"id": 9, "name": "surefire-Java-21"}]},
                ("GET", f"/repos/{REPO}/actions/artifacts/9/zip"): zipped,
                ("POST", f"{RUN_PATH}/rerun-failed-jobs"): {},
                ("GET", f"{RUN_PATH}/attempts/2/jobs"): jobs_response(job("Java 21", "failure", 1, failed=True)),
                ("GET", ISSUES_PATH): [],
                ("GET", PULLS_PATH): [],
                ("GET", RUNS_PATH): {"workflow_runs": []},
                ("GET", REPO_PATH): {"default_branch": "master"},
                ("POST", f"/repos/{REPO}/issues"): {"number": 42},
                ("POST", f"/repos/{REPO}/issues/42/labels"): [],
            },
            queues={("GET", RUN_PATH): [run(), run(run_attempt=2)]},
        )
        fd, summary_path = tempfile.mkstemp()
        os.close(fd)
        self.addCleanup(os.remove, summary_path)
        code, _, _ = run_triage(api, env={"GITHUB_STEP_SUMMARY": summary_path}, ls_files=["src/test/java/slash/nav/FooTest.java"])
        self.assertEqual(code, 0)
        with open(summary_path, encoding="utf-8") as f:
            content = f.read()
        self.assertIn("| Class | Signature | Jobs | Action | Issue |", content)
        self.assertIn("open_issue", content)
        self.assertIn("deterministic", content)


class ApiPaginateTest(unittest.TestCase):
    def test_paginate_follows_link_header(self):
        api = nightly_triage.Api("token", "https://api.github.com")
        page1 = FakeResponse(
            b'[{"n": 1}, {"n": 2}]',
            {"Link": '<https://api.github.com/repos/o/r/issues?page=2>; rel="next"'},
        )
        page2 = FakeResponse(b'[{"n": 3}]', {})
        with mock.patch.object(nightly_triage._OPENER, "open", side_effect=[page1, page2]) as urlopen:
            items = list(api.paginate("/repos/o/r/issues?page=1"))
        self.assertEqual([item["n"] for item in items], [1, 2, 3])
        self.assertEqual(urlopen.call_count, 2)
        self.assertEqual(urlopen.call_args_list[1].args[0].full_url, "https://api.github.com/repos/o/r/issues?page=2")


class RedirectAuthTest(unittest.TestCase):
    def _redirect(self, newurl):
        req = urllib.request.Request(
            "https://api.github.com/repos/o/r/actions/jobs/1/logs",
            headers={"Authorization": "Bearer token", "Accept": "application/vnd.github+json"},
        )
        handler = nightly_triage._DropAuthOnRedirect()
        return handler.redirect_request(req, None, 302, "Found", {}, newurl)

    def test_cross_host_redirect_drops_authorization(self):
        new = self._redirect("https://productionresultssa1.blob.core.windows.net/logs/1.txt?sig=x")
        self.assertFalse(new.has_header("Authorization"))
        self.assertEqual(new.get_header("Accept"), "application/vnd.github+json")

    def test_same_host_redirect_keeps_authorization(self):
        new = self._redirect("https://api.github.com/repositories/1/actions/jobs/1/logs")
        self.assertEqual(new.get_header("Authorization"), "Bearer token")


class FakeResponse:
    def __init__(self, data, headers):
        self._data = data
        self.headers = headers

    def __enter__(self):
        return self

    def __exit__(self, *exc_info):
        return False

    def read(self):
        return self._data


class CancelledJobTest(unittest.TestCase):
    def test_cancelled_job_gets_no_log_and_no_finding(self):
        api = FakeApi(
            {
                ("GET", f"{RUN_PATH}/attempts/1/jobs"): jobs_response(
                    job("Java 21", "failure", 1, failed=True), job("Java 25", "cancelled", 2)
                ),
                ("GET", f"/repos/{REPO}/actions/jobs/1/logs"): LOG,
                ("GET", f"{RUN_PATH}/artifacts"): {"artifacts": []},
                ("POST", f"{RUN_PATH}/rerun-failed-jobs"): {},
                ("GET", f"{RUN_PATH}/attempts/2/jobs"): jobs_response(
                    job("Java 21", "failure", 1, failed=True), job("Java 25", "success", 2)
                ),
                ("GET", ISSUES_PATH): [],
                ("GET", PULLS_PATH): [],
                ("GET", RUNS_PATH): {"workflow_runs": []},
                ("GET", REPO_PATH): {"default_branch": "master"},
                ("POST", f"/repos/{REPO}/issues"): {"number": 42},
            },
            queues={("GET", RUN_PATH): [run(), run(run_attempt=2)]},
        )
        code, _, _ = run_triage(api, ls_files=[])
        self.assertEqual(code, 0)
        self.assertEqual([c for c in api.calls if c[1] == f"/repos/{REPO}/actions/jobs/2/logs"], [])
        create = create_issue_call(api)
        self.assertIn("Java 21", create[2]["body"])
        self.assertNotIn("Java 25", create[2]["body"])


class CancelledRunTest(unittest.TestCase):
    def test_cancelled_run_is_not_triaged(self):
        fd, summary_path = tempfile.mkstemp()
        os.close(fd)
        self.addCleanup(os.remove, summary_path)
        api = FakeApi({("GET", RUN_PATH): run(conclusion="cancelled")})
        code, _, _ = run_triage(api, env={"GITHUB_STEP_SUMMARY": summary_path})
        self.assertEqual(code, 0)
        self.assertEqual([(m, p) for m, p, _ in api.calls], [("GET", RUN_PATH)])
        with open(summary_path, encoding="utf-8") as f:
            content = f.read()
        self.assertIn(f"run {RUN_ID} was cancelled — not triaged", content)


class ArtifactNameTest(unittest.TestCase):
    def test_job_name_with_slash_is_sanitised(self):
        zipped = surefire_zip("slash.nav.FooTest", "testFoo")
        api = FakeApi(
            {
                ("GET", f"{RUN_PATH}/attempts/1/jobs"): jobs_response(
                    job("build-windows / build", "failure", 1, failed=True)
                ),
                ("GET", f"/repos/{REPO}/actions/jobs/1/logs"): LOG,
                ("GET", f"{RUN_PATH}/artifacts"): {
                    "artifacts": [{"id": 9, "name": "surefire-build-windows-build"}]
                },
                ("GET", f"/repos/{REPO}/actions/artifacts/9/zip"): zipped,
                ("POST", f"{RUN_PATH}/rerun-failed-jobs"): {},
                ("GET", f"{RUN_PATH}/attempts/2/jobs"): jobs_response(
                    job("build-windows / build", "failure", 1, failed=True)
                ),
                ("GET", ISSUES_PATH): [],
                ("GET", PULLS_PATH): [],
                ("GET", RUNS_PATH): {"workflow_runs": []},
                ("GET", REPO_PATH): {"default_branch": "master"},
                ("POST", f"/repos/{REPO}/issues"): {"number": 42},
            },
            queues={("GET", RUN_PATH): [run(), run(run_attempt=2)]},
        )
        code, _, _ = run_triage(api, ls_files=[])
        self.assertEqual(code, 0)
        self.assertIn(("GET", f"/repos/{REPO}/actions/artifacts/9/zip"), [c[:2] for c in api.calls])


class FetchDriftTest(unittest.TestCase):
    def test_missing_artifact_returns_empty(self):
        api = FakeApi({("GET", f"/repos/{REPO}/actions/runs/{RUN_ID}/artifacts"): {"artifacts": []}})
        self.assertEqual(nightly_triage.fetch_drift(api, REPO, RUN_ID), {})

    def test_zip_without_member_returns_empty(self):
        buf = io.BytesIO()
        with zipfile.ZipFile(buf, "w") as zf:
            zf.writestr("other.json", "{}")
        api = FakeApi(
            {
                ("GET", f"/repos/{REPO}/actions/runs/{RUN_ID}/artifacts"): {
                    "artifacts": [{"id": 1, "name": "drift-snapshot"}]
                },
                ("GET", f"/repos/{REPO}/actions/artifacts/1/zip"): buf.getvalue(),
            }
        )
        self.assertEqual(nightly_triage.fetch_drift(api, REPO, RUN_ID), {})

    def test_present_artifact_parses_drift_json(self):
        api = FakeApi(
            {
                ("GET", f"/repos/{REPO}/actions/runs/{RUN_ID}/artifacts"): {
                    "artifacts": [{"id": 1, "name": "drift-snapshot"}]
                },
                ("GET", f"/repos/{REPO}/actions/artifacts/1/zip"): drift_zip({"java": "21"}),
            }
        )
        self.assertEqual(nightly_triage.fetch_drift(api, REPO, RUN_ID), {"java": "21"})


class FetchDriftEdgeTest(unittest.TestCase):
    def test_expired_artifact_counts_as_missing(self):
        api = FakeApi({("GET", f"/repos/{REPO}/actions/runs/{RUN_ID}/artifacts"): {
            "artifacts": [{"id": 1, "name": "drift-snapshot", "expired": True}]}})
        self.assertEqual(nightly_triage.fetch_drift(api, REPO, RUN_ID), {})
        self.assertNotIn(("GET", f"/repos/{REPO}/actions/artifacts/1/zip"), [c[:2] for c in api.calls])

    def test_http_error_on_download_returns_empty(self):
        class ErrApi(FakeApi):
            def request(self, method, path, body=None, raw=False):
                if path.endswith("/zip"):
                    raise urllib.error.HTTPError(path, 410, "Gone", {}, None)
                return super().request(method, path, body, raw)
        api = ErrApi({("GET", f"/repos/{REPO}/actions/runs/{RUN_ID}/artifacts"): {
            "artifacts": [{"id": 1, "name": "drift-snapshot"}]}})
        self.assertEqual(nightly_triage.fetch_drift(api, REPO, RUN_ID), {})


class ListRunsTest(unittest.TestCase):
    def test_uses_per_workflow_endpoint(self):
        api = FakeApi({("GET", RUNS_PATH): {"workflow_runs": [{"id": 1}]}})
        self.assertEqual(nightly_triage._list_runs(api, REPO, {"workflow_id": 77}), [{"id": 1}])
        self.assertEqual(api.calls[0][:2], ("GET", f"/repos/{REPO}/actions/workflows/77/runs?per_page=100"))


class PreviousScheduledTest(unittest.TestCase):
    def test_skips_push_and_in_progress_picks_latest_earlier_scheduled(self):
        runs = [
            run(id=RUN_ID, event="schedule", status="completed", run_started_at="2026-10-12T03:00:00Z"),
            run(id=200, event="push", status="completed", run_started_at="2026-10-11T12:00:00Z"),
            run(id=201, event="schedule", status="in_progress", run_started_at="2026-10-11T03:00:00Z"),
            run(id=202, event="schedule", status="completed", run_started_at="2026-10-10T03:00:00Z"),
            run(id=203, event="schedule", status="completed", run_started_at="2026-10-09T03:00:00Z"),
        ]
        previous = nightly_triage._previous_scheduled(runs, RUN_ID)
        self.assertEqual(previous["id"], 202)

    def test_cancelled_previous_night_is_skipped(self):
        runs = [
            run(id=RUN_ID, event="schedule", status="completed", run_started_at="2026-10-12T03:00:00Z"),
            run(id=301, event="schedule", status="completed", conclusion="cancelled", run_started_at="2026-10-11T03:00:00Z"),
            run(id=302, event="schedule", status="completed", conclusion="failure", run_started_at="2026-10-10T03:00:00Z"),
        ]
        self.assertEqual(nightly_triage._previous_scheduled(runs, RUN_ID)["id"], 302)

    def test_no_previous_scheduled_run_returns_none(self):
        runs = [run(id=RUN_ID, event="schedule", status="completed")]
        self.assertIsNone(nightly_triage._previous_scheduled(runs, RUN_ID))


class DriftCommentTest(unittest.TestCase):
    def test_changed_drift_comments_on_existing_ledger_issue(self):
        current_zip = drift_zip({"java": "21.0.1"})
        previous_zip = drift_zip({"java": "21.0.0"})
        api = FakeApi(
            {
                ("GET", RUN_PATH): run(conclusion="success"),
                ("GET", REPO_PATH): {"default_branch": "master"},
                ("GET", f"{RUN_PATH}/attempts/1/jobs"): jobs_response(job("Java 21", "success", 1)),
                ("GET", f"{RUN_PATH}/artifacts"): {"artifacts": [{"id": 1, "name": "drift-snapshot"}]},
                ("GET", f"/repos/{REPO}/actions/artifacts/1/zip"): current_zip,
                ("GET", ISSUES_PATH): [issue(60, "nightly: drift log", {}, ["from:nightly"])],
                ("GET", PULLS_PATH): [],
                ("GET", RUNS_PATH): {
                    "workflow_runs": [
                        run(conclusion="success"),
                        run(id=100, run_started_at="2026-10-10T03:00:00Z", created_at="2026-10-10T03:00:00Z"),
                    ]
                },
                ("GET", f"/repos/{REPO}/actions/runs/100/artifacts"): {
                    "artifacts": [{"id": 2, "name": "drift-snapshot"}]
                },
                ("GET", f"/repos/{REPO}/actions/artifacts/2/zip"): previous_zip,
                ("POST", f"/repos/{REPO}/issues/60/comments"): {},
            }
        )
        code, _, _ = run_triage(api)
        self.assertEqual(code, 0)
        writes = [c for c in api.calls if c[0] in ("POST", "PATCH", "DELETE")]
        self.assertEqual(len(writes), 1)
        self.assertEqual(writes[0][1], f"/repos/{REPO}/issues/60/comments")
        self.assertIn("java", writes[0][2]["body"])

    def test_creates_drift_ledger_issue_when_missing(self):
        current_zip = drift_zip({"java": "21.0.1"})
        previous_zip = drift_zip({"java": "21.0.0"})
        api = FakeApi(
            {
                ("GET", RUN_PATH): run(conclusion="success"),
                ("GET", REPO_PATH): {"default_branch": "master"},
                ("GET", f"{RUN_PATH}/attempts/1/jobs"): jobs_response(job("Java 21", "success", 1)),
                ("GET", f"{RUN_PATH}/artifacts"): {"artifacts": [{"id": 1, "name": "drift-snapshot"}]},
                ("GET", f"/repos/{REPO}/actions/artifacts/1/zip"): current_zip,
                ("GET", ISSUES_PATH): [],
                ("GET", PULLS_PATH): [],
                ("GET", RUNS_PATH): {
                    "workflow_runs": [
                        run(conclusion="success"),
                        run(id=100, run_started_at="2026-10-10T03:00:00Z", created_at="2026-10-10T03:00:00Z"),
                    ]
                },
                ("GET", f"/repos/{REPO}/actions/runs/100/artifacts"): {
                    "artifacts": [{"id": 2, "name": "drift-snapshot"}]
                },
                ("GET", f"/repos/{REPO}/actions/artifacts/2/zip"): previous_zip,
                ("POST", f"/repos/{REPO}/issues"): {"number": 51},
                ("POST", f"/repos/{REPO}/issues/51/comments"): {},
            }
        )
        code, _, _ = run_triage(api)
        self.assertEqual(code, 0)
        create = create_issue_call(api)
        self.assertEqual(create[2]["title"], "nightly: drift log")
        self.assertEqual(create[2]["labels"], ["from:nightly"])
        comments = [c for c in api.calls if c[0] == "POST" and c[1] == f"/repos/{REPO}/issues/51/comments"]
        self.assertEqual(len(comments), 1)

    def test_identical_snapshots_no_comment(self):
        same_zip = drift_zip({"java": "21.0.1"})
        api = FakeApi(
            {
                ("GET", RUN_PATH): run(conclusion="success"),
                ("GET", REPO_PATH): {"default_branch": "master"},
                ("GET", f"{RUN_PATH}/attempts/1/jobs"): jobs_response(job("Java 21", "success", 1)),
                ("GET", f"{RUN_PATH}/artifacts"): {"artifacts": [{"id": 1, "name": "drift-snapshot"}]},
                ("GET", f"/repos/{REPO}/actions/artifacts/1/zip"): same_zip,
                ("GET", ISSUES_PATH): [],
                ("GET", PULLS_PATH): [],
                ("GET", RUNS_PATH): {
                    "workflow_runs": [
                        run(conclusion="success"),
                        run(id=100, run_started_at="2026-10-10T03:00:00Z", created_at="2026-10-10T03:00:00Z"),
                    ]
                },
                ("GET", f"/repos/{REPO}/actions/runs/100/artifacts"): {
                    "artifacts": [{"id": 2, "name": "drift-snapshot"}]
                },
                ("GET", f"/repos/{REPO}/actions/artifacts/2/zip"): same_zip,
            }
        )
        code, _, _ = run_triage(api)
        self.assertEqual(code, 0)
        writes = [c for c in api.calls if c[0] in ("POST", "PATCH", "DELETE")]
        self.assertEqual(writes, [])

    def test_no_previous_run_means_no_diff_and_no_comment(self):
        current_zip = drift_zip({"java": "21.0.1"})
        api = FakeApi(
            {
                ("GET", RUN_PATH): run(conclusion="success"),
                ("GET", REPO_PATH): {"default_branch": "master"},
                ("GET", f"{RUN_PATH}/attempts/1/jobs"): jobs_response(job("Java 21", "success", 1)),
                ("GET", f"{RUN_PATH}/artifacts"): {"artifacts": [{"id": 1, "name": "drift-snapshot"}]},
                ("GET", f"/repos/{REPO}/actions/artifacts/1/zip"): current_zip,
                ("GET", ISSUES_PATH): [],
                ("GET", PULLS_PATH): [],
                ("GET", RUNS_PATH): {"workflow_runs": [run(conclusion="success")]},
            }
        )
        code, _, _ = run_triage(api)
        self.assertEqual(code, 0)
        writes = [c for c in api.calls if c[0] in ("POST", "PATCH", "DELETE")]
        self.assertEqual(writes, [])

    def test_missing_current_artifact_no_comment(self):
        previous_zip = drift_zip({"java": "21.0.0"})
        api = FakeApi(
            {
                ("GET", RUN_PATH): run(conclusion="success"),
                ("GET", REPO_PATH): {"default_branch": "master"},
                ("GET", f"{RUN_PATH}/attempts/1/jobs"): jobs_response(job("Java 21", "success", 1)),
                ("GET", f"{RUN_PATH}/artifacts"): {"artifacts": []},
                ("GET", ISSUES_PATH): [],
                ("GET", PULLS_PATH): [],
                ("GET", RUNS_PATH): {
                    "workflow_runs": [
                        run(conclusion="success"),
                        run(id=100, run_started_at="2026-10-10T03:00:00Z", created_at="2026-10-10T03:00:00Z"),
                    ]
                },
                ("GET", f"/repos/{REPO}/actions/runs/100/artifacts"): {
                    "artifacts": [{"id": 2, "name": "drift-snapshot"}]
                },
                ("GET", f"/repos/{REPO}/actions/artifacts/2/zip"): previous_zip,
            }
        )
        code, _, _ = run_triage(api)
        self.assertEqual(code, 0)
        writes = [c for c in api.calls if c[0] in ("POST", "PATCH", "DELETE")]
        self.assertEqual(writes, [])


class DeterministicWithDriftTest(unittest.TestCase):
    def test_open_issue_body_includes_env_diff_table(self):
        surefire_zipped = surefire_zip("slash.nav.FooTest", "testFoo")
        current_drift_zip = drift_zip({"java": "21.0.1"})
        previous_drift_zip = drift_zip({"java": "21.0.0"})
        api = FakeApi(
            {
                ("GET", REPO_PATH): {"default_branch": "master"},
                ("GET", f"{RUN_PATH}/attempts/1/jobs"): jobs_response(job("Java 21", "failure", 1, failed=True)),
                ("GET", f"/repos/{REPO}/actions/jobs/1/logs"): LOG,
                ("GET", f"{RUN_PATH}/artifacts"): {
                    "artifacts": [
                        {"id": 9, "name": "surefire-Java-21"},
                        {"id": 1, "name": "drift-snapshot"},
                    ]
                },
                ("GET", f"/repos/{REPO}/actions/artifacts/9/zip"): surefire_zipped,
                ("GET", f"/repos/{REPO}/actions/artifacts/1/zip"): current_drift_zip,
                ("POST", f"{RUN_PATH}/rerun-failed-jobs"): {},
                ("GET", f"{RUN_PATH}/attempts/2/jobs"): jobs_response(job("Java 21", "failure", 1, failed=True)),
                ("GET", ISSUES_PATH): [issue(60, "nightly: drift log", {}, ["from:nightly"])],
                ("GET", PULLS_PATH): [],
                ("GET", RUNS_PATH): {
                    "workflow_runs": [
                        run(),
                        run(id=100, run_started_at="2026-10-10T03:00:00Z", created_at="2026-10-10T03:00:00Z"),
                    ]
                },
                ("GET", f"/repos/{REPO}/actions/runs/100/artifacts"): {
                    "artifacts": [{"id": 2, "name": "drift-snapshot"}]
                },
                ("GET", f"/repos/{REPO}/actions/artifacts/2/zip"): previous_drift_zip,
                ("POST", f"/repos/{REPO}/issues"): {"number": 42},
                ("POST", f"/repos/{REPO}/issues/42/labels"): [],
                ("POST", f"/repos/{REPO}/issues/60/comments"): {},
            },
            queues={("GET", RUN_PATH): [run(), run(run_attempt=2)]},
        )
        code, _, _ = run_triage(api, ls_files=["src/test/java/slash/nav/FooTest.java"])
        self.assertEqual(code, 0)
        create = create_issue_call(api)
        self.assertIn("### Env diff vs last green nightly", create[2]["body"])
        self.assertIn("| java | 21.0.0 | 21.0.1 |", create[2]["body"])
        comment = next(c for c in api.calls if c[0] == "POST" and c[1] == f"/repos/{REPO}/issues/60/comments")
        self.assertIn("java", comment[2]["body"])


class DriftDryRunTest(unittest.TestCase):
    def test_dry_run_prints_drift_comment_line_and_no_writes(self):
        current_zip = drift_zip({"java": "21.0.1"})
        previous_zip = drift_zip({"java": "21.0.0"})
        api = FakeApi(
            {
                ("GET", RUN_PATH): run(conclusion="success"),
                ("GET", REPO_PATH): {"default_branch": "master"},
                ("GET", f"{RUN_PATH}/attempts/1/jobs"): jobs_response(job("Java 21", "success", 1)),
                ("GET", f"{RUN_PATH}/artifacts"): {"artifacts": [{"id": 1, "name": "drift-snapshot"}]},
                ("GET", f"/repos/{REPO}/actions/artifacts/1/zip"): current_zip,
                ("GET", ISSUES_PATH): [issue(60, "nightly: drift log", {}, ["from:nightly"])],
                ("GET", PULLS_PATH): [],
                ("GET", RUNS_PATH): {
                    "workflow_runs": [
                        run(conclusion="success"),
                        run(id=100, run_started_at="2026-10-10T03:00:00Z", created_at="2026-10-10T03:00:00Z"),
                    ]
                },
                ("GET", f"/repos/{REPO}/actions/runs/100/artifacts"): {
                    "artifacts": [{"id": 2, "name": "drift-snapshot"}]
                },
                ("GET", f"/repos/{REPO}/actions/artifacts/2/zip"): previous_zip,
            }
        )
        code, out, _ = run_triage(api, dry_run=True)
        self.assertEqual(code, 0)
        self.assertIn("DRY-RUN update_ledger #60 drift (1 keys)", out)
        writes = [c for c in api.calls if c[0] in ("POST", "PATCH", "DELETE")]
        self.assertEqual(writes, [])


if __name__ == "__main__":
    unittest.main()
