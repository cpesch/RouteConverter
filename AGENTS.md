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

## 5. GPL header on every Java file

Every `.java` file under `slash.navigation.*` starts with the GPL boilerplate. Copy it verbatim from a sibling file. `@author Christian Pesch` line is convention.

## 6. Plural Java getters for repeated XML elements

XML element `<include>` (singular, repeated). JAXB binding field `include` returns `List<String>`. Interface getter is `getIncludes()` (plural). Follow this idiom — see `Source.getIncludes()` vs `SourceType.getInclude()`.

## 7. Agent working process

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

