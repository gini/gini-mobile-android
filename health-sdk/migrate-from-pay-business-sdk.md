# Migrate from Pay API Library to Health API Library

See the Health API Library's [migration guide](../health-api-library/migrate-from-pay-api-lib.md).

# Update classes

* Replace `net.gini.pay.ginipaybusiness.ginipayapi.GiniPayApi` with `net.gini.android.health.sdk.util.GiniHealthAPI`.
* Replace `net.gini.pay.ginipaybusiness.GiniBusiness` with `net.gini.android.health.sdk.GiniHealth`.

# Update packages

* Replace `net.gini.pay.ginipaybusiness.*` with `net.gini.android.health.sdk.*`.

# Update styles

* Replace `gpb_` prefixes of android resource names with `ghs_`.
* Replace `GiniPay` in style names with `GiniHealth`.
* 
