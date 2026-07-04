---
name: 00011-opt-in-crash-telemetry
status: proposed
phases_done: []
phases_next: []
last_touched: 2026-06-15
---

# 00011 - Crash / init-error diagnostics and opt-in telemetry

## Status

Proposed. Created on June 15, 2026. Refined June 15, 2026 (critique applied:
startup/EDT capture, bundled-JRE detection, GDPR coherence, endpoint auth,
schema versioning, offline spool, discovery step, done/kill criteria, and a
reorder making the local "copy report" the real Phase 1).

Follow-up to the 2026-06 minimized-JRE incident (httpclient5 5.6 needing
`jdk.net`, then a latent `java.scripting` gap). Those bugs reached a real user
before anyone noticed, and the only signal was a screenshot of a dialog that
showed the *secondary* error, not the root cause.

## Context

The build-time gate (`scripts/verify-runtime.sh`, ideas #1-#6) now catches the
known class of failure - a stripped JRE missing a JDK module a dependency
touches - before release. But it can only catch what it exercises:

- jdeps over-approximates, so #1/#2/#5 are advisory.
- The #4 forced-init sweep and #3/#6 smokes only cover code paths they run.
- A reflective / lazily-loaded path (e.g. a PAC-proxy script engine reached
  only on certain corporate networks) can still slip through every gate.

When something does slip through, today there is no feedback loop: the user
sees a dialog, maybe sends a screenshot, maybe not. We want latent
bundle/JRE bugs to surface from the field in hours, not on the next manual
report.

## Goal

Turn an opaque dialog screenshot into a complete, structured root-cause report,
and - only if that proves insufficient - an opt-in channel that alerts when a
*new* failure signature appears after a release. Always able to tell "our
bundle is broken" from "this user's unusual system Java".

## Discovery first (do before building anything)

RouteConverter already has a `send-error-report-log` bundle key and a
`RouteFeedback` REST path. Before writing code:

- grep the existing error-report flow; determine what it captures and sends.
- Decide extend vs. replace. Do not duplicate an existing reporting path.

This step gates the rest of the plan.

## What to capture

- Uncaught exceptions, plus the existing
  `RouteServiceOperator.handleServiceError` path.
- Priority signatures (the bundle-integrity class): `NoClassDefFoundError`,
  `ExceptionInInitializerError`, `ClassNotFoundException`, `UnsatisfiedLinkError`.
- Capture must cover the threads where these actually fire:
  - **main thread** - install a `Thread.setDefaultUncaughtExceptionHandler`
    as the *literal first statement* in `main`, before any other class loads,
    so an early init crash is still caught (see C1 below).
  - **EDT** - a plain default handler does NOT see Swing dispatch exceptions
    (and `-Dsun.awt.exception.handler` is gone in 17). Install a custom
    `EventQueue` whose `dispatchEvent` wraps in try/catch, or catch at each
    `invokeLater` boundary. The failing dialog was a GUI path - this is
    required, not optional.
- Payload, allowlist-only (mirrors the no-regex-masking rule - extract known
  fields, never scrape free text):
  - `schema_version` (first field; the wire format will evolve)
  - root-cause class + cause chain (reuse `ExceptionHelper.getMessageWithCauses`)
  - app version + build number
  - OS name/arch, `java.version`, `java.vendor` (reuse the About dialog's
    existing system-info assembly rather than re-collecting)
  - **bundled-JRE flag** - whether running the shipped stripped JRE or a
    system Java. Source it explicitly: the launcher injects a marker
    (`-Drouteconverter.bundledJre=true`) or a marker file ships in the bundle.
    Do not infer it from `java.home`.
- Explicitly NOT captured: file paths, route/track contents, file names,
  usernames, IP-derived data, any user document. The About dialog already
  masks the e-mail; apply the same discipline.

## Privacy / GDPR

- Anonymous by design: the payload above contains no personal data and no
  stable identifier. Document this explicitly - because there is nothing
  personal and nothing that ties a report to an individual, there is no
  per-user deletion obligation to honour. (Do NOT add an install id; that
  would create the personal-data and deletion burden this design avoids, and
  would also make "1 user crashing 100x" indistinguishable-by-volume anyway.)
- Server telemetry (Phase 2) is still default **off** and explicit opt-in -
  see below.

## Phase 1 - local diagnostics (the main deliverable)

Highest value, lowest cost, no consent/retention burden. Ship and validate
this first; it may be the whole project.

1. Install the main + EDT capture described above.
2. On a captured error, assemble the allowlisted payload and:
   - write it to a structured local crash file (a small spool directory), and
   - in the error dialog (built on the 2026-06 root-cause unwrap) add a
     one-click **"Copy diagnostic report"** button that puts the same payload
     on the clipboard for the user to paste into a bug report.
3. Pre-consent / pre-handler crashes: the buffered local file is offered for
   sending (or copying) on the next successful launch.

Done when: a user-reported crash arrives as a full root-cause chain + env,
not a screenshot of a truncated dialog.

## Phase 2 - opt-in server telemetry (only if Phase 1 is insufficient)

Gate this on evidence that Phase 1 reports do not arrive often enough to catch
post-release regressions. RouteConverter is niche; with default-off opt-in and
a small user base, the pipeline may never reach statistical signal, so do not
build it speculatively.

- **Consent:** default off. One-time opt-in dialog ("Send anonymous error
  reports to help fix crashes?") + a persistent preferences toggle stored in
  `Preferences`. No pre-ticked box. Short privacy note in the dialog.
- **Transport:** reuse the RouteConverter service / `RouteFeedback` REST infra;
  no new vendor or SDK. POST JSON to a `static.routeconverter.com` / service
  endpoint.
  - **Auth:** sign reports with the existing HMAC / `GH_WEBHOOK_SECRET` prior
    art (specs/00041 webhook canary). The endpoint rate-limits and size-caps to
    blunt the open-POST spam/DoS vector.
  - **Offline:** reuse the Phase 1 spool as the send queue - persist unsent
    reports, flush on the next online launch, drop after a cap. Never retry
    aggressively.
  - Fully async and fail-silent - telemetry must never block, slow, or crash
    the app.

## Phase 3 - aggregation + alert (DB-less)

- The endpoint is a tiny script: dedup by `{schema_version, version,
  root-cause class, missing-class}` and push via the existing ntfy setup
  (`factory-publisher` -> `maintainer`) when a signature not seen before a
  release appears. No database, no dashboard to start.

## Success metric / kill criteria

- **Success:** a new post-release failure signature produces an alert (Phase 3)
  or a usable local report (Phase 1) within 24h of first occurrence.
- **Kill:** if after one release cycle Phase 1 reports are sufficient to
  diagnose field issues, do not build Phase 2/3. If Phase 2 opt-in rate or
  volume stays too low to signal, disable the sender and keep Phase 1.

## Effort

- Phase 1: small. Capture hook + signature extraction reuse `ExceptionHelper`;
  the copy button and local spool are modest. EDT wrapping is the only subtle
  part.
- Phase 2/3: medium, and deferred behind the kill criteria above. Real cost is
  the consent UX, endpoint auth, and retention wording - not the client.

## Related

- `scripts/verify-runtime.sh` - the build-time gate this complements.
- `specs/00009-reduce-bundle-size-generically.md` - bundle/stripping context.
- `specs/00041-factory-reliability-hardening.md` (rc-meta) - HMAC / webhook
  secret prior art reused for Phase 2 endpoint auth.
- `ExceptionHelper.getRootCause` / `getMessageWithCauses` - reused for payloads.
