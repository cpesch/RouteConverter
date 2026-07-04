---
name: 00003-automate-route-catalog-jaxb-generation
status: shipped
phases_done: []
phases_next: []
last_touched: 2026-06-01
---

# Issue note: automate `route-catalog` JAXB binding generation

## Suggested GitHub issue title

Automate JAXB binding generation for the route catalog schema

## Status

Implemented on June 1, 2026.

### Implemented outcome

- `route-catalog/pom.xml` now generates JAXB bindings during Maven `generate-sources`
- generated output goes to `route-catalog/target/generated-sources/jaxb`
- the generated package remains `slash.navigation.routes.remote.binding`
- committed generated Java files were removed from `route-catalog/src/main/java/slash/navigation/routes/remote/binding/`
- `RemoteCatalog`, `RemoteCategory`, `RemoteRoute`, and `RoutesUtil` continued to compile unchanged against the generated bindings
- a focused JAXB round-trip regression test was added in `route-catalog/src/test/java/slash/navigation/routes/remote/helpers/RoutesUtilTest.java`

### Chosen plugin

- `org.codehaus.mojo:jaxb2-maven-plugin:3.3.0`

Reason selected:

- it is already used successfully for the completed `datasource` and `download` migrations in this repository
- it generated `jakarta.xml.bind`-compatible bindings under Java 17
- the generated API preserved the expected `CatalogType`, `CategoryType`, `RouteType`, `FileType`, and `ObjectFactory` surface

## Goal

Make `route-catalog/src/main/doc/route-catalog.xsd` the single source of truth for the JAXB classes in
`slash.navigation.routes.remote.binding`.

## Why this was straightforward

This module was another small catalog-style schema with a compact generated package:

- one XSD: `route-catalog/src/main/doc/route-catalog.xsd`
- one committed generated binding package: `route-catalog/src/main/java/slash/navigation/routes/remote/binding/`
- small generated surface:
  - `CatalogType`
  - `CategoryType`
  - `RouteType`
  - `FileType`
  - `ObjectFactory`
  - `package-info.java`
- one JAXB helper: `route-catalog/src/main/java/slash/navigation/routes/remote/helpers/RoutesUtil.java`

At implementation time, the committed binding classes still carried generated headers, while `route-catalog/pom.xml` had no JAXB generation configured.

## Original state observed

- schema namespace: `http://api.routeconverter.com/v1/schemas/route-catalog`
- `RoutesUtil` bootstraps JAXB via `ObjectFactory.class`
- binding classes are used directly by:
  - `RemoteCatalog`
  - `RemoteCategory`
  - `RemoteRoute`
  - integration tests under `route-catalog/src/test/java/slash/navigation/routes/remote/`

## Implemented changes

1. added JAXB generation to `route-catalog/pom.xml`
2. generated bindings from `src/main/doc/route-catalog.xsd` into `target/generated-sources/jaxb`
3. preserved package name `slash.navigation.routes.remote.binding`
4. removed the committed generated Java files from `src/main/java/slash/navigation/routes/remote/binding/`
5. verified the existing remote model classes still compile with no API drift
6. added a focused `RoutesUtilTest` round-trip regression test for XML stability because the existing remote tests depend on a live server

## Validation plan

Minimum validation:

```zsh
cd /Users/christian.pesch/IdeaProjects/RouteConverter
source ~/.sdkman/bin/sdkman-init.sh
sdk use java 17.0.19-tem
./mvnw -pl route-catalog -am -DskipTests compile
```

Suggested focused tests:

```zsh
cd /Users/christian.pesch/IdeaProjects/RouteConverter
source ~/.sdkman/bin/sdkman-init.sh
sdk use java 17.0.19-tem
./mvnw -pl route-catalog -am -Dskip.integration.tests=true test
```

### Validation results

These validations passed during implementation:

- `./mvnw -pl route-catalog generate-sources`
- `./mvnw -pl route-catalog -am -DskipTests compile`
- `./mvnw -pl route-catalog -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=slash.navigation.routes.remote.helpers.RoutesUtilTest,slash.navigation.routes.remote.TempFileIT test`
- `./mvnw -pl route-catalog -am clean test -Dsurefire.failIfNoSpecifiedTests=false -Dtest=slash.navigation.routes.remote.helpers.RoutesUtilTest,slash.navigation.routes.remote.TempFileIT`

## Compatibility checks

Reviewers should confirm:

- package name stays `slash.navigation.routes.remote.binding`
- `CatalogType`, `CategoryType`, `RouteType`, and `FileType` retain their current accessor names
- `ObjectFactory` still matches what `RoutesUtil` expects
- current route catalog XML still unmarshals and marshals correctly
- no caller-side rewrites are needed in `RemoteCatalog`, `RemoteCategory`, or `RemoteRoute`

## Suggested next follow-up step

Apply the same Maven/XJC pattern already proven in `datasource`, `download`, and now `route-catalog` to the next JAXB-backed schema module.

