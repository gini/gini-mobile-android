Migrating from the Gini Vision Library
======================================

Please note that this guide is for migrating from Gini Vision Library version 3.x or 4.x. In case you are using an older
version please consult our Gini Vision Library `migration guides
<https://developer.gini.net/gini-vision-lib-android/html/updating-to-3-0-0.html>`_ to update to the latest version.

Kotlin
------

We switched to Kotlin as our primary development language. The Gini Bank SDK is still usable from Java, but we
recommend upgrading to Kotlin to avoid the overhead incurred by the non-Java idiomatic style when using it with Java.

Gini Capture SDK
----------------

The :root_html_path_capture_sdk:`Gini Capture SDK <index.html>` supersedes the Gini Vision Library.

This migration guide will often refer to the Gini Capture SDK because it is used to fulfill the same functionality as
the Gini Vision Library did.

Gini Bank API Library
--------------------

The `Gini Bank API Library <https://github.com/gini/gini-mobile-android/tree/main/bank-api-library>`_ supersedes the Gini API SDK and is used
to communicate with the `Gini Bank API <https://pay-api.gini.net/documentation/#gini-pay-api-documentation-v1-0>`_.

You will only need to directly interact with the Gini Bank API Library if you implement a custom networking layer. If you
use the default networking implementation you don't need to interact with it.

Configuration
-------------

The entry point is the ``GiniBank`` singleton and to configure the capture feature you need to pass a
``CaptureConfiguration`` object to its ``setCaptureConfiguration()`` method.

The ``CaptureConfiguration`` contains the same options as the Gini Vision Library's ``GiniVision.Builder``.

The configuration is immutable and needs to be released before setting a new configuration.

This is how it was used in the Gini Vision Library:

.. code-block:: java

    GiniVision.cleanup(this)

    GiniVision.newInstance()
        .(...)
        .build()

This is how you need to use it with the Gini Bank SDK:

.. code-block:: java

    GiniBank.releaseCapture(this)

    val captureConfiguration = CaptureConfiguration(
        ...
    )
    
    GiniBank.setCaptureConfiguration(captureConfiguration)

Requirements
------------

To check the requirements you need to call ``GiniBank.checkCaptureRequirements()``. It will return a
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

This is how you need to use it with the Gini Bank SDK:

    .. code-block:: java
    
        val report = GiniBank.checkCaptureRequirements(this)
    
        if (!report.isFulfilled) {
            report.requirementReports.forEach { report ->
                if (!report.isFulfilled) {
                    (...)
                }
            }
        }

Screen API
----------

We unified the Screen API and Component API into one public API. The new public API is based on the Screen API.

Launching the SDK is done using the Android Result API. We provide the ``CaptureFlowContract()`` and you only need
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

This is how you need to use it with the Gini Bank SDK:

.. code-block:: java

    val captureLauncher = registerForActivityResult(CaptureFlowContract(), ::onCaptureResult)

    fun launchCapture() {
        GiniBank.startCaptureFlow(captureLauncher)
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

The Component API allowed more UI customization options for the cost of a more difficult integration and maintenance. It
was based on fragments, and you had to manage navigation between them and also update the navigation whenever we introduced
breaking changes.

Maintaining the Component API along with the simpler Screen API required an increasing amount of effort as we added new
features. We decided therefore to unify both APIs and introduce the ability to inject fully custom UI elements.

The following steps will help you migrate to the new public API:

* If you used a custom navigation bar, then you can use the new ability to inject fully custom UI elements. For this you
  need to implement the ``NavigationBarTopAdapter`` interface and pass it to
  ``CaptureConfiguration``. The ``NavigationBarTopAdapter`` interface declares the
  contract your view has to fulfill and allows the SDK to ask for your view instance when needed.
* Launch the SDK and handle results as described above in the Screen API section.
* Remove all code related to interacting with the SDK's fragments. From now on the entry point is the one described in the Screen API section above.

Open With
---------

When receiving a file through an intent from another app the intent has to be passed to helper methods in the
``GiniBank`` singleton.

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

This is how you need to use it with the Gini Bank SDK:

.. code-block:: java

    private captureImportLauncher = registerForActivityResult(CaptureFlowImportContract(), ::onCaptureResult)

    private var fileImportCancellationToken: CancellationToken? = null

    fun launchCaptureForIntent(intent: Intent) {
        fileImportCancellationToken = GiniBank.startCaptureFlowForIntent(captureImportLauncher, this, intent)
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

Networking
----------

The networking abstraction layer works the same way as in the Gini Vision Library. The only changes are in the class and
interface names where ``GiniVision`` was replaced with ``GiniCapture``.

Default
~~~~~~~

Migrating the default networking implementation is straight forward:

* rename imported packages: replace ``net.gini.android.vision`` with ``net.gini.android.capture``,
* rename class names: replace ``GiniVision`` with ``GiniCapture``,
* use the Gini Bank SDK capture configuration

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

This is how you need to use it with the Gini Bank SDK:

.. code-block:: java

    val networkService = GiniCaptureDefaultNetworkService.builder(this)
            .(...)
            .build();
    val networkApi = GiniCaptureDefaultNetworkApi.builder()
            .withGiniVisionDefaultNetworkService(networkService)
            .build();

    GiniBank.releaseCapture(this)

    GiniBank.setCaptureConfiguration(
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
* if you are using the `Gini API SDK <https://developer.gini.net/gini-sdk-android/index.html>`_ you must replace it with
  the newer `Gini Bank API Library <https://developer.gini.net/gini-mobile-android/bank-api-library/library>`_ which
  offers kotlin coroutine support.

Event Tracking
--------------

Event tracking works the same way as in the GiniVisionLibrary. You only need to update the package name and set your
``EventTracker`` implementation when configuring the Gini Bank SDK.

This is how it was used in the Gini Vision Library:

.. code-block:: java

    val eventTracker: EventTracker = (...)

    GiniVision.cleanup(this)

    GiniVision.newInstance()
        .setEventTracker(eventTracker)
        .(...)
        .build()

This is how you need to use it with the Gini Bank SDK:

.. code-block:: java

    val eventTracker: EventTracker = (...)

    GiniBank.releaseCapture(this)

    GiniBank.setCaptureConfiguration(
        CaptureConfiguration(
            eventTracker = eventTracker,
            ...
        )
    )

Customization
-------------

Customization is done the same way via overriding of app resources.

You need to rename the assets first:
* rename ``gv_`` prefixes to ``gc_``,
* replace ``GiniVision`` in theme and style names with ``GiniCapture``.

After that you need to follow the Gini Capture SDK's :root_html_path_capture_sdk:`migration guide <migrate-to-2-0-0.html#overview-of-new-ui-customization-options>`
to migrate to the new customization options.