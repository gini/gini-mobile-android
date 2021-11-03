# Update dependency declaration

We are publishing to Maven Central and you can remove our maven repo 
("https://repo.gini.net/nexus/content/repositories/open") from your repositories.

Replace `net.gini:gini-pay-api-lib-android:<version>` with `net.gini.android:gini-bank-api-lib:<version>`.

# Update packages

* For all classes except `Gini` and `GiniBuilder` update the imports:
  * From `net.gini.android.*` to `net.gini.android.core.api`.

# Update classes

* Replace `net.gini.android.Gini` with `net.gini.android.bank.api.GiniBankAPI`.
* Replace `net.gini.android.GiniBuilder` with `net.gini.android.bank.api.GiniBankAPIBuilder`.

