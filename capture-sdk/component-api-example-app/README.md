Component API Example App
=========================

This example app provides you with a sample usage of the Gini Capture SDK's Component API.

// TODO: update links

The [Gini Bank API Library](https://github.com/gini/gini-pay-api-lib-android) is used for analyzing documents and sending feedback.

Before analyzing documents with the Component API example app, you need to set your Gini Bank API client id and secret by creating a
`local.properties` file in this folder containing a `clientId` and a `clientSecret` property.

// TODO: update links

You can find more information about the Component API in our [integration guide](https://developer.gini.net/gini-capture-sdk-android/html/integration.html#component-api).

Overview
========

The entry point of the app is the `MainActivity`. It starts the `CameraExampleAppCompatActivity`. The
`MainActivity` also requests storage and camera permissions when required.

GiniCaptureCoordinator
---------------------

The `GiniCaptureCoordinator` is used to show the onboarding on the first run. After the camera was started
`GiniCaptureCoordinator#onCameraStarted()` is called and, if this is the first run, the `OnboardingFragment` is shown in
`GiniCaptureActivity#onShowOnboarding()`.

Camera Screen
-------------

It starts by showing the `CameraFragment`. When a picture was taken or file was imported using the document import button the
`CameraFragmentListener#onDocumentAvailable()` is called where either the Review Screen or the Analysis Screen is shown.

For imported files using the document import button the `CameraFragmentListener#onCheckImportedDocument()` is called to allow custom file
checks before continuing.

When a file was received from another app the `CameraFragment` is not shown. Instead the Review Screen or the Analysis Screen is started.

In case of an error the `CameraFragmentListener#onError()` will be called.

On first launch or when using the `Tipps` menu item the `OnboardingFragment` is shown. The Onboarding should be closed when the
`OnboardingFragmentListener#onCloseOnboarding()` is called.

The Help Screen is shown by clicking the help menu item (question mark).

If there is a `GiniCapture` instance and multi-page is enabled, it starts the Multi-Page Review Screen when the
`CameraFragmentListener#onProceedToMultiPageReviewScreen()` is called.

Help Screen
-----------

This screen is shown using the `HelpActivity`. This Activity is used with both the Component and Screen APIs. Customization options are
documented in the `HelpActivity`'s javadoc.

Review Screen
--------------

You should create a `GiniCapture` instance first. If you have done that, then analysis is executed internally
by the networking service implementation you have configured. When the user clicks the next button then
`ReviewFragmentListener#onProceedToAnalysisScreen()` is called.

Multi-Page Review Screen
------------------------

This screen requires a `GiniCapture` instance and is shown if you have enabled multi-page scanning. It shows the `MultiPageReviewFragment`
and goes to the Analysis Screen when `MultiPageReviewFragmentListener#onProceedToAnalysisScreen()` is called. If the user opted to go back
to the Camera Screen to add more pages the `MultiPageReviewFragmentListener#onReturnToCameraScreen()` method is called.

Analysis Screen
----------------

You should create a `GiniCapture` instance first. If you have done that, then analysis is executed internally
by the networking service implementation you have configured. When the `AnalysisFragment` receives the extractions the
`AnalysisFragmentListener#onExtractionsAvailable()` is called and you may proceed with the extractions as desired.

If there were no extractions, then the `AnalysisFragment` calls the `AnalysisFragmentListener#onProceedToNoExtractionsScreen()` method.

No Results Screen
-----------------

This screen is not shown for PDFs as it shows tips for taking better pictures of documents.

The `NoResultsFragment` is shown with tips for achieving better results. For pictures taken with the camera or files imported from the
document import button a back to camera button is shown. When this button is clicked the `NoResultsFragmentListener#onBackToCameraPressed()`
will be called.

ExtractionsActivity
-------------------

Displays the extractions with the possibility to send feedback to the Gini API. It only shows the extractions needed for transactions, we
call them the Pay5: payment recipient, IBAN, BIC, amount and payment reference.

Feedback should be sent only for the user visible fields. Other fields should be filtered out.

SingleDocumentAnalyzer
----------------------

Helps with managing the document analysis using our Gini Bank API Library.

Gini Bank API Library
====================

The Gini Bank API Library is not used directly. The default networking plugin, which was used when configuring and creating a `GiniCapture` instance,
takes care of communicating with the Gini Bank API.

Customization
=============

Customization options are detailed in each Screen API Activity's javadoc: `CameraActivity`, `HelpActivity`, `OnboardingActivity`,
`ReviewActivity` and `AnalysisActivity`.

To experiment with customizing the images used in the Gini Capture SDK you can copy the contents of the folder
`screenapiexample/customized-drawables` to `componentapiexample/src/main/res`.

Text customizations can be tried out by uncommenting and modifying the string resources in the
`componentapiexample/src/main/res/values/strings.xml`.

Text styles and fonts can be customized by uncommenting and altering the styles in the `componentapiexample/src/main/res/values/styles.xml`

To customize the colors you can uncomment and modify the color resources in the `componentapiexample/src/main/res/values/colors.xml`.

Customizing the opacity of the onboarding pages' background you can uncomment and modify the string resource in the
`componentapiexample/src/main/res/values/config.xml`.

ProGuard
========

A sample ProGuard configuration file is included in the Component API example app's directory called `proguard-rules.pro`.

The release build is configured to run ProGuard. You need a keystore with a key to sign it. Create a keystore with a key and provide them in
the `gradle.properties` or as arguments for the build command:
```
$ ./gradlew componentapiexample::assembleRelease \
    -PreleaseKeystoreFile=<path to keystore> \
    -PreleaseKeystorePassword=<keystore password> \
    -PreleaseKeyAlias=<key alias> \
    -PreleaseKeyPassword=<key password> \
    -PclientId=<Gini API client id> \
    -PclientSecret=<Gini API client secret>
```

