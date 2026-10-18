"""Data model for the nightly failure-triage pipeline.

Shared, logic-free dataclasses describing a scheduled workflow run:
the raw per-job result (:class:`JobResult`), a parsed JUnit failure
(:class:`TestFailure`), and a classified, deduplicated finding
(:class:`Finding`). ``nightly_classify.py`` builds these; later chunks
(issue rendering, the decision engine, the CLI) consume them.

Python 3 stdlib only.
"""

import re
from dataclasses import dataclass

CLASSES = ("flaky", "infra", "forward_compat", "deterministic")

# Sentinel returned by the re-run poll when its deadline passes before the
# re-run completes; distinct from ``None`` ("no re-run") and from a job list.
TIMEOUT = object()

INFRA_PATTERNS = (
    re.compile(r"Could not transfer artifact"),
    re.compile(r"status code: 5\d\d"),
    re.compile(r"Connection reset"),
    re.compile(r"lost communication with the server"),
    re.compile(r"The operation was canceled"),
    re.compile(r"RATE_LIMITED"),
    re.compile(r"Unable to download"),
    re.compile(r"Unable to connect to source"),
)


@dataclass(frozen=True)
class JobResult:
    name: str
    conclusion: str
    failed_step: str | None
    log: str


@dataclass(frozen=True)
class TestFailure:
    test_class: str
    method: str
    exception: str
    frames: tuple[str, ...]


@dataclass(frozen=True)
class Finding:
    klass: str
    signature: str
    jobs: tuple[str, ...]
    tests: tuple[TestFailure, ...]
    module: str | None
    goal: str | None
    error_line: str | None
    excerpt: str
    workflow: str = "build"
