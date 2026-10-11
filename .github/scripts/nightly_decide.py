"""Decision engine for the nightly failure-triage pipeline.

Turns today's classified findings (:func:`nightly_classify.classify`) plus
the current tracker state of open nightly-tracked issues into a
deterministic list of :class:`Action`: open or close an issue, comment,
relabel, or update tracked state. A pure function of its inputs — no
network access and no ``gh`` calls. Chunk 2/5 renders the issue/comment
bodies; chunk 4/5 executes the actions this module plans.

Python 3 stdlib only.
"""

from dataclasses import dataclass
from datetime import date

LABEL_FROM_NIGHTLY = "from:nightly"
LABEL_NEEDS_TRIAGE = "needs-triage"
LABEL_AGENT_APPROVED = "agent:approved"


@dataclass(frozen=True)
class Issue:
    number: int
    labels: frozenset[str]
    state: dict
    open_pr: int | None
    attempt_pr_state: str | None


@dataclass(frozen=True)
class Settings:
    autoapprove: bool
    max_open_approved: int = 3
    max_failed_attempts: int = 2
    flaky_threshold: int = 2
    flaky_window_days: int = 7
    infra_streak_threshold: int = 3


@dataclass(frozen=True)
class Context:
    today: date
    green_jobs: frozenset[str]
    files_by_sig: dict
    trunk_overlap_sigs: frozenset[str]
    flaky_ledger: dict
    infra_streak: int


@dataclass(frozen=True)
class Action:
    kind: str
    issue: int | None
    data: dict


def _match(finding, issues):
    test_classes = {t.test_class for t in finding.tests}
    for issue in issues:
        if issue.state.get("sig") == finding.signature:
            return issue
        if test_classes & set(issue.state.get("tests", ())):
            return issue
    return None


def _new_state(finding, today_iso):
    return {
        "sig": finding.signature,
        "jobs": sorted(finding.jobs),
        "tests": sorted({t.test_class for t in finding.tests}),
        "nights": [today_iso],
        "escalated": False,
    }


def _within_flaky_window(date_iso, today, window_days):
    return (today - date.fromisoformat(date_iso)).days < window_days


def _plan_flaky(finding, matched, ctx, settings, sort_key, today_iso):
    entries = []
    dates = ctx.flaky_ledger.get(finding.signature, ())
    count = sum(1 for d in dates if _within_flaky_window(d, ctx.today, settings.flaky_window_days)) + 1
    entries.append(
        (sort_key, Action("update_ledger", None, {"kind": "flaky", "sig": finding.signature, "date": today_iso}))
    )
    if matched is None and count >= settings.flaky_threshold:
        entries.append(
            (
                sort_key,
                Action(
                    "open_issue",
                    None,
                    {
                        "finding": finding,
                        "labels": sorted({LABEL_FROM_NIGHTLY, "nightly:flaky", LABEL_NEEDS_TRIAGE}),
                        "state": _new_state(finding, today_iso),
                    },
                ),
            )
        )
    return entries


def _plan_infra(finding, matched, ctx, settings, sort_key, today_iso):
    entries = []
    entries.append((sort_key, Action("ntfy", matched.number if matched else None, {"reason": "infra", "jobs": finding.jobs})))
    streak = ctx.infra_streak + 1
    entries.append((sort_key, Action("update_ledger", None, {"kind": "infra", "streak": streak})))
    if matched is None and streak >= settings.infra_streak_threshold:
        entries.append(
            (
                sort_key,
                Action(
                    "open_issue",
                    None,
                    {
                        "finding": finding,
                        "labels": sorted({LABEL_FROM_NIGHTLY, "nightly:infra", LABEL_NEEDS_TRIAGE}),
                        "state": _new_state(finding, today_iso),
                    },
                ),
            )
        )
    return entries


def _plan_forward_compat(finding, matched, sort_key, today_iso):
    if matched is not None:
        return []
    return [
        (
            sort_key,
            Action(
                "open_issue",
                None,
                {
                    "finding": finding,
                    "labels": sorted({LABEL_FROM_NIGHTLY, "nightly:forward-compat", LABEL_NEEDS_TRIAGE}),
                    "state": _new_state(finding, today_iso),
                },
            ),
        )
    ]


def _plan_deterministic_new(finding, ctx, settings, approved_count, sort_key, today_iso):
    sig = finding.signature
    files = ctx.files_by_sig.get(sig, [])
    trunk_overlap = sig in ctx.trunk_overlap_sigs
    approve = (
        settings.autoapprove
        and bool(files)
        and not trunk_overlap
        and approved_count < settings.max_open_approved
    )
    state = _new_state(finding, today_iso)
    if approve:
        labels = {LABEL_FROM_NIGHTLY, "nightly:deterministic", LABEL_AGENT_APPROVED}
        state["attempts"] = [{"date": today_iso, "result": "pending"}]
        approved_count += 1
    else:
        labels = {LABEL_FROM_NIGHTLY, "nightly:deterministic", LABEL_NEEDS_TRIAGE}

    entries = [(sort_key, Action("open_issue", None, {"finding": finding, "labels": sorted(labels), "state": state}))]
    if trunk_overlap:
        entries.append((sort_key, Action("comment", None, {"reason": "trunk-overlap"})))
    return entries, approved_count


def _attempt_failed(issue, latest):
    swapped_without_approval = (
        LABEL_NEEDS_TRIAGE in issue.labels
        and LABEL_AGENT_APPROVED not in issue.labels
        and latest.get("result") == "pending"
    )
    return swapped_without_approval or issue.attempt_pr_state in ("merged", "closed")


def _plan_deterministic_matched(issue, ctx, settings, approved_count, sort_key, today_iso):
    entries = []
    attempts = list(issue.state.get("attempts", ()))
    escalated = bool(issue.state.get("escalated", False))

    if attempts and _attempt_failed(issue, attempts[-1]):
        attempts = attempts[:-1] + [{**attempts[-1], "result": "failed"}]
        entries.append((sort_key, Action("update_state", issue.number, {"attempts": attempts})))

    failed_count = sum(1 for a in attempts if a.get("result") == "failed")

    if failed_count >= settings.max_failed_attempts and not escalated:
        entries.append((sort_key, Action("ntfy", issue.number, {"reason": "escalation"})))
        entries.append((sort_key, Action("remove_label", issue.number, {"label": LABEL_AGENT_APPROVED})))
        entries.append((sort_key, Action("add_label", issue.number, {"label": LABEL_NEEDS_TRIAGE})))
        entries.append((sort_key, Action("update_state", issue.number, {"escalated": True})))
        return entries, approved_count

    if escalated:
        return entries, approved_count

    sig = issue.state.get("sig")
    files = ctx.files_by_sig.get(sig, [])
    trunk_overlap = sig in ctx.trunk_overlap_sigs
    already_today = any(a.get("date") == today_iso for a in attempts)
    currently_approved = LABEL_AGENT_APPROVED in issue.labels
    other_approved = approved_count - (1 if currently_approved else 0)
    cap_free = other_approved < settings.max_open_approved

    if (
        settings.autoapprove
        and not already_today
        and issue.open_pr is None
        and bool(files)
        and not trunk_overlap
        and cap_free
    ):
        if currently_approved:
            entries.append((sort_key, Action("remove_label", issue.number, {"label": LABEL_AGENT_APPROVED})))
        else:
            approved_count += 1
            entries.append((sort_key, Action("remove_label", issue.number, {"label": LABEL_NEEDS_TRIAGE})))
        entries.append((sort_key, Action("add_label", issue.number, {"label": LABEL_AGENT_APPROVED})))
        new_attempts = attempts + [{"date": today_iso, "result": "pending"}]
        entries.append((sort_key, Action("update_state", issue.number, {"attempts": new_attempts})))

    return entries, approved_count


def _plan_close(issues, matched_numbers, ctx):
    entries = []
    for issue in issues:
        if issue.number in matched_numbers:
            continue
        if "nightly:deterministic" not in issue.labels and "nightly:infra" not in issue.labels:
            continue
        jobs = issue.state.get("jobs", ())
        if not jobs or not all(job in ctx.green_jobs for job in jobs):
            continue
        sort_key = (0, issue.number)
        if issue.open_pr is not None:
            entries.append(
                (sort_key, Action("comment", issue.number, {"reason": "resolved-pr-open", "pr": issue.open_pr}))
            )
        else:
            entries.append((sort_key, Action("close", issue.number, {"pr": issue.open_pr})))
    return entries


def plan(findings, issues, ctx, settings):
    issues_sorted = sorted(issues, key=lambda i: i.number)
    today_iso = ctx.today.isoformat()
    approved_count = sum(1 for i in issues if LABEL_AGENT_APPROVED in i.labels)
    matched_numbers = set()
    entries = []

    for finding in findings:
        matched = _match(finding, issues_sorted)
        if matched is not None:
            matched_numbers.add(matched.number)
            sort_key = (0, matched.number)
            nights = list(matched.state.get("nights", ()))
            entries.append((sort_key, Action("comment", matched.number, {"night": len(nights) + 1})))
            jobs = sorted(set(matched.state.get("jobs", ())) | set(finding.jobs))
            tests = sorted(set(matched.state.get("tests", ())) | {t.test_class for t in finding.tests})
            entries.append(
                (sort_key, Action("update_state", matched.number, {"nights": nights + [today_iso], "jobs": jobs, "tests": tests}))
            )
        else:
            sort_key = (1, finding.signature)

        if finding.klass == "flaky":
            entries.extend(_plan_flaky(finding, matched, ctx, settings, sort_key, today_iso))
        elif finding.klass == "infra":
            entries.extend(_plan_infra(finding, matched, ctx, settings, sort_key, today_iso))
        elif finding.klass == "forward_compat":
            entries.extend(_plan_forward_compat(finding, matched, sort_key, today_iso))
        elif finding.klass == "deterministic":
            if matched is None:
                new_entries, approved_count = _plan_deterministic_new(
                    finding, ctx, settings, approved_count, sort_key, today_iso
                )
            else:
                new_entries, approved_count = _plan_deterministic_matched(
                    matched, ctx, settings, approved_count, sort_key, today_iso
                )
            entries.extend(new_entries)

    entries.extend(_plan_close(issues_sorted, matched_numbers, ctx))

    entries.sort(key=lambda pair: pair[0])
    return [action for _, action in entries]
