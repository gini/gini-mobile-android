Gini Health SDK Example App
=========================

This example app provides you with a sample usage of the Gini Health SDK.

The Gini Health API Library is used for analyzing documents and creating payment requests.

Before using the Gini Health SDK example app, you need to set your Gini Health API client id and secret by creating a
`local.properties` file in this folder containing a `clientId` and a `clientSecret` property.

ProGuard 
========
 
A sample ProGuard configuration file is included in the example app's directory called `proguard-rules.pro`. 
 
The release build is configured to run ProGuard. You need a keystore with a key to sign it. Create a keystore with a key and provide them in
the `gradle.properties` or as arguments for the build command:

``` 
$ ./gradlew health-sdk:example-app:assembleRelease \ 
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

Payment Providers for testing the Health SDK
============================================

Payment Providers for testing are provided by the Gini Bank SDK's example app. Run `bundle exec fastlane install_test_payment_provider_apps` in the repository root to install test payment provider apps 
on all running emulators and connected devices. You can run `bundle exec fastlane uninstall_test_payment_provider_apps`
to uninstall the test payment provider apps.

Please view the Gini Bank SDK example app's readme at `bank-sdk/example-app/README.md` for more details.
