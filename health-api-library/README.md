![Gini Health API Library for Android](./logo.png)

Gini Health API Library for Android
===================================

A library for communicating with the [Gini Health API](https://health-api.gini.net/documentation/v3/#gini-health-api-documentation-v3-0).
It allows you to easily add payment information extraction ([see "payment" compound extraction](https://health-api.gini.net/documentation/v3/#available-compound-extractions))
capabilities to your app. It also enables your app to create payment requests and retrieve payment providers.

The Gini Health API provides an information extraction service for analyzing invoices. Specifically it extracts information
such as the document sender or the payment relevant information (amount to pay, IBAN, BIC, payment reference, etc.).
It also provides secure payment information sharing between clients via payment requests.

Documentation
-------------

* [Integration Guide](https://developer.gini.net/gini-mobile-android/health-api-library/library)

Dependencies
------------

The Gini Health API Library has the following dependencies:

* [Retrofit2](https://square.github.io/retrofit/)
* [TrustKit from DataTheorem](https://github.com/datatheorem/TrustKit-Android)

License
-------

The Gini Health API Library is available under the MIT license. See the LICENSE file for details.
