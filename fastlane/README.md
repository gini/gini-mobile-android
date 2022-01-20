fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## Android

### android publish_to_maven_repo

```sh
[bundle exec] fastlane android publish_to_maven_repo
```

Publish a project to a maven repository.

Parameters:
  repo_url            - the url of the maven repository
  repo_user           - the username to use for authentication
  repo_password       - the password to use for authentication
  project_id          - the id of the project to be released (e.g., health-sdk, health-api-lib)
  module_id           - the id of the project's module to be released (e.g., sdk, lib)
  git_tag             - the git tag name used to release the project
  build_number        - the build number to use in the release
  signing_key_base64  - the base64 encoded ascii-armored pgp secret key (see https://docs.gradle.org/current/userguide/signing_plugin.html#sec:in-memory-keys)
  signing_password    - the password for the signing key


### android publish_to_maven_snapshots_repo

```sh
[bundle exec] fastlane android publish_to_maven_snapshots_repo
```

Publish a project to a maven snapshots repository.

Parameters:
  repo_url            - the url of the maven snapshots repository
  repo_user           - the username to use for authentication
  repo_password       - the password to use for authentication
  project_id          - the id of the project to be released (e.g., health-sdk, health-api-lib)
  module_id           - the id of the project's module to be released (e.g., sdk, lib)
  signing_key_base64  - the base64 ascii-armored pgp secret key (see https://docs.gradle.org/current/userguide/signing_plugin.html#sec:in-memory-keys)
  signing_password    - the password for the signing key


### android build_documentation

```sh
[bundle exec] fastlane android build_documentation
```

Build project documentation.

Parameters:
  project_id    - the id of the project to be released (e.g., health-sdk, health-api-lib)"
  module_id     - the id of the project's module to be released (e.g., sdk, lib)"


### android release_documentation

```sh
[bundle exec] fastlane android release_documentation
```

Release project documentation.

Parameters:
  project_id    - the id of the project to be released (e.g., health-sdk, health-api-lib)
  module_id     - the id of the project's module to be released (e.g., sdk, lib)
  git_tag       - the git tag name used to release the documentation
  ci            - set to "true" if running on a CI machine
  git_user     - the username to use for git authentication
  git_password - the password to use for git authentication


### android create_release_tags

```sh
[bundle exec] fastlane android create_release_tags
```

Create release tags for all projects that have different versions than their latest release tag.


### android create_documentation_release_tags

```sh
[bundle exec] fastlane android create_documentation_release_tags
```

Create documentation release tags for all projects that have documentation that changed since their latest release.


----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
