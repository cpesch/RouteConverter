import contextlib
import io
import os
import sys
import tempfile
import unittest
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
RUNS_PATH = f"/repos/{REPO}/actions/runs?workflow_id=77&per_page=100"

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


def job(name, conclusion, job_id, failed=False):
    if failed:
        steps = [
            {"name": "Set up JDK", "conclusion": "success"},
            {"name": "Build with Maven", "conclusion": "failure"},
        ]
    elif conclusion == "success":
        steps = [{"name": "Build with Maven", "conclusion": "success"}]
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
                ("GET", ISSUES_PATH): [
                    issue(5, "nightly: FooTest [abc123def456]", state, ["from:nightly", "nightly:deterministic"])
                ],
                ("GET", PULLS_PATH): [],
                ("GET", RUNS_PATH): {"workflow_runs": []},
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
                ("GET", f"/repos/{REPO}/actions/jobs/1/logs"): b"Connection reset by peer",
                ("GET", f"{RUN_PATH}/artifacts"): {"artifacts": []},
                ("POST", f"{RUN_PATH}/rerun-failed-jobs"): {},
                ("GET", ISSUES_PATH): [],
                ("GET", PULLS_PATH): [],
                ("GET", RUNS_PATH): {"workflow_runs": []},
                ("POST", f"/repos/{REPO}/issues"): {"number": 99},
            },
            queues={("GET", RUN_PATH): [run(), run()]},
        )
        code, _, err = run_triage(api, env={"NIGHTLY_POLL_TIMEOUT_S": "0"})
        self.assertEqual(code, 0)
        self.assertIn("ntfy skipped", err)
        create = create_issue_call(api)
        self.assertEqual(create[2]["title"], "nightly: infra streak")


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


if __name__ == "__main__":
    unittest.main()
