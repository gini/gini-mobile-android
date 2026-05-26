---
name: gini-release-process
description: Guide for the release process of Gini Android SDKs. Use this when asked to prepare a release, bump a version, create a release tag, understand release order, or publish to Maven Central.
---

This monorepo publishes multiple independent SDK and library modules to Maven Central under the `net.gini.android` group. Each module is versioned independently.

## Module versions

Each module's version is stored in its own `gradle.properties`:

| Module path | Property key | Maven artifact |
|---|---|---|
| `core-api-library/` | `VERSION_NAME` | `gini-internal-core-api-lib` |
| `bank-api-library/` | `VERSION_NAME` | `gini-bank-api-lib` |
| `health-api-library/` | `VERSION_NAME` | `gini-health-api-lib` |
| `capture-sdk/` | `VERSION_NAME` | `gini-capture-sdk` + `gini-capture-sdk-default-network` |
| `bank-sdk/` | `VERSION_NAME` | `gini-bank-sdk` |
| `health-sdk/` | `VERSION_NAME` | `gini-health-sdk` |
| `internal-payment-sdk/` | `VERSION_NAME` | `gini-internal-payment-sdk` |
| `merchant-sdk/` | `VERSION_NAME` | `gini-merchant-sdk` |

## Release order dependency rules

Module dependencies must be released in order. The current order is documented in `RELEASE-ORDER.md`. **Always update it** before a release run:

```bash
./gradlew updateReleaseOrderFile
```

Key rules from `RELEASE.md`:
- `capture-sdk:default-network` is **always bumped together with `capture-sdk`**
- Downstream SDKs must be released after their upstream API library dependencies
- Typical order: `core-api-library` → `bank-api-library` / `health-api-library` → `capture-sdk` → `bank-sdk` / `health-sdk`

## Step-by-step release process

### 1. Bump the version
Edit the module's `gradle.properties` and update `VERSION_NAME`. Follow semantic versioning:
- `MAJOR.MINOR.PATCH` for stable releases
- `MAJOR.MINOR.PATCH-SNAPSHOT` for snapshots

### 2. Update RELEASE-ORDER.md
```bash
./gradlew updateReleaseOrderFile
```

### 3. Commit and push the version bump
```bash
git add <module>/gradle.properties RELEASE-ORDER.md
git commit -m "chore: bump <module> version to <version>"
git push
```

### 4. Create and push the release tag
Tag format is strictly: `<module-identifier>;<semver>`

Examples:
```bash
git tag "bank-sdk;4.1.0"
git tag "capture-sdk;4.1.0"
git tag "core-api-library;3.1.0"
git push origin "bank-sdk;4.1.0"
```

This tag push triggers the `<module>.release.yml` GitHub Actions workflow.

### 5. What the release workflow does automatically
1. Runs the full `<module>.check.yml` workflow (unit tests, lint, detekt, etc.)
2. Runs Fastlane lane:
   - Stable release → `publish_to_maven_repo` → publishes to Maven Central staging
   - Snapshot → `publish_to_maven_snapshots_repo` → publishes to Sonatype Snapshots
3. Triggers the docs release workflow to publish KDoc to GitHub Pages

### 6. Required secrets for publishing
These must be configured in GitHub repository secrets:
- `MAVEN_CENTRAL_USER_TOKEN_USERNAME` / `MAVEN_CENTRAL_USER_TOKEN_PASSWORD`
- `MAVEN_CENTRAL_SIGNING_KEY_ID`, `MAVEN_CENTRAL_SIGNING_KEY`, `MAVEN_CENTRAL_SIGNING_PASSWORD`
- `MAVEN_SNAPSHOTS_REPO_URL` (for snapshot publishing)

## Snapshot vs release

| Type | Version suffix | Repository | Task |
|---|---|---|---|
| Snapshot | `-SNAPSHOT` | Sonatype Snapshots | `publishReleasePublicationToSnapshotsRepository` |
| Release | No suffix | Maven Central staging | `publishReleasePublicationToMavenCentralRepository` |

## Fastlane lanes (reference)

Located in `fastlane/Fastfile`:
- `publish_to_maven_repo` — publishes release to Maven Central
- `publish_to_maven_snapshots_repo` — publishes snapshot
- `build_documentation` — builds Dokka HTML docs
