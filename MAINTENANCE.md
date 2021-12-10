# Update dependencies

To check which dependencies have newer versions run
`./gradlew dependencyUpdatesForAndroidProjects`

You will get a list of dependencies. For each dependency you will see all the projects they are used in. 

Using this list you can update each dependency and will know which projects will be affected.

After you have updated all dependencies and your PR has been merged you can follow the release steps in `RELEASE.md`.