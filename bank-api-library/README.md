![Gini Bank API Library for Android](./logo.png)

Gini Bank API Library for Android
===================================

A library for communicating with the [Gini Bank API](https://pay-api.gini.net/documentation/). It allows you to easily add
[payment information extraction](https://pay-api.gini.net/documentation/#document-extractions-for-payment) capabilities
to your app. It also enables your app to create or resolve [payment requests](https://pay-api.gini.net/documentation/#payments).

The Gini Bank API provides an information extraction service for analyzing invoices. Specifically it extracts information
such as the document sender or the payment relevant information (amount to pay, IBAN, BIC, payment reference, etc.).
It also provides secure payment information sharing between clients via payment requests.

Documentation
-------------

* [Integration Guide](https://developer.gini.net/gini-mobile-android/bank-api-library/library/html/)
* [Reference Docs](https://developer.gini.net/gini-mobile-android/bank-api-library/library/dokka/index.html)

Dependencies
------------

The Gini Bank API Library has the following dependencies:

* [Retrofit2](https://square.github.io/retrofit/)
* [TrustKit from DataTheorem](https://github.com/datatheorem/TrustKit-Android)

License
-------

The Gini Bank API Library is available under the MIT license. See the LICENSE file for details.
