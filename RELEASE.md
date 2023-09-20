To publish releases follow these steps:
1. Create releases in Jira in PIA's project: https://ginis.atlassian.net/projects/PIA?selectedItem=com.atlassian.jira.jira-projects-plugin%3Arelease-page
   1. Connect tickets to each release: for each to-be-released ticket add the release to its "Fix versions" field.
   2. Add release notes in markdown format to each release's description (you can copy and update the release notes of a previous release).  
      Markdown formatting is needed because these release notes will be published on GitHub's [releases page](https://github.com/gini/gini-mobile-android/releases).
2. Read this
   [wiki](https://ginis.atlassian.net/wiki/spaces/BANKING/pages/17236285/Support+multiple+Android+SDK+and+library+versions+parallely)
   page to determine which branch to use because we support three main version (1.x.x, 2.x.x and 3.x.x).
3. Check out or create the required branch and bump the version numbers:
   1. Bump the version in each to-be-released module's:
       1. `gradle.properties` file,
       2. documentation file.
   2. Update the `RELEASE-ORDER.md` file by running: `./gradlew updateReleaseOrderFile`.
   3. Check `RELEASE-ORDER.md` to find out which projects contain the to-be-released modules in their release order.  
      These will be the dependent modules.
   4. Commit the version bump changes using this commit message template: `feat(<project>): Bump version to <version number>`.  
      Example: `feat(bank-sdk): Bump version to 4.3.1`.
   5. For each dependent module:
      1. Bump the version in each module's:
          1. `gradle.properties` file,
          2. documentation file.
      2. Update the `RELEASE-ORDER.md` file by running: `./gradlew updateReleaseOrderFile`.
      3. Commit the version bump changes using the commit message template from above.
   6. Push the commits to the required branch.
4. Create and push release tags by running: `bundle exec fastlane create_release_tags`.
5. Check that the release workflows have started in [GitHub Actions](https://github.com/gini/gini-mobile-android/actions).
6. After all builds have finished log into [Sonatype's Nexus Repository Manager](https://oss.sonatype.org/#welcome)
   (for Maven Central) and view the `Staging Repositories`. Credentials are in 1Password: "Maven Central Sonatype account for net.gini".
   1. Run pre-release checks: select all staging repositories and click `Close`.
   2. After the checks have completed check your emails to see whether Sonatype Lift detected any vulnerabilities.
   3. Finalize release: after the automated checks are done select all staging repositories and click `Release`.
7. Create and publish releases in [GitHub](https://github.com/gini/gini-mobile-android/releases) for each of the created release tags using the release notes from the Jira release.

