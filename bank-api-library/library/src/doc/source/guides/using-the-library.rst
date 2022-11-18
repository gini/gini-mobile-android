Using the Gini Bank API Library
===============================

..
  Audience: Android dev who has no experience using the library
  Purpose: Show how the library can be used to communicate with the Gini Bank API
  Content type: Procedural - How-To

  Headers:
  h1 =====
  h2 -----
  h3 ~~~~~
  h4 +++++
  h5 ^^^^^

The Gini Bank API Library uses kotlin coroutine suspend functions for a convenient and elegant way to work with
asynchronous Gini Bank API requests. All the public suspend functions are main-safe, meaning they're safe to call from
the main thread.

Each suspend function returns an instance of the ``Resource`` sealed class. Depending on the request result the
following ``Resource`` instances are returned:

- successful request: ``Resource.Success`` which contains the deserialized response payload in the ``data`` property.
- failed request: ``Resource.Error`` which contains the response details and/or the exception which caused the failure.
- cancelled request: ``Resource.Cancelled``.

``Resource`` also provides a helper instance method for chaining requests called ``mapSuccess()``. For more details please
consult the :root_dokka_path:`reference documentation <library/net.gini.android.core.api/-resource/index.html>`.

Upload a document
-----------------

As the key aspect of the Gini Bank API is to provide information extraction for analyzing documents, the
API is mainly built around the concept of documents. A document can be any written representation of
information such as invoices, reminders, contracts and so on.

The Gini Bank API Library supports creating documents from images, PDFs or UTF-8 encoded text. Images are
usually a picture of a paper document which was taken with the device's camera.

The following example shows how to create a new document from a byte array containing a JPEG image.

.. code-block:: java
    
    // Assuming that `giniBankApi` is an instance of the `GiniBankAPI` facade class and `imageBytes`
    // is an instance of a byte array containing a JPEG image, 
    // e.g. from a picture taken by the camera
    
    coroutineScope.launch {
        // Create a partial document by uploading the document data
        val partialDocumentResource =
            giniBankApi.documentManager.createPartialDocument(imageBytes, "image/jpeg", "myFirstDocument.jpg")

        when (partialDocumentResource) {
            is Resource.Success -> {
                // Use the partial document
                val partialDocument = extractionsResource.data
            }
            is Resource.Error -> // Handle error
            is Resource.Cancelled -> // Handle cancellation
        }
    }

Each page of a document needs to uploaded as a partial document. In addition documents consisting of
one page also should be uploaded as a partial document.

.. note::

    PDFs and UTF-8 encoded text should also be uploaded as partial documents. Even though PDFs might
    contain multiple pages and text is "pageless", creating partial documents for these keeps your
    interaction with the library consistent for all the supported document types.

Extractions are not available for partial documents. Creating a partial document is analogous to an
upload. For retrieving extractions see :ref:`getting-extractions`.

.. note::
    
    The filename (``myFirstDocument.jpg`` in the example) is not required, it could be ``null``, but
    setting a filename is a good practice for human readable document identification.

Setting the document type hint
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

To easily set the document type hint we introduced the ``DocumentType`` enum. It is safer and easier
to use than a ``String``. For more details about the document type hints see the `Document Type
Hints in the Gini Bank API documentation
<https://pay-api.gini.net/documentation/#document-types>`_.

.. _getting-extractions:

Getting extractions
-------------------

After you have successfully created the partial documents, you most likely want to get the
extractions for the document. *Composite documents* consist of
previously created *partial documents*. You can consider creating partial documents analogous to
uploading pages of a document and creating a composite document analogous to processing those pages
as a single document.

Before retrieving extractions you need to create a composite document from your partial documents.
The ``createCompositeDocument()`` method accepts either a ``List`` of partial ``Documents`` or a
``LinkedHashMap``. The ``LinkedHashMap`` contains partial ``Documents`` as keys and the user applied
rotation as values. In both cases the order is important and the partial documents should be in the
same order as the pages of the scanned document.

Gini needs to process the composite document first before you can fetch the extractions. Effectively
this means that you won't get any extractions before the composite document is fully processed. The
processing time may vary, usually it is in the range of a couple of seconds, but blurred or slightly
rotated images are known to drasticly increase the processing time. 

The ``BankApiDocumentManager`` provides the ``getAllExtractionsWithPolling`` method which can be
used to fetch the extractions after the processing of the document is completed. The following
example shows how to achieve this in detail.

.. code-block:: java
    
    // Assuming that `giniBankApi` is an instance of the `GiniBankAPI` facade class and `partialDocuments` is
    // a list of `Documents` which were returned by `createPartialDocument(...)` calls

    coroutineScope.launch {
        // Create a partial document by uploading the document data
        val extractionsResource =
            giniBankApi.documentManager.createCompositeDocument(partialDocuments)
                .mapSuccess { compositeDocumentResource ->
                    // Poll the document and retrieve the extractions
                    giniBankApi.documentManager.getAllExtractionsWithPolling(compositeDocumentResource.data)
                }

        when (extractionsResource) {
            is Resource.Success -> {
                // You may use the extractions to fulfill your use-case
                val extractionsContainer = extractionsResource.data
                val amountToPay: SpecificExtraction? = 
                    extractionsContainer.specificExtractions["amountToPay"]
                val lineItems: CompoundExtraction? = 
                    extractionsContainer.compoundExtractions["lineItems"]
            }
            is Resource.Error -> // Handle error
            is Resource.Cancelled -> // Handle cancellation
        }
    }

Sending feedback
----------------

Depending on your use case your app probably presents the extractions to the user and offers the
opportunity to correct them. We do our best to prevent errors. You can help improve our service if
your app sends feedback for the extractions Gini delivered. Your app should send feedback only for
the extractions the *user has seen and accepted*. Feedback should be sent for corrected extractions
**and** for *correct extractions*. The code example below shows how to correct extractions and send
feedback.

.. note::

    We also provide a sample test case `here
    <https://github.com/gini/gini-mobile-android/blob/main/bank-api-library/library/src/androidTest/java/net/gini/android/bank/api/ExtractionFeedbackIntegrationTest.kt>`_
    to verify that extraction feedback sending works. You may use it along with the example pdf and json files as a
    starting point to write your own test case.

    The sample test case is based on the Bank API documentation's `recommended steps
    <https://pay-api.gini.net/documentation/#test-example>`_ for testing extraction feedback sending.

.. code-block:: java

    // Assuming that `giniBankApi` is an instance of the `GiniBankAPI` facade class

    coroutineScope.launch {
        val retrievedExtractions: ExtractionsContainer // provided
        val compositeDocument: Document // provided

        // amounToPay was wrong, we'll correct it
        val amountToPay: SpecificExtraction = retrievedExtractions.specificiExtractions["amountToPay"];
        amountToPay.value = "31.00:EUR";
        
        // we should send only feedback for extractions we have seen and accepted
        // all extractions we've seen were correct except amountToPay
        val feedback: Map<String, SpecificExtraction> = mutableMapOf(
            "iban" to retrievedExtractions.specificiExtractions["iban"],
            "amountToPay" to amountToPay,
            "paymentRecipient" to retrievedExtractions.specificiExtractions["paymentRecipient"],
            "paymentReference" to retrievedExtractions.specificiExtractions["paymentReference"],
        );

        val feedbackResource = giniBankApi.documentManager.sendFeedbackForExtractions(document, feedback);
    }

Handling errors
---------------

Errors are returned via ``Resource.Error`` instances which contain the response details and/or the exception which
caused the failure. You can use these to log the error and decide whether to allow the user to retry the request or not.

Debugging
---------

You can enable the debugging mode by passing ``true`` to the ``GiniBankAPIBuilder.setDebuggingEnabled()``. This will
cause all requests and responses to be logged.
