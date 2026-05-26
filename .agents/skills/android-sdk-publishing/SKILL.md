---
name: android-sdk-publishing
description: Guide for Android SDK publishing best practices. Use this when designing public APIs, writing consumer ProGuard rules, checking binary compatibility, managing SDK versioning, or preparing a library for Maven Central publication.
---

All modules in this repo are published Android libraries (AARs) to Maven Central under the `net.gini.android` group. They are consumed by third-party developers, so API design, ProGuard rules, and binary compatibility matter greatly.

## Consumer ProGuard rules

Every published module must include a `consumer-rules.pro` file. These rules are automatically applied to the consuming app's build — the SDK author is responsible for them.

```
# consumer-rules.pro location: <module>/library/consumer-rules.pro
```

### What to include

```proguard
# Keep all public API classes and their members
-keep public class net.gini.android.<module>.** { public *; }

# Keep Retrofit service interfaces (needed for reflection-based proxy)
-keep interface net.gini.android.<module>.api.** { *; }

# Keep Moshi @JsonClass models (KSP generates adapters, but class names must survive)
-keepnames @com.squareup.moshi.JsonClass class **

# Keep Kotlin data classes used in public API (field names used in serialization)
-keepclassmembers class net.gini.android.<module>.model.** {
    <fields>;
}

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
```

### What NOT to include
- Rules for internal implementation classes — only public API surface needs keeping
- Overly broad `-keep **` rules — they bloat the consuming app

## API surface design

### Visibility rules
- `public` — part of the stable API; changing it is a breaking change
- `internal` — Kotlin-only; not accessible to Java consumers or other modules
- Prefer `internal` for implementation details; only expose what consumers need

### Backward-compatible changes (safe)
- Adding new methods/classes
- Adding optional parameters with defaults (Kotlin only — be careful with Java consumers)
- Adding new interface implementations

### Breaking changes (require MAJOR version bump)
- Removing or renaming public methods, classes, or fields
- Changing method signatures
- Changing return types
- Making a previously open class `final`

## Binary compatibility — API dump

Use the [Binary Compatibility Validator](https://github.com/Kotlin/binary-compatibility-validator) to prevent accidental breaking changes:

```bash
# Generate API dump (run after adding new public API)
./gradlew :<module>:apiDump

# Check for breaking changes against the saved API dump
./gradlew :<module>:apiCheck
```

The `.api` file checked into the repo acts as a contract. If `apiCheck` fails, you've introduced a breaking change — either fix the API or bump the major version and update the dump.

## Versioning — Semantic Versioning

Follow [SemVer](https://semver.org/):

| Change type | Version bump | Example |
|---|---|---|
| Breaking API change | MAJOR (`X.0.0`) | Removed a public method |
| New backward-compatible feature | MINOR (`x.Y.0`) | Added a new optional builder method |
| Bug fix, internal change | PATCH (`x.y.Z`) | Fixed a crash, refactored internals |
| Pre-release | `-SNAPSHOT` suffix | `4.1.1-SNAPSHOT` |

Version is set in each module's `gradle.properties` → `VERSION_NAME`.

## AAR content checklist before publishing

- [ ] `consumer-rules.pro` is present and correct
- [ ] All public API classes have KDoc documentation
- [ ] No internal implementation classes are accidentally `public`
- [ ] No test dependencies leaked into `implementation` (use `testImplementation`)
- [ ] No debug/logging code left in release builds — use `BuildConfig.DEBUG` guards or SLF4J (no Android Log directly)
- [ ] `proguard-rules.pro` (module-internal shrinking rules) is separate from `consumer-rules.pro`

## Documentation with Dokka

KDoc comments on all public API elements are required. The Dokka HTML output is published to GitHub Pages on each release.

```kotlin
/**
 * Initializes the Gini SDK with the provided credentials.
 *
 * @param clientId The OAuth client ID obtained from Gini.
 * @param clientSecret The OAuth client secret.
 * @param emailDomain The email domain used for anonymous user accounts.
 * @return A configured [GiniBank] instance.
 */
fun createGiniBank(clientId: String, clientSecret: String, emailDomain: String): GiniBank
```

## Avoiding common SDK pitfalls

- **Don't initialize singletons at class-load time** — let the consuming app control initialization
- **Don't call `Log.d/e/w`** — use SLF4J (`LoggerFactory.getLogger(...)`) so consumers can control logging
- **Don't declare `<application>` components in `AndroidManifest.xml`** unless essential — use manifest merger carefully
- **Don't depend on specific Activity/Fragment subclasses** from the consuming app — use interfaces or callbacks
- **Provide a builder pattern** for SDK configuration rather than a constructor with many parameters
