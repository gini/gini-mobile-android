# Gini Mobile Android - Copilot Instructions

## Repository Overview

This is a multi-module Gradle monorepo containing Gini's Android SDKs and API libraries for bank, health, merchant, and capture functionality. Each SDK/library has its own versioning and is independently released to Maven Central under the `net.gini.android` group.

## Project Structure

The repository contains top-level project modules with sub-modules:
- **core-api-library**: Core API client (shared by other libraries)
- **bank-api-library**, **health-api-library**: API client libraries
- **capture-sdk**: Document capture and scanning SDK with default-network sub-module
- **bank-sdk**, **health-sdk**, **merchant-sdk**: Full-featured SDKs
- **internal-payment-sdk**: Shared internal payment components (not published with release notes)

Each project has:
- `<project>/sdk` or `<project>/library`: Main releasable module
- `<project>/example-app`: Optional demonstration app

## Build, Test, and Lint

### Running Tests

**Unit tests (all modules):**
```bash
./gradlew test
```

**Unit tests (specific module):**
```bash
./gradlew :bank-sdk:sdk:testDebugUnitTest
./gradlew :core-api-library:library:testDebugUnitTest
```

**Instrumented tests:**
```bash
./gradlew :bank-sdk:sdk:connectedDebugAndroidTest
```

Instrumented tests for `core-api-library` require test credentials in `core-api-library/library/local.properties`:
```
testClientId=*******
testClientSecret=*******
testApiType=DEFAULT
testApiUri=https://pay-api.gini.net
testUserCenterUri=https://user.gini.net
```

### Code Quality

**Lint:**
```bash
./gradlew :bank-sdk:sdk:lintDebug
./gradlew lint  # all modules
```

**ktlint:**
```bash
./gradlew ktlintCheck
./gradlew ktlintFormat  # auto-fix
```

**detekt:**
```bash
./gradlew detekt
```

### Building

**Assemble all modules:**
```bash
./gradlew assemble
```

**Assemble specific module:**
```bash
./gradlew :bank-sdk:sdk:assembleDebug
```

## Documentation

Documentation is built with Dokka and includes reference docs (kdoc/javadoc) and integration guides.

**Build documentation (fastlane - recommended):**
```bash
bundle exec fastlane build_documentation project_id:capture-sdk module_id:sdk
bundle exec fastlane build_documentation project_id:bank-api-library module_id:library
```

Output: `<project>/<module>/build/docs/dokka/` (reference) and `<project>/<module>/build/docs/html/` (guides)

**Build documentation (Gradle):**
```bash
./gradlew capture-sdk:sdk:dokkaHtmlSiblingCollector
```

## Dependencies

### Version Catalog

All shared dependencies are managed via [gradle/libs.versions.toml](../gradle/libs.versions.toml). Access through generated `libs` accessors:
```kotlin
implementation(libs.androidx.lifecycle.viewmodel)
```

### Updating Dependencies

```bash
./gradlew dependencyUpdatesForAndroidProjects
```

This shows which dependencies have updates and which modules use them.

### Inter-Project Dependencies

Internal SDK/library dependencies are always resolved locally as project dependencies during development. This enables cross-cutting feature development. Check `RELEASE-ORDER.md` for dependency chains when releasing.

## Commit Message Format

All commits MUST follow this format:
```
<type>(<project>): <subject>

<body>

<ticket-id>
```

**Types:**
- `feat`: New or modified features, UI changes, public API changes, or adding tests to features
- `fix`: Bug fixes, test fixes, dependency updates for fixes
- `refactor`: Code changes without breaking public API or changing functional behavior
- `docs`: Documentation changes
- `ci`: Build script, automation, or git-related changes

**Examples:**
```
feat(bank-sdk): Add error logging interface

fix(capture-sdk): Correct image rotation on Pixel devices

refactor(core-api-library): Extract authentication interceptor

docs(health-sdk): Update extraction feedback guide

ci: Update android gradle plugin
```

Leave `<project>` blank (without parentheses) for multi-project changes.

## Architecture Patterns

### Authentication

Authentication uses interceptor-based patterns:
- `GiniAuthenticationInterceptor`: Adds bearer token to requests
- `GiniAuthenticator`: Handles token refresh on 401 responses
- User API client has no auth interceptor to avoid circular dependencies

### HTTP Client Provider

The `GiniHttpClientProvider` interface allows consumers to inject custom `OkHttpClient` configurations via `GiniCoreAPIBuilder.setHttpClientProvider()`.

### UI Architecture

- **ViewModels**: Use `androidx.lifecycle.ViewModel` for UI state management
- **Compose**: Modern UI components use Jetpack Compose with custom Gini design system (`GiniTheme`, `GiniButton`, `GiniTextInput`, etc.)
- **View Binding**: Legacy screens use View Binding (enabled in build.gradle.kts)
- **Navigation**: Uses Navigation Component with Safe Args plugin
- **DI**: Mix of Koin and Hilt depending on module

## Release Process

Releases are coordinated across dependent modules following `RELEASE-ORDER.md`. Each SDK/library has independent versioning set in `<project>/<module>/gradle.properties`.

**Release tags format:**
```
<project-name>;<version>
```

Examples: `bank-sdk;1.0.2`, `health-api-lib;2.0.3`

**Create release tags:**
```bash
bundle exec fastlane create_release_tags
```

**Publish to Maven (via CI):**
Publishing happens via GitHub Actions triggered by git tags. Manual publishing:
```bash
bundle exec fastlane publish_to_maven_repo \
  repo_url:<url> \
  repo_user:<user> \
  repo_password:<pass> \
  project_id:bank-sdk \
  module_id:sdk \
  git_tag:bank-sdk;1.0.2 \
  build_number:1 \
  signing_key_base64:<key> \
  signing_password:<pass>
```

## Custom Gradle Plugins

Custom build logic lives in `buildSrc/src/main/kotlin/net/gini/gradle/`:
- `PublishToMavenPlugin.kt`: Maven Central publishing configuration
- `DokkaPlugin.kt`: Documentation generation
- `ReleaseOrderPlugin.kt`: Generates RELEASE-ORDER.md dependency graph
- `CodeAnalysisPlugin.kt`: Code quality checks
- `DependencyUpdatesPlugin.kt`: Dependency update detection
- `SBOMPlugin.kt`: Software Bill of Materials generation

## CI/CD

GitHub Actions workflows are in `.github/workflows/` following naming pattern:
```
<project-name>.<subject/action>.<optional-additional-subjects>.yml
```

Examples: `bank-sdk.check.yml`, `bank-sdk.docs.release.yml`

Complex tasks are implemented as fastlane lanes (not GitHub Action steps) to enable local execution.

## Common Issues

**"java.lang.IllegalAccessError: superclass access check failed"**

Solution: In Android Studio, go to Tools → SDK Manager → Build, Execution, Deployment → Build Tools → Gradle, and set Gradle JDK to version 17 or lower (17 preferred).

## Key Files to Know

- `settings.gradle.kts`: Module inclusion and repository configuration
- `build.gradle.kts`: Root build configuration with custom plugins
- `gradle/libs.versions.toml`: Centralized dependency version catalog
- `RELEASE-ORDER.md`: Auto-generated module dependency order for releases
- `RELEASE.md`: Step-by-step release guide
- `.git-stuff/commit-msg-template.txt`: Commit message template
