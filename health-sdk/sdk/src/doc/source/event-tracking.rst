Event Tracking
==============

GiniHealth
----------

The ``GiniHealth`` class exposes kotlin flows which you can collect to track events and receive updates. The following flows are available:

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
   * ``PaymentState.Cancel()`` when the user cancelled the payment flow.
   .. warning::
       As the SDK is not responsible for navigation flows outside of it, removing the payment fragment from the hierarchy is the responsibility of implementers at ``PaymentState.Success(paymentRequest)`` or ``PaymentState.Cancel()`` events.
.. code-block:: java
            launch {
                    viewModel.openBankState.collect { paymentState ->
                        when (paymentState) {
                            is GiniHealth.PaymentState.Success -> {
                                ...
                                supportFragmentManager.popBackStack()

                            }
                            is GiniHealth.PaymentState.Cancel -> {
                                supportFragmentManager.popBackStack()
                            }
                            else -> {}
                        }
                    }
                }

* ``displayedScreen`` is a ``SharedFlow`` of ``DisplayedScreen`` which emits the currently displayed screen in the ``PaymentFragment``. It can be collected to update the UI if needed, such as the toolbar title. It emits the following values:
   * ``DisplayedScreen.Nothing`` is the default state.
   * ``DisplayedScreen.PaymentComponentBottomSheet`` the ``PaymentComponentBottomSheet`` is displayed, showing either
     the selected bank, or prompting the user to select one.
   * ``DisplayedScreen.BankSelectionBottomSheet`` when the ``BankSelectionBottomSheet`` is displayed, with the list of payment providers
     to choose from.
   * ``DisplayedScreen.MoreInformationFragment`` when the ``MoreInformationFragment`` is displayed.
   * ``DisplayedScreen.InstallAppBottomSheet`` when the selected payment provider is not installed.
   * ``DisplayedScreen.OpenWithBottomSheet`` when the selected payment provider does not support GPC.
   * ``DisplayedScreen.ShareSheet`` when the native share sheet is displayed.
   * ``DisplayedScreen.ReviewBottomSheet`` the payment details are shown in a bottom sheet. Emitted if payment flow was started without document id.
   * ``DisplayedScreen.ReviewFragment`` the payment details are shown in a fragment. Emitted if payment flow was started with a document id.
* ``trustMarkersFlow`` is a ``Flow`` of ``ResultWrapper<TrustMarkerResponse>`` which emits the icons of two payment providers, along with the
  additional payment providers count.
   * ``ResultWrapper.Loading()`` when the payment providers are still being loaded.
   * ``ResultWrapper.Error(throwable)`` when there was an error loading the payment providers.
   * ``ResultWrapper.Success(trustMarkerResponse)`` when the payment providers have been loaded.

PaymentComponent
----------------

The ``PaymentComponent`` class also exposes kotlin flows which you can collect to track events. The payment component flows can be collected
via ``giniHealth.giniInternalPaymentManager.paymentComponent``. The following flows are available:

* ``paymentProviderAppsFlow`` is a ``StateFlow`` of ``PaymentProviderAppsState`` which emits the available payment provider apps used by
  the ``PaymentComponentView`` and related screens. It emits the following states:
   * ``PaymentProviderAppsState.Loading()`` when the payment provider apps are being loaded.
   * ``PaymentProviderAppsState.Success(paymentProviderApps)`` when the list of payment provider apps is available.
   * ``PaymentProviderAppsState.Error(throwable)`` when there was an error loading the payment provider apps.
* ``selectedPaymentProviderAppFlow`` is a ``StateFlow`` of ``SelectedPaymentProviderAppState`` which emits selected payment provider app shown in
  the ``PaymentComponentView`` and related screens. It emits the following states:
   * ``SelectedPaymentProviderAppState.NothingSelected()`` when there is no selection.
   * ``SelectedPaymentProviderAppState.AppSelected(paymentProviderApp)`` when a payment provider app has been selected.
