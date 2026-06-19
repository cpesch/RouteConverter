# Issue note: automate `datasource` JAXB binding generation

## Suggested GitHub issue title

Automate JAXB binding generation for `datasource`

## Suggested issue / docs filename pattern

- current long-lived docs note: `specs/00001-automate-datasource-jaxb-generation.md`
- if a GitHub issue number exists later: `docs/issue-<number>-automate-datasource-jaxb-generation.md`

## Status

Implemented on June 1, 2026.

### Implemented outcome

- `datasource/pom.xml` now generates JAXB bindings during Maven `generate-sources`
- generated output goes to `datasource/target/generated-sources/jaxb`
- the generated package remains `slash.navigation.datasources.binding`
- committed generated Java files were removed from `datasource/src/main/java/slash/navigation/datasources/binding/`
- a JAXB round-trip regression test was added in `datasource/src/test/java/slash/navigation/datasources/helpers/DataSourcesUtilTest.java`
- downstream compatibility was validated against `datasource`, `download-tools`, and `hgt`

### Chosen plugin

- `org.codehaus.mojo:jaxb2-maven-plugin:3.3.0`

Reason selected:

- it generated `jakarta.xml.bind`-compatible classes successfully in this repository
- it preserved the expected API shape for `DatasourceType`, `SourceType`, and `ObjectFactory`
- the resulting clean reactor build passed for the relevant modules

## Short summary for future reference

The `datasource` module now generates its JAXB binding layer from
`datasource/src/main/doc/datasource-catalog.xsd` during Maven `generate-sources`.
The generated sources live under `target/generated-sources/jaxb`, while the hand-written
wrapper abstractions remain in source control.

This removed the need to manually maintain `slash.navigation.datasources.binding` classes
whenever the schema changes and added a JAXB round-trip regression test to guard the wire format.

## Next follow-up steps

1. keep this docs note as the issue/reference document for the JAXB migration,
2. optionally link a future GitHub issue or PR number in the filename,
3. continue with the next catalog-automation work that depends on schema evolution,
4. consider updating any higher-level project docs that still describe the bindings as hand-maintained.

## Goal

Make `datasource/src/main/doc/datasource-catalog.xsd` the single source of truth for the JAXB binding classes in `slash.navigation.datasources.binding`.

The `datasource` module should generate its JAXB classes during Maven `generate-sources`, write them to `target/generated-sources/jaxb`, and stop relying on hand-maintained committed binding sources.

## Why now

The schema already evolved to include `<source>` in `datasource/src/main/doc/datasource-catalog.xsd`, and the surrounding wrapper API has already been adapted:

- `datasource/src/main/java/slash/navigation/datasources/DataSource.java`
- `datasource/src/main/java/slash/navigation/datasources/Source.java`
- `datasource/src/main/java/slash/navigation/datasources/impl/DataSourceImpl.java`
- `datasource/src/main/java/slash/navigation/datasources/impl/SourceImpl.java`

At the moment, the JAXB classes under `datasource/src/main/java/slash/navigation/datasources/binding/` are still committed source files. Every schema change requires manual synchronization of:

- the XSD,
- the generated-looking binding classes,
- `ObjectFactory.java`,
- and occasionally wrapper code.

Automating generation reduces drift risk before the larger catalog migration work continues.

## Original state observed

- `datasource/pom.xml` currently has no JAXB/XJC plugin configuration.
- The binding package still contains generated-looking committed Java files, including `package-info.java`.
- `TODO.md` already calls out this exact improvement.
- `datasource/src/test/resources/slash/navigation/datasources/testdatasources.xml` already covers:
  - populated `<source url="..." level="...">`
  - empty `<source/>`
  - absent `<source>`
- `slash.navigation.datasources.DataSourceServiceTest` currently passes against the existing implementation.
- `.gitignore` already ignores `target/`, so `target/generated-sources/jaxb` will not need additional ignore rules.

## Proposed implementation strategy

### 1. Use a JAXB Maven plugin compatible with Java 17 + Jakarta JAXB

Implemented choice:

- `org.codehaus.mojo:jaxb2-maven-plugin:3.3.0`

Observed result:

- generated classes use `jakarta.xml.bind.annotation.*`
- the generated bindings retained the expected package and accessor names
- downstream compilation succeeded without caller-side rewrites

### 2. Generate into `target/generated-sources/jaxb`

The generated Java output should live under:

- `datasource/target/generated-sources/jaxb`

This keeps generated code out of version control and matches the intent already documented in `TODO.md`.

### 3. Keep the generated package name stable

The generated bindings should continue to use:

- `slash.navigation.datasources.binding`

That avoids churn in existing imports and keeps wrappers such as `DataSourceImpl` and `SourceImpl` unchanged or nearly unchanged.

### 4. Remove committed binding sources after generation is working

Once generation is confirmed, remove the committed source files from:

- `datasource/src/main/java/slash/navigation/datasources/binding/`

Expected files affected include:

- `ActionType.java`
- `BoundingBoxType.java`
- `CatalogType.java`
- `ChecksumType.java`
- `DatasourceType.java`
- `DownloadableType.java`
- `EditionType.java`
- `FileType.java`
- `FragmentType.java`
- `MapType.java`
- `ObjectFactory.java`
- `PositionType.java`
- `SourceType.java`
- `ThemeType.java`
- `package-info.java`

### 5. Keep the higher-level public model intact

These hand-written files should remain in source control:

- `datasource/src/main/java/slash/navigation/datasources/DataSource.java`
- `datasource/src/main/java/slash/navigation/datasources/Source.java`
- `datasource/src/main/java/slash/navigation/datasources/impl/DataSourceImpl.java`
- `datasource/src/main/java/slash/navigation/datasources/impl/SourceImpl.java`
- all other non-generated wrappers and service classes

The goal is to regenerate only the low-level binding layer, not to replace the hand-written abstraction layer.

## Detailed step-by-step plan

### Step 0. Baseline and diff safety

Before editing anything:

1. Run the existing `datasource` tests.
2. Generate bindings into a temporary location once and diff them against the committed binding package.
3. Note any differences in:
   - field order,
   - `@XmlType(propOrder = ...)`,
   - list accessor naming,
   - schema documentation comments,
   - `package-info.java` namespace annotation.

This gives a clean before/after review surface.

### Step 1. Add plugin configuration to `datasource/pom.xml`

Planned changes:

- Add a JAXB generation plugin execution bound to `generate-sources`.
- Point it to:
  - schema: `src/main/doc/datasource-catalog.xsd`
  - output: `${project.build.directory}/generated-sources/jaxb`
- Keep the generated package as `slash.navigation.datasources.binding`.
- Ensure the generated directory is added to the compile source roots if the plugin does not do that automatically.

Review checkpoints:

- avoid changing unrelated module configuration,
- pin versions explicitly for reproducibility,
- keep the `datasource` module self-contained.

### Step 2. Run generation and inspect output

Run Maven generation for only the `datasource` module.

Expected result:

- all binding classes are emitted under `target/generated-sources/jaxb/slash/navigation/datasources/binding/`
- `ObjectFactory` is generated
- `package-info.java` or equivalent package namespace metadata is generated
- `SourceType` reflects the `<source>` element with:
  - `List<String> getInclude()`
  - `List<String> getExclude()`
  - `String getUrl()`
  - `Integer getLevel()`

### Step 3. Compare generated classes with current wrappers

Verify existing hand-written wrappers still align:

- `DataSourceImpl#getSource()` wraps generated `DatasourceType#getSource()` correctly
- `SourceImpl#getIncludes()` maps to generated `SourceType#getInclude()`
- `SourceImpl#getExcludes()` maps to generated `SourceType#getExclude()`

Important repo convention to preserve:

- repeated XML elements remain singular in JAXB (`getInclude()`)
- hand-written interface getters remain plural (`getIncludes()`)

### Step 4. Delete committed binding sources

After successful generation and compile verification:

- remove the Java files currently committed under `datasource/src/main/java/slash/navigation/datasources/binding/`

This should be done in the same change set as the plugin addition so the repository never sits in a broken half-migrated state.

### Step 5. Verify IDE/build behavior

Check that a clean Maven build succeeds from scratch and that importing the Maven project in the IDE picks up generated sources.

Review points:

- a developer cloning the repo should be able to run Maven without manually copying generated files into `src/main/java`
- IntelliJ should recognize `target/generated-sources/jaxb` after Maven import or first `generate-sources`

### Step 6. Strengthen tests only where needed

The existing `DataSourceServiceTest` already covers reading `<source>`.

Add one focused test only if needed for confidence:

- a JAXB round-trip test that loads a catalog with `<source>`, marshals it again, and verifies the `<source>` content survives unchanged

This is especially useful if generated annotations or ordering differ from the current committed classes.

## Verification plan

### Minimum validation

```zsh
cd /Users/christian.pesch/IdeaProjects/RouteConverter
source ~/.sdkman/bin/sdkman-init.sh
sdk use java 17.0.19-tem
./mvnw -pl datasource -am -DskipTests compile
./mvnw -pl datasource -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=slash.navigation.datasources.DataSourceServiceTest -Dskip.integration.tests=true test
```

### Stronger validation

```zsh
cd /Users/christian.pesch/IdeaProjects/RouteConverter
source ~/.sdkman/bin/sdkman-init.sh
sdk use java 17.0.19-tem
./mvnw -pl datasource -am clean test
```

### Implemented validation results

These validations passed during implementation:

- focused datasource regression tests for `DataSourceServiceTest` and `DataSourcesUtilTest`
- downstream compile validation for `datasource`, `download-tools`, and `hgt`
- clean reactor build and test run for `datasource`, `download-tools`, and `hgt`

### What should be checked in the diff

- no hand-written edits are needed in generated binding classes
- generated namespace matches `http://api.routeconverter.com/v1/schemas/datasource-catalog`
- existing wrapper classes compile unchanged or with only minimal import/config adjustments
- `DataSourceServiceTest` still passes
- the generated `SourceType` shape matches current wrapper expectations

## Compatibility validation

The migration should not be considered complete until the generated bindings are validated on three levels:

1. **Java API compatibility** for existing callers,
2. **compile-time compatibility** across all modules that directly import generated bindings,
3. **JAXB wire compatibility** for existing XML input and output.

### 1. Validate the generated API surface

Before deleting the committed classes under `datasource/src/main/java/slash/navigation/datasources/binding/`, generate the replacement classes and compare them side-by-side.

Focus on these types first:

- `DatasourceType`
- `SourceType`
- `CatalogType`
- `EditionType`
- `ObjectFactory`
- `ActionType`
- `package-info.java`

Check for incompatible drift in:

- package name: must remain `slash.navigation.datasources.binding`
- public class names
- enum constant names
- getter and setter names
- list accessor names
- factory method names in `ObjectFactory`
- namespace annotation in `package-info.java`
- `jakarta.xml.bind` imports vs accidental `javax.xml.bind` generation

Most critical expectations for this repository:

- `DatasourceType#getSource()` must still exist and return `SourceType`
- `SourceType#getInclude()` and `SourceType#getExclude()` must remain list accessors
- `SourceType#getUrl()` and `SourceType#getLevel()` must remain available with the same types
- `ObjectFactory` must still expose the factory methods used by `DataSourcesUtil`

### 2. Validate all known downstream compile-time usages

Compilation must succeed not only in `datasource`, but also in modules that directly import generated binding classes.

Known direct consumers currently include:

- `datasource/src/main/java/slash/navigation/datasources/helpers/DataSourcesUtil.java`
- `datasource/src/main/java/slash/navigation/datasources/helpers/DataSourceService.java`
- `download-tools/src/main/java/slash/navigation/download/tools/UpdateCatalog.java`
- `download-tools/src/main/java/slash/navigation/download/tools/ScanWebsite.java`
- `download-tools/src/main/java/slash/navigation/download/tools/migration/MigrateCatalogSources.java`
- `download-tools/src/main/java/slash/navigation/download/tools/migration/SourceMerger.java`
- `hgt/src/test/java/slash/navigation/hgt/HgtFilesIT.java`

The minimum compile-time check should be:

```zsh
cd /Users/christian.pesch/IdeaProjects/RouteConverter
source ~/.sdkman/bin/sdkman-init.sh
sdk use java 17.0.19-tem
./mvnw -pl datasource,download-tools,hgt -am -DskipTests compile
```

This catches method-signature, package, and import breakage quickly.

### 3. Validate existing JAXB unmarshalling behavior

The existing regression anchor is `slash.navigation.datasources.DataSourceServiceTest`.

It already verifies these cases from `datasource/src/test/resources/slash/navigation/datasources/testdatasources.xml`:

- populated `<source url="..." level="...">`
- empty `<source/>`
- absent `<source>`

That test should continue to pass unchanged after the switch to generated bindings.

Focused validation command:

```zsh
cd /Users/christian.pesch/IdeaProjects/RouteConverter
source ~/.sdkman/bin/sdkman-init.sh
sdk use java 17.0.19-tem
./mvnw -pl datasource -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=slash.navigation.datasources.DataSourceServiceTest -Dskip.integration.tests=true test
```

### 4. Add one JAXB round-trip regression test if needed

If the generated classes differ in annotation layout, `propOrder`, or package metadata, add one focused round-trip test.

The test should:

1. unmarshal `testdatasources.xml` into `CatalogType`,
2. marshal it back to XML,
3. unmarshal the result again,
4. verify that these values survive unchanged:
   - datasource ids,
   - `source.url`,
   - `source.level`,
   - `source.include`,
   - `source.exclude`,
   - namespace `http://api.routeconverter.com/v1/schemas/datasource-catalog`.

This protects against changes that are source-compatible in Java but wire-incompatible in JAXB.

### 5. Validate the JAXB bridge code directly

`datasource/src/main/java/slash/navigation/datasources/helpers/DataSourcesUtil.java` is the key bridge between XML and the generated model.

Compatibility is only proven if all of these still work:

- `JAXBContext` creation via `ObjectFactory.class`
- unmarshalling with the generated `ObjectFactory`
- marshalling through `new ObjectFactory().createCatalog(...)`
- conversion helpers like `toXml(DatasourceType)`

If `ObjectFactory` method names or package metadata drift, this is where failures will show up first.

### 6. Finish with a clean build

Do not rely on incremental compilation. A clean build is required to prove that generated sources are wired correctly and no stale committed classes are masking problems.

Recommended end-to-end validation:

```zsh
cd /Users/christian.pesch/IdeaProjects/RouteConverter
source ~/.sdkman/bin/sdkman-init.sh
sdk use java 17.0.19-tem
./mvnw -pl datasource,download-tools,hgt -am clean test
```

### Acceptance criteria for compatibility

The generated classes should be considered compatible only if all of the following are true:

1. the generated package diff shows no unexpected API drift,
2. downstream modules compile without caller-side rewrites,
3. `DataSourceServiceTest` still passes,
4. round-trip JAXB verification passes when added or needed,
5. a clean reactor build succeeds without the committed binding sources.

## PR review checklist

Use this checklist during review of the JAXB generation change.

### A. Maven/plugin setup

- [ ] `datasource/pom.xml` adds JAXB generation in `generate-sources`
- [ ] plugin and JAXB versions are pinned explicitly
- [ ] generated output is written to `target/generated-sources/jaxb`
- [ ] generated sources are available to normal compilation without manual IDE steps
- [ ] the setup uses `jakarta.xml.bind`, not `javax.xml.bind`

### B. Source-control layout

- [ ] committed generated Java files under `datasource/src/main/java/slash/navigation/datasources/binding/` are removed
- [ ] no new generated Java files are committed elsewhere
- [ ] no `.gitignore` change is needed beyond the existing `target/` rule

### C. Generated API compatibility

- [ ] package name remains `slash.navigation.datasources.binding`
- [ ] `DatasourceType` still exposes `getSource()`
- [ ] `SourceType` still exposes `getInclude()`, `getExclude()`, `getUrl()`, and `getLevel()`
- [ ] `ObjectFactory` still exposes the factory methods used by `DataSourcesUtil`
- [ ] enum names and public class names match prior expectations
- [ ] package namespace metadata remains correct in generated output

### D. Hand-written wrapper compatibility

- [ ] `DataSource.java` remains a hand-written abstraction
- [ ] `Source.java` remains a hand-written abstraction
- [ ] `DataSourceImpl` still compiles with minimal or no changes
- [ ] `SourceImpl` still compiles with minimal or no changes
- [ ] plural wrapper getters such as `getIncludes()` / `getExcludes()` still map correctly to singular JAXB list accessors

### E. XML and JAXB compatibility

- [ ] existing test XML in `datasource/src/test/resources/slash/navigation/datasources/testdatasources.xml` still unmarshals successfully
- [ ] populated `<source>` values are preserved
- [ ] empty `<source/>` is preserved
- [ ] absent `<source>` still behaves as null at the wrapper level
- [ ] namespace remains `http://api.routeconverter.com/v1/schemas/datasource-catalog`
- [ ] marshalled XML still uses `<include>` / `<exclude>` child elements and `url` / `level` attributes as expected

### F. Downstream module compatibility

- [ ] `datasource` compiles and tests pass
- [ ] `download-tools` compiles against the generated bindings
- [ ] `hgt` tests or compilation still succeed where direct binding imports are used
- [ ] no caller-side rewrites are needed outside the intended migration scope

### G. Validation commands completed

- [ ] generated-vs-committed binding diff was inspected
- [ ] focused datasource regression test was run
- [ ] downstream compile check was run
- [ ] clean reactor build was run

Recommended commands:

```zsh
cd /Users/christian.pesch/IdeaProjects/RouteConverter
source ~/.sdkman/bin/sdkman-init.sh
sdk use java 17.0.19-tem
./mvnw -pl datasource generate-sources
./mvnw -pl datasource -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=slash.navigation.datasources.DataSourceServiceTest -Dskip.integration.tests=true test
./mvnw -pl datasource,download-tools,hgt -am -DskipTests compile
./mvnw -pl datasource,download-tools,hgt -am clean test
```

### H. Reviewer approval gate

The PR should be approved only if the reviewer can answer **yes** to all of these:

1. Is the XSD now the real source of truth for the binding layer?
2. Can the project regenerate bindings from scratch on a clean checkout?
3. Did the migration avoid incompatible API drift in the generated classes?
4. Did existing JAXB behavior remain stable for the current catalog XML?
5. Did downstream modules continue to compile and test successfully?

## Risks and mitigations

### Risk: generated output differs structurally from the committed bindings

Possible differences:

- reordered members,
- slightly different comments,
- different nullability boxing,
- changed annotation placement.

Mitigation:

- diff generated output before deleting the committed sources,
- adjust wrappers only if the public abstraction requires it,
- keep the schema and wrapper API stable.

### Risk: plugin generates `javax.xml.bind` imports instead of `jakarta.xml.bind`

Mitigation:

- verify plugin/version choice before committing,
- reject the plugin configuration if it produces the wrong API flavor.

### Risk: IDE users do not see generated sources immediately

Mitigation:

- rely on Maven import generated-source recognition,
- document that `generate-sources` is the first recovery step if the IDE does not pick them up automatically.

### Risk: package-level namespace file is not generated exactly as today

Mitigation:

- explicitly verify generated namespace annotations,
- add plugin options if needed to keep package metadata correct.

## Out of scope for this change

This plan does **not** include:

- migrating `catalog-sources.zsh` or `catalog-mirror-jobs.zsh` into XML
- refactoring `ScanWebsite`
- changing `sync-download-catalog.sh`
- removing shell-based catalog configuration
- redesigning the hand-written `DataSource` / `Source` abstraction

Those should happen after JAXB generation is automated.

## Suggested PR shape

### PR 1: JAXB generation automation

Include:

- `datasource/pom.xml` plugin configuration
- deletion of committed binding files
- minimal test additions only if needed
- no functional changes outside `datasource`

### PR review focus

Reviewers should check:

1. generated code location and plugin configuration,
2. compatibility with Java 17 + Jakarta JAXB,
3. absence of source-controlled generated bindings,
4. zero or minimal wrapper API churn,
5. passing `datasource` tests.

## Decision points for review

Please review these choices before implementation:

1. **Plugin choice**: `org.jvnet.jaxb:jaxb-maven-plugin` unless compatibility testing shows `jaxb2-maven-plugin` is a better fit.
2. **Generated package**: keep `slash.navigation.datasources.binding` unchanged.
3. **Source control policy**: remove all generated binding Java files from `src/main/java` once generation is confirmed.
4. **Testing depth**: rely on the existing `DataSourceServiceTest` plus an optional round-trip test if generation introduces uncertainty.

## Expected next step after this plan is approved

Implement the plugin in `datasource/pom.xml`, generate the bindings, remove the committed binding package sources, and run the focused `datasource` tests followed by a clean module build.



