Features
========

..
  Headers:
  h1 =====
  h2 -----
  h3 ~~~~~
  h4 +++++
  h5 ^^^^^

The Gini Capture SDK provides various features you can enable and configure. All the features are configured during
creation of a new ``GiniCapture`` instance. Specifically the ``GiniCapture.Builder`` is used to configure the Gini
Capture SDK. It's :root_dokka_path:`reference documentation
<sdk/net.gini.android.capture/-gini-capture/-builder/index.html>` lists all the options.

.. note::

    Some features require additional contractual agreements and may not be used without prior confirmation. Please get
    in touch with us in case you are not sure which features your contract includes.

The following sections list all the features along with the related configuration options.

Document Capture
----------------

This is the core feature of the Gini Capture SDK. It enables your app to capture documents with the camera and prepares
them to be analyzed by the Gini Bank API.

Custom UI Elements
~~~~~~~~~~~~~~~~~~

Certain elements of the UI can now be fully customized via UI injection. It utilizes view adapter interfaces which you
can implement and pass to ``GiniCapture`` when configuring the SDK. These interfaces declare the contract the injected
view has to fulfill and allow the SDK to ask for your view instance when needed.

Top Navigation Bar
++++++++++++++++++

To inject your own navigation bar view implement the ``NavigationBarTopAdapter`` and pass it to
``GiniCapture.newInstance().setNavigationBarTopAdapter()``. Your view will then be displayed on all screens as the top
navigation bar.

Bottom Navigation Bar
+++++++++++++++++++++

You can opt to show a bottom navigation bar. To enable it pass ``true`` to
``GiniCapture.newInstance().setBottomNavigationBarEnabled()``.

.. note::

    The top navigation bar will still be used, but its functionality will be limited to showing the screen's title and
    an optional close button. Please inject a custom top navigation bar if your design requires it even if you have
    enabled the bottom navigation bar.

Each screen has a slightly different bottom navigation bar because they contain screen specific call-to-action buttons.

To inject your own views implement each screen's view adapter interface (e.g., ``OnboardingNavigationBarBottomAdapter``)
and pass it to ``GiniCapture`` (e.g., ``GiniCapture.newInstance().setOnboardingNavigationBarBottomAdapter()``). Your
view will then be displayed on the relevant screen.

Onboarding
~~~~~~~~~~

The onboarding feature presents essential information to the user on how to best capture documents.

You can customize the onboarding in the following ways:

* Disable showing the onboarding at first run:
   By default the onboarding is shown at first run. To disable this pass ``false`` to
   ``GiniCapture.Builder.setShouldShowOnboardingAtFirstRun()``.

* Customize the onboarding pages:
   If you wish to show different onboarding pages then pass a list of ``OnboardingPage`` objects to
   ``GiniCapture.Builder.setCustomOnboardingPages()``.

* Force show the onboarding:
   If you wish to show the onboarding after the first run then pass ``true`` to
   ``GiniCapture.Builder.setShouldShowOnboarding()``.

* Animate illustrations by injecting custom views:
   If you need to animate the illustrations on the onboarding pages implement the ``OnboardingIllustrationAdapter``
   interface to inject a view that can animate images (e.g., `Lottie <https://github.com/airbnb/lottie-android>`_) and
   pass it to the relevant onboarding illustration adapter setters (e.g.,
   ``setOnboardingAlignCornersIllustrationAdapter()``) when building the ``GiniCapture`` instance. The `reference
   documentation
   <https://developer.gini.net/gini-mobile-android/capture-sdk/sdk/dokka/sdk/net.gini.android.capture/-gini-capture/-builder/index.html>`_
   of ``GiniCapture.Builder`` lists all the setters.

Single Page
~~~~~~~~~~~

By default, the Gini Capture SDK is configured to capture single page documents. No further configuration is required for
this.

Multi-Page
~~~~~~~~~~

The multi-page feature allows the SDK to capture documents with multiple pages.

To enable this simply pass ``true`` to ``GiniCapture.Builder.setMultiPageEnabled()``.

Camera
~~~~~~

* Enable the flash toggle button:
   To allow users toggle the camera flash pass ``true`` to ``GiniCapture.Builder.setFlashButtonEnabled()``.

* Turn off flash by default:
   Flash is on by default, and you can turn it off by passing ``false`` to ``GiniCapture.Builder.setFlashOnByDefault()``.

QR Code Scanning
----------------

By using the Google Mobile Vision API the SDK can read payment data from QR Codes. We support the `BezahlCode
<http://www.bezahlcode.de/>`_ , `EPC069-12
<https://www.europeanpaymentscouncil.eu/document-library/guidance-documents/quick-response-code-guidelines-enable-data-capture-initiation>`_
(`Stuzza (AT) <https://www.stuzza.at/de/zahlungsverkehr/qr-code.html>`_ and `GiroCode (DE)
<https://www.girocode.de/rechnungsempfaenger/>`_) and `EPS <https://eservice.stuzza.at/de/eps-ueberweisung-dokumentation/category/5-dokumentation.html>`_ formats.

When a supported QR code is detected with valid payment data, white camera frame will turn into the green color with proper message that QR code is detected.

If the QR code does not have a supported payment format, white camera frame will turn into the yellow color with proper message that QR code is not supported.

Payment information extraction starts immediately after supported QR code is successfully scanned. The extractions are
returned in the result of the ``CameraActivity``.

QR Code Scanning is available on devices running Android with Google Play Services installed.

To enable this feature simply pass ``true`` to ``GiniCapture.Builder.setQRCodeScanningEnabled``.

.. important::

    When your application is installed Google Mobile Services will download libraries to the device in order to do QR
    code detection. If another app already uses QR code detection on the device the library won't be downloaded again.
    Under certain circumstances (user not online, slow connection or lack of sufficient storage space) the libraries
    will not be ready at the time your app starts the camera screen and QR code detection will be silently disabled
    until the next time the camera screen starts.


Only QR Code Scanning
----------------

The app supports QR code only scanning mode. To enable this feature simply pass ``true`` to ``GiniCapture.Builder.setOnlyQRCodeScanning``.
Enabling this feature removes all UI elements related to taking pictures from the Camera screen and leaves only QR code-related UI elements.

.. note::

    To use this feature ``setQRCodeScanningEnabled`` must be set to ``true`` otherwise ``setOnlyQRCodeScanning`` will be ignored.

Document Import
---------------

This feature enables the Gini Capture SDK to import documents from the camera screen. When it's enabled an additional
button is shown next to the camera trigger. Using this button allows the user to pick either an image or a pdf from the
device.

You can specify the document types the user will be able to select when enabling this feature. You can enable only
images, only PDFs or both images and PDFs.

To enable it simply pass a ``DocumentImportEnabledFileTypes`` enum value to
``GiniCapture.Builder.setDocumentImportEnabledFileTypes()``.

Android Manifest
~~~~~~~~~~~~~~~~

You need to declare the ``READ_EXTERNAL_STORAGE`` permission in your app's ``AndroidManifest.xml``:

.. code-block:: xml

    <manifest xmlns:android="http://schemas.android.com/apk/res/android"
        package="...">
        
        <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />

    </manifest>

When targeting Android 13 and later you will also have to declare the ``READ_MEDIA_IMAGES`` permission:

.. code-block:: xml

    <manifest xmlns:android="http://schemas.android.com/apk/res/android"
        package="...">

        <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />

    </manifest>

If the permission has not been granted the Gini Capture SDK will prompt the user to grant the permission when they use
the document import feature.

You will also need to declare that your app queries for apps that can handle intents to pick or open image and pdf
documents (you can read more about package visibility filtering introduced in Android 11
`here <https://developer.android.com/training/package-visibility>`_) :

.. code-block:: xml

    <queries>
        <intent>
            <action android:name="android.intent.action.PICK" />
            <data android:mimeType="image/*" />
        </intent>
        <intent>
            <action android:name="android.intent.action.OPEN_DOCUMENT" />
            <data android:mimeType="image/*" />
        </intent>
        <intent>
            <action android:name="android.intent.action.OPEN_DOCUMENT" />
            <data android:mimeType="application/pdf" />
        </intent>
    </queries>

Intercepting the imported document
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

You can intercept the imported document and deny the Gini Capture SDK from using it.

You need to subclass the ``CameraActivity`` and override the
``onCheckImportedDocument`` method. See it's :root_dokka_path:`reference documentation
<sdk/net.gini.android.capture.camera/-camera-activity/on-check-imported-document.html>` for details.

File Import (Open With)
-----------------------

The file import (or "open with") feature allows importing of files from other apps via Android's "open with" or "share"
functionality.

.. note::

    We are using the term ``file import`` to refer to the "open with" feature within the Gini Capture SDK. From the
    point of view of the SDK files are imported into the SDK from an outside source. It is not aware and cannot set
    configuration related to enabling the client app to receive files via Android's "open with" or "share"
    functionality.

To enable it pass ``true`` to ``GiniCapture.Builder.setFileImportEnabled()``.

In addition to enabling it your app needs to declare intent filters for receiving PDFs and/or images from other apps and
then forward the incoming intent to the Gini Capture SDK.

Registering pdf and image file types
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Add the following intent filter to the activity in your ``AndroidManifest.xml`` you wish to receive incoming PDFs and
images:

.. code-block:: xml

    <activity android:name=".ui.MyActivity">
        <!-- Receiving images: -->
        <intent-filter
            android:label="@string/label_for_image_open_with">
            <action android:name="android.intent.action.VIEW" />
            <action android:name="android.intent.action.SEND" />
            <!-- The below SEND_MULTIPLE action is only needed if you enabled scanning of multi-page documents: -->
            <action android:name="android.intent.action.SEND_MULTIPLE" />
            <category android:name="android.intent.category.DEFAULT" />
            <data android:mimeType="image/*" />
        </intent-filter>
        <!-- Receiving pdfs: -->
        <intent-filter
            android:label="@string/label_for_pdf_open_with">
            <action android:name="android.intent.action.VIEW" />
            <action android:name="android.intent.action.SEND" />
            <category android:name="android.intent.category.DEFAULT" />
            <data android:mimeType="application/pdf" />
        </intent-filter>
    </activity>

.. note::

    We recommend adding `ACTION_VIEW <https://developer.android.com/reference/android/content/Intent.html#ACTION_VIEW>`_
    to the intent filter to also allow users to send pdfs and images to your app from apps that don’t implement sharing
    with `ACTION_SEND <https://developer.android.com/reference/android/content/Intent.html#ACTION_SEND>`_ but enable
    viewing the pdf or file with other apps.

Handling Imported Files
~~~~~~~~~~~~~~~~~~~~~~~

When your app is requested to handle a pdf or an image your activity (declaring the intent filter shown above) is
launched or resumed (``onNewIntent()``) with an Intent having ``ACTION_VIEW`` or ``ACTION_SEND``.

We recommend checking whether the Intent has the required action before proceeding with it:

.. code-block:: java

    String action = intent.getAction();
    if (Intent.ACTION_VIEW.equals(action) || Intent.ACTION_SEND.equals(action)) {
        ...
    }

You need to create an Intent for launching the Gini Capture SDK with
``GiniCapture.getInstance().createIntentForImportedFile()`` or if you enabled scanning of multi-page documents
``GiniCapture.getInstance().createIntentForImportedFiles()``. The first method will throw an
``ImportedFileValidationException``, if the file was invalid and the latter will return the same exception in the
callback.

.. code-block:: java

    void startGiniCaptureSDKForImportedFile(final Intent importedFileIntent) {
        // Configure the Gini Capture SDK first
        configureGiniCapture();
        if (GiniCapture.hasInstance() && GiniCapture.getInstance().isMultiPageEnabled()) {
            mFileImportCancellationToken = GiniCapture.getInstance().createIntentForImportedFiles(
                    importedFileIntent, (Context) this,
                    new AsyncCallback<Intent, ImportedFileValidationException>() {
                        @Override
                        public void onSuccess(final Intent result) {
                            mFileImportCancellationToken = null;
                            startActivityForResult(result, REQUEST_SCAN);
                        }

                        @Override
                        public void onError(final ImportedFileValidationException exception) {
                            mFileImportCancellationToken = null;
                            handleFileImportError(exception);
                        }

                        @Override
                        public void onCancelled() {
                            mFileImportCancellationToken = null;
                        }
                    });
        } else {
            try {
                final Intent giniCaptureIntent =
                        GiniCapture.createIntentForImportedFile(
                                importedFileIntent,
                                (Context) this, null, null);
                startActivityForResult(giniCaptureIntent, REQUEST_SCAN);
            } catch (final ImportedFileValidationException e) {
                e.printStackTrace();
                handleFileImportError(e);
            }
        }
    }

    void handleFileImportError(final ImportedFileValidationException exception) {
        String message = ...
        if (exception.getValidationError() != null) {
            // Get the default message
            message = getString(exception.getValidationError().getTextResource());
            // Or use custom messages
            switch (exception.getValidationError()) {
                case TYPE_NOT_SUPPORTED:
                    message = ...
                    break;
                case SIZE_TOO_LARGE:
                    message = ...
                    break;
                case TOO_MANY_PDF_PAGES:
                    message = ...
                    break;
                case PASSWORD_PROTECTED_PDF:
                    message = ...
                    break;
                case TOO_MANY_DOCUMENT_PAGES:
                    message = ...
                    break;
            }
        }
        new AlertDialog.Builder((Context) this)
                .setMessage(message)
                .setPositiveButton("OK", (dialogInterface, i) -> finish())
                .show();
    }

If scanning of multi-page documents is enabled then either the multi-page review screen or the analysis screen will be
launched. PDFs cannot be reviewed by the user and in that case the analysis screen is launched directly. You should not
expect the review screen to be launched every time.

When multi-page is disabled then the difference is only that the single-page review screen will be used instead.

Help Screen Customization
-------------------------

You can show your own help screens in the Gini Capture SDK. These screens could be shown from a list on the main Help screen opened from the Camera screen
navigation bars.
You can pass the title and activity for each screen to the
``GiniCapture.Builder`` using a list of ``HelpItem.Custom`` objects:

.. code-block:: java

    List<HelpItem.Custom> customHelpItems = new ArrayList<>();

    customHelpItems.add(new HelpItem.Custom(R.string.custom_help_screen_title,
            new Intent((Context) this, CustomHelpActivity.class)));

    GiniCapture.newInstance()
            .setCustomHelpItems(customHelpItems)
            .build();

You can also disable the supported formats help screen by passing ``false`` to
``GiniCapture.Builder.setSupportedFormatsHelpScreenEnabled()``.

Furthermore you can show back navigation button on bottom navigation bar. You can pass your custom ``HelpNavigationBarBottomAdapter`` implementation to
``GiniCapture.Builder``:

.. code-block:: java

    CustomHelpNavigationBarBottomAdapter customHelpNavigationBarBottomAdapter = new CustomHelpNavigationBarBottomAdapter();

    GiniCapture.newInstance()
            .setHelpNavigationBarBottomAdapter(customHelpNavigationBarBottomAdapter)
            .build();

Analysis Screen Customization
-----------------------------

.. note::

    This screen does not show a bottom navigation bar even if the value passed to ``GiniCapture.newInstance().setBottomNavigationBarEnabled()`` is ``true``

You can show customized activity indicator on this screen. You can pass your custom ``CustomLoadingIndicatorAdapter`` implementation to
``GiniCapture.Builder`` :

.. code-block:: java

    CustomLottiLoadingIndicatorAdapter customLottiLoadingIndicatorAdapter = new CustomLottiLoadingIndicatorAdapter(getResources().getIdentifier("custom_loading", "raw", this.getPackageName()));

    GiniCapture.newInstance()
            .setLoadingIndicatorAdapter(customLottiLoadingIndicatorAdapter)
            .build();

Error Screen Customization
--------------------------

You can show custom back navigation button on bottom navigation bar. You can pass your custom ``ErrorNavigationBarBottomAdapter`` implementation to
``GiniCapture.Builder``:

.. code-block:: java

    CustomErrorNavigationBarBottomAdapter customErrorNavigationBarBottomAdapter = new CustomErrorNavigationBarBottomAdapter();

    GiniCapture.newInstance()
            .setErrorNavigationBarBottomAdapter(customErrorNavigationBarBottomAdapter)
            .build();

You can show your own UI if an error occured and the user chooses to enter details manually. For this you must handle ``CameraActivity.RESULT_ENTER_MANUALLY``
result code, which is returned by the SDK when the user clicks the "Enter manually" button on the error screen, in your main activity.

.. code-block:: java

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode,
                                    final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_SCAN) {
             (...)
             if (resultCode == CameraActivity.RESULT_ENTER_MANUALLY) {
                 handleEnterManuallyAction();
             }
             return;

            switch (resultCode) {
                case CameraActivity.RESULT_ENTER_MANUALLY:
                    handleEnterManuallyAction();
                    break;
                (...)
            }
            (...)
        }

No results Screen Customization
-------------------------------

You can show custom back navigation button on bottom navigation bar. You can pass your custom ``NoResultsNavigationBarBottomAdapter`` implementation to
``GiniCapture.Builder``:

.. code-block:: java

    CustomNoResultsNavigationBarBottomAdapter customNoResultsNavigationBarBottomAdapter = new CustomNoResultsNavigationBarBottomAdapter();

    GiniCapture.newInstance()
            .setNoResultsNavigationBarBottomAdapter(customNoResultsNavigationBarBottomAdapter)
            .build();

You can show your own UI if the document analysis returned with no result and the user chooses to enter details manually. For this you must handle ``CameraActivity.RESULT_ENTER_MANUALLY``
result code, which is returned by the SDK when the user clicks the "Enter manually" button on the no results screen, in your main activity.

.. code-block:: java

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode,
                                    final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_SCAN) {
             (...)
             if (resultCode == CameraActivity.RESULT_ENTER_MANUALLY) {
                 handleEnterManuallyAction();
             }
             return;

            switch (resultCode) {
                case CameraActivity.RESULT_ENTER_MANUALLY:
                    handleEnterManuallyAction();
                    break;
                (...)
            }
            (...)
        }

Event Tracking
--------------

You have the possibility to track various events which occur during the usage of the Gini Capture SDK.

To subscribe to the events you need to implement the ``EventTracker`` interface and pass it to the builder when creating
a new ``GiniCapture`` instance:

.. code-block:: java

    GiniCapture.newInstance()
        .setEventTracker(new MyEventTracker());
        .build();

In ``MyEventTracker`` you can handle the events you are interested in.

.. code-block:: java

    class MyEventTracker implements EventTracker {

        @Override
        public void onCameraScreenEvent(final Event<CameraScreenEvent> event) {
            switch (event.getType()) {
                case TAKE_PICTURE:
                    // handle the picture taken event
                    break;
                case HELP:
                    // handle the show help event
                    break;
                case EXIT:
                    // handle the exit event
                    break;
            }
        }

        @Override
        public void onOnboardingScreenEvent(final Event<OnboardingScreenEvent> event) {
            (...)
        }

        @Override
        public void onAnalysisScreenEvent(final Event<AnalysisScreenEvent> event) {
            (...)
        }

        @Override
        public void onReviewScreenEvent(final Event<ReviewScreenEvent> event) {
            (...)
        }

    }

Events
~~~~~~

Event types are partitioned into different domains according to the screens that they appear on. Each domain has a
number of event types. Some events may supply additional details in a map.

========================  ===================================================================  =====================================================  ==========================
Domain                    Event enum value and details map keys                                Comment                                                Introduced in (updated in)
========================  ===================================================================  =====================================================  ==========================
Onboarding                ``OnboardingScreenEvent.START``                                      Onboarding started                                     1.0.0
Onboarding                ``OnboardingScreenEvent.FINISH``                                     User completes onboarding                              1.0.0
Camera Screen             ``CameraScreenEvent.EXIT``                                           User closes the camera screen                          1.0.0
Camera Screen             ``CameraScreenEvent.HELP``                                           User taps "Help" on the camera screen                  1.0.0
Camera Screen             ``CameraScreenEvent.TAKE_PICTURE``                                   User takes a picture                                   1.0.0
Review Screen             ``ReviewScreenEvent.BACK``                                           User goes back from the review screen                  1.0.0
Review Screen             ``ReviewScreenEvent.NEXT``                                           User advances from the review screen                   1.0.0
Review Screen             ``ReviewScreenEvent.UPLOAD_ERROR``                                   Upload error in the review screen                      1.0.0
                          ``ReviewScreenEvent.UPLOAD_ERROR_DETAILS_MAP_KEY.MESSAGE``
                          ``ReviewScreenEvent.UPLOAD_ERROR_DETAILS_MAP_KEY.ERROR_OBJECT``
Analysis Screen           ``AnalysisScreenEvent.CANCEL``                                       User cancels the process during analysis               1.0.0
Analysis Screen           ``AnalysisScreenEvent.ERROR``                                        The analysis ended with an error.                      1.0.0
                          ``AnalysisScreenEvent.ERROR_DETAILS_MAP_KEY.MESSAGE``
                          ``AnalysisScreenEvent.ERROR_DETAILS_MAP_KEY.ERROR_OBJECT``
Analysis Screen           ``AnalysisScreenEvent.RETRY``                                        The user decides to retry after an analysis error.     1.0.0
========================  ===================================================================  =====================================================  ==========================

The supported events are listed for each screen in a dedicated enum. You can view these enums in our
:root_dokka_path:`reference documentation <sdk/net.gini.android.capture.tracking/index.html>`.

Error Logging
-------------

The SDK logs errors to the Gini Bank API when the default networking implementation is used (see the `Default networking
implementation <integration.html#default-implementation>`_ section).

You can disable the default error logging by passing ``false`` to ``GiniCapture.Builder.setGiniErrorLoggerIsOn()``.

If you would like to get informed of error logging events you can pass your implementation of the
``ErrorLoggerListener`` interface to ``GiniCapture.Builder``:

.. code-block:: java

    GiniCapture.newInstance()
        .setCustomErrorLoggerListener(new MyErrorLoggerListener())
        .build();