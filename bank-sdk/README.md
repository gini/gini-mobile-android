![Gini Bank SDK for Android](logo.png)

Gini Bank SDK for Android
=========================

The Gini Bank SDK for Android provides all the UI and functionality needed to use the Gini Bank API in your app to
extract payment information from invoices and to resolve payment requests originating from other apps.

The Gini Bank API provides an information extraction service for analyzing invoices. Specifically it extracts information
such as the document sender or the payment relevant information (amount to pay, IBAN, etc.). In addition, it also
provides a secure channel for sharing payment related information between clients.

Documentation
-------------
* [Integration Guide](https://gini.atlassian.net/wiki/spaces/GBSV/overview)
* [Reference Docs](https://developer.gini.net/gini-mobile-android/bank-sdk/sdk/dokka/index.html)

Examples
--------

## Invoice Capture

We are providing an example app in the `:bank-sdk:example-app` module, which demonstrates how to integrate the Gini Bank SDK's invoice capture feature.

## Payment Requests

You can find an example on how to handle payment requests in the example app's `PayActivity`. 

For testing the payment flow integration you can use the example app from the [Health
SDK](https://github.com/gini/gini-mobile-android/tree/main/health-sdk#example-apps) to create payment requests.
 
## License

Gini Bank SDK is available under a commercial license.
See the LICENSE file for more info.
