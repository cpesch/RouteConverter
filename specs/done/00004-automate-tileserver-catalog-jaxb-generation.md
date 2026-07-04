---
name: 00004-automate-tileserver-catalog-jaxb-generation
status: shipped
phases_done: []
phases_next: []
last_touched: 2026-06-01
---

# Issue note: automate `tileserver-maps` JAXB binding generation

## Suggested GitHub issue title

Automate JAXB binding generation for the tile server catalog schemas

## Status

Implemented on June 1, 2026.

### Implemented outcome

- `tileserver-maps/pom.xml` now generates JAXB bindings during Maven `generate-sources`
- generated output goes to `tileserver-maps/target/generated-sources/jaxb`
- the generated packages remain:
  - `slash.navigation.maps.tileserver.bindingmap`
  - `slash.navigation.maps.tileserver.bindingoverlay`
- committed generated Java files were removed from both binding directories under `tileserver-maps/src/main/java/`
- `MapServerUtil`, `OverlayServerUtil`, `TileServerService`, and `TileServerMapManager` continued to compile against the generated bindings
- focused JAXB unmarshalling regression tests were added in:
  - `tileserver-maps/src/test/java/slash/navigation/maps/tileserver/helpers/MapServerUtilTest.java`
  - `tileserver-maps/src/test/java/slash/navigation/maps/tileserver/helpers/OverlayServerUtilTest.java`

### Chosen plugin

- `org.codehaus.mojo:jaxb2-maven-plugin:3.3.0`

Reason selected:

- it is already used successfully for the completed JAXB migrations in this repository
- it generated `jakarta.xml.bind`-compatible bindings under Java 17
- it preserved the expected packages and `ObjectFactory`-based JAXB bootstrap used by the helper utilities

## Goal

Make these XSDs the single sources of truth for the tile server catalog bindings:

- `tileserver-maps/src/main/doc/mapserver-catalog.xsd`
- `tileserver-maps/src/main/doc/overlayserver-catalog.xsd`

Target generated packages:

- `slash.navigation.maps.tileserver.bindingmap`
- `slash.navigation.maps.tileserver.bindingoverlay`

## Why this looks easy

This module has two very small catalog schemas and two correspondingly tiny committed binding packages.

Current generated-looking packages:

- `tileserver-maps/src/main/java/slash/navigation/maps/tileserver/bindingmap/`
- `tileserver-maps/src/main/java/slash/navigation/maps/tileserver/bindingoverlay/`

Current consumers are narrow and easy to validate:

- `MapServerUtil`
- `OverlayServerUtil`
- `TileServerService`
- `TileServerMapManager`
- `TileServerMapManagerTest`

## Original state observed

Namespaces:

- map server: `http://api.routeconverter.com/v1/schemas/mapserver-catalog`
- overlay server: `http://api.routeconverter.com/v1/schemas/overlayserver-catalog`

At implementation time, `tileserver-maps/pom.xml` had no JAXB generation configured, and the committed binding classes still carried generated headers.

## Implemented changes

1. added JAXB generation to `tileserver-maps/pom.xml` for both schemas
2. generated both schema sets during `generate-sources`
3. wrote output under `target/generated-sources/jaxb`
4. preserved the existing package names for both binding families
5. removed the committed generated Java files from both binding directories
6. aligned callers with the generated `active` accessor shape, which remains `isActive()`
7. added focused JAXB unmarshalling tests for both catalog types
8. verified the JAXB helper utilities and service layer compile unchanged apart from the generated-source transition

## Validation plan

Minimum validation:

```zsh
cd /Users/christian.pesch/IdeaProjects/RouteConverter
source ~/.sdkman/bin/sdkman-init.sh
sdk use java 17.0.19-tem
./mvnw -pl tileserver-maps -am -DskipTests compile
./mvnw -pl tileserver-maps -am -Dskip.integration.tests=true test
```

## Compatibility checks

Reviewers should confirm:

- both package names remain stable
- `CatalogType`, `MapServerType`, and `OverlayServerType` preserve their accessor names
- `ObjectFactory` remains compatible with `MapServerUtil` and `OverlayServerUtil`
- `TileServerService` still unmarshals both catalog types successfully
- `TileServerMapManagerTest` remains green after the switch

### Validation results

These validations passed during implementation:

- `./mvnw -pl tileserver-maps -am clean test`

Observed results:

- duplicate-class compilation failures disappeared after removing the committed generated bindings
- the current JAXB generation still exposes `Boolean active` via `isActive()` for both `MapServerType` and `OverlayServerType`
- `TileServerMapManagerTest`, `MapServerUtilTest`, and `OverlayServerUtilTest` all passed

## Suggested next follow-up step

Automate both schemas in one PR because they live in the same module and share the same validation path.

