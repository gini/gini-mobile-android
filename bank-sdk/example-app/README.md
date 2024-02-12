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
