Testing
=======

Requirements
------------

Example banking app
~~~~~~~~~~~~~~~~~~~

To pass the requirements for the Gini Health SDK a supported banking app has to be installed on the device running
your app.

An example banking app is available in the `Gini Pay Bank SDK's <https://github.com/gini/gini-pay-bank-sdk-android>`_
repository called ``appscreenapi``.

You can use the same Gini Pay API client credentials in the example banking app as in your app, if not otherwise
specified.

Development Gini Pay API client credentials
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

In order to test using our example banking app you need to use development client credentials. This will make sure
the Gini Health SDK uses a test payment provider which will open our example banking app.

End to end testing
------------------

After you've set the client credentials in the example banking app and installed it on your device you can run your app
and verify that ``GiniBusiness.checkRequirements()`` returns an empty list.

Following the `flow guide <flow.html>`_ show the ``ReviewFragment`` after analyzing a document.

Check that the extractions and the document preview are shown and then press the ``Pay`` button:

.. image:: images/testing/business_review_fragment.png
    :alt: Review Fragment
    :width: 150px
    :align: center

|

You should be redirected to the example banking app where the final extractions are shown:

.. image:: images/testing/bank_payment_details.png
    :alt: Banking App - Payment Details
    :width: 150px
    :align: center

|

After you press the ``Pay`` button the Gini Pay Bank SDK resolves the payment and allows you to return to your app:

.. image:: images/testing/bank_resolved_payment.png
    :alt: Banking App - Resolved Payment
    :width: 150px
    :align: center

|

With these steps completed you have verified that your app, the Gini Pay API, the Gini Health SDK and the Gini Pay
Bank SDK work together correctly.

Testing in production
---------------------

The steps are the same but instead of the development client credentials you will need to use production client
credentials. This will make sure the Gini Health SDK receives real payment providers which open real banking apps.

You will also need to install a banking app which uses the Gini Pay Bank SDK. Please contact us in case you don't know
which banking app(s) to install.

Lastly make sure that for production you register the scheme we provided you for deep linking and you are not using 
`ginipay-business://payment-requester`.
