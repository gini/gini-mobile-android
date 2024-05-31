Flow
====

``GiniHealth`` and ``PaymentComponent`` are the main classes for interacting with the Gini Health SDK. ``GiniHealth``
manages interaction with the Gini Health API and ``PaymentComponent`` manages the data and state used by every
``PaymentComponentView`` and related screens.

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

Create the PaymentComponent instance
------------------------------------

For creating an instance of the ``PaymentComponent`` you need to pass in the Android context (either the application or
an activity context) and the ``GiniHealth`` instance:

.. code-block:: kotlin

    val paymentComponent = PaymentComponent(context, giniHealth)

Add a listener to the PaymentComponent
--------------------------------------

Set a listener on the ``PaymentComponent`` to get informed of events from every ``PaymentComponentView``:

.. code-block:: kotlin

    paymentComponent.listener = object: PaymentComponent.Listener {
            override fun onMoreInformationClicked() {
                // Show the MoreInformationFragment.
            }

            override fun onBankPickerClicked() {
                // Show the BankSelectionBottomSheet.
            }

            override fun onPayInvoiceClicked(documentId: String) {
                // Show the ReviewFragment.
            }
        }

Load payment providers
----------------------

Call ``paymentComponent.loadPaymentProviderApps()`` to load the available payment providers from the Gini Health API and
to check which ones are installed on the user's device.

.. note::

    It should be sufficient to call ``paymentComponent.loadPaymentProviderApps()`` only once when your app starts.

Show the PaymentComponentViews
------------------------------

The ``PaymentComponentView`` is a custom view widget and the main entry point for users. It allows them to pick a bank
and initiate the payment process. In addition, it also allows users to view more information about the payment feature.

The ``PaymentComponentView`` is hidden by default and should be added to the layout of each invoice item:

.. code-block:: xml

    <net.gini.android.health.sdk.paymentcomponent.PaymentComponentView
        android:id="@+id/payment_component"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        ... />

When creating the view holder for the invoice item, pass the ``PaymentComponent`` instance to the view holder:

.. code-block:: kotlin

    val paymentComponentView = view.findViewById(R.id.payment_component)
    paymentComponentView.paymentComponent = paymentComponent

When binding the view holder of the invoice item, prepare it for reuse, set the payable state and the document id:

.. code-block:: kotlin

    viewHolder.paymentComponentView.prepareForReuse()
    viewHolder.paymentComponentView.isPayable = invoiceItem.isPayable
    viewHolder.paymentComponentView.documentId = invoiceItem.documentId

.. note::

    The ``PaymentComponentView`` will only be visible if its ``isPayable`` property is ``true``.

Show the MoreInformationFragment
--------------------------------

The ``MoreInformationFragment`` shows the Payment Feature Info Screen. It displays information and an FAQ section about the payment feature. It requires a
``PaymentComponent`` instance to show the icons of the available banks.

To instantiate it use ``MoreInformationFragment.newInstance()`` and pass in your ``PaymentComponent`` instance:

.. code-block:: kotlin

    MoreInformationFragment.newInstance(paymentComponent)

.. note::

    The ``MoreInformationFragment`` doesn't handle navigation related events and doesn't show a navigation bar. You are
    free to design navigation to and from the fragment as you see fit.
    
    For the navigation bar title you should use the ``ghs_more_information_fragment_title`` string resource.

.. warning::

    You need to override the ``ghs_privacy_policy_link_url`` string resource to provide a link to your company's privacy
    policy page. This link will be shown to users in the answer to the "Who or what is Gini?" question.

Show the BankSelectionBottomSheet
---------------------------------

The ``BankSelectionBottomSheet`` displays a list of available banks for the user to choose from. If a banking app is not
installed it will also display its Play Store link.

To instantiate it use ``BankSelectionBottomSheet.newInstance()`` and pass in your ``PaymentComponent`` instance:

.. code-block:: kotlin

    BankSelectionBottomSheet.newInstance(paymentComponent)


Show the ReviewFragment
-----------------------

The ``ReviewFragment`` displays an invoice's pages and extractions. It also lets users pay the invoice with the bank
they selected in the ``BankSelectionBottomSheet``.

To instantiate it use ``paymentComponent.getPaymentReviewFragment()`` and pass in the Gini Health API's document id of
the invoice and the configuration for the screen. Also set a listener to get informed of events from the fragment:

.. code-block:: kotlin

    val reviewConfiguration = ReviewConfiguration(...)

    val paymentReviewFragment = paymentComponent.getPaymentReviewFragment(
        documentId, reviewConfiguration
    )

    paymentReviewFragment.listener = object : ReviewFragmentListener {
        override fun onCloseReview() {
            // Called only when the ``ReviewConfiguration.showCloseButton`` was set to ``true``.
            // Dismiss the ReviewFragment.
        }

        override fun onToTheBankButtonClicked(paymentProviderName: String) {
            // Log or track the used payment provider name.
            // No action required, the payment process is handled by the Gini Health SDK.
        }
    }

.. note::

    ``paymentComponent.getPaymentReviewFragment()`` will load the document extractions asynchronously. It's a suspend
    function and must be called from a coroutine. 

    The ``ReviewFragment`` doesn't handle navigation related events and doesn't show a navigation bar. You are
    free to design navigation to and from the fragment as you see fit.

The ``ReviewConfiguration`` class contains the following options:

- ``handleErrorsInternally``: If set to ``true``, the ``ReviewFragment`` will handle errors internally and show
  snackbars for errors. If set to ``false``, errors will be ignored by the ``ReviewFragment``. In this case the flows
  exposed by ``GiniHealth`` should be observed for errors. Default value is ``true``.
- ``showCloseButton``: If set to ``true``, a floating close button will be shown in the top right corner of the screen. Default value is ``false``.
