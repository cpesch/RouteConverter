#!/usr/bin/env python3
"""Nightly failure-triage CLI: wire a ``build.yml`` run to the decision engine.

Entry point for the ``nightly-triage`` workflow. Fetches a scheduled run,
re-runs the failed jobs, classifies the outcome into deduplicated
:class:`nightly_model.Finding`s, and executes the actions
:func:`nightly_decide.plan` decides on (open/close issues, relabel, comment,
update rolling ledgers, notify). Talks to the GitHub REST API through an
injectable :class:`Api`, so tests can substitute a fake transport.

Python 3 stdlib only.
"""

import argparse
import io
import json
import os
import re
import subprocess
import sys
import time
import traceback
import urllib.error
import urllib.parse
import urllib.request
import zipfile
from datetime import date

import nightly_classify
import nightly_decide
import nightly_drift
import nightly_issue
from nightly_model import JobResult

DEFAULT_API_URL = "https://api.github.com"
DEFAULT_POLL_TIMEOUT_S = 1500
POLL_INTERVAL_S = 30
NTFY_URL = "https://ntfy.routeconverter.com/factory"
REPRO = (
    "JAVA_TOOL_OPTIONS=-XX:TieredStopAtLevel=1 mvn --batch-mode -T 1C "
    "-Dsurefire.forkCount=1 -Dspotbugs.skip=true -Dshade.skip=true verify"
)

_SIG_RE = re.compile(r"\b[0-9a-f]{12}\b")


class _DropAuthOnRedirect(urllib.request.HTTPRedirectHandler):
    """Job logs and artifact zips answer with a 302 to signed blob storage on another
    host, which rejects a forwarded `Authorization` header with 401. urllib keeps the
    header on redirects, so drop it whenever the redirect leaves the original host."""

    def redirect_request(self, req, fp, code, msg, headers, newurl):
        new = super().redirect_request(req, fp, code, msg, headers, newurl)
        if new is not None and urllib.parse.urlsplit(newurl).netloc != urllib.parse.urlsplit(req.full_url).netloc:
            new.remove_header("Authorization")
        return new


_OPENER = urllib.request.build_opener(_DropAuthOnRedirect)


class Api:
    """Minimal GitHub REST client (urllib) with JSON parsing, pagination and 5xx retries."""

    def __init__(self, token, base_url=DEFAULT_API_URL):
        self.token = token
        self.base_url = base_url.rstrip("/")

    def _open(self, method, path, body=None):
        url = self.base_url + path
        headers = {"Accept": "application/vnd.github+json", "User-Agent": "nightly-triage"}
        if self.token:
            headers["Authorization"] = f"Bearer {self.token}"
        data = None
        if body is not None:
            data = json.dumps(body).encode("utf-8")
            headers["Content-Type"] = "application/json"
        delays = (2, 4, 8)
        for attempt in range(len(delays) + 1):
            req = urllib.request.Request(url, data=data, headers=headers, method=method)
            try:
                with _OPENER.open(req) as resp:
                    return dict(resp.headers), resp.read()
            except urllib.error.HTTPError as exc:
                if exc.code >= 500 and attempt < len(delays):
                    time.sleep(delays[attempt])
                    continue
                raise

    def request(self, method, path, body=None, raw=False):
        _, payload = self._open(method, path, body)
        if raw:
            return payload
        return json.loads(payload) if payload else None

    def paginate(self, path):
        next_path = path
        while next_path:
            headers, payload = self._open("GET", next_path)
            items = json.loads(payload) if payload else []
            yield from items
            next_path = self._next_path(headers)

    def _next_path(self, headers):
        link = headers.get("Link") or headers.get("link")
        if not link:
            return None
        for part in link.split(","):
            if 'rel="next"' not in part:
                continue
            start = part.find("<")
            end = part.find(">")
            if start == -1 or end == -1:
                continue
            url = part[start + 1 : end]
            if url.startswith(self.base_url):
                return url[len(self.base_url) :]
            return url
        return None


def _workflow_name(run):
    path = run.get("path") or ""
    name = path.rsplit("/", 1)[-1]
    if name.endswith((".yml", ".yaml")):
        name = name.rsplit(".", 1)[0]
    return name


def _job_result(api, repo, job):
    conclusion = job.get("conclusion")
    failed_step = None
    for step in job.get("steps") or []:
        if step.get("conclusion") == "failure":
            failed_step = step.get("name")
            break
    log = ""
    if conclusion in ("failure", "timed_out"):
        payload = api.request("GET", f"/repos/{repo}/actions/jobs/{job['id']}/logs", raw=True)
        if payload:
            log = payload.decode("utf-8", "replace")
    return JobResult(name=job["name"], conclusion=conclusion, failed_step=failed_step, log=log)


def fetch_jobs(api, repo, run_id, attempt):
    data = api.request("GET", f"/repos/{repo}/actions/runs/{run_id}/attempts/{attempt}/jobs") or {}
    return [_job_result(api, repo, job) for job in data.get("jobs", [])]


def _read_surefire_xmls(zip_bytes):
    if not zip_bytes:
        return []
    try:
        with zipfile.ZipFile(io.BytesIO(zip_bytes)) as zf:
            return [
                zf.read(name).decode("utf-8", "replace")
                for name in zf.namelist()
                if name.rsplit("/", 1)[-1].startswith("TEST-") and name.endswith(".xml")
            ]
    except zipfile.BadZipFile:
        return []


def fetch_reports(api, repo, run_id, jobs):
    failed = [job for job in jobs if job.conclusion in nightly_classify.FAILED_CONCLUSIONS]
    if not failed:
        return {}
    data = api.request("GET", f"/repos/{repo}/actions/runs/{run_id}/artifacts") or {}
    by_name = {artifact.get("name"): artifact for artifact in data.get("artifacts", [])}
    reports = {}
    for job in failed:
        name = "surefire-" + job.name.replace(" ", "-")
        artifact = by_name.get(name)
        if artifact is None:
            reports[job.name] = []
            continue
        payload = api.request("GET", f"/repos/{repo}/actions/artifacts/{artifact['id']}/zip", raw=True)
        reports[job.name] = _read_surefire_xmls(payload)
    return reports


def _int_env(env, key, default):
    try:
        return int(env.get(key, default))
    except (TypeError, ValueError):
        return default


def rerun_and_poll(api, repo, run_id, env):
    timeout_s = _int_env(env, "NIGHTLY_POLL_TIMEOUT_S", DEFAULT_POLL_TIMEOUT_S)
    api.request("POST", f"/repos/{repo}/actions/runs/{run_id}/rerun-failed-jobs", {})
    deadline = time.monotonic() + timeout_s
    while True:
        run = api.request("GET", f"/repos/{repo}/actions/runs/{run_id}")
        if run.get("run_attempt", 1) >= 2 and run.get("status") == "completed":
            break
        if time.monotonic() >= deadline:
            return None
        time.sleep(POLL_INTERVAL_S)
    return fetch_jobs(api, repo, run_id, 2)


def _list_runs(api, repo, run):
    workflow_id = run.get("workflow_id")
    if workflow_id is None:
        return []
    data = api.request("GET", f"/repos/{repo}/actions/runs?workflow_id={workflow_id}&per_page=100") or {}
    return data.get("workflow_runs", [])


def _started(run):
    return run.get("run_started_at") or run.get("created_at") or ""


def fetch_drift(api, repo, run_id):
    """Download the ``drift-snapshot`` artifact of ``run_id`` and parse its ``drift.json``.

    Returns ``{}`` (never raises) when the artifact, its ``drift.json`` member, or the
    zip itself is missing or unreadable.
    """
    data = api.request("GET", f"/repos/{repo}/actions/runs/{run_id}/artifacts") or {}
    artifact = next((a for a in data.get("artifacts", []) if a.get("name") == "drift-snapshot"), None)
    if artifact is None:
        return {}
    payload = api.request("GET", f"/repos/{repo}/actions/artifacts/{artifact['id']}/zip", raw=True)
    if not payload:
        return {}
    try:
        with zipfile.ZipFile(io.BytesIO(payload)) as zf:
            text = zf.read("drift.json").decode("utf-8", "replace")
    except (zipfile.BadZipFile, KeyError):
        return {}
    return nightly_drift.parse(text)


def _previous_scheduled(runs, run_id):
    """Most recent scheduled, completed run in ``runs`` that started before ``run_id``.

    ``runs`` (as returned by :func:`_list_runs`) is expected to include the entry for
    ``run_id`` itself; its start time anchors the "before" comparison. Any conclusion
    counts, because the comparison is night to night.
    """
    current = next((run for run in runs if str(run.get("id")) == str(run_id)), None)
    current_started = _started(current) if current is not None else None
    best = None
    for run in runs:
        if str(run.get("id")) == str(run_id):
            continue
        if run.get("event") != "schedule" or run.get("status") != "completed":
            continue
        started = _started(run)
        if current_started and started >= current_started:
            continue
        if best is None or started > _started(best):
            best = run
    return best


def _find_last_green(runs, run_id):
    for run in runs:
        if str(run.get("id")) == str(run_id):
            continue
        if run.get("event") == "schedule" and run.get("conclusion") == "success":
            return run
    return None


def _default_branch(api, repo):
    data = api.request("GET", f"/repos/{repo}") or {}
    return data.get("default_branch") or "master"


def _push_runs(runs, run_id, branch, last_green, cap=5):
    result = []
    last_green_started = _started(last_green) if last_green is not None else None
    for run in runs:
        if str(run.get("id")) == str(run_id):
            continue
        if run.get("event") != "push" or run.get("head_branch") != branch:
            continue
        if run.get("conclusion") not in nightly_classify.FAILED_CONCLUSIONS:
            continue
        if last_green_started and _started(run) < last_green_started:
            continue
        result.append(run)
        if len(result) >= cap:
            break
    return result


def _signature_in(text):
    match = _SIG_RE.search(text or "")
    return match.group(0) if match else None


def _build_trunk_overlap(api, repo, run, workflow, prs, runs, last_green, default_branch):
    sigs = set()
    trunk_all = False
    for pr in prs:
        head_ref = (pr.get("head") or {}).get("ref") or ""
        if pr.get("state") == "open" and head_ref.startswith("agent/patcher/trunk-"):
            signature = _signature_in("\n".join(filter(None, (pr.get("title"), pr.get("body")))))
            if signature:
                sigs.add(signature)
            else:
                trunk_all = True
    for red_run in _push_runs(runs, run.get("id"), default_branch, last_green):
        jobs = fetch_jobs(api, repo, red_run["id"], 1)
        reports = fetch_reports(api, repo, red_run["id"], jobs)
        for finding in nightly_classify.classify(workflow, jobs, None, reports):
            sigs.add(finding.signature)
    return sigs, trunk_all


def _pr_matches(pr, issue_number):
    head_ref = (pr.get("head") or {}).get("ref") or ""
    if head_ref.startswith(f"agent/builder/{issue_number}-"):
        return True
    return f"Fixes #{issue_number}" in (pr.get("body") or "")


def _match_prs(issue_number, prs):
    matching = [pr for pr in prs if _pr_matches(pr, issue_number)]
    if any(pr.get("merged_at") for pr in matching):
        attempt_pr_state = "merged"
    elif any(pr.get("state") == "closed" for pr in matching):
        attempt_pr_state = "closed"
    else:
        attempt_pr_state = None
    open_prs = [pr for pr in matching if pr.get("state") == "open"]
    open_pr = min(pr["number"] for pr in open_prs) if open_prs else None
    return open_pr, attempt_pr_state


def _ledgers(open_issues):
    flaky_ledger = {}
    infra_streak = 0
    for issue in open_issues:
        title = issue.get("title")
        state = nightly_issue.parse_state(issue.get("body") or "") or {}
        if title == nightly_issue.LEDGER_TITLES["flaky"]:
            flaky_ledger = state
        elif title == nightly_issue.LEDGER_TITLES["infra"]:
            infra_streak = state.get("streak", 0)
    return flaky_ledger, infra_streak


def _ledger_numbers(open_issues):
    titles = {title: kind for kind, title in nightly_issue.LEDGER_TITLES.items()}
    numbers = {}
    for issue in open_issues:
        kind = titles.get(issue.get("title"))
        if kind:
            numbers[kind] = issue["number"]
    return numbers


def _build_issues(open_issues, prs):
    ledger_titles = set(nightly_issue.LEDGER_TITLES.values())
    issues = []
    for issue in open_issues:
        if issue.get("title") in ledger_titles:
            continue
        state = nightly_issue.parse_state(issue.get("body") or "") or {}
        labels = frozenset(label.get("name") for label in issue.get("labels") or [])
        open_pr, attempt_pr_state = _match_prs(issue["number"], prs)
        issues.append(
            nightly_decide.Issue(
                number=issue["number"],
                labels=labels,
                state=state,
                open_pr=open_pr,
                attempt_pr_state=attempt_pr_state,
            )
        )
    return issues


def _commits(api, repo, run, last_green):
    if last_green is None:
        return []
    path = f"/repos/{repo}/compare/{last_green['head_sha']}...{run['head_sha']}"
    data = api.request("GET", path) or {}
    commits = []
    for commit in data.get("commits", [])[:30]:
        sha = (commit.get("sha") or "")[:9]
        message = (commit.get("commit") or {}).get("message") or ""
        subject = message.splitlines()[0] if message else ""
        commits.append(f"{sha} {subject}")
    return commits


def _git_ls_files():
    return subprocess.run(["git", "ls-files"], capture_output=True, text=True, check=True).stdout.splitlines()


def _files_by_sig(findings):
    if not findings:
        return {}
    ls_files = _git_ls_files()
    return {finding.signature: nightly_issue.resolve_files(finding, ls_files) for finding in findings}


def _render_ctx(run, last_green, commits, env_diff):
    return {
        "run_url": run.get("html_url") or "",
        "last_green": (
            {"sha": last_green["head_sha"], "url": last_green["html_url"]} if last_green is not None else None
        ),
        "first_red": {"sha": run.get("head_sha") or "", "url": run.get("html_url") or ""},
        "commits": commits,
        "repro": REPRO,
        "seed": None,
        "env_diff": env_diff,
    }


def _do_open_issue(api, repo, action, render_ctx, files_by_sig):
    finding = action.data["finding"]
    labels = list(action.data["labels"])
    approved = "agent:approved" in labels
    create_labels = [label for label in labels if label != "agent:approved"]
    body_ctx = dict(render_ctx)
    body_ctx["state"] = action.data["state"]
    body = nightly_issue.render_body(finding, files_by_sig.get(finding.signature, []), body_ctx)
    created = api.request(
        "POST",
        f"/repos/{repo}/issues",
        {"title": nightly_issue.render_title(finding), "body": body, "labels": create_labels},
    )
    number = created["number"]
    if approved:
        api.request("POST", f"/repos/{repo}/issues/{number}/labels", {"labels": ["agent:approved"]})
    return number


def _do_comment(api, repo, action, current_issue, render_ctx):
    issue = action.issue if action.issue is not None else current_issue
    if issue is None:
        return
    if "night" in action.data:
        body = nightly_issue.still_failing_comment(action.data["night"], render_ctx["run_url"])
    elif action.data.get("reason") == "resolved-pr-open":
        body = nightly_issue.resolved_comment(render_ctx["run_url"], action.data.get("pr"))
    elif action.data.get("reason") == "trunk-overlap":
        body = "Not auto-approved: a trunk fix may already cover this failure."
    else:
        body = json.dumps(action.data)
    api.request("POST", f"/repos/{repo}/issues/{issue}/comments", {"body": body})


def _do_close(api, repo, action, render_ctx):
    body = nightly_issue.resolved_comment(render_ctx["run_url"], action.data.get("pr"))
    api.request("POST", f"/repos/{repo}/issues/{action.issue}/comments", {"body": body})
    api.request("PATCH", f"/repos/{repo}/issues/{action.issue}", {"state": "closed"})


def _do_add_label(api, repo, action):
    api.request("POST", f"/repos/{repo}/issues/{action.issue}/labels", {"labels": [action.data["label"]]})


def _do_remove_label(api, repo, action):
    api.request("DELETE", f"/repos/{repo}/issues/{action.issue}/labels/{action.data['label']}")


def _do_update_state(api, repo, action):
    issue = api.request("GET", f"/repos/{repo}/issues/{action.issue}")
    old = nightly_issue.parse_state(issue.get("body") or "") or {}
    new_state = {**old, **action.data}
    new_body = nightly_issue.write_state(issue.get("body") or "", new_state)
    api.request("PATCH", f"/repos/{repo}/issues/{action.issue}", {"body": new_body})


def _do_update_ledger(api, repo, action, ledger_numbers):
    kind = action.data["kind"]
    title = nightly_issue.LEDGER_TITLES[kind]
    number = ledger_numbers.get(kind)
    if kind == "flaky":
        signature = action.data["sig"]
        date_str = action.data["date"]
        if number is None:
            body = nightly_issue.ledger_body(kind, {signature: [date_str]})
            created = api.request("POST", f"/repos/{repo}/issues", {"title": title, "body": body, "labels": ["from:nightly"]})
            ledger_numbers[kind] = created["number"]
            return
        issue = api.request("GET", f"/repos/{repo}/issues/{number}")
        old = nightly_issue.parse_state(issue.get("body") or "") or {}
        new_state = dict(old)
        new_state[signature] = list(new_state.get(signature, ())) + [date_str]
        api.request("PATCH", f"/repos/{repo}/issues/{number}", {"body": nightly_issue.write_state(issue.get("body") or "", new_state)})
    elif kind == "infra":
        streak = action.data["streak"]
        if number is None:
            body = nightly_issue.ledger_body(kind, {"streak": streak})
            created = api.request("POST", f"/repos/{repo}/issues", {"title": title, "body": body, "labels": ["from:nightly"]})
            ledger_numbers[kind] = created["number"]
            return
        issue = api.request("GET", f"/repos/{repo}/issues/{number}")
        old = nightly_issue.parse_state(issue.get("body") or "") or {}
        new_body = nightly_issue.write_state(issue.get("body") or "", {**old, "streak": streak})
        api.request("PATCH", f"/repos/{repo}/issues/{number}", {"body": new_body})


def ntfy_publish(url, title, body, token):
    req = urllib.request.Request(
        url,
        data=body.encode("utf-8"),
        headers={"Authorization": f"Bearer {token}", "Title": title},
        method="POST",
    )
    with urllib.request.urlopen(req):
        pass


def _do_ntfy(env, action, repo, run):
    token = env.get("NTFY_PUBLISHER_TOKEN") or os.environ.get("NTFY_PUBLISHER_TOKEN")
    if not token:
        print("ntfy skipped: NTFY_PUBLISHER_TOKEN is not set", file=sys.stderr)
        return
    if action.issue is not None:
        body = f"https://github.com/{repo}/issues/{action.issue}"
    else:
        body = run.get("html_url") or ""
    title = f"nightly-triage: {action.data.get('reason', 'failure')}"
    ntfy_publish(NTFY_URL, title, body, token)


def _execute(api, repo, actions, render_ctx, files_by_sig, ledger_numbers, env, run):
    current_issue = None
    for action in actions:
        if action.kind == "open_issue":
            current_issue = _do_open_issue(api, repo, action, render_ctx, files_by_sig)
        elif action.kind == "comment":
            _do_comment(api, repo, action, current_issue, render_ctx)
        elif action.kind == "close":
            _do_close(api, repo, action, render_ctx)
        elif action.kind == "add_label":
            _do_add_label(api, repo, action)
        elif action.kind == "remove_label":
            _do_remove_label(api, repo, action)
        elif action.kind == "update_state":
            _do_update_state(api, repo, action)
        elif action.kind == "update_ledger":
            _do_update_ledger(api, repo, action, ledger_numbers)
        elif action.kind == "ntfy":
            _do_ntfy(env, action, repo, run)
        else:
            print(f"unknown action kind: {action.kind}", file=sys.stderr)


def _drift_log_comment(api, repo, env_diff, run_url, open_issues, dry_run):
    if not env_diff:
        return
    title = nightly_issue.LEDGER_TITLES["drift"]
    number = next((issue["number"] for issue in open_issues if issue.get("title") == title), None)
    if dry_run:
        print(f"DRY-RUN drift-comment #{number if number is not None else 'new'} {len(env_diff)} keys")
        return
    if number is None:
        body = nightly_issue.ledger_body("drift", {})
        created = api.request("POST", f"/repos/{repo}/issues", {"title": title, "body": body, "labels": ["from:nightly"]})
        number = created["number"]
    comment = nightly_drift.drift_comment(env_diff, run_url)
    api.request("POST", f"/repos/{repo}/issues/{number}/comments", {"body": comment})


def _dry_run_line(action):
    issue = action.issue if action.issue is not None else 0
    if action.kind == "open_issue":
        short = action.data["finding"].signature
    elif action.kind == "comment":
        short = action.data.get("reason") or f"night={action.data.get('night', '')}"
    elif action.kind in ("add_label", "remove_label"):
        short = action.data["label"]
    elif action.kind == "update_state":
        short = ",".join(sorted(action.data))
    elif action.kind == "update_ledger":
        short = action.data["kind"]
    elif action.kind == "close":
        short = action.data.get("pr") or ""
    elif action.kind == "ntfy":
        short = action.data["reason"]
    else:
        short = ""
    return f"DRY-RUN {action.kind} #{issue} {short}".rstrip()


def _step_summary(actions, env, env_diff):
    path = env.get("GITHUB_STEP_SUMMARY")
    if not path:
        return
    lines = [
        "| Class | Signature | Jobs | Action | Issue |",
        "| --- | --- | --- | --- | --- |",
    ]
    for action in actions:
        finding = action.data.get("finding") if action.kind == "open_issue" else None
        klass = finding.klass if finding else ""
        signature = finding.signature if finding else ""
        jobs = ", ".join(finding.jobs) if finding else ""
        issue = str(action.issue) if action.issue is not None else "-"
        lines.append(f"| {klass} | {signature} | {jobs} | {action.kind} | {issue} |")
    if env_diff:
        lines.append(f"Environment drift: {len(env_diff)} keys changed")
    with open(path, "a", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")


def triage(api, repo, run_id, dry_run=False, settings=None, env=None):
    env = os.environ if env is None else env
    if env.get("NIGHTLY_TRIAGE") == "0":
        print("nightly-triage disabled")
        return 0
    if settings is None:
        settings = nightly_decide.Settings(autoapprove=env.get("NIGHTLY_AUTOAPPROVE") != "0")

    run = api.request("GET", f"/repos/{repo}/actions/runs/{run_id}")
    workflow = _workflow_name(run)
    if run.get("run_attempt", 1) > 1 or run.get("event") not in ("schedule", "workflow_dispatch"):
        return 0

    first_jobs = fetch_jobs(api, repo, run_id, 1)
    reports = fetch_reports(api, repo, run_id, first_jobs)

    if any(job.conclusion in nightly_classify.FAILED_CONCLUSIONS for job in first_jobs) and not dry_run:
        rerun_jobs = rerun_and_poll(api, repo, run_id, env)
    else:
        rerun_jobs = None

    findings = nightly_classify.classify(workflow, first_jobs, rerun_jobs, reports)

    open_issues = list(api.paginate(f"/repos/{repo}/issues?labels=from:nightly&state=open"))
    prs = api.request("GET", f"/repos/{repo}/pulls?state=all&sort=updated&direction=desc&per_page=100") or []

    runs = _list_runs(api, repo, run)
    last_green = _find_last_green(runs, run_id)
    default_branch = _default_branch(api, repo)

    current_drift = fetch_drift(api, repo, run_id)
    previous_run = _previous_scheduled(runs, run_id)
    previous_drift = fetch_drift(api, repo, previous_run["id"]) if previous_run is not None else {}
    env_diff = nightly_drift.diff(previous_drift, current_drift) if current_drift and previous_drift else {}

    trunk_overlap_sigs, trunk_all = _build_trunk_overlap(
        api, repo, run, workflow, prs, runs, last_green, default_branch
    )
    if trunk_all:
        trunk_overlap_sigs |= {finding.signature for finding in findings if finding.klass == "deterministic"}

    flaky_ledger, infra_streak = _ledgers(open_issues)
    issues = _build_issues(open_issues, prs)
    ledger_numbers = _ledger_numbers(open_issues)

    final_jobs = rerun_jobs if rerun_jobs is not None else first_jobs
    green_jobs = frozenset(job.name for job in final_jobs if job.conclusion == "success")
    files_by_sig = _files_by_sig(findings)
    commits = _commits(api, repo, run, last_green)

    ctx = nightly_decide.Context(
        today=date.today(),
        green_jobs=green_jobs,
        files_by_sig=files_by_sig,
        trunk_overlap_sigs=frozenset(trunk_overlap_sigs),
        flaky_ledger=flaky_ledger,
        infra_streak=infra_streak,
    )
    actions = nightly_decide.plan(findings, issues, ctx, settings)

    render_ctx = _render_ctx(run, last_green, commits, env_diff)

    if dry_run:
        for action in actions:
            print(_dry_run_line(action))
    else:
        _execute(api, repo, actions, render_ctx, files_by_sig, ledger_numbers, env, run)

    _drift_log_comment(api, repo, env_diff, render_ctx["run_url"], open_issues, dry_run)

    _step_summary(actions, env, env_diff)
    return 0


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description="Nightly failure triage for the build.yml workflow")
    parser.add_argument("--run-id", type=int, required=True, help="GitHub Actions run id to triage")
    parser.add_argument("--dry-run", action="store_true", help="compute and print actions, but make no write calls")
    parser.add_argument("--repo", help="owner/name (default $GITHUB_REPOSITORY)")
    return parser.parse_args(argv)


def main(argv=None):
    args = parse_args(argv)
    try:
        repo = args.repo or os.environ.get("GITHUB_REPOSITORY") or ""
        token = os.environ.get("GH_TOKEN") or os.environ.get("GITHUB_TOKEN") or ""
        base_url = os.environ.get("GITHUB_API_URL", DEFAULT_API_URL)
        api = Api(token, base_url)
        return triage(api, repo, args.run_id, args.dry_run)
    except Exception:
        traceback.print_exc()
        return 1


if __name__ == "__main__":
    sys.exit(main())
