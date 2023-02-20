Invoice Capture Features
========================

Features from the Gini Capture SDK
----------------------------------

The capture feature uses our `Gini Capture SDK <https://github.com/gini/gini-mobile-android/tree/main/capture-sdk>`_. All features
listed in its :root_html_path_capture_sdk:`documentation <features.html>` can be used here
as well.

An important difference is in how you configure the capture features. In the Gini Bank SDK you need to use the
``CaptureConfiguration`` instead of the Gini Capture SDK's ``GiniCapture.Builder``. The configuration names are the same
so you can easily map them to the ``CaptureConfiguration``.

File Import (Open With)
~~~~~~~~~~~~~~~~~~~~~~~

Another difference is related to the :root_html_path_capture_sdk:`file import <features.html#file-import-open-with>` (or
"open with") feature which allows importing of files from other apps via Android's "open with" or "share" functionality.

To handle imported files using the Gini Bank SDK you need to register an activity result handler with the
``CaptureFlowImportContract()`` and then pass the incoming intent to
``GiniBank.startCaptureFlowForIntent()``:

.. code-block:: java

    // Use the androidx's Activity Result API to register a handler for the capture result.
    val captureImportLauncher = registerForActivityResult(CaptureFlowImportContract()) { result: CaptureResult ->
        when (result) {
            is CaptureResult.Success -> {
                handleExtractions(result.specificExtractions)
            }
            is CaptureResult.Error -> {
                when (result.value) {
                    is ResultError.Capture -> {
                        val captureError: GiniCaptureError = (result.value as ResultError.Capture).giniCaptureError
                        handleCaptureError(captureError)
                    }
                    is ResultError.FileImport -> {
                        val fileImportError = result.value as ResultError.FileImport
                        handleFileImportError(fileImportError)
                    }
                }
            }
            CaptureResult.Empty -> {
                handleNoExtractions()
            }
            CaptureResult.Cancel -> {
                handleCancellation()
            }
        }
    }

    fun handleFileImportError(exception: ImportedFileValidationException) {
        var message = ...
        exception.validationError?.let { validationError ->
            // Get the default message
            message = getString(validationError.textResource)
            // Or use custom messages
            message = when (validationError) {
                FileImportValidator.Error.TYPE_NOT_SUPPORTED -> ...
                FileImportValidator.Error.SIZE_TOO_LARGE -> ...
                FileImportValidator.Error.TOO_MANY_PDF_PAGES -> ...
                FileImportValidator.Error.PASSWORD_PROTECTED_PDF -> ...
                FileImportValidator.Error.TOO_MANY_DOCUMENT_PAGES -> ...
            }
        }
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton("OK") { _, _ -> finish() }
            .show()
    }

    fun startGiniBankSDKForImportedFile(importedFileIntent: Intent) {
        // Configure capture first
        configureCapture();

        fileImportCancellationToken = 
            GiniBank.startCaptureFlowForIntent(captureImportLauncher, this, importedFileIntent)
    }

Return Assistant
----------------

The return assistant feature allows your users to view and edit payable items in an invoice. The total amount is
updated to be the sum of only those items which the user opts to pay.

To enable this feature simply set ``returnAssistantEnabled`` to ``true`` in the ``CaptureConfiguration``: 

.. code-block:: java

    CaptureConfiguration(returnAssistantEnabled = true)

The Gini Bank SDK will
show the return assistant automatically if the invoice contained payable items and will update the extractions returned
to your app according to the user's changes.

The ``amountToPay`` extraction is updated to be the sum of items the user decided to pay. It includes discounts and
additional charges that might be present on the invoice.

The extractions related to the return assistant are stored in the ``compoundExtractions`` field of the
``CaptureResult``. See the Gini Bank API's `documentation
<https://pay-api.gini.net/documentation/#return-assistant-extractions>`_ to learn about the return assistant's compound
extractions.

Digital Invoice Help Screen Customization
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

You can show back navigation button on bottom navigation bar. You can pass your custom ``DigitalInvoiceHelpNavigationBarBottomAdapter`` implementation to
``GiniBank``:

.. code-block:: java

    CustomDigitalInvoiceHelpNavigationBarBottomAdapter customDigitalInvoiceHelpNavigationBarBottomAdapter = new CustomDigitalInvoiceHelpNavigationBarBottomAdapter();

    GiniBank.digitalInvoiceHelpNavigationBarBottomAdapter = customDigitalInvoiceHelpNavigationBarBottomAdapter


Digital Invoice Screen Customization
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

You can show invoice bottom navigation bar. You can pass your custom ``DigitalInvoiceNavigationBarBottomAdapter`` implementation to
``GiniBank``:

.. code-block:: java

    CustomDigitalInvoiceNavigationBarBottomAdapter customDigitalInvoiceNavigationBarBottomAdapter = new CustomDigitalInvoiceNavigationBarBottomAdapter();

    GiniBank.digitalInvoiceNavigationBarBottomAdapter = customDigitalInvoiceNavigationBarBottomAdapter;
