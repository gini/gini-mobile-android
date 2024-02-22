Gini Bank SDK Example App
=========================

This example app provides you with a sample usage of the Gini Bank SDK.

The Gini Bank API Library is used for analyzing documents and sending feedback.

Before analyzing documents with the Gini Bank SDK example app, you need to set your Gini Bank API client id and secret by creating a
`local.properties` file in this folder containing a `clientId` and a `clientSecret` property.

ProGuard 
========
 
A sample ProGuard configuration file is included in the example app's directory called `proguard-rules.pro`. 
 
The release build is configured to run ProGuard. You need a keystore with a key to sign it. Create a keystore with a key and provide them in
the `gradle.properties` or as arguments for the build command:

``` 
$ ./gradlew bank-sdk:example-app:assembleRelease \ 
    -PreleaseKeystoreFile=<path to keystore> \ 
    -PreleaseKeystorePassword=<keystore password> \ 
    -PreleaseKeyAlias=<key alias> \ 
    -PreleaseKeyPassword=<key password> \
    -PclientId=<Gini API client id> \
    -PclientSecret=<Gini API client secret>
``` 

Flavors
=======

The example app has three flavors: `dev`, `prod` and `qa`. The `dev` flavor is used by default. The `prod` flavor
is used for creating release builds which can be shared with clients while the `qa` flavor is used for creating release builds
for QA purposes. The difference between `prod` and `qa` is that `qa` allows using custom SSL root certificates for
SSL proxies (e.g. Charles Proxy).

Payment Providers for testing Gini Pay Connect and the Health SDK
==============================================================

Run `bundle exec fastlane install_test_payment_provider_apps` in the repository root to install test payment provider apps 
on all running emulators and connected devices. You can run `bundle exec fastlane uninstall_test_payment_provider_apps`
to uninstall the test payment provider apps.

The `paymentProviderApps` folder contains a toml file with the applicationId and app name for each test payment provider app.
You can also add further mock payment provider apps by creating a file called `mockPaymentProviderApps.toml`. The
`mockPaymentProviderApps.toml` file is ignored by git so you can safely add production payment provider apps to it
as well. **DO NOT ADD** production payment provider apps to the `testPaymentProviderApps.toml` file!

The example app's build gradle file will read the toml files in the `paymentProviderApps` folder and create a flavor
for each one. For example if there are three test payment provider apps in the toml files, then three flavors will be
created named `paymentProvider1`, `paymentProvider2` and `paymentProvider3`. These will be then combined with the
other flavor dimensions and build types, for ex. `devPaymentProvider1Debug`.
