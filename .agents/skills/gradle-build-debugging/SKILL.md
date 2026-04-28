---
name: gradle-build-debugging
description: Guide for debugging Gradle build issues in this Kotlin/Android monorepo. Use this when encountering build failures, dependency resolution errors, custom plugin errors, version catalog issues, or Gradle sync problems.
---

This repo uses **Gradle Kotlin DSL exclusively** (all `.gradle.kts`), AGP 8.9.0, Kotlin 2.0.20, JDK 17 (Temurin), and a `buildSrc/` directory for custom convention plugins.

## Key files to check first

| File | Purpose |
|---|---|
| `gradle/libs.versions.toml` | Version catalog — single source of truth for all dependency versions and aliases |
| `build.gradle.kts` (root) | Root build configuration, applies plugins to all subprojects |
| `settings.gradle.kts` | Module declarations and plugin management |
| `buildSrc/src/main/kotlin/net/gini/gradle/` | All custom convention plugins |
| `gradle.properties` | Per-module version properties (e.g., `VERSION_NAME=4.1.0`) |

## Custom buildSrc plugins

Each SDK module applies one or more of these plugins in its `build.gradle.kts`:

| Plugin class | Applied as | Purpose |
|---|---|---|
| `PublishToMavenPlugin` | `net.gini.publish` | Wires `maven-publish` + `signing` for Maven Central. Reads version from `gradle.properties`. |
| `DokkaPlugin` | `net.gini.dokka` | Configures Dokka for KDoc/Javadoc generation |
| `CodeAnalysisPlugin` | `net.gini.code-analysis` | Aggregates Detekt, ktlint, Checkstyle, PMD, Android Lint |
| `SBOMPlugin` | `net.gini.sbom` | Generates CycloneDX 1.4 SBOM via `org.cyclonedx.bom` |
| `DependencyUpdatesPlugin` | `net.gini.dependency-updates` | Ben Manes versions plugin |
| `PropertiesPlugin` | `net.gini.properties` | Reads module-level properties |
| `ReleaseOrderPlugin` | `net.gini.release-order` | Manages `RELEASE-ORDER.md` updates |

## Debugging steps

### Dependency resolution errors
1. Check `gradle/libs.versions.toml` for the correct alias and version
2. Run `./gradlew :<module>:dependencies --configuration releaseRuntimeClasspath` to inspect the resolution tree
3. Look for version conflicts between modules — all modules share the same version catalog
4. KSP (Kotlin Symbol Processing) is used for Moshi codegen — ensure `ksp` plugin version matches Kotlin version in the catalog

### buildSrc compilation errors
1. The buildSrc itself is a Kotlin project — check `buildSrc/build.gradle.kts` for its own dependencies
2. Plugin source is in `buildSrc/src/main/kotlin/net/gini/gradle/`
3. Changes to buildSrc require a full Gradle sync — run `./gradlew --rerun-tasks` if cached state is stale

### Version catalog issues
- Aliases use dots as separators: `libs.okhttp` or `libs.retrofit.converter.moshi`
- If a new dependency isn't resolving, verify it's declared in `[libraries]` section of `gradle/libs.versions.toml`
- Bundles are declared in `[bundles]` and referenced as `libs.bundles.<name>`

### JaCoCo / code coverage issues
- JaCoCo is configured per-module but **disabled for Bank SDK debug builds** due to a Kotlin SMAP bug
- Coverage XML reports are expected at `build/reports/jacoco/` for SonarCloud

### Gradle cache / stale state
```bash
./gradlew --stop                        # stop Gradle daemon
./gradlew clean                         # clean build outputs
./gradlew <task> --no-build-cache       # bypass build cache
./gradlew <task> --rerun-tasks          # force re-execution
```

### Checking which tasks run in CI
Look at the corresponding `.github/workflows/<module>.check.yml` — the `run:` steps show the exact Gradle commands used in CI. Match those locally to reproduce.

### Common Gradle commands
```bash
./gradlew :<module>:assembleDebug
./gradlew :<module>:testDebugUnitTest
./gradlew :<module>:detekt
./gradlew :<module>:ktlintCheck
./gradlew :<module>:lint
./gradlew updateReleaseOrderFile         # updates RELEASE-ORDER.md
./gradlew :<module>:generateBom         # generates CycloneDX SBOM
```
