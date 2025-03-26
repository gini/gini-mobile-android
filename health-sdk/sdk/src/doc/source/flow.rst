Flow
====

``GiniHealth`` and ``PaymentFragment`` are the main classes for interacting with the Gini Health SDK. ``GiniHealth``
manages interaction with the Gini Health API and ``PaymentFragment`` controls the payment flow and the displayed screens.

.. contents:: The recommended flow is:
   :local:

Create the GiniHealth instance
------------------------------

Before creating an instance of ``GiniHealth`` you need to create an instance of the ``GiniHealthAPI``. The Gini Health
SDK provides the following two helper methods for creating the  ``GiniHealthAPI`` instance:

* ``getGiniApi(context: Context, clientId: String, clientSecret: String, emailDomain: String)``
* ``getGiniApi(context: Context, sessionManager: SessionManager)``

After that you can create an instance of ``GiniHealth``:

.. code-block:: kotlin

    val giniHealth = GiniHealth(giniHealthApi)

.. note::

    ``GiniHealth`` exposes a method for loading payment providers manually: ``giniHealth.loadPaymentProviders()``.

    Although ``GiniHealth`` loads the payment providers when it is instantiated, this method can be used if there was an error when loading the payment providers, as a manual retry mechanism. If ``GiniHealth`` does not have internet access when instantiated, or if it was instantiated
    with a session manager without credentials, this method should be called when the SDK gains internet access (or session manager receives credentials).

    The ``trustMarkersFlow`` depends on the loaded payment providers, so please make sure you call this method manually if your SDK instantiation is similar to the cases described above.


Upload documents
----------------

Uploading documents is achieved via the ``GiniHealthAPI`` instance's ``documentManager``. You can access it by using the
``giniHealth.giniHealthAPI.documentManager`` property. 

For each document page a *partial document* needs to be created. The following example shows how to create a new partial
document from a byte array containing a JPEG image:

.. code-block:: kotlin

    // Assuming `imageBytes` is an instance of a byte array containing a JPEG image,
    // e.g. from a picture taken by the camera

    coroutineScope.launch {
        // Create a partial document by uploading the document data
        val partialDocumentResource =
            giniHealth.giniHealthApi.documentManager.createPartialDocument(imageBytes, "image/jpeg", "document_page_1.jpg")

        when (partialDocumentResource) {
            is Resource.Success -> {
                // Use the partial document
                val partialDocument = partialDocumentResource.data
            }
            is Resource.Error -> // Handle error
            is Resource.Cancelled -> // Handle cancellation
        }
    }

After all partial documents have been created you can create a *composite document* from the partials to bundle them
into one final document:

.. code-block:: kotlin
    
    // Assuming `partialDocuments` is a list of `Documents` which were 
    // returned by `createPartialDocument(...)` calls

    coroutineScope.launch {
        // Create a composite document by uploading the document data
        val compositeDocumentResource =
            giniHealth.giniHealthApi.documentManager.createCompositeDocument(partialDocuments)

        when (compositeDocumentResource) {
            is Resource.Success -> {
                // Use the composite document
                val compositeDocument = compositeDocumentResource.data
            }
            is Resource.Error -> // Handle error
            is Resource.Cancelled -> // Handle cancellation
        }
    }

Delete payment request
---------------------------------

``GiniHealthSDK`` provides a method to delete a payment request. You can do this by calling ``giniHealth.deletePaymentRequest(...)`` with a payment request ID.

.. code-block:: kotlin

    coroutineScope.launch {
        // Delete  payment requests
        val deletePaymentRequest = giniHealth.deletePaymentRequest(paymentRequestId)

        when (deletePaymentRequest) {
            is Resource.Success -> {
                // `null` will be returned here
            }
            is Resource.Error -> // Handle Error
            is Resource.Cancelled -> //  Handle cancellation
        }
    }

Delete multiple payment requests
---------------------------------

``GiniHealthSDK`` provides a  method to delete multiple payment request at once. You can do this by calling ``giniHealth.deletePaymentRequests(...)`` with a list of payment request IDs. The call will only succeed if all payment request were successfully deleted. If any payment request is invalid, unauthorized, or not found, the entire deletion request will fail, and no payment requests will be deleted. In the case of failures, an error or type ``DeletePaymentRequestErrorResponse`` will be provided, with more insight into why the deletion failed.

.. code-block:: kotlin

    // Assuming `paymentRequestIds` is a list of `String` which
    // representing the IDs of the payment requests to be deleted

    coroutineScope.launch {
        // Delete multiple payment requests at once
        val deletePaymentRequests = giniHealth.deletePaymentRequests(paymentRequestIds)

        when (deletePaymentRequests) {
            is Resource.Success -> {
                // `null` will be returned here
            }
            is Resource.Error -> // Handle `DeletePaymentRequestErrorResponse`
            is Resource.Cancelled -> // Handle `DeletePaymentRequestErrorResponse`
        }
    }


Delete multiple documents at once
---------------------------------

``GiniHealthSDK`` provides an easy method to delete multiple documents at once. You can call ``giniHealth.deleteDocuments(...)`` with the list of document
ids you want to delete. The call will only succeed if all documents were successfully deleted. If not all documents can be deleted, the whole call will fail
and no documents will be deleted. In the case of failures, an error or type ``DeleteDocumentErrorResponse`` will be provided, with more insight into why the deletion failed.

.. code-block:: kotlin

    // Assuming `documentIds` is a list of `String` which
    // represent the ids of the documents to be deleted

    coroutineScope.launch {
        // Delete multiple documents at once
        val deleteDocuments = giniHealth.deleteDocuments(documentIds)

        when (deleteDocuments) {
            is Resource.Success -> {
                // `null` will be returned here
            }
            is Resource.Error -> // Handle `DeleteDocumentErrorResponse`
            is Resource.Cancelled -> // Handle `DeleteDocumentErrorResponse`
        }
    }


Check which documents/invoices are payable
------------------------------------------

Call ``giniHealth.checkIfDocumentIsPayable()`` with the composite document id for each invoice to check whether it is
payable. We recommend performing this check only once right after the invoice has been uploaded and processed by Gini's
Health API. You can then store the ``isPayable`` state in your own data model.

.. code-block:: kotlin
    
    // Assuming `compositeDocument` is `Document` returned by `createCompositeDocument(...)`

    coroutineScope.launch {
        try {
            // Check whether the composite document is payable
            val isPayable = giniHealth.checkIfDocumentIsPayable(compositeDocument.id)
        } catch (e: Exception) {
            // Handle error
        }
    }

get Payment Details
------------------------

Call ``giniHealth.getPayment()`` with the payment request ID to retrieve the details of a specific payment.
The method returns a ``Payment`` object containing the relevant payment information.

If the request fails or is canceled, an exception will be thrown with an error message.

.. code-block:: kotlin

    // Assuming paymentId is the ID of the payment request

    coroutineScope.launch {
        try {
            // Retrieve payment details
            val paymentDetails = giniHealth.getPayment(paymentId)
        } catch (e: Exception) {
            // Handle error
        }
    }


Check if document has multiple invoices
---------------------------------------

Call ``giniHealth.checkIfDocumentContainsMultipleDocuments()`` with the composite document id to check whether it contains multiple invoices or not.
We recommend performing this check after checking if the document is payable. The method will return ``true`` if the document contains
multiple invoices, ``false`` if otherwise.

.. code-block:: kotlin

    // Assuming `compositeDocument` is `Document` returned by `createCompositeDocument(...)`

    coroutineScope.launch {
        try {
            // Check whether the composite document contains multiple invoices
            val containsMultipleInvoices = giniHealth.checkIfDocumentContainsMultipleDocuments(compositeDocument.id)
        } catch (e: Exception) {
            // Handle error
        }
    }

Create the PaymentFragment
--------------------------

For creating in instance of ``PaymentFragment`` for payments with a ``documentId`` you should use the
``getPaymentFragmentWithDocument`` method provided by  ``GiniHealth``. It should be added to your hierarchy, and will
handle the payment flow internally.

You need to pass in the ``documentId`` and, optionally, an instance of ``PaymentFlowConfiguration``.

.. code-block:: kotlin

    getPaymentFragmentWithDocument(documentId: String, paymentFlowConfiguration: PaymentFlowConfiguration?): PaymentFragment

An instance of ``PaymentFragment`` can also be created without a ``documentId`` by calling the ``getPaymentFragmentWithoutDocument``
method provided by ``GiniHealth``.

You need to pass the payment details as parameter to the method:

.. code-block:: kotlin

    getPaymentFragmentWithoutDocument(paymentDetails: PaymentDetails, paymentFlowConfiguration: PaymentFlowConfiguration?): PaymentFragment

.. warning::

    Currently, We support ``amount`` which is passed in ``PaymentDetails`` in the format 12345.67, meaning up to five digits before the decimal and two digits after the decimal. The maximum allowed amount is 99999.99.

.. note::

    The ``PaymentFragment`` handles the navigation for the screens shown during the payment flow. It doesn't handle external navigation related events and doesn't show a navigation bar. You are
    free to design navigation to and from the fragment as you see fit.

The ``PaymentFlowConfiguration`` class contains the following options:

- ``shouldHandleErrorsInternally``: If set to ``true``, the ``PaymentFragment`` will handle errors internally and show
  snackbars for errors. If set to ``false``, errors will be ignored by the ``PaymentFragment``. In this case the flows
  exposed by ``GiniHealth`` should be observed for errors. Default value is ``true``.
- ``showCloseButtonOnReviewFragment``: If set to ``true``, a floating close button will be shown in the top right corner of the screen. This parameter is used only for payments started with a ``documentId``. Default value is ``false``.
- ``shouldShowReviewBottomDialog``: If set to ``true``, the ``PaymentFragment`` will show a bottom sheet dialog containing the payment details. If set to ``false``, the payment details will not be visible during the payment flow. They will be available to be reviewed after redirecting to the selected payment provider,
    before finalizing the payment. This parameter is only used in the case of payment flows started without ``documentId``.
    Default value is ``false``

.. warning::
    As the SDK is not responsible for navigation flows outside of it, removing the payment fragment from the hierarchy is the responsibility of implementers at ``PaymentState.Success(paymentRequest)`` or ``PaymentState.Cancel()`` events.

.. code-block:: kotlin

   giniHealth.openBankState.collect { paymentState ->
        when (paymentState) {
            is GiniHealth.PaymentState.Success -> {
               ...
               // Remove fragment from view hierarchy
            }
            is GiniHealth.PaymentState.Cancel -> {
               // Remove fragment from view hierarchy
            }
            else -> {}
        }
   }

