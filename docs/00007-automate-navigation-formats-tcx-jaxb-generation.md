# Issue note: automate `navigation-formats` JAXB generation for TCX

## Suggested GitHub issue title

Automate JAXB binding generation for `navigation-formats` TCX schemas

## Status

Proposed on June 1, 2026.

## Goal

Make these XSDs the single sources of truth for the TCX binding packages:

- `navigation-formats/src/main/doc/tcx/TrainingCenterDatabasev1.xsd` ? `slash.navigation.tcx.binding1`
- `navigation-formats/src/main/doc/tcx/TrainingCenterDatabasev2.xsd` ? `slash.navigation.tcx.binding2`

Potentially also include the extension schemas in the same generation setup:

- `ActivityExtensionv1.xsd`
- `ActivityExtensionv2.xsd`

## Why this is a good follow-up candidate

TCX is larger than LMX and FPL, but it is still clearly schema-driven and already partitioned into dedicated binding packages:

- `navigation-formats/src/main/java/slash/navigation/tcx/binding1/`
- `navigation-formats/src/main/java/slash/navigation/tcx/binding2/`

The committed classes still carry old JAXB generator headers, and the binding packages are used through a small number of TCX-specific entry points such as `slash.navigation.tcx.Tcx1Format`.

There is already at least one TCX-focused test:

- `navigation-formats/src/test/java/slash/navigation/tcx/TcxFormatTest.java`

## Why this is a separate action

This is still tractable, but it is materially larger than the LMX/FPL opportunity and may require more care around:

- multiple schemas
- package naming
- extension schema inclusion
- generated type compatibility across TCX v1 and v2

That makes it a better standalone follow-up note than bundling it into the simpler `navigation-formats` action.

## Proposed implementation

1. add TCX JAXB generation to `navigation-formats/pom.xml`
2. generate bindings for both TCX schema versions during `generate-sources`
3. preserve package names `slash.navigation.tcx.binding1` and `slash.navigation.tcx.binding2`
4. remove the committed generated Java files from both binding directories
5. verify the TCX format classes still compile without caller-side rewrites
6. validate parsing and serialization behavior with the current TCX tests

## Validation plan

Minimum validation:

```zsh
cd /Users/christian.pesch/IdeaProjects/RouteConverter
source ~/.sdkman/bin/sdkman-init.sh
sdk use java 17.0.19-tem
./mvnw -pl navigation-formats -am -DskipTests compile
```

Suggested focused tests:

```zsh
cd /Users/christian.pesch/IdeaProjects/RouteConverter
source ~/.sdkman/bin/sdkman-init.sh
sdk use java 17.0.19-tem
./mvnw -pl navigation-formats -am -Dskip.integration.tests=true -Dtest=slash.navigation.tcx.TcxFormatTest test
```

## Compatibility checks

Reviewers should confirm:

- both TCX package names stay unchanged
- root types and list accessors retain their current names
- generated classes still work with the existing TCX format code
- TCX read/write behavior remains stable for current tests and fixtures
- extension schema handling is explicitly verified during implementation

## Suggested next step after approval

Attempt this only after the lower-risk `navigation-formats` LMX/FPL automation is in place, since the TCX setup is broader and will benefit from an already-proven multi-schema generation pattern.

