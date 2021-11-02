# Update dependency declaration

We are publishing to Maven Central and you can remove our maven repo ("https://repo.gini.net/nexus/content/repositories/open") from your repositories.

Replace `net.gini:gini-pay-bank-sdk:<version>` with `net.gini.android:gini-bank-sdk:<version>`.

# Update classes

* Replace `net.gini.pay.bank.ginipayapi.GiniPayApi` with `net.gini.android.bank.sdk.util.GiniBankAPI`.
* Replace `net.gini.pay.bank.GiniPayBank` with `net.gini.android.bank.sdk.GiniBank`.
* Replace `net.gini.pay.bank.pay.Business` with `net.gini.android.bank.sdk.pay^.PaymentRequestIntent`

# Update packages

* Replace `net.gini.pay.bank.*` with `net.gini.android.bank.sdk.*`.

# Update styles

* Replace `gpb_` prefixes of android resource names with `gbs_`.

# Migrate from Pay API Library to Bank API Library

See the Bank API Library's [migration guide](../bank-api-library/migrate-from-pay-api-lib.md).
