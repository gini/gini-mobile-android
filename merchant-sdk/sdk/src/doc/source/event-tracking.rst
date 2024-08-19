Event Tracking
==============

GiniMerchant
----------

The ``GiniMerchant`` class exposes a kotlin flow which you can collect to track events:

* ``eventsFlow`` is a ``SharedFlow`` of ``MerchantSDKEvents``. It emits the following events:
   * ``NoAction`` when the payment flow is finished and the user is being redirected to the bank to pay.
   * ``OnLoading`` when the payment details are being validated.
   * ``OnScreenDisplayed(displayedScreen)`` when there is a change in the screens displayed within the payment flow.
   * ``OnFinishedWithPaymentRequestCreated(paymentRequestId, paymentProviderName)`` when the payment request is ready and the user can be redirected to the bank.
   * ``OnFinishedWithCancellation`` when the payment flow was cancelled. Can be either from an internal error, or from exiting the payment flow without finalising it.
   * ``OnErrorOccurred(throwable)`` when there was an error within the payment flow

.. note::

    ``OnFinishedWithPaymentRequestCreated`` and ``OnErrorOccurred`` can be checked for to remove the ``PaymentFragment`` from the hierarchy.
