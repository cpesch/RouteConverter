"""Classify a red nightly ``build.yml`` run into deduplicated findings.

Turns the raw per-job results of a scheduled run (plus an optional re-run,
used to tell flaky failures from deterministic ones) into a sorted list of
:class:`nightly_model.Finding`. Pure functions, no network access and no
``gh`` calls — chunk 4/5 (CLI + ``gh`` layer) wires this to live data.

Python 3 stdlib only.
"""

import hashlib
import re
import xml.etree.ElementTree as ET

from nightly_model import Finding, INFRA_PATTERNS, TestFailure

FAILED_CONCLUSIONS = ("failure", "cancelled", "timed_out")

# A "cancelled" job is only an artifact of a sibling's fail-fast abort when
# another job in the same batch has one of these conclusions AND the
# cancelled job itself never produced real log output (fail-fast aborts a
# job before/soon after it starts); a lone or manually cancelled job that
# was actually running (it has log content) is still a genuine failure to
# triage even if an unrelated job happened to fail in the same run.
_TRUE_FAILURE_CONCLUSIONS = ("failure", "timed_out")

_FRAME_RE = re.compile(r"at (slash\.[\w.]+)\.\w+\(")

_GITHUB_TIMESTAMP_RE = re.compile(r"^\S+Z ")
_ANSI_ESCAPE_RE = re.compile(r"\x1b\[[0-9;]*[A-Za-z]")
_PATH_RE = re.compile(r"\S*[/\\]\S*")
_HEX_RUN_RE = re.compile(r"\b[0-9a-fA-F]{7,}\b")
_DIGIT_RUN_RE = re.compile(r"\d+")

_MAVEN_GOAL_RE = re.compile(
    r"Failed to execute goal [\w.-]+:[\w.-]+:[\w.-]+:(\w+)\s*(?:\([^)]*\))?\s*on project ([\w.-]+)"
)

_ZERO_WIDTH_SPACE = "​"


def parse_surefire_reports(xml_texts):
    """Parse JUnit ``TEST-*.xml`` texts into failed/errored testcases.

    Unparseable XML is skipped rather than raised, since a truncated or
    corrupted artifact must not abort the whole triage run.
    """
    failures = []
    for xml_text in xml_texts:
        try:
            root = ET.fromstring(xml_text)
        except ET.ParseError:
            continue
        for testcase in root.iter("testcase"):
            problem = testcase.find("failure")
            if problem is None:
                problem = testcase.find("error")
            if problem is None:
                continue
            exception = problem.get("type")
            if not exception:
                message = problem.get("message", "")
                tokens = message.split()
                exception = tokens[0] if tokens else ""
            failures.append(
                TestFailure(
                    test_class=testcase.get("classname", ""),
                    method=testcase.get("name", ""),
                    exception=exception,
                    frames=_extract_frames(problem.text or ""),
                )
            )
    return failures


def _extract_frames(text):
    frames = []
    for match in _FRAME_RE.finditer(text):
        frame = match.group(1)
        if frame not in frames:
            frames.append(frame)
    return tuple(frames)


def is_infra(job):
    if job.failed_step is not None and not job.failed_step.endswith("with Maven"):
        return True
    return any(pattern.search(job.log) for pattern in INFRA_PATTERNS)


def _normalise_error_line(line):
    line = _GITHUB_TIMESTAMP_RE.sub("", line)
    line = _ANSI_ESCAPE_RE.sub("", line)
    line = _PATH_RE.sub("<path>", line)
    line = _HEX_RUN_RE.sub("<hash>", line)
    line = _DIGIT_RUN_RE.sub("N", line)
    return line


def maven_error(log):
    """Return ``(module, goal, normalised first [ERROR] line)`` from a build log."""
    error_line = None
    for line in log.splitlines():
        if "[ERROR]" in line:
            error_line = _normalise_error_line(line.rstrip())
            break

    module = None
    goal = None
    match = _MAVEN_GOAL_RE.search(log)
    if match:
        goal = match.group(1)
        module = match.group(2)

    return module, goal, error_line


def signature(tests, module, goal, error_line):
    if tests:
        first = min(tests, key=lambda t: (t.test_class, t.method))
        key = f"test|{first.exception}|{first.test_class}|{first.method}"
    else:
        key = f"build|{module or ''}|{goal or ''}|{error_line or ''}"
    return hashlib.sha1(key.encode("utf-8")).hexdigest()[:12]


def excerpt(log, max_lines=80):
    lines = log.splitlines()
    start = None
    for i, line in enumerate(lines):
        if "[ERROR]" in line or "FAILURE!" in line:
            start = i
            break
    selected = lines[-max_lines:] if start is None else lines[start : start + max_lines]

    cleaned = []
    for line in selected:
        line = _GITHUB_TIMESTAMP_RE.sub("", line)
        line = _ANSI_ESCAPE_RE.sub("", line)
        cleaned.append(line)

    text = "\n".join(cleaned)
    text = text.replace("```", "'''")
    text = text.replace("@", "@" + _ZERO_WIDTH_SPACE)
    return text


def _is_failed(job):
    return job.conclusion in FAILED_CONCLUSIONS


def _build_finding(klass, job, reports):
    tests = tuple(
        sorted(
            parse_surefire_reports(reports.get(job.name, [])),
            key=lambda t: (t.test_class, t.method),
        )
    )
    module, goal, error_line = maven_error(job.log)
    return Finding(
        klass=klass,
        signature=signature(tests, module, goal, error_line),
        jobs=(job.name,),
        tests=tests,
        module=module,
        goal=goal,
        error_line=error_line,
        excerpt=excerpt(job.log),
    )


def _merge(findings):
    merged = {}
    for finding in findings:
        key = (finding.klass, finding.signature)
        existing = merged.get(key)
        if existing is None:
            merged[key] = finding
            continue
        merged[key] = Finding(
            klass=existing.klass,
            signature=existing.signature,
            jobs=tuple(sorted(set(existing.jobs) | set(finding.jobs))),
            tests=tuple(
                sorted(
                    set(existing.tests) | set(finding.tests),
                    key=lambda t: (t.test_class, t.method),
                )
            ),
            module=existing.module,
            goal=existing.goal,
            error_line=existing.error_line,
            excerpt=existing.excerpt,
        )
    return sorted(merged.values(), key=lambda f: (f.klass, f.signature))


def _fail_fast_cancelled_names(jobs):
    """Names of jobs cancelled only because a sibling in the same run failed."""
    return {
        job.name
        for job in jobs
        if job.conclusion == "cancelled"
        and not job.log.strip()
        and any(other.conclusion in _TRUE_FAILURE_CONCLUSIONS for other in jobs if other is not job)
    }


def classify(workflow, first, rerun, reports):
    findings = []
    fail_fast_cancelled = _fail_fast_cancelled_names(first)

    if workflow == "jdk-ea-canary":
        for job in first:
            if job.name in fail_fast_cancelled:
                continue
            if _is_failed(job):
                findings.append(_build_finding("forward_compat", job, reports))
        return _merge(findings)

    rerun_by_name = {job.name: job for job in rerun} if rerun is not None else None
    for job in first:
        if job.name in fail_fast_cancelled:
            continue
        if not _is_failed(job):
            continue
        if rerun_by_name is None:
            klass = "infra" if is_infra(job) else "deterministic"
        else:
            rerun_job = rerun_by_name.get(job.name)
            if rerun_job is None:
                klass = "infra" if is_infra(job) else "deterministic"
            elif not _is_failed(rerun_job):
                klass = "flaky"
            elif is_infra(rerun_job):
                klass = "infra"
            else:
                klass = "deterministic"
        findings.append(_build_finding(klass, job, reports))

    return _merge(findings)
