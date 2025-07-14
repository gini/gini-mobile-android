# Release order
1. Check the `RELEASE-ORDER.md` file.
2. If there are multiple modules affected by this version, release should be created from top to bottom
   (the ones with less dependencies are released first, so after their release, the dependent ones can be released). 
   So, with the new release of the `capture-sdk`, `bank-sdk` should be released as well.
3. `capture-sdk:default-network` version is always bumped when the `capture-sdk` is updated.
4. `core-api-library` and `capture-sdk:default-network` don't get their own release notes, but they still need to be released separately. 
   For `core-api-library`, we share release notes through `bank-api-library` and `healht-api-library`. 
   For `capture-sdk:default-network` we share the release notes through `capture-sdk`.
5. `internal-payment-sdk` also doesn't get it's own release notes, but a release is needed in order to Health and Merchant SDKs can function.
   It's main purpose is to gather common classes for the mentioned libraries without exposing them, 
   but some parts of it should be accessible from libraries which are using it.

# How to release
To publish releases follow these steps:
1. For Capture SDK, Bank SDK and Bank API Library create releases in Jira in Bank's [project](https://ginis.atlassian.net/projects/PP?orderField=RANK&selectedItem=com.atlassian.jira.jira-projects-plugin:release-page&status=released-unreleased) and 
   for Health SDK and Health API Library in the Insurance's [project](https://ginis.atlassian.net/projects/IPC?selectedItem=com.atlassian.jira.jira-projects-plugin:release-page). 
   1. Connect tickets to each release: for each to-be-released ticket add the release to its "Fix versions" field.
   2. Add release notes in markdown format to each release's description (you can copy and update the release notes of a previous release).  
      Markdown formatting is needed because these release notes will be published on GitHub's [releases page](https://github.com/gini/gini-mobile-android/releases).  
      Sample release notes for each project:
         [Capture SDK](https://github.com/gini/gini-mobile-android/releases/tag/capture-sdk%3B3.14.0) - 
         [Bank API Library](https://github.com/gini/gini-mobile-android/releases/tag/bank-api-library%3B3.5.0) - 
         [Bank SDK](https://github.com/gini/gini-mobile-android/releases/tag/bank-sdk%3B3.17.0) - 
         [Health API Library](https://github.com/gini/gini-mobile-android/releases/tag/health-api-library%3B5.3.0) - 
         [Health SDK](https://github.com/gini/gini-mobile-android/releases/tag/health-sdk%3B5.3.0)
         
2. Read this [wiki](https://ginis.atlassian.net/wiki/spaces/PLMO/pages/83787798/Support+multiple+Android+SDK+and+library+versions+parallely) page to determine which branch to use because we support three main version (1.x.x, 2.x.x and 3.x.x).
3. Create a release candidate ticket in the corresponding project (bank or insurance). 
   Sample: https://ginis.atlassian.net/browse/PP-1072
4. Create the required branch (this branch is used to release all the modules for the to-be-released version): 
   PP-XXX-RC-bank-SDK-x.x.x for bank and IPC-XXX-RC-Health-SDK-x.x.x for insurance releases
5. Bump the version numbers:
   1. Bump the version in each to-be-released module's:
       1. `gradle.properties` file,
       2. documentation file (if relevant).
   2. Update the `RELEASE-ORDER.md` file by running: `./gradlew updateReleaseOrderFile`.
   3. Check `RELEASE-ORDER.md` to find out which projects contain the to-be-released modules in their release order.  
      These will be the dependent modules.
   4. Commit the version bump changes using this commit message template and the RC ticket number for getting the build from GitHub Actions:   
         `feat(<project>): Bump version to <version number>`.  
      Complete example: 
         `feat(bank-sdk): Bump version to 4.3.1  
          PP-467`    
      Samples for each project (please add the RC ticket number to the commit message):    
         `feat(capture-sdk): Bump version to 3.13.0`    
         `feat(core-api-library): Bump version to 2.2.2`  
         `feat(bank-api-library): Bump version to 3.3.1`  
         `feat(default-network): Bump version to 3.12.1`  
         `feat(bank-sdk): Bump version to 3.15.0`  
         `feat(health-api-library): Bump version to 4.4.0`  
         `feat(health-sdk): Bump version to 4.4.0`  
   5. For each dependent module:
      1. Bump the version in each module's:
          1. `gradle.properties` file,
          2. documentation file.
      2. Update the `RELEASE-ORDER.md` file by running: `./gradlew updateReleaseOrderFile`.
      3. Commit the version bump changes using the commit message template from above.
   6. Push the commits to the required branch.
6. Wait for QA to assign back the release candidate tickets to you. 
7. Create and push release tags by running: `bundle exec fastlane create_release_tags`.
8. Check that the release workflows have started in [GitHub Actions](https://github.com/gini/gini-mobile-android/actions). 
9. After all builds have finished log into [Sonatype's Nexus Repository Manager](https://oss.sonatype.org/#welcome)
   (for Maven Central) and view the `Staging Repositories`. Credentials are in 1Password: "Maven Central Sonatype account for net.gini".
   1. Run pre-release checks: select all staging repositories and click `Close`.
   2. After the checks have completed check your emails to see whether Sonatype Lift detected any vulnerabilities.
   3. Finalize release: after the automated checks are done select all staging repositories and click `Release`.
10. Create and publish releases in [GitHub](https://github.com/gini/gini-mobile-android/releases) for each of the created release tags using the release notes from the Jira release.
11. Publish the releases in the Jira for each [project](https://ginis.atlassian.net/projects/PP?orderField=RANK&selectedItem=com.atlassian.jira.jira-projects-plugin%3Arelease-page&status=released-unreleased)
12. Put the RC ticket into `Done` and merge the RC branch into the main branch.