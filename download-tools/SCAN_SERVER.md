# Server requirements: serve `<source>` in datasource XML

Companion to [SCAN_CLIENT.md](SCAN_CLIENT.md). Captures what the server side (`api.routeconverter.com`) must do so the client side can drop `catalog-sources.zsh` + `catalog-mirror-jobs.zsh`.

## Status

**Implemented + deployed to production.** Migrations `datasources.0003_source` + `datasources.0004_administrators_group` applied. Source data populated via `MigrateCatalogSources`. Tools updated to send `?includeSource=true`. See [docs/00008-server-plan-backward-compatible-datasource-source-for-3-3-clients.md](../docs/00008-server-plan-backward-compatible-datasource-source-for-3-3-clients.md) for the RouteConverter 3.3 compatibility gate.

## Context

- Server is a separate codebase (not in this repo) hosted at `https://api.routeconverter.com/`.
- It owns the canonical datasource XML files. Tools fetch them via `SnapshotCatalog` and post updates via `UpdateCatalog`.
- Endpoint base: `V1 + "datasources/"` → `/v1/datasources/<id>.xml` (see `DataSourceManager.DATASOURCES_URI` in `datasource/src/main/java/slash/navigation/datasources/DataSourceManager.java:56`).
- Wire schema lives at `datasource/src/main/doc/datasource-catalog.xsd` (this repo). Server must accept the same XSD.

## Scope

Server must serve, persist, and (optionally) edit `<source>` child element on `<datasource>`. New schema described in [SCAN_CLIENT.md](SCAN_CLIENT.md). `<source>` carries scan + mirror config in one element. Presence = scan + mirror enabled. All attributes optional: `url` falls back to `datasource@baseUrl`. Server does **not** execute scan or mirror — those run on the client. Server only stores config + serves it back.

## Requirements

### R1 — Schema acceptance (mandatory)

- Server must accept incoming datasource XML containing `<source>` per new XSD.
- Server must validate against new XSD on write.
- Server must reject XML that violates `unique-datasource-id`.
- `<source>` has no required attributes — empty `<source/>` is valid.
- Backward compatibility: XML without `<source>` must continue to validate and load (`minOccurs=0`).

### R2 — Round-trip preservation (mandatory)

- Server must persist all attributes of `<source>`: `url`, `level`.
- Server must persist child elements verbatim: `<source><include>`, `<source><exclude>`.
- Element order, whitespace, and namespace prefixes do not need to be byte-identical, but content must round-trip without loss.
- Empty `<source/>` must round-trip as `<source/>` or equivalent (presence is the marker that scan + mirror are enabled).

### R3 — Read API (mandatory)

- `GET /v1/datasources/<id>.xml` **strips `<source>` by default** (RouteConverter 3.3 compat — JAXB rejects unknown elements). Tools opt in via `?includeSource=true`.
- `GET /v1/datasources/<id>.xml?includeSource=true` returns the full datasource document including `<source>` when present.
- `GET /v1/datasources/` (index/list, if exposed) returns IDs unchanged. No new endpoint required. List serializer never emits `<source>`.
- Snapshot generation includes the new element when the request opts in. `SnapshotCatalog` client must append `?includeSource=true` on every fetch (see [SCAN_CLIENT.md](SCAN_CLIENT.md) Background → Server backward-compat).

**Status:** delivered. `datasources/views.py:retrieve` strips `data['source']` unless `request.query_params.get('includeSource') == 'true'`. PUT path accepts `<source>` regardless.

### R4 — Write API (mandatory)

- Existing `UpdateCatalog` flow continues to work. It currently updates `<file>/<map>/<theme>` URIs + checksums. Must not strip `<source>` while updating downloadables.
- Partial-update endpoints (referenced as `updatePartially` in `download-tools/src/main/java/slash/navigation/download/tools/UpdateCatalog.java:316`) must merge, not replace — they preserve unrelated subtrees including `<source>`.
- A full-document `PUT /v1/datasources/<id>.xml` (or equivalent) is required for the one-off migration step (writing `<source>` for the first time). If one already exists, no work needed. If only partial endpoints exist, add a full PUT or extend the partial API to cover the new element.

**Status:** delivered. `datasources/views.py:create_file` calls `upsert_source` after downloadables — merge semantics. Omitting `<source>` in PUT body = no-op (preserve existing). Wrapped in `transaction.atomic()`. Source deletion via Django admin UI only (Q1 decision).

### R5 — Authorization

- `<source>` is operational config, not user data. Write requires membership in the `Administrators` Django group (auto-provisioned by migration `0004_administrators_group` with `add/change/delete/view` perms for `source` + `sourcepattern`).
- Non-admin PUT carrying `<source>` → HTTP 403, full request rejected (Q5 decision).
- Read endpoints: anonymous as before. `?includeSource=true` requires no auth.

**Status:** delivered. `SourceWritePermission.check(user)` gate in `datasources/views.py:create_file` runs before any DB mutation.

### R6 — Migration support

- Server accepts bulk PUT of edited XMLs (existing UpdateCatalog flow). No new endpoint needed.
- Backup: skipped server-side per Q3 decision. Handled externally by `config/backup...` scripts + filesystem snapshot/VCS.
- Migration tool (`MigrateCatalogSources`) runs against local snapshot, writes back via existing UpdateCatalog flow.

**Status:** delivered. Migration tool ran successfully — Source rows populated for automated ids.

### R7 — Schema versioning

- Namespace `http://api.routeconverter.com/v1/schemas/datasource-catalog` is reused (additive change, no breaking).
- If server enforces XSD by URL, host updated XSD at the same URL.
- **Correction**: prior assumption that "JAXB ignores unknown elements by default — safe" was wrong. Deployed RouteConverter 3.3 uses a plain JAXB `Unmarshaller` with no custom event handler — it rejects `<source>`. Mitigation: server gates `<source>` behind `?includeSource=true` (see R3 + doc 00008). When a 3.4 release ships with `<source>` JAXB binding, revisit: add `X-RouteConverter-Catalog-Features: source` header support, flip default to MODERN.

**Status:** delivered via the `?includeSource=true` gate.

### R8 — Validation rules (new constraints)

Server should enforce on write:

- `<source url=…>` (when present) parseable as HTTP/HTTPS URL.
- `<source level="…">` integer ≥ 0 if present.
- `<source><include>` and `<source><exclude>` patterns: non-empty when element present. Glob form (`*.zip`, `*-latest.osm.pbf`, `*/dummy.brf`).
- `<source><exclude>` glob must not contain `..` segments (prevents arbitrary delete outside client mirror root during post-wget cleanup).

Validation may be advisory (warn, do not reject) if hardening the migration is undesirable. Decide with operator.

**Status:** delivered. Strict by default (Q2 decision). Violations → HTTP 400. Opt-out via `DATASOURCES_STRICT_SOURCE_VALIDATION = False` Django setting. Implementation in `datasources/validation.py`.

### R9 — Editing UI (optional, nice-to-have)

If server exposes a web admin UI for editing datasources:

- Form fields for `<source>` attributes (`url` optional, `level`) + include/exclude list editor.
- Toggle to add/remove `<source>` block entirely.
- Preview of derived wget command would be useful but is not required (client GUI already does this — see `download-tools/src/main/java/slash/navigation/download/tools/gui/MirrorJobCommandBuilder.java`).

**Status:** delivered minimally. Django admin in `routesite3/datasources/admin.py` exposes `SourceInline` on `DataSourceAdmin` + `SourcePatternInline` on `SourceAdmin`. No wget preview. Group `Administrators` gates write perms.

### R10 — Telemetry (optional)

- Log when `<source>` is added/removed/edited (audit trail).
- Expose count of datasources with `<source>` for the admin to confirm migration completeness.

**Status:** deferred. Not implemented. Consider when operational questions arise. Trivial add: log in `upsert_source`; count visible via `Source.objects.count()` in admin dashboard or a `count_datasources_with_source` mgmt command.

## Non-requirements

- Server does **not** execute scans or wget mirrors. Those stay client-side.
- Server does **not** schedule the workflow. Cron/launchd is a client concern.
- Server does **not** validate that `<source url>` is reachable. Client `ScanWebsite` already handles errors.
- Server does **not** maintain mirror filesystem state. Client owns the mirror tree.

## Open questions for server maintainer — RESOLVED

1. ~~Full PUT vs partial-update extension?~~ → Existing PUT merges; new `upsert_source` extends it. No new endpoint.
2. ~~Backup strategy before migration?~~ → Skipped server-side. Handled by `config/backup...` scripts + filesystem snapshot/VCS.
3. ~~Admin editing UI~~ → Django admin inlines added (R9).
4. ~~Validation strictness~~ → Strict default (Q2). Setting override.
5. ~~Datasources without `<source>`~~ → Server agnostic. Operator responsibility.

Additional Qs resolved:
- **Source deletion semantics** → PUT-omit = preserve (Q1.b). Delete via Django admin only.
- **Non-admin PUT with `<source>`** → Hard-reject whole request (Q5).
- **3.3 compat** → `?includeSource=true` opt-in (doc 00008).

## Acceptance criteria

- `GET /v1/datasources/openandromaps.xml?includeSource=true` returns XML containing `<source><include>*.zip</include></source>` after migration (url omitted, baseUrl serves as starting point). ✓
- `GET /v1/datasources/openandromaps.xml` (no query param) returns the same XML **without** `<source>` — RouteConverter 3.3 compat. ✓
- Existing `UpdateCatalog` runs against migrated datasource without dropping `<source>`. ✓ (merge semantics in `upsert_source`)
- JAXB regeneration on client produces classes that round-trip the document. ✓ (client-side)
- Old 3.3 clients reading `/v1/` continue to parse XML successfully — `<source>` stripped by default gate. ✓
- One full PUT per id available for the migration step. ✓ (existing PUT extended)
- `Administrators` Django group exists with `add/change/delete/view` perms on `source` + `sourcepattern`. ✓
- PUT carrying `<source>` from non-admin user → HTTP 403, no DB mutation. ✓

## Coordination with client plan

- Client plan in [SCAN_CLIENT.md](SCAN_CLIENT.md) assumes server has R1, R2, R3, R4, R6 in place.
- Sequence: server changes land first (R1–R6) → migration tool runs → client tooling refactor (per client plan steps 3–7) → delete shell config.
- R7 versioning means client and server can deploy independently within the same namespace.
