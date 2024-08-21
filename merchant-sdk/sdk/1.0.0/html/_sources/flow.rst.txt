Flow
====

``GiniMerchant`` and ``PaymentFragment`` are the main classes for interacting with the Gini Merchant SDK. ``GiniMerchant``
manages interaction with the Gini Merchant API and ``PaymentFragment`` handles the data and the logic for displaying related screens.

.. contents:: The recommended flow is:
   :local:

Create the GiniMerchant instance
------------------------------

The ``GiniMerchant`` class can be built either with client credentials (clientId and clientSecret)
or with a ``SessionManager`` if you have a token:

- ``GiniMerchant(context: Context, clientId: String, clientSecret: String, emailDomain: String)``
- ``GiniMerchant(context: Context, sessionManager: SessionManager)``

``SessionManager`` is an interface which you need to implement to send the token.

Load payment providers
----------------------

You can call ``giniMerchant.loadPaymentProviderApps()`` to load the available payment providers and to check which ones are installed on the user's device.

This step is optional, as the method is called when starting the payment flow. Calling it beforehand will lead to a faster load of the payment flow.

Create the PaymentFragment instance
------------------------------------

Create an instance of ``PaymentFragment`` and load it into the hierarchy.

For creating an instance of the ``PaymentFragment`` you need to use the ``createFragment`` method provided by ``GiniMerchant``, passing in the payment details (IBAN, Amount, Purpose and Recipient).
The ``GiniMerchantSDK`` expects the payment details to not be empty, and will throw an ``IllegalStateException`` if any of the fields are empty.

.. code-block:: kotlin

    val paymentFragment = giniMerchant.createFragment(iban, recipient, amount, purpose)

.. note::

    Optionally, you can pass in an instance of ``PaymentFlowConfiguration``.

    The ``PaymentFlowConfiguration`` class contains the following options:

    - ``shouldHandleErrorsInternally``: If set to ``true``, ``GiniMerchant`` will handle errors internally and show
      snackbars for errors. If set to ``false``, errors will be ignored by the ``GiniMerchant``. In this case the flows
      exposed by ``GiniMerchant`` should be observed for errors. Default value is ``true``.
    - ``showReviewFragment``: If set to ``true``, the user will be able to review the payment details before continuing with the payment. Default value is ``false``.

.. note::

    Users will have the chance to edit their payment information in their banking app, even if ``showReviewFragment`` is set to false.

Event Tracking
--------------

``GiniMerchant`` exposes ``eventFlow``, which can be collected by client apps to be aware of state / events / errors happening within the SDK.