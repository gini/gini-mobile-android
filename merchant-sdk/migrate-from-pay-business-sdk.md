# Migrate from the [Gini Pay Business SDK](https://github.com/gini/gini-pay-business-sdk-android)

## Update dependency declaration

We are publishing to Maven Central and you can remove our maven repo ("https://repo.gini.net/nexus/content/repositories/open") from your repositories.

Replace `net.gini:gini-pay-business-sdk:<version>` with `net.gini.android:gini-merchant-sdk:<version>`.

## Update classes

* Replace `net.gini.pay.ginipaybusiness.ginipayapi.GiniPayApi` with `net.gini.android.merchant.sdk.util.GiniHealthAPI`.
* Replace `net.gini.pay.ginipaybusiness.GiniBusiness` with `net.gini.android.merchant.sdk.GiniMerchant`.

## Update packages

* Replace `net.gini.pay.ginipaybusiness.*` with `net.gini.android.merchant.sdk.*`.

## Update styles

* Replace `gpb_` prefixes of android resource names with `gms_`.

## Migrate from Pay API Library to Health API Library

See the Health API Library's [migration guide](../health-api-library/migrate-from-pay-api-lib.md).
