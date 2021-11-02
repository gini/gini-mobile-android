Integration
===========

The Gini Pay Bank SDK has two main features: `capturing invoices`_ for information extraction and `handling payment requests`_. Both
can be used independently and you may opt to use only one or both in your app.

Capturing Invoices
------------------

The capture feature uses our `Gini Capture SDK <https://github.com/gini/gini-capture-sdk-android/>`_ to provide
Activities and Fragments to capture invoices and prepare them for upload to the Gini Pay API. It also allows documents
to be imported from other apps. The captured images can be reviewed and are optimized on the device to provide the best
results when used with the Gini Pay API. 

Android Manifest
~~~~~~~~~~~~~~~~

The capture feature of the SDK uses the camera therefore the camera permission is required:

.. code-block:: xml

    <manifest xmlns:android="http://schemas.android.com/apk/res/android"
        package="...">
        
        <uses-permission android:name="android.permission.CAMERA" />

    </manifest>

.. note::

    Make sure you request the camera permission before starting the SDK.

Requirements Check
~~~~~~~~~~~~~~~~~~

We recommend running our runtime requirements check first before launching the Gini Capture SDK to ensure the device is
capable of taking pictures of adequate quality.

Simply run ``GiniPayBank.checkCaptureRequirements()`` and inspect the returned ``RequirementsReport`` for the result:

.. note::

    On Android 6.0 and later the camera permission is required before checking the requirements.

.. code-block:: java

    final RequirementsReport report = GiniPayBank.checkCaptureRequirements((Context) this);
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
~~~~~~~~~~~~~

Configuration and interaction is done using ``CaptureConfiguration``. To set the configuration use the
``GiniPayBank.setCaptureConfiguration()`` static method. 

.. important::

    The configuration is immutable and make sure to call ``GiniPayBank.releaseCapture()`` before setting a new
    configuration.

You should call ``GiniPayBank.releaseCapture()`` after the SDK returned control to your application and your app has
sent feedback to the Gini Pay API and is not using the capture feature anymore.

To view all the configuration options see the documentation of `CaptureConfiguration
<http://developer.gini.net/gini-pay-bank-sdk-android/kdoc/ginipaybank/net.gini.pay.bank.capture/-capture-configuration/index.html>`_.

Information about the configurable features are available on the `Capture Features <capture-features.html>`_ page.

Tablet Support
~~~~~~~~~~~~~~

The Gini Pay Bank SDK can be used on tablets, too. Some UI elements adapt to the larger screen to offer the best user
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
~~~~~~~~~~

The Gini Pay Bank SDK allows you to use the default networking implementation of our Gini Capture SDK to communicate with the Gini
Pay API. You may also implement your own networking layer.

.. note::

    You should have received Gini Pay API client credentials from us. Please get in touch with us in case you don’t have
    them. Without credentials you won't be able to use the Gini Pay API.

Default Implementation
^^^^^^^^^^^^^^^^^^^^^^

The capture feature is not aware of any networking implementations and requires you to set them in the
``CaptureConfiguration``. 

The default networking implementations are the ``GiniCaptureDefaultNetworkService`` and
``GiniCaptureDefaultNetworkApi``. We provide you with two helper methods to create them with the minimal configuration:

.. code-block:: java

    val networkService = getDefaultNetworkService(
        context = this,
        clientId = myClientId,
        clientSecret = myClientSecret,
        emailDomain = myEmailDomain,
        documentMetadata = myDocumentMetadata
    )
    
    val networkApi = getDefaultNetworkApi(networkService)
    
    GiniPayBank.setCaptureConfiguration(
        CaptureConfiguration(
            networkService = networkService,
            networkApi = networkApi
        )
    )

For all configuration options of the default networking implementation see the documentation of
`GiniCaptureDefaultNetworkService.Builder
<http://developer.gini.net/gini-capture-sdk-android/network/javadoc/net/gini/android/capture/network/GiniCaptureDefaultNetworkService.Builder.html>`_
and `GiniCaptureDefaultNetworkApi.Builder
<http://developer.gini.net/gini-capture-sdk-android/network/javadoc/net/gini/android/capture/network/GiniCaptureDefaultNetworkApi.Builder.html>`_.

Custom Implementation
^^^^^^^^^^^^^^^^^^^^^

You can also provide your own networking by implementing the ``GiniCaptureNetworkService`` and the
``GiniCaptureNetworkApi`` interfaces:

* ``GiniCaptureNetworkService``
   This interface is used to upload, analyze and delete documents. See the `reference documentation
   <http://developer.gini.net/gini-capture-sdk-android/ginicapture/dokka/ginicapture/net.gini.android.capture.network/-gini-capture-network-service/index.html>`_
   for details.

* ``GiniCaptureNetworkApi``
   This interface is used to declare network tasks which should be called by you outside of the Gini Capture SDK (e.g.,
   for sending feedback after the user saw and potentielly corrected the extractions).  See the `reference documentation
   <http://developer.gini.net/gini-capture-sdk-android/ginicapture/dokka/ginicapture/net.gini.android.capture.network/-gini-capture-network-api/index.html>`_
   for details.

You may also use the `Gini Pay API Library <https://github.com/gini/gini-pay-api-lib-android>`_ for Android or implement
communication with the Gini API yourself.

Sending Feedback
^^^^^^^^^^^^^^^^

Your app should send feedback for the extractions the Gini Pay API delivered. Feedback should be sent *only* for the
extractions the user has seen and accepted (or corrected).

For addition information about feedback see the `Gini Pay API documentation
<https://pay-api.gini.net/documentation/#send-feedback-and-get-even-better-extractions-next-time>`_.

Default Implementation
++++++++++++++++++++++

The example below shows how to correct extractions and send feedback using the default networking implementation:

.. code-block:: java

   val networkApi: GiniCaptureDefaultNetworkApi // Provided

   val extractions: Map<String, GiniCaptureSpecificExtraction> // Provided

   // Modify the amount to pay extraction's value.
   GiniCaptureSpecificExtraction amountToPay = extractions["amountToPay"];
   amountToPay.value = "31.00:EUR"

   // You should send feedback only for extractions the user has seen and accepted.
   // In this example only the amountToPay was wrong and we can reuse the other extractions.
    val feedback = mapOf<String, GiniCaptureSpecificExtraction>(
        "iban" to mExtractions["iban"],
        "amountToPay" to amountTopay,
        "bic" to mExtractions["bic"],
        "senderName" to mExtractions["sencerName"]
    )

    networkApi.sendFeedback(feedback, object : GiniCaptureNetworkCallback<Void, Error> {
        override fun failure(error: Error) {
            // Handle the error.
        }

        override fun success(result: Void?) {
            // Feedback sent successfully.
        }

        override fun cancelled() {
            // Handle cancellation.
        }
    })

Custom Implementation
+++++++++++++++++++++

If you use your own networking implementation and directly communicate with the Gini Pay API then see `this section
<https://pay-api.gini.net/documentation/#submitting-feedback-on-extractions>`_ in its documentation on how to send
feedback.

In case you use the Gini Pay API Library then see `this section
<https://developer.gini.net/gini-pay-api-lib-android/guides/common-tasks.html#sending-feedback>`_ in its documentation
for details.

Capture Flow
~~~~~~~~~~~~

The capture flow can be used in two ways, either by using the `Screen API`_ or the `Component API`_:

* The *Screen API* provides activities for easy integration that can be customized in a
  limited way. The screen and configuration design is based on our long-lasting experience with
  integration in customer apps.

* In the *Component API* we provide fragments for advanced integration
  with more freedom for customization.

Screen API
^^^^^^^^^^

This is the easiest way to use the capture flow. You only need to:

#. Request camera access,
#. Configure the capture feature using the ``CaptureConfiguration``,
#. Register an activity result handler with the ``CaptureFlowContract()``,
#. Start the capture flow.

.. note::

   Check out the `Screen API example app
   <https://github.com/gini/gini-pay-bank-sdk-android/tree/main/appscreenapi>`_ to see how an integration could look
   like.

The following example shows how to launch the capture flow using the *Screen API* and how to handle the results:

.. code-block:: java

    // Use the androidx's Activity Result API to register a handler for the capture result.
    val captureLauncher = registerForActivityResult(CaptureFlowContract()) { result: CaptureResult ->
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
                        // See the File Import section on the Capture Features page for more details.
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

    fun launchGiniCapture() {
        // Make sure camera permission has been already granted at this point.
        
        // Check that the device fulfills the requirements.
        val report = GiniCaptureRequirements.checkRequirements((Context) this)
        if (!report.isFulfilled()) {
            handleUnfulfilledRequirements(report)
            return
        }
        
        // Instantiate the networking implementations.
        val networkService: GiniCaptureNetworkService  = ...
        val networkApi: GiniCaptureNetworkApi = ...

        // Cleanup to make sure everything is reset.
        GiniPayBank.releaseCapture(this)

        // Configure the capture feature.
        GiniPayBank.setCaptureConfiguration(
            CaptureConfiguration(
                networkService = networkService,
                networkApi = networkApi,
                ...
            )
        )
                
        // Launch and wait for the result.
        GiniPayBank.startCaptureFlow(captureLauncher)
    }

Component API
^^^^^^^^^^^^^

This is the more complicated way of using the capture flow. The advantage is that it is based on fragments and you
have full control over how these are shown in your UI.

.. note::

   Check out the `Component API example app
   <https://github.com/gini/gini-pay-bank-sdk-android/tree/main/appcomponentapi>`_ to see how an integration could look
   like.

Before launching the first fragment you need to:

#. Request camera access,
#. Configure the capture feature using the ``CaptureConfiguration``.

The Component API is exposed as-is from the Gini Capture SDK and you can follow `it's guide
<https://developer.gini.net/gini-capture-sdk-android/html/integration.html#component-api>`_ to learn how to integrate
it.

Handling Payment Requests
-------------------------

The Gini Pay Bank SDK enables your app to handle payment requests started by the Gini Pay Business SDK in another app.
You can retrieve the payment requests's content, mark the payment request as payed and also return your user to the app
that created the payment request.

Networking
~~~~~~~~~~

The pay feature depends on the `Gini Pay API Library <https://github.com/gini/gini-pay-api-lib-android>`_, which
provides an entry point through the ``Gini`` class.

.. note::

    You should have received Gini Pay API client credentials from us. Please get in touch with us in case you don’t have
    them. Without credentials you won't be able to use the Gini Pay API.

The ``Gini`` class can be built either with client credentials or with a ``SessionManager`` if you already have an
authorization token. We provide helper methods for each case:

.. code-block:: java

    getGiniApi(context: Context, clientId: String, clientSecret: String, 
               emailDomain: String)

.. code-block:: java 
  
    getGiniApi(context: Context, sessionManager: SessionManager)

``SessionManager`` is an interface which you need to implement to send the token.

For more details about the ``Gini`` class see the Gini Pay API Library's `documentation
<https://developer.gini.net/gini-pay-api-lib-android/guides/getting-started.html#creating-the-gini-instance>`_.

Once you have a ``Gini`` instance you need to pass it to ``GiniPayBank.setGiniApi()``:

.. code-block:: java

    val giniApi = getGiniApi(this, myClientId, myClientSecret, myEmailDomain)
    
    GiniPayBank.setGiniApi(giniApi)

Android Manifest
~~~~~~~~~~~~~~~~

To be able to receive payment requests you need to add an intent filter for the ginipay URI to your manifest. This also
allows the Gini Pay Business SDK to detect if your app is installed:

.. code-block:: xml

    <intent-filter>
        <action android:name="android.intent.action.VIEW" />

        <category android:name="android.intent.category.DEFAULT" />

        <data
            android:host="payment"
            android:scheme="ginipay" />
    </intent-filter>

The intent filter can be added to the activity which will handle the payment flow of that payment request.

Package Name
~~~~~~~~~~~~

You also need to tell us your app's package name. It will be associated with the payment provider we 
create for your banking app in the Gini Pay API. The Gini Pay Business SDK will only open your banking app if
it is installed and it has the same package name as the one known by the Gini Pay API.

If you have different package names for development and production
then please share both of them with us so that we can use the right one for each environment.

Receive Payment Requests
~~~~~~~~~~~~~~~~~~~~~~~~

.. note::

    You can see an example implementation in the Screen API example app's `pay
    <https://github.com/gini/gini-pay-bank-sdk-android/tree/main/appscreenapi/src/main/java/net/gini/pay/appscreenapi/pay>`_
    package.

When your activity is launched with an intent you should follow the steps below to receive and handle the payment
request:

#. Extract the payment request id from the intent with ``getRequestId()``:

   .. code-block:: java

        val requestId = getRequestId(intent)

#. Retrieve the payment details set by the business app using ``GiniPayBank.getPaymentRequest()``:

   .. code-block:: java

        val paymentRequest: PaymentRequest = giniPayBank.getPaymentRequest(requestId)

#. Show the payment details to your user:

   .. code-block:: java

        showPaymentDetails(
            paymentRequest.recipient,
            paymentRequest.iban,
            paymentRequest.bic,
            paymentRequest.amount,
            paymentRequest.purpose
        )

#. After your user has initiated the payment mark the payment request as paid using
   ``GiniPayBank.resolvePaymentRequest()``:

   .. code-block:: java

        // The actual payment details used for the payment (as corrected and accepted by the user).
        val usedPaymentDetails = ResolvePaymentInput(
            recipient = "...",
            iban = "...",
            bic = "...",
            amount = "...",
            purpose = "..."
        )

        val resolvedPayment: ResolvedPayment = giniPayBank.resolvePaymentRequest(requestId, usedPaymentDetails)

#. You can allow your user to return to the business app that started the flow using ``GiniPayBank.returnToBusiness()``:

   .. code-block:: java

        giniPayBank.returnToBusiness(context, resolvedPayment)

Testing
~~~~~~~

Testing the payment feature requires an app which uses the Gini Pay Business SDK to create payment requests and to
forward them to your banking app to view and resolve those payment requests.

Requirements
^^^^^^^^^^^^

Example business app
++++++++++++++++++++

An example business app is available in the `Gini Pay Business SDK's <https://github.com/gini/gini-pay-business-sdk-android>`_
repository.

You can use the same Gini Pay API client credentials in the example business app as in your app, if not otherwise
specified.

Development Gini Pay API client credentials
___________________________________________

In order to test using our example business app you need to use development client credentials. This will make sure
the Gini Pay Business SDK will use a payment provider which will open your development banking app.

End to end testing
^^^^^^^^^^^^^^^^^^

After you've set the client credentials in the example business app you can install it along with your banking app on
your device.

Run the example business app and import an invoice or take a picture of one to start the payment flow.

After following the integration steps above your banking app will be launched and you'll be able to fetch the payment
request, show the payment information and resolve the payment after the transaction has been confirmed. At this point,
you may redirect back to the business app.

With these steps completed you have verified that your app, the Gini Pay API, the Gini Pay Business SDK and the Gini Pay
Bank SDK work together correctly.

Testing in production
^^^^^^^^^^^^^^^^^^^^^

The steps are the same but instead of the development client credentials you will need to use production client
credentials. This will make sure the Gini Pay Business SDK receives real payment providers including the one which
opens your production banking app.

For testing the flow using the example business app please make sure that the production client credentials are used
before installing it.

You can also test with a real business app. Please contact us in case you don't know which business app(s) to install
for starting the payment flow.
