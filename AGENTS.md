# AGENTS.md

Essential context for agents working in this repository.

## 1. Java via sdkman, not system

`java` / `javac` are not on `PATH` by default. Before any `mvn` call:

```sh
source ~/.sdkman/bin/sdkman-init.sh && sdk use java 17.0.19-tem
```

Project targets Java 17.

## 2. Datasource schema location

Single source of truth for the catalog wire format:
[datasource/src/main/doc/datasource-catalog.xsd](datasource/src/main/doc/datasource-catalog.xsd)

Namespace: `http://api.routeconverter.com/v1/schemas/datasource-catalog`.

## 3. Server is a separate codebase

Server hosts `https://api.routeconverter.com/` — **not in this repo**. This repo defines the wire schema + client tools. Server-side changes require coordination (see [download-tools/SCAN_SERVER.md](download-tools/SCAN_SERVER.md)).

## 4. Three catalog tools in `download-tools/`

- `SnapshotCatalog` — pulls server XMLs into local snapshot.
- `ScanWebsite` — crawls HTML index, populates `<file>/<map>/<theme>` URIs.
- `UpdateCatalog` — pushes metadata (checksums, bounding boxes) back to server.

## Repository implementation conventions

### 5. GPL header on every Java file

Every `.java` file under `slash.navigation.*` starts with the GPL boilerplate. Copy it verbatim from a sibling file. `@author Christian Pesch` line is convention.

### 6. Plural Java getters for repeated XML elements

XML element `<include>` (singular, repeated). JAXB binding field `include` returns `List<String>`. Interface getter is `getIncludes()` (plural). Follow this idiom — see `Source.getIncludes()` vs `SourceType.getInclude()`.

### 7. IntelliJ GUI Designer: don't hand-edit `$$$setupUI$$$`

A `*.form` file is the canonical layout source; the matching `$$$setupUI$$$` block in the Java file is regenerated from it. Hand edits to the Java block drift from the form and are overwritten on next regen. To change layout, edit the `.form` in IntelliJ. To hide a widget without touching the layout, call `setVisible(false)` at runtime.

### 8. `./mvnw -pl <module>` needs `-am`

Without `-am` ("also make"), sibling-module dependencies fail to resolve with cached "could not find artifact slash.navigation:X" errors. Always pass `-am` when building or testing a single module. Add `-U` to bust the negative dependency cache after dependency changes.

### 9. Use real JAXB binding objects in tests, not mock interfaces

When testing code that consumes `DataSource` / `Source` / `File` / `Map` / `Theme`, construct real binding objects (`new ObjectFactory().createDatasourceType()`, set fields, wrap in `new DataSourceImpl(...)`). Mocking the interfaces explodes into hand-rolled stubs covering 10+ methods (e.g. `getDownloadable`, `getFragmentBySHA1`) that the test doesn't care about. Real bindings are zero-arg and let tests focus on the field combinations under exercise — see `WgetCommandBuilderTest` for the pattern.

### 10. Be explicit about integration-test naming and scope

The repository currently distinguishes between small deterministic tests and integration-style tests, but naming must stay aligned with the Maven configuration.

- If a test is intended to run as part of the normal integration-test flow, its filename must match the configured Failsafe convention.
- When changing the convention, prefer updating Maven includes deliberately rather than silently introducing a second naming scheme.
- Hermetic integration tests (temporary files, sample data, multi-module interactions without live services) are good candidates for normal coverage runs.
- External live-service tests (real HTTP services, externally hosted files, credentials, mutable remote state) must stay opt-in and must not be assumed to run in normal coverage builds.
- For routine coverage-related integration runs, prefer the hermetic Maven profiles over the broader live-service-capable ones.
- When adding or reviewing `*IT`-style tests, classify them explicitly as hermetic or external so coverage expectations stay clear.

## 11. Agent working process

- Continue autonomously when the next step is reversible and strongly implied by repository context.
- Do not stop for review unless there is a real product, compatibility, or architectural decision to make.
- When presenting options, always include a recommendation and briefly explain why it is preferred.
- Treat user corrections on naming, file placement, scope, and documentation style as standing preferences for the rest of the task.
- When recurring workflow or collaboration improvements become clear during a task, propose them explicitly and ask whether they should be added to `AGENTS.md`.
- Classify written output explicitly:
  - `AGENTS.md` for stable agent instructions
  - `docs/` for issue notes, proposals, migration plans, and design records
  - conversation-only for temporary exploration
- Prefer durable documentation in `docs/` with numbered, issue-like filenames when creating new long-lived notes.


