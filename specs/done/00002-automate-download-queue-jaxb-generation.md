---
name: 00002-automate-download-queue-jaxb-generation
status: shipped
phases_done: []
phases_next: []
last_touched: 2026-06-01
---

# Issue note: automate `download` queue JAXB binding generation

## Suggested GitHub issue title

Automate JAXB binding generation for the download queue schema

## Status

Implemented on June 1, 2026.

### Implemented outcome

- `download/pom.xml` now generates JAXB bindings during Maven `generate-sources`
- generated output goes to `download/target/generated-sources/jaxb`
- the generated package remains `slash.navigation.download.queue.binding`
- committed generated Java files were removed from `download/src/main/java/slash/navigation/download/queue/binding/`
- `QueueUtil` and `QueuePersister` continued to compile unchanged against the generated bindings
- existing queue persistence regression coverage passed without adding new production-side compatibility shims

### Chosen plugin

- `org.codehaus.mojo:jaxb2-maven-plugin:3.3.0`

Reason selected:

- it already worked for the completed `datasource` migration in this repository
- it generated `jakarta.xml.bind`-compatible bindings under Java 17
- the generated API kept the expected `ObjectFactory`, `QueueType`, `DownloadType`, and `ChecksumType` surface

## Goal

Make `download/src/main/doc/queue.xsd` the single source of truth for the JAXB classes in
`slash.navigation.download.queue.binding`.

## Why this was straightforward

This module was structurally very similar to the already-completed `datasource` migration:

- one XSD: `download/src/main/doc/queue.xsd`
- one committed generated binding package: `download/src/main/java/slash/navigation/download/queue/binding/`
- one JAXB bridge utility: `download/src/main/java/slash/navigation/download/queue/QueueUtil.java`
- a small set of generated classes:
  - `QueueType`
  - `DownloadType`
  - `DownloadableType`
  - `FragmentType`
  - `ChecksumType`
  - `ObjectFactory`
  - `package-info.java`

At implementation time, the generated files still carried the old JAXB generator header comment, and `download/pom.xml` had no XJC plugin configured.

## Original state observed

- schema namespace: `http://api.routeconverter.com/v1/schemas/download-queue`
- binding package is committed under source control
- `QueueUtil` creates `JAXBContext` from `ObjectFactory.class`
- likely regression coverage exists in:
  - `download/src/test/java/slash/navigation/download/queue/QueuePersisterIT.java`
  - `download/src/test/java/slash/navigation/download/DownloadManagerIT.java`

## Implemented changes

1. added JAXB generation to `download/pom.xml`
2. generated classes from `src/main/doc/queue.xsd` into `target/generated-sources/jaxb`
3. kept the package name `slash.navigation.download.queue.binding`
4. removed the committed Java files from `src/main/java/slash/navigation/download/queue/binding/`
5. verified `QueueUtil` and `QueuePersister` still compile unchanged
6. relied on the existing `QueuePersisterIT` round-trip coverage because it already exercises save/load queue persistence through JAXB

## Validation plan

Minimum validation:

```zsh
cd /Users/christian.pesch/IdeaProjects/RouteConverter
source ~/.sdkman/bin/sdkman-init.sh
sdk use java 17.0.19-tem
./mvnw -pl download -am -DskipTests compile
./mvnw -pl download -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=slash.navigation.download.queue.QueuePersisterIT -Dskip.integration.tests=true test
```

Stronger validation:

```zsh
cd /Users/christian.pesch/IdeaProjects/RouteConverter
source ~/.sdkman/bin/sdkman-init.sh
sdk use java 17.0.19-tem
./mvnw -pl download -am clean test -Dskip.integration.tests=true
```

### Validation results

These validations passed during implementation:

- `./mvnw -pl download generate-sources`
- `./mvnw -pl download -am -DskipTests compile`
- `./mvnw -pl download -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=slash.navigation.download.queue.QueuePersisterIT -Dskip.integration.tests=true test`
- `./mvnw -pl download -am clean test -Dskip.integration.tests=true`

## Compatibility checks

Reviewers should confirm:

- package name stays `slash.navigation.download.queue.binding`
- `ObjectFactory` still exposes the factory methods used by `QueueUtil`
- `QueueType` remains the queue root object
- `QueueUtil.unmarshal()` and `QueueUtil.marshal()` still work unchanged
- existing persisted queue XML still round-trips successfully

## Suggested next follow-up step

Apply the same `generate-sources` migration pattern to the next JAXB-backed schema module, using the `datasource` and `download` migrations as the stable reference implementations.

