Event Tracking
==============

GiniHealth
----------

The ``GiniHealth`` class exposes kotlin flows which you can collect to track events. The following flows are available:

* ``documentFlow`` is a ``StateFlow`` of ``ResultWrapper<Document>`` which emits the Gini Health API's document used by
  the ``ReviewFragment``. It emits the following states:
   * ``ResultWrapper.Loading()`` when the document is being loaded.
   * ``ResultWrapper.Success(document)`` when the document is available.
   * ``ResultWrapper.Error(throwable)`` when there was an error loading the document.
* ``paymentFlow`` is a ``StateFlow`` of ``ResultWrapper<PaymentDetails>`` which emits the payment information shown in
  the ``ReviewFragment``.
   * ``ResultWrapper.Loading()`` when the payment details are being loaded.
   * ``ResultWrapper.Success(paymentDetails)`` when the payment details are available.
   * ``ResultWrapper.Error(throwable)`` when there was an error loading the payment details.
* ``openBankState`` is a ``StateFlow`` of ``PaymentState`` which emits the state of opening the banking app. It emits
  the following states:
   * ``PaymentState.NoAction()`` is the idle state.
   * ``PaymentState.Loading()`` when the user requested to open the banking app and the Health SDK started creating a
     payment request.
   * ``PaymentState.Success(paymentRequest)`` when the payment request is ready and the banking app will be opened.
   * ``PaymentState.Error(throwable)`` when there was an error creating the payment request or opening the banking app.

PaymentComponent
----------------

The ``PaymentComponent`` class also exposes kotlin flows which you can collect to track events. The following flows are available:

* ``paymentProviderAppsFlow`` is a ``StateFlow`` of ``PaymentProviderAppsState`` which emits the available payment provider apps used by
  the ``PaymentComponentView`` and related screens. It emits the following states:
   * ``PaymentProviderAppsState.Loading()`` when the payment provider apps are being loaded.
   * ``PaymentProviderAppsState.Success(paymentProviderApps)`` when the list of payment provider apps is available.
   * ``PaymentProviderAppsState.Error(throwable)`` when there was an error loading the payment provider apps.
* ``selectedPaymentProviderAppFlow`` is a ``StateFlow`` of ``SelectedPaymentProviderAppState`` which emits selected payment provider app shown in
  the ``PaymentComponentView`` and related screens. It emits the following states:
   * ``SelectedPaymentProviderAppState.NothingSelected()`` when there is no selection.
   * ``SelectedPaymentProviderAppState.AppSelected(paymentProviderApp)`` when a payment provider app has been selected.

ReviewFragment
--------------

To get informed of ``ReviewFragment`` events (like the user clicking the "close" or "next" button) you can implement
the ``ReviewFragmentListener`` and set it on the fragment.
