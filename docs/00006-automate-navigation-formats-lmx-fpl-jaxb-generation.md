# Issue note: automate `navigation-formats` JAXB generation for LMX and FPL

## Suggested GitHub issue title

Automate JAXB binding generation for `navigation-formats` LMX and FPL schemas

## Status

Implemented on June 3, 2026.

## Goal

Make these XSDs the single sources of truth for their binding packages:

- `navigation-formats/src/main/doc/lmx/lmx.xsd` ? `slash.navigation.lmx.binding`
- `navigation-formats/src/main/doc/fpl/fpl.xsd` ? `slash.navigation.fpl.binding`

## Why these look easy

These are good first `navigation-formats` candidates because they are narrower than GPX or KML and already have small, isolated JAXB bridge layers.

Current consumers:

- LMX:
  - `NokiaLandmarkExchangeFormat`
  - `NokiaLandmarkExchangeUtil`
  - `NokiaLandmarkExchangeReadWriteRoundtripIT`
- FPL:
  - `GarminFlightPlanUtil`
  - `GarminFlightPlanFormat`

Current generated-looking binding packages:

- `navigation-formats/src/main/java/slash/navigation/lmx/binding/`
- `navigation-formats/src/main/java/slash/navigation/fpl/binding/`

The committed binding files still carry old JAXB generator headers, and `navigation-formats/pom.xml` has no JAXB generation configured.

## Why this is separate from GPX/KML

GPX and KML contain many more schemas, imported schemas, and extension namespaces. LMX and FPL are smaller and are better first candidates for proving a multi-schema generation setup inside `navigation-formats`.

## Implemented changes

1. added JAXB generation to `navigation-formats/pom.xml`
2. generate LMX and FPL bindings during `generate-sources` into `target/generated-sources/jaxb`
3. preserved package names `slash.navigation.lmx.binding` and `slash.navigation.fpl.binding`
4. removed the committed generated Java files from both binding directories
5. verified the existing LMX and FPL format/util classes compile unchanged against the generated bindings
6. added focused JAXB compatibility tests:
   - `navigation-formats/src/test/java/slash/navigation/lmx/NokiaLandmarkExchangeUtilTest.java`
   - `navigation-formats/src/test/java/slash/navigation/fpl/GarminFlightPlanUtilTest.java`

## Validation performed

Compile validation:

```zsh
cd /Users/christian.pesch/IdeaProjects/RouteConverter
source ~/.sdkman/bin/sdkman-init.sh
sdk use java 17.0.19-tem
./mvnw -pl navigation-formats -am -U -DskipTests compile
```

Module test validation:

```zsh
cd /Users/christian.pesch/IdeaProjects/RouteConverter
source ~/.sdkman/bin/sdkman-init.sh
sdk use java 17.0.19-tem
./mvnw -pl navigation-formats -am -U -Dskip.integration.tests=true test
```

Focused JAXB regression tests:

```zsh
cd /Users/christian.pesch/IdeaProjects/RouteConverter
source ~/.sdkman/bin/sdkman-init.sh
sdk use java 17.0.19-tem
./mvnw -pl navigation-formats -am -U -Dskip.integration.tests=true -Dtest=slash.navigation.lmx.NokiaLandmarkExchangeUtilTest,slash.navigation.fpl.GarminFlightPlanUtilTest -Dsurefire.failIfNoSpecifiedTests=false test
```

## Compatibility checks

Reviewers should confirm:

- package names stay unchanged
- `Lmx` and `FlightPlan` root types retain their current accessor names
- `ObjectFactory` remains compatible with the LMX and FPL util classes
- existing read/write round-trip behavior remains stable
- no unrelated navigation format code needs to change

## Result

`navigation-formats/src/main/doc/lmx/lmx.xsd` and
`navigation-formats/src/main/doc/fpl/fpl.xsd` are now the sources of truth for
their binding packages, with Maven-generated JAXB classes and focused
compatibility coverage for both XML bridge layers.

