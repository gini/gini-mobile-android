Screen API Example App
=========================

This example app provides you with a sample usage of the Gini Capture SDK's Screen API.

The Gini Bank API Library is used for analyzing documents and sending feedback.

Before analyzing documents with the Screen API example app, you need to set your Gini Bank API client id and secret by creating a
`local.properties` file in this folder containing a `clientId` and a `clientSecret` property.

You can find more information about the Screen API in our [integration guide](https://developer.gini.net/gini-mobile-android/capture-sdk/sdk/html/integration.html#screen-api).

Overview
========

The entry point of the app is the `MainActivity`. It starts the Gini Capture SDK and handles the result.

You only need to configure and create a `GiniCapture` instance and start the 
`CameraActivity` for result.

ExtractionsActivity
-------------------

Displays the extractions with the possibility to send feedback to the Gini API. It only shows the extractions needed for transactions, we
call them the Pay5: payment recipient, IBAN, BIC, amount and payment reference.

Feedback should be sent only for the user visible fields. Other fields should be filtered out.

NoExtractionsActivity
---------------------

Displays tips to the user, if no Pay5 extractions were found. 

We recommend implementing a similar screen to aid the user in the taking better pictures and improve the quality of the extractions.

Gini Bank API Library
====================

The Gini Bank API Library is not used directly. The default networking plugin, which was used when configuring and creating a `GiniCapture` instance,
takes care of communicating with the Gini Bank API.

Customization
=============

Customization options are detailed in each Screen API Activity's javadoc: `CameraActivity`, `HelpActivity`, `OnboardingActivity`,
`ReviewActivity` and `AnalysisActivity`.

To experiment with customizing the images used in the Gini Capture SDK you can copy the contents of the folder
`screenapiexample/customized-drawables` to `screenapiexample/src/main/res`.

Text customizations can be tried out by uncommenting and modifying the string resources in the
`screenapiexample/src/main/res/values/strings.xml`.

Text styles and fonts can be customized by uncommenting and altering the styles in the `screenapiexample/src/main/res/values/styles.xml`

To customize the colors you can uncomment and modify the color resources in the `screenapiexample/src/main/res/values/colors.xml`.

Customizing the opacity of the onboarding pages' background you can uncomment and modify the string resource in the
`screenapiexample/src/main/res/values/config.xml`.

ProGuard 
======== 
 
A sample ProGuard configuration file is included in the Screen API example app's directory called `proguard-rules.pro`. 
 
The release build is configured to run ProGuard. You need a keystore with a key to sign it. Create a keystore with a key and provide them in
the `gradle.properties` or as arguments for the build command:

``` 
$ ./gradlew screenapiexample::assembleRelease \ 
    -PreleaseKeystoreFile=<path to keystore> \ 
    -PreleaseKeystorePassword=<keystore password> \ 
    -PreleaseKeyAlias=<key alias> \ 
    -PreleaseKeyPassword=<key password> \
    -PclientId=<Gini API client id> \
    -PclientSecret=<Gini API client secret>
``` 
 