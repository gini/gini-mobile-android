Gini Pay API Library
====================

A library for communicating with the [Gini Pay API](https://pay-api.gini.net/documentation/). It allows you to easily add
[payment information extraction](https://pay-api.gini.net/documentation/#document-extractions-for-payment) capabilities
to your app. It also enables your app to create or resolve [payment requests](https://pay-api.gini.net/documentation/#payments).

The Gini Pay API provides an information extraction service for analyzing invoices. Specifically it extracts information
such as the document sender or the payment relevant information (amount to pay, IBAN, BIC, payment reference, etc.).
It also provides secure payment information sharing between clients via payment requests.

Documentation
-------------

See the [integration guide](https://developer.gini.net/gini-pay-api-lib-android/) for detailed guidance on how to
integrate the Gini Pay API Library into your app.

Dependencies
------------

The Gini Pay API Library has the following dependencies:

* [Volley from Google](http://developer.android.com/training/volley/index.html) ([AOSP Repository](https://android.googlesource.com/platform/frameworks/volley))
* [Bolts from facebook](https://github.com/BoltsFramework/Bolts-Android)
* [TrustKit from DataTheorem](https://github.com/datatheorem/TrustKit-Android)

License
-------

The Gini Pay API Library is available under the MIT license. See the LICENSE file for details.
