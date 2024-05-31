![Gini Health SDK for Android](./logo.png)

Gini Health SDK for Android
===========================

The Gini Health SDK for Android provides all the UI and functionality needed to use the Gini Health API in your app to
extract payment and health information from invoices. The payment information can be reviewed and then the invoice can
be paid using any available payment provider app (e.g., banking app).

The Gini Health API provides an information extraction service for analyzing health invoices. Specifically, it extracts
information such as the document sender or the payment relevant information (amount to pay, IBAN, etc.). In addition it
also provides a secure channel for sharing payment related information between clients. 

Documentation
-------------

* [Integration Guide](https://developer.gini.net/gini-mobile-android/health-sdk/sdk)

Example Apps
------------

### Health Example App

You can see a sample usage of the Gini Health SDK in the `:health-sdk:example-app` module. 

It requires Gini Health API credentials which are injected automatically if you create this file `health-sdk/example-app/local.properties` with the following properties:
```
clientId=*******
clientSecret=*******
```

### Bank Example App

An example bank app is available in the [Gini Bank SDK](https://github.com/gini/gini-mobile-android/tree/main/bank-sdk) called
[`example-app`](https://github.com/gini/gini-mobile-android/tree/main/bank-sdk/example-app).

License
-------

The Gini Health SDK for Android is available under a commercial license.
See the LICENSE file for more info.
