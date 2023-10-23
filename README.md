# Monorepo for Gini Android SDKs and Libraries

The monorepo contains a multi-module gradle project. Each SDK and library has a top-level project module containing sub-modules for each of its main components. You can find all used modules in [settings.gradle.kts](settings.gradle.kts).

The main components include a primary sub-module which contains the main SDK or library code. Other main components are the example apps or separately released extensions (for example the default networking implementation for the Capture SDK).

In the following we will refer to the top-level project modules simply as projects.

# Dependency Management

Dependencies between our SDKs and libraries are set as project dependencies and are thus always resolved locally during development. This allows easy development of cross-cutting features.

External dependencies are managed using [gradle's version catalogs](https://docs.gradle.org/current/userguide/platforms.html). Shared versions are declared in [gradle/libs.versions.toml](gradle/libs.versions.toml) and are accessed through generated accessors on the `libs` extension.

# Versioning

Each SDK, library and example app have their own version which is set in their releasable sub-modules' `gradle.properties` file. In case of
apps this also contains the `versionCode`.

# Documentation

We provide two types of documentation: reference documentation and integration guides.

The reference documentation is part of the source code (kdoc or javadoc) and is compiled to a static website using [Dokka](https://github.com/Kotlin/dokka) for both java and kotlin source.

## How to build the documentation

The reference documentation can be built using fastlane with the following command:

```
$ bundle exec fastlane build_documentation project_id:<project-id> module_id:<module-id>
```

Where `<project-id>` is the name of the project folder (e.g., `capture-sdk` or `bank-api-library`) and `<module-id>` is the name of the sub-module in the project folder (e.g., `sdk` or `default-network`).

The built documentation can be then found under `<project-id>/<module-id>/build/docs`. The `dokka` subfolder contains the static website for the reference documentation and the `html` subfolder contains the static website for the guides.

For example the following command builds the reference documentation and guides for Capture SDK:

```
$ bundle exec fastlane build_documentation project_id:capture-sdk module_id:sdk
```

The following commands open the reference documentation and the guides in the browser:

```
$ open capture-sdk/sdk/build/docs/dokka/index.html
$ open capture-sdk/sdk/build/docs/html/index.html
```

### Build the reference documentation using Gradle

The reference documentation can be built using Gradle with the following command:

```
$ ./gradlew <project-id>:<module-id>:dokkaHtmlSiblingCollector
```

Where `<project-id>` is the name of the project folder (e.g., `capture-sdk` or `bank-api-library`) and `<module-id>` is the name of the sub-module in the project folder (e.g., `sdk` or `default-network`).

The built documentation can be then found under `<project-id>/<module-id>/build/docs/dokka`.

For example this command builds the reference documentation for the Capture SDK:

```
$ ./gradlew capture-sdk:sdk:dokkaHtmlSiblingCollector
```

With the following command you can open the reference documentation in the browser:

```
$ open capture-sdk/sdk/build/docs/dokka/index.html
```

# Example Apps

Some SDKs have accompanying example apps. These can be found as sub-modules under the SDK's module. The example apps serve as a tool for us during development as well as an aid for our clients.

# Publishing

We are using git tags to mark published versions. These also trigger the release workflows on GitHub.

## SDKs and Libraries

You can find a step-by-step release guide in [RELEASE.md](RELEASE.md).

SDKs and libraries are released to [Maven Central](https://search.maven.org/search?q=net.gini.android) under the `net.gini.android` group id.

Release notes need to be published on the repo's [releases page](https://github.com/gini/gini-mobile-android/releases).

Each SDK's and library's Maven POM properties are set in their releasable sub-module's `gradle.properties` file.

The release tags must adhere to this pattern:
```
<project-name>;<version>
```

For example `bank-sdk;1.0.2` or `health-api-lib;2.0.3`.

## Documentation

Documentation is implicitly versioned via the associated SDK or library version tag and is automatically released with the SDK or library.

The reference documentation is published in the `gh-pages` branch in the `<project-name>/<sub-module-name>` sub-folder. For example, the reference documentation for the default networking implementation at http://developer.gini.net/gini-mobile-android/capture-sdk/default-network/dokka/.

When only documentation release is necessary, then use a subtag of the last SDK or library version tag following this pattern:
```
<project-name>;<latest-version>;doc-<number>
```

For example `bank-sdk;2.0.1;doc-1` or `health-api-lib;1.5.4;doc-1` or `health-api-lib;1.5.4;doc-2`.

## Example Apps

Note: automated example app publishing is not working yet ([PIA-1857](https://ginis.atlassian.net/browse/PIA-1857)).

# CI

Our main tool for automation is [fastlane](https://fastlane.tools/). It serves as the main entry point for tasks executed on CI machines. Using fastlane we can isolate complex tasks into lanes which we are able to execute locally as well when necessary.

For a CI environment we are using [GitHub Actions](https://github.com/features/actions). It is important to use GitHub Actions only to trigger specific workflows and to create the necessary environment for fastlane to run (including of course all Android SDK, java and other dependencies). 

All complex tasks should live in lanes which can be executed locally when necessary and never as GitHub Action steps. Otherwise when there is a GitHub outage we are completely blocked.

## GitHub Actions

Each SDK and library has multiple workflows. These can be found at [.github/workflows/](.github/workflows/).

The workflow file names must follow this pattern: 
```
<project-name>.<subject/action>.<optional additional dot separated subjects/actions>.yml
```

For example: `bank-sdk.check.yml` or `bank-sdk.docs.release.yml`.

It is very important to not include plain-text secrets in the workflow files. Make sure to use [GitHub Secrets](https://github.com/gini/gini-mobile-android/settings/secrets/actions) for accessing confidential data in the workflows.

## Fastlane

We are using fastlane to automate tasks like releasing to Maven Central, building and releasing the
documentation, or creating release tags.

You can find fastlane related code in the [fastlane](fastlane/) folder.

# Gradle

We use the Kotlin DSL for gradle as it is also used by the [gradle project itself](https://github.com/gradle/gradle) and it has many quality-of-life benefits over Groovy.

Custom gradle tasks are organised in custom plugins and can be found in the [buildSrc](buildSrc/) folder.

# Git

We are using [GitHub flow](https://docs.github.com/en/get-started/quickstart/github-flow) so the rule of thumb is to create a branch from `main` and when ready create a pull request to merge the work into `main`.

## Commit Guidelines

Please follow the advice from [this article](https://chris.beams.io/posts/git-commit/) to write good commits.

### Format

We use an own simplified adaptation of the [conventional commits spec](https://www.conventionalcommits.org/en/v1.0.0/) (inspired by the [oh-my-zsh project](https://github.com/ohmyzsh/ohmyzsh/blob/master/CONTRIBUTING.md#commit-guidelines)). This allows us to generate release notes or filter commits by projects.

All commits must adhere to this format:
```
<type>(<project>): <subject>

<body>

<ticket-id>
```

A detailed explanation can be found in the [.git-stuff/commit-msg-template.txt](.git-stuff/commit-msg-template.txt). It also includes instructions on how to use the template with git and Sourcetree.

## Merging

We are using merge commits and restrict rebasing and squashing to feature branches.
