Invoice Capture Features
========================

Features from the Gini Capture SDK
----------------------------------

The capture feature uses our `Gini Capture SDK <https://github.com/gini/gini-mobile-android/tree/main/capture-sdk>`_. All features
listed in its `documentation <https://developer.gini.net/gini-mobile-android/capture-sdk/sdk/html/features.html>`_ can be used here
as well.

An important difference is in how you configure the capture features. In the Gini Bank SDK you need to use the
``CaptureConfiguration`` instead of the Gini Capture SDK's ``GiniCapture.Builder``. The configuration names are the same
so you can easily map them to the ``CaptureConfiguration``.

File Import (Open With)
~~~~~~~~~~~~~~~~~~~~~~~

Another difference is related to the `file import
<https://developer.gini.net/gini-mobile-android/capture-sdk/sdk/html/features.html#file-import-open-with>`_ (or "open with")
feature which allows importing of files from other apps via Android's "open with" or "share" functionality.

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

Sending Feedback
~~~~~~~~~~~~~~~~

Your app should send feedback for the extractions related to the return assistant. These extractions are found in the
``compoundExtractions`` field of the ``CaptureResult``.

Default Networking Implementation
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If you use the ``GiniCaptureDefaultNetworkService`` and the ``GiniCaptureDefaultNetworkApi`` then sending feedback for
the return assistant extractions is done by the ``GiniCaptureDefaultNetworkApi`` when you send feedback for the payment
data extractions as described in the `Sending Feedback <integration.html#sending-feedback>`_ section.

Custom Networking Implementation
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If you use your own networking implementation and directly communicate with the Gini Bank API then see `this section
<https://pay-api.gini.net/documentation/#submitting-feedback-on-extractions>`_ in its documentation on how to send
feedback for the compound extractions.

In case you use the Gini Bank API Library then sending compound extraction feedback is very similar to how it's shown in
its `documentation <https://developer.gini.net/gini-mobile-android/bank-api-library/library/>`_. The only difference is
that you need to also pass in the ``CompoundExtraction`` map to ``DocumentTaskManager.sendFeebackForExtractions()``:

.. code-block:: java

    // Extractions seen and accepted by the user (including user modifications)
    Map<String, SpecificExtraction> specificExtractionFeedback;

    // Return assistant extractions as returned by the CaptureResult or DigitalInvoiceFragmentListener
    Map<String, CompoundExtraction> compoundExtractionFeedback;

    final Task<Document> sendFeedback = documentTaskManager.sendFeedbackForExtractions(document, 
            specificExtractionFeedback, compoundExtractionFeedback);
    sendFeedback.waitForCompletion();

Digital Invoice Help Screen Customization
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

You can show back navigation button on bottom navigation bar. You can pass your custom ``DigitalInvoiceHelpNavigationBarBottomAdapter`` implementation to
``GiniBank``:

.. code-block:: java

    CustomDigitalInvoiceHelpNavigationBarBottomAdapter customDigitalInvoiceHelpNavigationBarBottomAdapter = new CustomDigitalInvoiceHelpNavigationBarBottomAdapter();

    GiniBank.digitalInvoiceHelpNavigationBarBottomAdapter = customDigitalInvoiceHelpNavigationBarBottomAdapter
