---
name: android-module-navigation
description: Guide for navigating and understanding the layered module architecture of this Android monorepo. Use this when asked where to make a change, which module owns a feature, how modules depend on each other, or how to add code to the right place.
---

This repo is a multi-module Android monorepo for Gini GmbH's document capture and payment SDKs. Modules are organized in two layers.

## Architecture layers

```
┌─────────────────────────────────────────────────────┐
│                   SDK Layer (Product)                 │
│  bank-sdk       health-sdk       merchant-sdk         │
│     │               │                 │               │
│     └──────┬─────────┘                │               │
│            │         capture-sdk      │               │
│            │              │           │               │
│   internal-payment-sdk ───┘           │               │
└────────────┼──────────────────────────┼───────────────┘
             │                          │
┌────────────▼──────────────────────────▼───────────────┐
│               API Library Layer (Foundation)           │
│        bank-api-library     health-api-library         │
│                  └──────────┘                          │
│                       │                                │
│              core-api-library                          │
└────────────────────────────────────────────────────────┘
```

## Module directory structure

Each module follows this structure:
```
<module>/
  library/          ← The publishable library (src/main, src/test, src/androidTest)
  example-app/      ← Demo app (not published, used for manual/automated testing)
  build.gradle.kts  ← Module-level build config
  gradle.properties ← Module version (VERSION_NAME=x.y.z)
```

## Module responsibilities

### `core-api-library/library`
- **Package**: `net.gini.android.core.api`
- **Purpose**: Shared networking foundation for all API libraries
- **Contains**: `GiniCoreAPIBuilder`, `OkHttpClient` setup, auth interceptors (`GiniAuthenticationInterceptor`, `GiniAuthenticator`), `GiniHttpClientProvider`/`DefaultGiniHttpClientProvider`, `DocumentMetadata`
- **Change here when**: Modifying auth flow, HTTP client configuration, base API request/response handling

### `bank-api-library/library`
- **Package**: `net.gini.android.bank.api`
- **Purpose**: REST client for the Gini Bank API
- **Change here when**: Adding/modifying Bank API endpoints, request/response models, bank-specific API auth

### `health-api-library/library`
- **Package**: `net.gini.android.health.api`
- **Purpose**: REST client for the Gini Health API
- **Change here when**: Adding/modifying Health API endpoints, health-specific API auth

### `capture-sdk/library`
- **Package**: `net.gini.android.capture`
- **Purpose**: Document capture UI (camera, import, review screens). Uses CameraX, ML Kit
- **Change here when**: Modifying capture flow, camera behavior, document review screens, file import

### `capture-sdk/default-network`
- **Package**: `net.gini.android.capture.network`
- **Purpose**: Default networking implementation for `capture-sdk` using `bank-api-library`
- **Always released together with `capture-sdk`**
- **Change here when**: Changing how captured documents are uploaded to the Gini API

### `bank-sdk/library`
- **Package**: `net.gini.android.bank.sdk`
- **Purpose**: Full banking payment UI — extracts payment info, resolves payment requests from other apps. Composites `capture-sdk` + `bank-api-library`
- **Change here when**: Payment flow UI, invoice extraction screens, bank-specific payment resolution

### `health-sdk/library`
- **Package**: `net.gini.android.health.sdk`
- **Purpose**: Health insurance payment UI — extracts payment & health info
- **Change here when**: Health payment flow UI, health-specific extraction screens

### `internal-payment-sdk/library`
- **Package**: `net.gini.android.internal.payment`
- **Purpose**: Shared payment UI components reused by `bank-sdk`, `health-sdk`, and `merchant-sdk`
- **Change here when**: Shared payment screens, shared payment models used across multiple SDKs

### `merchant-sdk/library`
- **Package**: `net.gini.android.merchant.sdk`
- **Purpose**: Enables Gini Pay Connect in merchant/e-commerce apps
- **Change here when**: Merchant-side payment integration

## Architecture patterns used

- **Orbit MVI** (`orbit-viewmodel`, `orbit-compose`): All ViewModels use MVI with `ContainerHost`. State, sideEffects, and intents are the unit of change.
- **Koin 4.x**: DI framework. Module definitions are in `*Module.kt` files. Use `koinInject()` in Composables, `by inject()` in classes.
- **Retrofit + Moshi**: All REST clients. Moshi models use `@JsonClass(generateAdapter = true)` for KSP codegen.
- **Jetpack Compose + ViewBinding**: Hybrid UI — newer screens use Compose (Material 3), older screens use ViewBinding. Migration is in progress.

## Where to put new code

| Type of change | Module |
|---|---|
| New HTTP endpoint for banking | `bank-api-library/library` |
| New HTTP endpoint for health | `health-api-library/library` |
| New base networking feature | `core-api-library/library` |
| New capture/camera UI | `capture-sdk/library` |
| New payment UI shared across SDKs | `internal-payment-sdk/library` |
| New bank-specific payment screen | `bank-sdk/library` |
| New health-specific screen | `health-sdk/library` |
| New merchant integration feature | `merchant-sdk/library` |
