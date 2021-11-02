Migrating from the Gini Vision Library
======================================

Please note that this guide is for migrating from Gini Vision Library version 3.x or 4.x. In case you are using an older
version please consult our Gini Vision Library `migration guides
<https://developer.gini.net/gini-vision-lib-android/html/updating-to-3-0-0.html>`_ to update to the latest version.

Kotlin
------

We switched to Kotlin as our primary development language. The Gini Pay Bank SDK is still usable from Java, but we
recommend to upgrade to Kotlin to avoid the overhead incurred by the non-Java idiomatic style when using it with Java.

Gini Capture SDK
----------------

The `Gini Capture SDK <https://developer.gini.net/gini-capture-sdk-android/html/#>`_ supersedes the Gini Vision Library.

This migration guide will often refer to the Gini Capture SDK because it is used to fulfill the same functionality as
the Gini Vision Library did.

Gini Pay API Library
--------------------

The `Gini Pay API Library <https://github.com/gini/gini-pay-api-lib-android>`_ supersedes the Gini API SDK and is used
to communicate with the `Gini Pay API <https://pay-api.gini.net/documentation/#gini-pay-api-documentation-v1-0>`_.

You will only need to directly interact with the Gini Pay API Library if you implement a custom networking layer. If you
use the default networking implementation you don't need to interact with it.

Configuration
-------------

The entry point is the ``GiniPayBank`` singleton and to configure the capture feature you need to pass a
``CaptureConfiguration`` object to its ``setCaptureConfiguration()`` method.

The ``CaptureConfiguration`` contains the same options as the Gini Vision Library's ``GiniVision.Builder``.

The configuration is immutable and needs to be released before setting a new configuration.

This is how it was used in the Gini Vision Library:

.. code-block:: java

    GiniVision.cleanup(this)

    GiniVision.newInstance()
        .(...)
        .build()

This is how you need to use it with the Gini Pay Bank SDK:

.. code-block:: java

    GiniPayBank.releaseCapture(this)

    val captureConfiguration = CaptureConfiguration(
        ...
    )
    
    GiniPayBank.setCaptureConfiguration(captureConfiguration)

Requirements
------------

To check the requirements you need to call ``GiniPayBank.checkCaptureRequirements()``. It will return a
``RequirementsReport`` which has the same signature as the one in the Gini Vision Library.

This is how it was used in the Gini Vision Library:

.. code-block:: java

    val report = GiniVisionRequirements.checkRequirements(this)

    if (!report.isFulfilled) {
        report.requirementReports.forEach { report ->
            if (!report.isFulfilled) {
                (...)
            }
        }
    }

This is how you need to use it with the Gini Pay Bank SDK:

    .. code-block:: java
    
        val report = GiniPayBank.checkCaptureRequirements(this)
    
        if (!report.isFulfilled) {
            report.requirementReports.forEach { report ->
                if (!report.isFulfilled) {
                    (...)
                }
            }
        }

Screen API
----------

Launching the Screen API is done using the Android Result API. We provide the ``CaptureFlowContract()`` and you only need
to set an ``ActivityResultCallback<CaptureResult>`` to handle the result.

This is how it was used in the Gini Vision Library:

.. code-block:: java

    const val REQUEST_SCAN = 1

    fun launchCapture() {
        startActivityForResult(Intent(context, CameraActivity::class.java), REQUEST_SCAN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SCAN) {
            when (resultCode) {
                Activity.RESULT_CANCELED -> {
                    (...)
                }
                Activity.RESULT_OK -> {
                    (...)
                }
                CameraActivity.RESULT_ERROR -> {
                    (...)
                }
            }
        }
    }

This is how you need to use it with the Gini Pay Bank SDK:

.. code-block:: java

    val captureLauncher = registerForActivityResult(CaptureFlowContract(), ::onCaptureResult)

    fun launchCapture() {
        GiniPayBank.startCaptureFlow(captureLauncher)
    }

    fun onCaptureResult(result: CaptureResult) {
        when (result) {
            is CaptureResult.Success -> {
                (...)
            }
            is CaptureResult.Error -> {
                (...)
            }
            CaptureResult.Empty -> {
                (...)
            }
            CaptureResult.Cancel -> {
                (...)
            }
        }
    }

Component API
-------------

For the Component API you will interact with the Gini Capture SDK directly.

The most important changes compared to the Gini Vision Library are:

* deprecated code was removed,
* support for native activities and fragments was dropped,
* package was renamed from ``net.gini.android.vision.*`` to ``net.gini.android.capture.*``,
* ``GiniVision`` in class or interface names was renamed to ``GiniCapture``.

If you are already using Jetpack, then migrating the Component API should be fairly straight forward. In case you are
using native activities and fragments, then please first switch to using the Jetpack ``AppCompatActivity``.

Apply the following steps to migrate:

* rename imported packages: replace ``net.gini.android.vision`` with ``net.gini.android.capture``,
* remove deprecated fragment listener methods,
* rename class names: replace ``GiniVision`` with ``GiniCapture``.

Open With
---------

When receiving a file through an intent from another app the intent has to be passed to helper methods in the
``GiniPayBank`` singleton.

Screen API
~~~~~~~~~~

When using the Screen API we provide a helper method which uses the Activity Result API.

This is how it was used in the Gini Vision Library:

.. code-block:: java

    const val REQUEST_SCAN = 1

    private var fileImportCancellationToken: CancellationToken? = null

    fun launchGiniVisionForIntent(intent: Intent) {
        fileImportCancellationToken = GiniVision.getInstance().createIntentForImportedFiles(intent, this,
            object : AsyncCallback<Intent, ImportedFileValidationException> {
                override fun onSuccess(result: Intent) {
                    fileImportCancellationToken = null
                    startActivityForResult(result, REQUEST_SCAN)
                }

                override fun onError(exception: ImportedFileValidationException) {
                    fileImportCancellationToken = null
                    handleFileImportError(exception)
                }

                override fun onCancelled() {
                    fileImportCancellationToken = null
                }
            })
    }

This is how you need to use it with the Gini Pay Bank SDK:

.. code-block:: java

    private captureImportLauncher = registerForActivityResult(CaptureFlowImportContract(), ::onCaptureResult)

    private var fileImportCancellationToken: CancellationToken? = null

    fun launchCaptureForIntent(intent: Intent) {
        fileImportCancellationToken = GiniPayBank.startCaptureFlowForIntent(captureImportLauncher, this, intent)
    }

Component API
~~~~~~~~~~~~~

When using the Component API we provide a helper method to create a Gini Capture SDK ``Document`` which can be passed to
the review or analysis fragment.

This is how it was used in the Gini Vision Library:

.. code-block:: java

    private var fileImportCancellationToken: CancellationToken? = null

    fun launchGiniVisionForIntent(intent: Intent) {
        fileImportCancellationToken = GiniVision.getInstance().createDocumentForImportedFiles(intent, this,
            object : AsyncCallback<Document, ImportedFileValidationException> {
                override fun onSuccess(result: Document) {
                    fileImportCancellationToken = null
                    if (result.isReviewable()) {
                        launchMultiPageReviewScreen();
                    } else {
                        launchAnalysisScreen(result);
                    }
                }

                override fun onError(exception: ImportedFileValidationException) {
                    fileImportCancellationToken = null
                    handleFileImportError(exception)
                }

                override fun onCancelled() {
                    fileImportCancellationToken = null
                }
            }) 
    }   

This is how you need to use it with the Gini Pay Bank SDK:

.. code-block:: java

    private var fileImportCancellationToken: CancellationToken? = null

    fun launchGiniVisionForIntent(intent: Intent) {
        fileImportCancellationToken = GiniPayBank.createDocumentForImportedFiles(intent, this,
            object : AsyncCallback<Document, ImportedFileValidationException> {
                override fun onSuccess(result: Document) {
                    fileImportCancellationToken = null
                    if (result.isReviewable()) {
                        launchMultiPageReviewScreen();
                    } else {
                        launchAnalysisScreen(result);
                    }
                }

                override fun onError(exception: ImportedFileValidationException) {
                    fileImportCancellationToken = null
                    handleFileImportError(exception)
                }

                override fun onCancelled() {
                    fileImportCancellationToken = null
                }
            }) 
    }

Networking
----------

The networking abstraction layer works the same way as in the Gini Vision Library. The only changes are in the class and
interface names where ``GiniVision`` was replaced with ``GiniCapture``.

Default
~~~~~~~

Migrating the default networking implementation is straight forward:

* rename imported packages: replace ``net.gini.android.vision`` with ``net.gini.android.capture``,
* rename class names: replace ``GiniVision`` with ``GiniCapture``,
* use the Gini Pay Bank SDK capture configuration

This is how it was used in the Gini Vision Library:

.. code-block:: java

    val networkService = GiniVisionDefaultNetworkService.builder(this)
            .(...)
            .build();
    val networkApi = GiniVisionDefaultNetworkApi.builder()
            .withGiniVisionDefaultNetworkService(networkService)
            .build();
    
    GiniVision.cleanup(this)

    GiniVision.newInstance()
        .setGiniVisionNetworkService(networkService)
        .setGiniVisionNetworkApi(networkApi)
        .(...)
        .build()

This is how you need to use it with the Gini Pay Bank SDK:

.. code-block:: java

    val networkService = GiniCaptureDefaultNetworkService.builder(this)
            .(...)
            .build();
    val networkApi = GiniCaptureDefaultNetworkApi.builder()
            .withGiniVisionDefaultNetworkService(networkService)
            .build();

    GiniPayBank.releaseCapture(this)

    GiniPayBank.setCaptureConfiguration(
        CaptureConfiguration(
            networkService = networkService,
            networkApi = networkApi,
            ...
        )
    )

Custom
~~~~~~

Migrating a custom networking layer implementation is also straight forward:

* rename imported packages: replace ``net.gini.android.vision`` with ``net.gini.android.capture``,
* rename interface names: replace ``GiniVision`` with ``GiniCapture``,
* we recommend moving from the Gini API SDK to the newer Gini Pay API Library which offers kotlin coroutine support.

Event Tracking
--------------

Event tracking works the same way as in the GiniVisionLibrary. You only need to update the package name and set your
``EventTracker`` implementation when configuring the Gini Pay Bank SDK.

This is how it was used in the Gini Vision Library:

.. code-block:: java

    val eventTracker: EventTracker = (...)

    GiniVision.cleanup(this)

    GiniVision.newInstance()
        .setEventTracker(eventTracker)
        .(...)
        .build()

This is how you need to use it with the Gini Pay Bank SDK:

.. code-block:: java

    val eventTracker: EventTracker = (...)

    GiniPayBank.releaseCapture(this)

    GiniPayBank.setCaptureConfiguration(
        CaptureConfiguration(
            eventTracker = eventTracker,
            ...
        )
    )

Customization
-------------

Customization is done the same way via overriding of app resources. You only need to rename the assets:

* rename ``gv_`` prefixes to ``gc_``,
* replace ``GiniVision`` in theme and style names with ``GiniCapture``.