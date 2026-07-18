# QUALITY.md — RouteConverter

Per-repo quality grades for the `scorer` minion (specs/00004 §2), aggregated into
[`rc-meta/QUALITY.md`](https://git.routeconverter.com/rc/rc-meta/src/branch/main/QUALITY.md).
Grades A (production-ready) → F (broken).

_Last reviewed: 2026-07-18 (scorer shadow dry-run, run 11575, confidence 0.75)._

**Headline:** Release pipeline hardening (checksum validation, hosted JRE 21, prerelease catalog publishing) earns an upgrade, but CI pass rate slipping to 76% over 560 runs is worth watching.

| Dimension | Grade | Evidence |
|-----------|-------|----------|
| Test coverage | C | Held at baseline C — no evidence moved it this cycle (see reasoning). |
| Error handling | C | Held at baseline C — no evidence moved it this cycle (see reasoning). |
| Release reproducibility | B | C → B. Multiple concrete reproducibility improvements landed this window: download checksum validation against known-good hashes, hosted JRE 21.0.11 artifacts published, and both SnapshotCatalog.jar and UpdateCatalog.jar now flow to a prerelease channel with credentials sourced from env vars or a password file rather than hardcoded values. |
| Documentation | C | Held at baseline C — no evidence moved it this cycle (see reasoning). |
| Dependency freshness | C | Held at baseline C — no evidence moved it this cycle (see reasoning). |

## Scorer reasoning

release_reproducibility moves C to B on direct evidence of pinned/verified artefact publishing and credential-handling cleanup in the release tooling, even though the release_artefacts_30d counter reads 0 (likely undercounting prerelease/snapshot publishes rather than reflecting no activity). test_coverage stays C: commits reference added coverage (#154) and an i18n key-parity test, but test_files_count is reported as 0/not-measured-v0, so the instrumentation can't yet substantiate a grade change. error_handling stays C — checksum validation and env/file-based credential handling are positive signals but too narrow to justify a broad upgrade without visibility into exception-handling patterns elsewhere. documentation stays C: README is thin (460 words) and spec-driven docs commits reflect process discipline but not user-facing documentation depth. dependency_freshness stays C by default — dep_manifests is empty, giving no basis for a grade change in either direction.

> Seeded from the scorer shadow dry-run (run 11575). Shadow mode does not
> auto-write this file yet (live in v0.1); until then it is a hand-maintained
> baseline the scorer reads as `previous_grades`.
