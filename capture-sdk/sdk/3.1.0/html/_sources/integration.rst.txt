Integration
===========

..
  Audience: Android dev who integrates for the first time
  Purpose: Describe what app configuration is needed, which preconditions have to be met, how to configure the SDK and how to run it.
  Content type: Getting started - as defined in the Docs for Developers book (https://docsfordevelopers.com/)

  Headers:
  h1 =====
  h2 -----
  h3 ~~~~~
  h4 ^^^^^

Android Manifest
----------------

Permissions
~~~~~~~~~~~

The Gini Capture SDK uses the camera therefore the camera permission is required:

.. code-block:: xml

    <manifest xmlns:android="http://schemas.android.com/apk/res/android"
        package="...">
        
        <uses-permission android:name="android.permission.CAMERA" />

    </manifest>

.. note::

    Make sure you request the camera permission before starting the SDK.

Predictive Back Gesture
~~~~~~~~~~~~~~~~~~~~~~~

Starting from Android 13 apps should apply changes to support `predictive back gestures
<https://developer.android.com/guide/navigation/predictive-back-gesture>`_. The Gini Capture SDK already contains the
necessary changes.

Once your app is ready to support it (by targeting Android 13 and `enabling it in the Android Manifest
<https://developer.android.com/guide/navigation/predictive-back-gesture#opt-predictive>`_), then no further changes are
required from you.

Requirements Check
------------------

We recommend running our runtime requirements check first before launching the Gini Capture SDK to ensure the device is
capable of taking pictures of adequate quality.

Simply run ``GiniCaptureRequirements.checkRequirements()`` and inspect the returned ``RequirementsReport`` for the result:

.. note::

    On Android 6.0 and later the camera permission is required before checking the requirements.

.. code-block:: java

    final RequirementsReport report = GiniCaptureRequirements.checkRequirements(this);
    if (!report.isFulfilled()) {
        final StringBuilder stringBuilder = new StringBuilder();
        report.getRequirementReports().forEach(requirementReport -> {
            if (!requirementReport.isFulfilled()) {
                stringBuilder.append(requirementReport.getRequirementId());
                stringBuilder.append(": ");
                stringBuilder.append(requirementReport.getDetails());
                stringBuilder.append("\n");
            }
        });
        Toast.makeText(this, "Requirements not fulfilled:\n" + stringBuilder,
                Toast.LENGTH_LONG).show();
    }

Configuration
-------------

Configuration and interaction is done using the ``GiniCapture`` singleton.

To configure and create a new instance use the ``GiniCapture.Builder`` returned by ``GiniCapture.newInstance()``. The
builder creates a new ``GiniCapture`` singleton which you will need to destroy later with ``GiniCapture.cleanup()``.
This will also free up any used resources.

You must call ``GiniCapture.cleanup()`` after the user has seen (and potentially corrected) the extractions. You
need to pass the updated extraction values to ``cleanup()``. If the SDK didn't return any extractions you can
pass in empty strings.

Failing to call ``GiniCapture.cleanup()`` will throw an ``IllegalStateException`` when
``GiniCapture.newInstance()`` is called again.

To view all the configuration options see the documentation of :root_dokka_path:`GiniCapture.Builder
<sdk/net.gini.android.capture/-gini-capture/-builder/index.html?query=public%20class%20Builder>`.

Information about the configurable features are available on the `Features <features.html>`_ page and UI customization
options can be viewed in the `Customization Guide <customization-guide.html>`_.

Tablet Support
---------------

The Gini Capture SDK can be used on tablets, too. Some UI elements adapt to the larger screen to offer the best user
experience for tablet users.

Many tablets with at least 8MP cameras don't have an LED flash. Therefore we don't require flash for tablets. For this
reason the extraction quality on those tablets might be lower compared to smartphones.

On tablets landscape orientation is also supported (smartphones are portrait only). We advise you to test your
integration on tablets in both orientations.

In landscape the camera screen's UI displays the camera trigger button on the right side of the screen. Users
can reach the camera trigger more easily this way. The camera preview along with the document corner guides are shown in
landscape to match the device's orientation.

Other UI elements on all the screens maintain their relative position and the screen layouts are scaled automatically to
fit the current orientation.

Networking
----------

Communication with the Gini Bank API is not part of the Gini Capture SDK in order to allow you the freedom to use a
networking implementation of your own choosing.

.. note::

    You should have received Gini Bank API client credentials from us. Please get in touch with us in case you don’t have
    them. Without credentials you won't be able to use the Gini Bank API.

We provide the ``GiniCaptureNetworkService`` interface which is used to upload, analyze and delete documents. See the
:root_dokka_path:`reference documentation <sdk/net.gini.android.capture.network/-gini-capture-network-service/index.html>`
for details.

Default Implementation
~~~~~~~~~~~~~~~~~~~~~~

The quickest way to add networking is to use the `Gini Capture Network
Library <https://github.com/gini/gini-mobile-android/tree/main/capture-sdk/default-network>`_.

To use it add the ``gini-capture-network-lib`` dependency to your app's ``build.gradle``:

.. code-block:: groovy

    dependencies {
        ...
        implementation 'net.gini.android:gini-capture-sdk-default-network:3.1.0'
    }

For the Gini Capture SDK to be aware of the default implementation create an instance and pass
it to the builder of ``GiniCapture``:

.. code-block:: java

    GiniCaptureDefaultNetworkService networkService = 
        GiniCaptureDefaultNetworkService.builder((Context) this)
            .setClientCredentials(myClientId, myClientSecret, myEmailDomain)
            .build();

    GiniCapture.newInstance()
        .setGiniCaptureNetworkService(networkService)
        .build();

The default implementation follows the builder pattern. See the documentation of
:root_dokka_path_default_network_library:`GiniCaptureDefaultNetworkService.Builder <default-network/net.gini.android.capture.network/-gini-capture-default-network-service/-builder/index.html>`
for configuration options.

Retrieve the Analyzed Document
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

You can call ``GiniCaptureDefaultNetworkService.getAnalyzedGiniApiDocument()`` after the Gini Capture SDK has returned
extractions to your application. It returns the Gini Bank API document which was created when the user uploaded an
image or pdf for analysis.

When extractions were retrieved without using the Gini Bank API, then it will return ``null``. For example when the
extractions came from an EPS QR Code.

.. note::

    Make sure to call it before calling ``GiniCapture.cleanup()``. Otherwise the analyzed document won't be available anymore.

Custom Implementation
~~~~~~~~~~~~~~~~~~~~~

You can also provide your own networking by implementing the ``GiniCaptureNetworkService`` interface. Pass your
instances to the builder of ``GiniCapture`` as shown above.

You may also use the `Gini Bank API Library <https://github.com/gini/gini-mobile-android/tree/main/bank-api-library>`_ for Android
or implement communication with the `Gini Bank API <https://pay-api.gini.net/documentation/>`_ yourself.

Cleanup and Sending Feedback
----------------------------

Your app should clean up the SDK and provide feedback for the extractions the Gini Bank API delivered. Feedback should
be sent only for the extractions the user has seen and accepted (or corrected).

.. code-block:: java

    void stopGiniCaptureSDK() {
        // After the user has seen and potentially corrected the extractions
        // cleanup the SDK while passing in the final extraction values 
        // which will be used as feedback to improve the future extraction accuracy:
        GiniCapture.cleanup((Context) this,
                paymentRecipient,
                paymentReference,
                paymentPurpose,
                iban,
                bic,
                amount
            )
    }

We provide a sample test case `here
<https://github.com/gini/gini-mobile-android/blob/capture-sdk%3B3.1.0/capture-sdk/default-network/src/androidTest/java/net/gini/android/capture/network/ExtractionFeedbackIntegrationTest.kt>`_
to verify that extraction feedback sending works. You may use it along with the example pdf and json files as a starting
point to write your own test case.

The sample test case is based on the Bank API documentation's `recommended steps
<https://pay-api.gini.net/documentation/#test-example>`_ for testing extraction feedback sending.

For additional information about feedback see the `Gini Bank API documentation
<https://pay-api.gini.net/documentation/#send-feedback-and-get-even-better-extractions-next-time>`_.

Capturing documents
-------------------

To launch the Gini Capture SDK you only need to:

#. Request camera access,
#. Configure a new instance of ``GiniCapture``,
#. Launch the ``CameraActivity``,
#. Handle the extraction results,
#. Cleanup the SDK by calling ``GiniCapture.cleanup()`` while also providing the required extraction feedback to improve
   the future extraction accuracy. You don't need to implement any extra steps, just follow the recommendations below:
    * Please provide values for all necessary fields, including those that were not extracted.
    * Provide the final data approved by the user (and not the initially extracted only).
    * Do cleanup after TAN verification.

The following diagram shows the interaction between your app and the SDK:

.. figure:: _static/integration/Screen-API.png
   :alt: Diagram of interaction between your app and the SDK
   :width: 100%

.. note::

   Check out the `example app
   <https://github.com/gini/gini-mobile-android/tree/main/capture-sdk/screen-api-example-app>`_ to see how an integration could look
   like.

The ``CameraActivity`` can return with the following result codes:

* ``Activity.RESULT_OK``

   Document was analyzed and the extractions are available in the ``EXTRA_OUT_EXTRACTIONS`` result extra. It contains a
   ``Bundle`` with the extraction labels as keys and ``GiniCaptureSpecificExtraction`` parcelables as values.

* ``Activity.RESULT_CANCELED``
   
   User has canceled the Gini Capture SDK.

* ``CameraActivity.RESULT_ERROR``

   An error occured and the details are available in the ``EXTRA_OUT_ERROR`` result extra. It contains a parcelable extra
   of type ``GiniCaptureError`` detailing what went wrong.

* ``CameraActivity.RESULT_ENTER_MANUALLY``

   The document analysis finished with no results or an error and the user clicked the "Enter manually" button.

The following example shows how to launch the Gini Capture SDK and how to handle the results:

.. code-block:: java

    void launchGiniCapture() {
        // Make sure camera permission has been already granted at this point.
        
        // Check that the device fulfills the requirements.
        RequirementsReport report = GiniCaptureRequirements.checkRequirements((Context) this);
        if (!report.isFulfilled()) {
            handleUnfulfilledRequirements(report);
            return;
        }
        
        // Instantiate the networking implementations.
        GiniCaptureNetworkService networkService = ...
        
        // Configure GiniCapture and create a new singleton instance.
        GiniCapture.newInstance()
                .setGiniCaptureNetworkService(networkService)
                ...
                .build();
                
        // Launch the CameraActivity and wait for the result.
        Intent intent = new Intent(this, CameraActivity.class);
        startActivityForResult(intent, GINI_CAPTURE_REQUEST);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode,
            final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GINI_CAPTURE_REQUEST) {
            switch (resultCode) {
                case Activity.RESULT_CANCELED:
                    GiniCapture.cleanup(this, "", "",
                                "", "","", Amount.EMPTY);
                    break;

                case Activity.RESULT_OK:
                    // Retrieve the extractions
                    Bundle extractionsBundle = data.getBundleExtra(
                            CameraActivity.EXTRA_OUT_EXTRACTIONS);
                    
                    // Retrieve the extractions from the extractionsBundle
                    Map<String, GiniCaptureSpecificExtraction> extractions = new HashMap<>();
                    for (String extractionLabel : extractionsBundle.keySet()) {
                        GiniCaptureSpecificExtraction extraction = extractionsBundle.getParcelable(extractionLabel);
                        extractions.put(extractionLabel, extraction);
                    }
                    handleExtractions(extractions);

                    break;

                case CameraActivity.RESULT_ERROR:
                    // Something went wrong, retrieve and handle the error
                    final GiniCaptureError error = data.getParcelableExtra(
                            CameraActivity.EXTRA_OUT_ERROR);
                    if (error != null) {
                        handleError(error);
                    }
                    GiniCapture.cleanup(this, "", "",
                            "", "","", Amount.EMPTY);
                    break;

                case CameraActivity.RESULT_ENTER_MANUALLY:
                    handleEnterManually();
                    GiniCapture.cleanup(this, "", "",
                            "", "","", Amount.EMPTY);
                    break;
            }
        }
    }

    void stopGiniCaptureSDK() {
        // After the user has seen and potentially corrected the extractions
        // cleanup the SDK while passing in the final extraction values 
        // which will be used as feedback to improve the future extraction accuracy:
        GiniCapture.cleanup((Context) this,
                paymentRecipient,
                paymentReference,
                paymentPurpose,
                iban,
                bic,
                amount
            )
    }
