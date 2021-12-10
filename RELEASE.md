To publish releases follow these steps:
1. Make sure you are on the `main` branch.
2. Bump the version in each to-be-released module's:
    1. `gradle.properties` file,
    2. documentation file.
3. Make sure the `RELEASE-ORDER.md` file is up-to-date by running: `./gradlew updateReleaseOrderFile`.
4. Check `RELEASE-ORDER.md` to find out which projects contain the to-be-released modules in their release order.
   These will be the dependent modules.
5. Bump the version in each dependent module's:
    1. `gradle.properties` file,
    2. documentation file.
6. Update the `RELEASE-ORDER.md` file by running: `./gradlew updateReleaseOrderFile`.
7. Commit and push the changes to `main`.
8. Create and push release tags by running: `bundle exec fastlane create_release_tags`.
9. After all builds have finished log into [Sonatype's Nexus Repository Manager](https://oss.sonatype.org/#welcome)
   (for Maven Central) and view the `Staging Repositories`. Credentials are in 1Password: "Maven Central Sonatype account for net.gini".
10. Run pre-release checks: select all staging repositories and click `Close`.
11. Finalise release: after the automated checks are done select all staging repositories and click `Release`.
   