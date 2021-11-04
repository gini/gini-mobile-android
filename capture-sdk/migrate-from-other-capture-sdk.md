# Migrate from the old [Gini Capture SDK](https://github.com/gini/gini-capture-sdk-android)

## Gini Capture SDK

### Update dependency declaration

We are publishing to Maven Central and you can remove our maven repo ("https://repo.gini.net/nexus/content/repositories/open") from your repositories.

Replace `net.gini:gini-capture-sdk:<version>` with `net.gini.android:gini-capture-sdk:<version>`.

## Default Networking Implementation

### Update dependency declaration

We are publishing to Maven Central and you can remove our maven repo ("https://repo.gini.net/nexus/content/repositories/open") from your repositories.

Replace `net.gini:gini-capture-network-lib:<version>` with `net.gini.android:gini-capture-sdk-default-network:<version>`.

### Update packages

* Replace `net.gini.android.authorization.CredentialsStore` with `net.gini.android.core.api.authorization.CredentialsStore`.
* Replace `net.gini.android.DocumentMetadata` with `net.gini.android.core.api.DocumentMetadata`.
* Replace `net.gini.android.authorization.SessionManager` with `net.gini.android.core.api.authorization.SessionManager`.

## Accounting Networking Implementation

Not available anymore.