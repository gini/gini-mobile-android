---
name: detekt-ktlint-fixes
description: Guide for understanding and fixing Detekt and ktlint code analysis failures in this repo. Use this when asked to fix lint errors, code style violations, Detekt rule failures, or ktlint formatting issues.
---

This repo uses `CodeAnalysisPlugin` (in `buildSrc/`) which configures both **Detekt** and **ktlint** for all SDK modules.

## Key difference: Detekt vs ktlint

| Tool | What it checks | Failure mode |
|---|---|---|
| **Detekt** | Kotlin code quality, complexity, style rules | **Enforced** — build fails on violations in most modules |
| **ktlint** | Kotlin code formatting | Configured with `ignoreFailures = true` — warnings only in CI, but fix anyway |

## Running the checks locally

```bash
# Check a specific module
./gradlew :<module>:detekt
./gradlew :<module>:ktlintCheck

# Auto-fix ktlint formatting issues
./gradlew :<module>:ktlintFormat

# Run all checks for a module
./gradlew :<module>:check
```

Replace `<module>` with the Gradle path, e.g., `:bank-sdk:library`, `:capture-sdk:library`, `:health-sdk:library`, `:core-api-library:library`.

## Detekt configuration

- Config file location: `config/detekt/detekt.yml` (shared across all modules)
- Baseline files per module: `<module>/detekt-baseline.xml` (existing violations suppressed here)
- To add a suppression for an unavoidable violation:
  ```bash
  ./gradlew :<module>:detektBaseline
  ```
  This regenerates the baseline — only do this for pre-existing issues, not new code.

## Common Detekt violations and fixes

### `MagicNumber`
Replace literal numbers with named constants:
```kotlin
// Bad
val timeout = 30000L
// Good
val TIMEOUT_MS = 30_000L
```

### `LongParameterList`
Extract parameters into a data class or use a builder pattern.

### `TooManyFunctions` / `LargeClass`
Split responsibilities — move related functions to extension files or separate classes.

### `ReturnCount`
Refactor to reduce early returns; use `when` expressions instead.

### `ComplexCondition` / `ComplexMethod`
Extract boolean expressions to named variables; break methods into smaller functions.

### `UnusedPrivateMember`
Remove or use the private member. If it's needed for future use, add `@Suppress("UnusedPrivateMember")` with a comment.

### `ForbiddenComment` (TODO/FIXME)
Resolve the TODO or convert to a GitHub issue, then remove the comment.

## ktlint formatting issues

ktlint follows the [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html). The fastest fix is:
```bash
./gradlew :<module>:ktlintFormat
```

Common issues that need manual attention:
- **Import ordering**: ktlint enforces alphabetical imports within groups — use IDE auto-import formatting
- **Trailing commas**: Required in multi-line function calls and declarations in Kotlin 1.6+
- **Wildcard imports**: Avoid `import foo.*`

## Suppressing violations (use sparingly)

```kotlin
@Suppress("MagicNumber")
val value = 42

// For a whole file
@file:Suppress("TooManyFunctions")
```

Only suppress when the violation is a known false positive or genuinely unavoidable. Document why with a comment.
