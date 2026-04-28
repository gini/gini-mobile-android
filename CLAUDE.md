# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Overview

Monorepo containing Gini's Android SDKs for document capture, bank integration, health insurance, and merchant payment processing. All SDKs are Android libraries managed as Gradle modules in a single repository.

### SDK Modules

| Module | Gradle path | Description |
|---|---|---|
| `core-api-library` | `:core-api-library:library` | Base API library; foundation for all other libraries |
| `bank-api-library` | `:bank-api-library:library` | Bank API wrapper |
| `health-api-library` | `:health-api-library:library` | Health API wrapper |
| `capture-sdk` | `:capture-sdk:sdk` | Document capture SDK |
| `bank-sdk` | `:bank-sdk:sdk` | Banking features SDK |
| `health-sdk` | `:health-sdk:sdk` | Health/payment features SDK |
| `internal-payment-sdk` | `:internal-payment-sdk:sdk` | Internal payment features |
| `merchant-sdk` | `:merchant-sdk:sdk` | Merchant features SDK |

### `/generate-feature-docs`

Generates a plain Markdown feature documentation page for Confluence from GiniBankSDK code changes on the current branch. Output is written to `docs/[platform]/features/[feature-slug]/` in this repository.

- **Skill prompt:** `.claude/skills/generate-feature-docs/SKILL.md`
- **Usage:** `/generate-feature-docs --platform <platform> --feature-slug <slug> [--note "..."]`

Example:
```
/generate-feature-docs --platform android --feature-slug cross-border-payments
```
